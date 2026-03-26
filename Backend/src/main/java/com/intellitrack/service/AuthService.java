package com.intellitrack.service;

import com.intellitrack.dto.LoginRequest;
import com.intellitrack.dto.LoginResponse;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse authenticate(LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // For demo purposes, we'll use plain text password comparison
            // In production, use passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())
            if (loginRequest.getPassword().equals(user.getPassword())) {
                // Generate a simple token (in production, use JWT)
                String token = "demo-token-" + user.getId() + "-" + System.currentTimeMillis();

                return new LoginResponse(token, user);
            }
        }

        throw new RuntimeException("Invalid credentials");
    }

    public User register(User user) {
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }
}