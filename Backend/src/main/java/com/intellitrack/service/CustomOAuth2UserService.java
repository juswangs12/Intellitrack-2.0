package com.intellitrack.service;

import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.User;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public CustomOAuth2UserService() {
        System.out.println("=== CustomOAuth2UserService CONSTRUCTOR CALLED ===");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("=== CUSTOMOAUTH2USERSERVICE.START ===");
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract user info from Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");

        System.out.println("Email from Google: [" + email + "]");
        System.out.println("Name from Google: " + name);
        System.out.println("Google ID (sub): " + googleId);

        // First let's list ALL users in the DB for debugging
        System.out.println("=== LISTING ALL DB USERS ===");
        userRepository.findAll().forEach(u -> 
            System.out.println("  DB User: id=" + u.getId() + ", email=[" + u.getEmail() + "], role=" + u.getRole())
        );
        System.out.println("=== END DB USERS ===");

        // Also list ALL StudentEnrollments
        System.out.println("=== LISTING ALL STUDENT ENROLLMENTS ===");
        studentEnrollmentRepository.findAll().forEach(e -> 
            System.out.println("  Enrollment: id=" + e.getId() + ", email=[" + e.getEmail() + "], student=" + (e.getStudent() != null ? e.getStudent().getId() : "null") + ", name=" + e.getFullName())
        );
        System.out.println("=== END ENROLLMENTS ===");

        // Only allow student role for Google OAuth
        Optional<User> existingUser = userRepository.findByEmail(email);
        System.out.println("existingUser.isPresent()? " + existingUser.isPresent());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
<<<<<<< HEAD
            System.out.println("✅ Found existing user: id=" + user.getId() + ", email=[" + user.getEmail() + "]");
            // Update Google ID if not set (store separately)
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                userRepository.save(user);
                System.out.println("Updated user's googleId");
            }
        } else {
            System.out.println("⚠️ NO existing user found for email=[" + email + "], CREATING NEW USER!");
            // Create new student user
=======
            System.out.println("Found existing user: " + user.getEmail() + " (ID: " + user.getId() + ")");
            // Store the Google sub ID in its dedicated column (not in studentId)
            if (user.getGoogleSub() == null) {
                user.setGoogleSub(googleId);
                userRepository.save(user);
                System.out.println("Stored Google sub in googleSub column");
            }
        } else {
            // Create new student user — studentId starts as null; student fills it in later
>>>>>>> c319f7ab1202d419c45c6aa3cad6804e5c23a247
            user = new User();
            user.setEmail(email);
            user.setFirstName(name.split(" ")[0]);
            user.setLastName(name.split(" ").length > 1 ? name.split(" ")[1] : "");
            user.setRole("student");
<<<<<<< HEAD
            user.setGoogleId(googleId); // Store Google provider ID separately
            user.setStudentId(null); // Initialize to null, will set from enrollment if matched
=======
            user.setGoogleSub(googleId); // store sub in dedicated column, NOT in studentId
>>>>>>> c319f7ab1202d419c45c6aa3cad6804e5c23a247
            user.setCreatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            System.out.println("✅ Created NEW user: id=" + user.getId() + ", email=[" + user.getEmail() + "]");
        }

        // Auto-link to StudentEnrollment (EVERY TIME you log in!)
        System.out.println("Starting auto-link process for user: " + user.getEmail());
        autoLinkEnrollments(user, email, googleId, name);
        System.out.println("Auto-link process complete");

<<<<<<< HEAD
        // EXPLICITLY save and flush the user to ensure all changes are committed to DB
        user = userRepository.saveAndFlush(user);
        System.out.println("✅ User explicitly saved and flushed to DB: id=" + user.getId());

        // If user role is not student, log a warning but allow OAuth login (development)
=======
        // If user role is not student, log a warning but allow OAuth login
        // (development)
>>>>>>> c319f7ab1202d419c45c6aa3cad6804e5c23a247
        if (!"student".equals(user.getRole())) {
            // In production, consider rejecting with OAuth2AuthenticationException
            System.out.println(
                    "Warning: OAuth login for non-student role: " + user.getEmail() + " role=" + user.getRole());
        }

        System.out.println("=== CustomOAuth2UserService returning OAuth2User ===");
        System.out.println("  User id: " + user.getId());
        System.out.println("  User email: " + user.getEmail());
        System.out.println("  User group: " + (user.getGroup() != null ? user.getGroup().getId() + " (" + user.getGroup().getTitle() + ")" : "null"));
        System.out.println("  User studentId: " + user.getStudentId());
        System.out.println("  User googleId: " + user.getGoogleId());

        return oauth2User;
    }

    private void autoLinkEnrollments(User user, String email, String googleId, String name) {
        System.out.println("  Looking for StudentEnrollments...");

        // First, try to find by email (case-insensitive)
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByEmailIgnoreCase(email);
        System.out.println("  Found " + enrollments.size() + " enrollments by email (case-insensitive): " + email);

        // If no matches by email, try by studentId (if available in enrollment)
        if (enrollments.isEmpty()) {
            System.out.println("  No enrollments found by email, trying by studentId (Google ID): " + googleId);
            enrollments = studentEnrollmentRepository.findByStudentId(googleId);
            System.out.println("  Found " + enrollments.size() + " enrollments by studentId");
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
            System.out.println(
                    "  Processing enrollment: " + enrollment.getFullName() + " (ID: " + enrollment.getId() + ")");
            System.out.println("    Enrollment email: " + enrollment.getEmail());
            System.out.println("    Enrollment studentId: " + enrollment.getStudentId());
            System.out.println("    Enrollment already has user linked? " + (enrollment.getStudent() != null));

            if (enrollment.getStudent() == null) {
                enrollment.setStudent(user);
                studentEnrollmentRepository.save(enrollment);
<<<<<<< HEAD
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
=======
                System.out.println("    ✅ SUCCESS: Auto-linked enrollment: " + enrollment.getFullName() + " to user: "
                        + user.getEmail());
>>>>>>> c319f7ab1202d419c45c6aa3cad6804e5c23a247
            } else {
                System.out.println("    ⚠️  SKIPPED: Enrollment already linked to user with ID: "
                        + enrollment.getStudent().getId());
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
}
