package com.intellitrack.service;

import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
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
            userRepository.save(user);
        }

        // Verify user is a student
        if (!"student".equals(user.getRole())) {
            throw new OAuth2AuthenticationException("Only students can login with Google OAuth");
        }

        return oauth2User;
    }
}