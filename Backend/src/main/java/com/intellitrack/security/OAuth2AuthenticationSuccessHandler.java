package com.intellitrack.security;

import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.User;
import com.intellitrack.dto.LoginResponse;
import com.intellitrack.dto.UserDTO;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final String frontendRedirect = "http://localhost:3000";
    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationSuccessHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            System.out.println("=== OAuth2AuthenticationSuccessHandler.onAuthenticationSuccess ===");
            
            if (authentication == null) {
                System.err.println("OAuth2: Authentication object is null");
                redirectWithError(response, "oauth_failure", "authentication_null");
                return;
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof OAuth2User)) {
                String cls = principal != null ? principal.getClass().getName() : "null";
                System.err.println("OAuth2: Principal is not OAuth2User, got: " + cls);
                redirectWithError(response, "oauth_invalid_user", "principal_type=" + cls);
                return;
            }

            OAuth2User oauthUser = (OAuth2User) principal;

            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");
            String googleId = oauthUser.getAttribute("sub");

            System.out.println("=== OAuth2AuthenticationSuccessHandler ===");
            System.out.println("OAuth2 attributes - email: " + email + ", name: " + name + ", googleId: " + googleId);
            System.out.println("All OAuth attributes keys: " + String.join(", ", oauthUser.getAttributes().keySet()));

            if (email == null || email.isEmpty()) {
                String attrs = String.join(",", oauthUser.getAttributes().keySet());
                System.err.println("OAuth2: Email attribute missing from OAuth2User. Available attributes: " + attrs);
                redirectWithError(response, "oauth_handler_error", "missing_email;attrs=" + attrs);
                return;
            }

            System.out.println("Now loading user from DB by email: " + email);
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                System.out.println("⚠️ User NOT found by email=" + email + " → CREATING NEW USER NOW!");
                User newUser = new User();
                newUser.setEmail(email);
                if (name != null && !name.isEmpty()) {
                    String[] parts = name.split(" ", 2);
                    newUser.setFirstName(parts[0]);
                    newUser.setLastName(parts.length > 1 ? parts[1] : "");
                } else {
                    newUser.setFirstName("Student");
                    newUser.setLastName("");
                }
                newUser.setRole("student");
                newUser.setGoogleId(googleId);
                newUser.setStudentId(null);
                newUser.setPassword(""); // OAuth users don't have passwords
                newUser.setCreatedAt(java.time.LocalDateTime.now());
                User saved = userRepository.save(newUser);
                System.out.println("✅ NEW USER CREATED: id=" + saved.getId() + ", email=" + saved.getEmail());
                return saved;
            });

            System.out.println("✅ SUCCESS: User loaded from DB: id=" + user.getId() + ", email=" + user.getEmail() + 
                             ", role=" + user.getRole() + ", studentId=" + user.getStudentId() + 
                             ", googleId=" + user.getGoogleId());
            System.out.println("User has group (before auto-link): " + (user.getGroup() != null ? user.getGroup().getId() : "null"));

            // AUTO-LINK TO STUDENT ENROLLMENTS!
            System.out.println("=== Starting auto-link process in OAuth2AuthenticationSuccessHandler ===");
            autoLinkEnrollments(user, email, googleId, name);
            System.out.println("=== Auto-link process complete ===");

            // Re-load user to get fresh state with group
            user = userRepository.findById(user.getId()).orElse(user);
            System.out.println("User has group (after auto-link): " + (user.getGroup() != null ? user.getGroup().getId() + " (" + user.getGroup().getTitle() + ")" : "null"));

            String token = jwtTokenProvider.generateToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
            UserDTO userDTO = new UserDTO(user);
            String role = user.getRole();

            System.out.println("Generated tokens for user: token=" + (token != null ? "yes" : "no") + 
                             ", refreshToken=" + (refreshToken != null ? "yes" : "no"));
            System.out.println("UserDTO has groupId: " + userDTO.getGroupId());

            // Create compact redirect URL with parameters
            String userJson = objectMapper.writeValueAsString(userDTO);
            String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String encodedRefresh = URLEncoder.encode(refreshToken != null ? refreshToken : "", StandardCharsets.UTF_8);

            String redirectUrl = String.format("%s/?token=%s&refreshToken=%s&user=%s&role=%s", 
                    frontendRedirect, encodedToken, encodedRefresh, encodedUser, role);

            System.out.println("OAuth2 login successful for user: " + email + " with role: " + role);
            System.out.println("Redirecting to: " + redirectUrl);
            System.out.println("=== OAuth2AuthenticationSuccessHandler complete ===");
            response.sendRedirect(redirectUrl);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "unknown_error";
            System.err.println("=== OAuth2AuthenticationSuccessHandler ERROR ===");
            System.err.println("  Error type: " + ex.getClass().getName());
            System.err.println("  Error message: " + msg);
            ex.printStackTrace();
            System.err.println("=== END ERROR ===");
            redirectWithError(response, "oauth_handler_error", msg);
        }
    }

    private void autoLinkEnrollments(User user, String email, String googleId, String name) {
        System.out.println("  Looking for StudentEnrollments...");
        
        // First, try to find by email (case-insensitive) WITH GROUPS EAGERLY FETCHED
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByEmailIgnoreCaseWithGroups(email);
        System.out.println("  Found " + enrollments.size() + " enrollments by email (case-insensitive with groups): " + email);
        
        // If no matches by email, try by studentId (if available in enrollment) WITH GROUPS EAGERLY FETCHED
        if (enrollments.isEmpty()) {
            System.out.println("  No enrollments found by email, trying by studentId (Google ID with groups): " + googleId);
            enrollments = studentEnrollmentRepository.findByStudentIdWithGroups(googleId);
            System.out.println("  Found " + enrollments.size() + " enrollments by studentId (with groups)");
        }

        // If still no matches, try intelligent name matching
        if (enrollments.isEmpty() && name != null && !name.isBlank()) {
            System.out.println("  No enrollments found by email or studentId, trying intelligent name matching for: " + name);
            List<StudentEnrollment> allUnlinkedEnrollments = studentEnrollmentRepository.findAll().stream()
                    .filter(e -> e.getStudent() == null)
                    .toList();
            List<StudentEnrollment> matchedEnrollments = new ArrayList<>();
            for (StudentEnrollment enrollment : allUnlinkedEnrollments) {
                if (isLikelyMatch(name, enrollment.getFullName())) {
                    System.out.println("  ✅ Found likely match: OAuth name=" + name + ", Enrollment name=" + enrollment.getFullName());
                    matchedEnrollments.add(enrollment);
                }
            }
            enrollments = matchedEnrollments;
            System.out.println("  Found " + enrollments.size() + " enrollments by name matching");
        }

        // Link all matching enrollments that are not already linked
        for (StudentEnrollment enrollment : enrollments) {
            System.out.println("  Processing enrollment: " + enrollment.getFullName() + " (ID: " + enrollment.getId() + ")");
            System.out.println("    Enrollment email: " + enrollment.getEmail());
            System.out.println("    Enrollment studentId: " + enrollment.getStudentId());
            System.out.println("    Enrollment already has user linked? " + (enrollment.getStudent() != null));
            
            if (enrollment.getStudent() == null) {
                enrollment.setStudent(user);
                studentEnrollmentRepository.save(enrollment);
                System.out.println("    ✅ SUCCESS: Auto-linked enrollment: " + enrollment.getFullName() + " to user: " + user.getEmail());
                
                // Set user.studentId from the enrollment (real institutional student ID)
                if (enrollment.getStudentId() != null && !enrollment.getStudentId().isBlank()) {
                    user.setStudentId(enrollment.getStudentId());
                    System.out.println("    ✅ Set user.studentId from enrollment: " + enrollment.getStudentId());
                }
                
                // Backward compatibility: also set user.group for frontend
                if (enrollment.getGroups() != null && !enrollment.getGroups().isEmpty()) {
                    user.setGroup(enrollment.getGroups().get(0));
                    userRepository.save(user);
                    System.out.println("    ✅ Also set user.group (for frontend compatibility): " + enrollment.getGroups().get(0).getId());
                }
            } else {
                System.out.println("    ⚠️  SKIPPED: Enrollment already linked to user with ID: " + enrollment.getStudent().getId());
            }
        }
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        // Lowercase, remove punctuation, trim spaces
        String normalized = name.toLowerCase().trim();
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }

    private double calculateNameSimilarity(String name1, String name2) {
        String n1 = normalizeName(name1);
        String n2 = normalizeName(name2);
        
        if (n1.equals(n2)) return 1.0;
        
        // Token-based matching
        String[] tokens1 = n1.split(" ");
        String[] tokens2 = n2.split(" ");
        
        int matches = 0;
        for (String token1 : tokens1) {
            for (String token2 : tokens2) {
                if (token1.equals(token2)) {
                    matches++;
                    break;
                }
            }
        }
        
        // Calculate similarity score
        int maxTokens = Math.max(tokens1.length, tokens2.length);
        double tokenSimilarity = (double) matches / maxTokens;
        
        // Also check if one is a subset of the other
        if (n1.contains(n2) || n2.contains(n1)) {
            tokenSimilarity = Math.max(tokenSimilarity, 0.8);
        }
        
        return tokenSimilarity;
    }

    private boolean isLikelyMatch(String oauthName, String enrollmentName) {
        if (oauthName == null || enrollmentName == null) return false;
        double similarity = calculateNameSimilarity(oauthName, enrollmentName);
        System.out.println("    Name similarity: OAuth='" + oauthName + "', Enrollment='" + enrollmentName + "', Score=" + similarity);
        return similarity >= 0.6;
    }

    private void redirectWithError(HttpServletResponse response, String errorCode, String details) throws IOException {
        String encoded = URLEncoder.encode(details != null ? details : "", StandardCharsets.UTF_8);
        String redirectUrl = String.format("%s/?error=%s&errorMsg=%s", frontendRedirect, errorCode, encoded);
        response.sendRedirect(redirectUrl);
    }
}

