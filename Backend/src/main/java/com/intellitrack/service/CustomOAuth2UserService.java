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

        // Only allow student role for Google OAuth
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update Google ID if not set
            if (user.getStudentId() == null) {
                user.setStudentId(googleId);
                userRepository.save(user);
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
        }

        // Auto-link to StudentEnrollment
        autoLinkEnrollments(user, email, googleId);

        // If user role is not student, log a warning but allow OAuth login (development)
        if (!"student".equals(user.getRole())) {
            // In production, consider rejecting with OAuth2AuthenticationException
            System.out.println("Warning: OAuth login for non-student role: " + user.getEmail() + " role=" + user.getRole());
        }

        return oauth2User;
    }

    private void autoLinkEnrollments(User user, String email, String googleId) {
        // First, try to find by email
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByEmail(email);
        
        // If no matches by email, try by studentId (if available in enrollment)
        if (enrollments.isEmpty()) {
            enrollments = studentEnrollmentRepository.findByStudentId(googleId);
        }

        // Link all matching enrollments that are not already linked
        for (StudentEnrollment enrollment : enrollments) {
            if (enrollment.getStudent() == null) {
                enrollment.setStudent(user);
                studentEnrollmentRepository.save(enrollment);
                System.out.println("Auto-linked enrollment: " + enrollment.getFullName() + " to user: " + user.getEmail());
            }
        }
    }
}