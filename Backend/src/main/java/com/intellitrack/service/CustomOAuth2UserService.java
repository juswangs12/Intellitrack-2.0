package com.intellitrack.service;

import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.User;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email    = oauth2User.getAttribute("email");
        String name     = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");
        String picture  = oauth2User.getAttribute("picture");

        logger.debug("OAuth2 login - email: {}", email);

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            logger.info("OAuth2: existing user found - id={}, role={}", user.getId(), user.getRole());
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            if (user.getGoogleSub() == null) {
                user.setGoogleSub(googleId);
            }
            if (user.getAvatarFilename() == null && picture != null && !picture.isBlank()) {
                user.setAvatarFilename(picture);
            }
            userRepository.save(user);
        } else {
            logger.info("OAuth2: no user found for email, creating new student account");
            user = new User();
            user.setEmail(email);
            if (name != null && !name.isBlank()) {
                String[] parts = name.split(" ", 2);
                user.setFirstName(parts[0]);
                user.setLastName(parts.length > 1 ? parts[1] : "");
            } else {
                user.setFirstName("Student");
                user.setLastName("");
            }
            user.setRole("student");
            user.setGoogleId(googleId);
            user.setGoogleSub(googleId);
            user.setStudentId(null);
            user.setPassword("");
            user.setCreatedAt(LocalDateTime.now());
            if (picture != null && !picture.isBlank()) {
                user.setAvatarFilename(picture);
            }
            user = userRepository.save(user);
            logger.info("OAuth2: new user created - id={}", user.getId());
        }

        autoLinkEnrollments(user, email, googleId, name);

        user = userRepository.saveAndFlush(user);

        if (!"student".equals(user.getRole())) {
            logger.warn("OAuth2 login for non-student role: {} (role={})", user.getEmail(), user.getRole());
        }

        return oauth2User;
    }

    private void autoLinkEnrollments(User user, String email, String googleId, String name) {
        // Try by email (case-insensitive)
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByEmailIgnoreCase(email);
        logger.debug("Auto-link: found {} enrollment(s) by email", enrollments.size());

        // Try by studentId (Google sub)
        if (enrollments.isEmpty()) {
            enrollments = studentEnrollmentRepository.findByStudentId(googleId);
            logger.debug("Auto-link: found {} enrollment(s) by studentId", enrollments.size());
        }

        // Try intelligent name matching
        if (enrollments.isEmpty() && name != null && !name.isBlank()) {
            List<StudentEnrollment> allUnlinked = studentEnrollmentRepository.findAll().stream()
                    .filter(e -> e.getStudent() == null)
                    .toList();
            List<StudentEnrollment> matched = new ArrayList<>();
            for (StudentEnrollment e : allUnlinked) {
                if (isLikelyMatch(name, e.getFullName())) {
                    matched.add(e);
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
                    user.setGroup(enrollment.getGroups().get(0));
                    userRepository.save(user);
                }
            } else {
                logger.debug("Auto-link: enrollment {} already linked to user {}", enrollment.getId(),
                        enrollment.getStudent().getId());
            }
        }
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
