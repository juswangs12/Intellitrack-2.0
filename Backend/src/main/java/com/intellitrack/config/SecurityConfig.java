package com.intellitrack.config;

import com.intellitrack.security.JwtAuthenticationFilter;
import com.intellitrack.service.CustomOAuth2UserService;
import com.intellitrack.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Autowired
    private com.intellitrack.security.OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                // JWT is stateless — no server-side session needed
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/login/**", "/oauth2/**", "/h2-console/**").permitAll()
                        .requestMatchers(
                                "/api/users/**",
                                "/api/dashboard/**",
                                "/api/analytics/**",
                                "/api/deadlines/**",
                                "/api/status-monitoring/**",
                                "/api/submission-summary/**")
                        .authenticated()
                        .anyRequest().permitAll())
                // Validate JWT before Spring Security's built-in auth filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler))
                .headers(headers -> headers
                        // Restrict X-Frame-Options to H2 console path only, block framing everywhere
                        // else
                        .frameOptions(frame -> frame.sameOrigin())
                        // Enable XSS protection header
                        .xssProtection(
                                xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // Prevent MIME-type sniffing
                        .contentTypeOptions(ct -> {
                        }));

        return http.build();
    }
}