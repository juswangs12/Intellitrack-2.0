package com.intellitrack.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the Authorization: Bearer <token> header on every request,
 * validates the JWT, and sets the authenticated principal in the
 * SecurityContext so Spring Security's access rules can evaluate it.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("=== JwtAuthenticationFilter for " + request.getMethod() + " " + request.getRequestURI() + " ===");

        String token = extractBearerToken(request);
        System.out.println("  Token extracted: " + (token != null ? "yes (length: " + token.length() + ")" : "no"));

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);

            System.out.println("  Token valid! userId=" + userId + ", role=" + role + ", email=" + email);

            List<SimpleGrantedAuthority> authorities = (role != null)
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();
            System.out.println("  Authorities: " + authorities);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null,
                    authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("  Authentication set in SecurityContext");
        } else if (StringUtils.hasText(token)) {
            System.err.println("  Invalid token");
        } else {
            System.out.println("  No token found");
        }

        System.out.println("=== JwtAuthenticationFilter complete ===");
        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
