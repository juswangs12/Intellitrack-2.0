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
import java.util.List;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract user info from Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");

        System.out.println("=== Google OAuth Login ===");
        System.out.println("Email from Google: " + email);
        System.out.println("Name from Google: " + name);
        System.out.println("Google ID (sub): " + googleId);

        // Only allow student role for Google OAuth
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            System.out.println("Found existing user: " + user.getEmail() + " (ID: " + user.getId() + ")");
            // Update Google ID if not set
            if (user.getStudentId() == null) {
                user.setStudentId(googleId);
                userRepository.save(user);
                System.out.println("Updated user's studentId to Google ID");
            }
        } else {
            // Create new student user
            user = new User();
            user.setEmail(email);
            user.setFirstName(name.split(" ")[0]);
            user.setLastName(name.split(" ").length > 1 ? name.split(" ")[1] : "");
            user.setRole("student");
            user.setStudentId(googleId);
            user.setCreatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            System.out.println("Created new user: " + user.getEmail() + " (ID: " + user.getId() + ")");
        }

        // Auto-link to StudentEnrollment (EVERY TIME you log in!)
        System.out.println("Starting auto-link process for user: " + user.getEmail());
        autoLinkEnrollments(user, email, googleId);
        System.out.println("Auto-link process complete");

        // If user role is not student, log a warning but allow OAuth login (development)
        if (!"student".equals(user.getRole())) {
            // In production, consider rejecting with OAuth2AuthenticationException
            System.out.println("Warning: OAuth login for non-student role: " + user.getEmail() + " role=" + user.getRole());
        }

        return oauth2User;
    }

    private void autoLinkEnrollments(User user, String email, String googleId) {
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
            } else {
                System.out.println("    ⚠️  SKIPPED: Enrollment already linked to user with ID: " + enrollment.getStudent().getId());
            }
        }
    }
}