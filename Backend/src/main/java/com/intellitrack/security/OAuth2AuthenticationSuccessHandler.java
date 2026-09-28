package com.intellitrack.security;

import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.User;
import com.intellitrack.dto.UserDTO;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendRedirect;

    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationSuccessHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            if (authentication == null) {
                logger.error("OAuth2: Authentication object is null");
                redirectWithError(response, "oauth_failure", "authentication_null");
                return;
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof OAuth2User oauthUser)) {
                String cls = principal != null ? principal.getClass().getName() : "null";
                logger.error("OAuth2: Principal is not OAuth2User, got: {}", cls);
                redirectWithError(response, "oauth_invalid_user", "principal_type=" + cls);
                return;
            }

            String email    = oauthUser.getAttribute("email");
            String name     = oauthUser.getAttribute("name");
            String googleId = oauthUser.getAttribute("sub");
            String picture  = oauthUser.getAttribute("picture");

            if (email == null || email.isEmpty()) {
                String attrs = String.join(",", oauthUser.getAttributes().keySet());
                logger.error("OAuth2: Email attribute missing. Available attributes: {}", attrs);
                redirectWithError(response, "oauth_handler_error", "missing_email");
                return;
            }

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                logger.info("OAuth2: user not found for {}, creating new student", email);
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
                newUser.setGoogleSub(googleId);
                newUser.setStudentId(null);
                newUser.setPassword("");
                if (picture != null && !picture.isBlank()) {
                    newUser.setAvatarFilename(picture);
                }
                newUser.setCreatedAt(java.time.LocalDateTime.now());
                return userRepository.save(newUser);
            });

            autoLinkEnrollments(user, email, googleId, name);

            @SuppressWarnings("null")
            User reloaded = userRepository.findById(user.getId()).orElse(user);
            user = reloaded;

            String token        = jwtTokenProvider.generateToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
            UserDTO userDTO     = new UserDTO(user);
            String role         = user.getRole();

            String userJson      = objectMapper.writeValueAsString(userDTO);
            String encodedUser   = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
            String encodedToken  = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String encodedRefresh = URLEncoder.encode(refreshToken != null ? refreshToken : "", StandardCharsets.UTF_8);

            String redirectUrl = String.format("%s/?token=%s&refreshToken=%s&user=%s&role=%s",
                    frontendRedirect, encodedToken, encodedRefresh, encodedUser, role);

            logger.info("OAuth2 login successful for user: {} (role: {})", email, role);
            response.sendRedirect(redirectUrl);

        } catch (Exception ex) {
            logger.error("OAuth2 success handler error: {}", ex.getMessage(), ex);
            redirectWithError(response, "oauth_handler_error",
                    ex.getMessage() != null ? ex.getMessage() : "unknown_error");
        }
    }

    private void autoLinkEnrollments(User user, String email, String googleId, String name) {
        List<StudentEnrollment> enrollments =
                studentEnrollmentRepository.findByEmailIgnoreCaseWithGroups(email);
        logger.debug("Auto-link: found {} enrollment(s) by email (with groups)", enrollments.size());

        if (enrollments.isEmpty()) {
            enrollments = studentEnrollmentRepository.findByStudentIdWithGroups(googleId);
            logger.debug("Auto-link: found {} enrollment(s) by studentId (with groups)", enrollments.size());
        }

        if (enrollments.isEmpty() && name != null && !name.isBlank()) {
            List<StudentEnrollment> allUnlinked = studentEnrollmentRepository.findAll().stream()
                    .filter(e -> e.getStudent() == null)
                    .toList();
            List<StudentEnrollment> matched = new ArrayList<>();
            for (StudentEnrollment enrollment : allUnlinked) {
                if (isLikelyMatch(name, enrollment.getFullName())) {
                    matched.add(enrollment);
                }
            }
            enrollments = matched;
            logger.debug("Auto-link: found {} enrollment(s) by name matching", enrollments.size());
        }

        for (StudentEnrollment enrollment : enrollments) {
            if (enrollment.getStudent() == null) {
                enrollment.setStudent(user);
                studentEnrollmentRepository.save(enrollment);
                logger.info("Auto-link: linked enrollment {} to user {}", enrollment.getId(), user.getId());

                if (enrollment.getStudentId() != null && !enrollment.getStudentId().isBlank()) {
                    user.setStudentId(enrollment.getStudentId());
                }

                if (enrollment.getGroups() != null && !enrollment.getGroups().isEmpty()) {
                    ProjectGroup group = enrollment.getGroups().get(0);
                    user.setGroup(group);
                    userRepository.save(user);
                }
            } else {
                logger.debug("Auto-link: enrollment {} already linked to user {}",
                        enrollment.getId(), enrollment.getStudent().getId());
            }
        }
    }

    private void redirectWithError(HttpServletResponse response, String errorCode, String details)
            throws IOException {
        String encoded = URLEncoder.encode(details != null ? details : "", StandardCharsets.UTF_8);
        String redirectUrl = String.format("%s/?error=%s&errorMsg=%s", frontendRedirect, errorCode, encoded);
        response.sendRedirect(redirectUrl);
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        String normalized = name.toLowerCase().trim();
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }

    private double calculateNameSimilarity(String name1, String name2) {
        String n1 = normalizeName(name1);
        String n2 = normalizeName(name2);

        if (n1.equals(n2)) return 1.0;

        String[] tokens1 = n1.split(" ");
        String[] tokens2 = n2.split(" ");

        int matches = 0;
        for (String t1 : tokens1) {
            for (String t2 : tokens2) {
                if (t1.equals(t2)) { matches++; break; }
            }
        }

        int maxTokens = Math.max(tokens1.length, tokens2.length);
        double tokenSimilarity = (double) matches / maxTokens;

        if (n1.contains(n2) || n2.contains(n1)) {
            tokenSimilarity = Math.max(tokenSimilarity, 0.8);
        }

        return tokenSimilarity;
    }

    private boolean isLikelyMatch(String oauthName, String enrollmentName) {
        if (oauthName == null || enrollmentName == null) return false;
        return calculateNameSimilarity(oauthName, enrollmentName) >= 0.6;
    }
}
