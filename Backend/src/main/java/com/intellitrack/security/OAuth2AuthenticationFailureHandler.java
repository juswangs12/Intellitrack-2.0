package com.intellitrack.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String frontendRedirect = "http://localhost:3000";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String detail = exception.getMessage() != null ? exception.getMessage() : "unknown";
        System.err.println("=== OAuth2 Authentication Failure ===");
        System.err.println("  Exception type: " + exception.getClass().getName());
        System.err.println("  Message: " + detail);
        exception.printStackTrace();
        System.err.println("=== END OAuth2 Failure ===");
        String redirect = frontendRedirect + "/?error=oauth_failure&detail="
                + java.net.URLEncoder.encode(detail, java.nio.charset.StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }
}
