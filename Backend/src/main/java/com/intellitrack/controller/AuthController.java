package com.intellitrack.controller;

import com.intellitrack.dto.LoginRequest;
import com.intellitrack.dto.LoginResponse;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Staff login with email and password
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().build();
            }

            LoginResponse response = authService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Google OAuth2 success callback
     */
    @GetMapping("/oauth2/success")
    public ResponseEntity<LoginResponse> oauth2Success(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            if (oauth2User != null) {
                String email = oauth2User.getAttribute("email");
                String name = oauth2User.getAttribute("name");
                String googleId = oauth2User.getAttribute("sub");

                User user = new User();
                user.setEmail(email);
                if (name != null && !name.isEmpty()) {
                    String[] nameParts = name.split(" ", 2);
                    user.setFirstName(nameParts[0]);
                    user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                } else {
                    user.setFirstName("User");
                    user.setLastName("");
                }
                user.setStudentId(googleId);
                user.setPassword(""); // OAuth users don't have passwords

                LoginResponse response = authService.handleOAuth2User(user);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Register a new staff user
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody User user, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().build();
            }

            LoginResponse response = authService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        try {
            LoginResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Initiate password reset — sends a reset link to the provided email address.
     * Always returns 200 OK to avoid leaking whether the email is registered.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email != null && !email.isBlank()) {
            authService.forgotPassword(email.trim());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Complete password reset using the token that was emailed.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody java.util.Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            authService.resetPassword(token.trim(), newPassword);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}