package com.intellitrack.controller;

import com.intellitrack.dto.LoginRequest;
import com.intellitrack.dto.LoginResponse;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<LoginResponse> oauth2Success(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            if (oauth2User != null) {
                String email = oauth2User.getAttribute("email");
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    String token = "oauth2-token-" + user.getId() + "-" + System.currentTimeMillis();
                    LoginResponse response = new LoginResponse(token, user);
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}