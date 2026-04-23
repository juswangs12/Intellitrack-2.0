package com.intellitrack.security;

import com.intellitrack.entity.User;
import com.intellitrack.dto.LoginResponse;
import com.intellitrack.dto.UserDTO;
import com.intellitrack.service.AuthService;
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

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AuthService authService;

    private final String frontendRedirect = "http://localhost:3000";
    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationSuccessHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
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

            System.out.println("OAuth2 attributes - email: " + email + ", name: " + name + ", googleId: " + googleId);

            if (email == null || email.isEmpty()) {
                String attrs = String.join(",", oauthUser.getAttributes().keySet());
                System.err.println("OAuth2: Email attribute missing from OAuth2User. Available attributes: " + attrs);
                redirectWithError(response, "oauth_handler_error", "missing_email;attrs=" + attrs);
                return;
            }

            User user = new User();
            user.setEmail(email);
            if (name != null && !name.isEmpty()) {
                String[] parts = name.split(" ", 2);
                user.setFirstName(parts[0]);
                user.setLastName(parts.length > 1 ? parts[1] : "");
            } else {
                user.setFirstName("User");
                user.setLastName("");
            }
            user.setStudentId(googleId);
            user.setPassword(""); // OAuth users don't have passwords

            LoginResponse loginResponse = authService.handleOAuth2User(user);

            String token = loginResponse.getToken();
            String refreshToken = loginResponse.getRefreshToken();
            UserDTO userDTO = loginResponse.getUser();
            String role = loginResponse.getRole();

            // Create compact redirect URL with parameters
            String userJson = objectMapper.writeValueAsString(userDTO);
            String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String encodedRefresh = URLEncoder.encode(refreshToken != null ? refreshToken : "", StandardCharsets.UTF_8);

            String redirectUrl = String.format("%s/?token=%s&refreshToken=%s&user=%s&role=%s", 
                    frontendRedirect, encodedToken, encodedRefresh, encodedUser, role);

            System.out.println("OAuth2 login successful for user: " + email + " with role: " + role);
            response.sendRedirect(redirectUrl);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "unknown_error";
            System.err.println("OAuth2AuthenticationSuccessHandler error: " + msg);
            ex.printStackTrace();
            redirectWithError(response, "oauth_handler_error", msg);
        }
    }

    private void redirectWithError(HttpServletResponse response, String errorCode, String details) throws IOException {
        String encoded = URLEncoder.encode(details != null ? details : "", StandardCharsets.UTF_8);
        String redirectUrl = String.format("%s/?error=%s&errorMsg=%s", frontendRedirect, errorCode, encoded);
        response.sendRedirect(redirectUrl);
    }
}

