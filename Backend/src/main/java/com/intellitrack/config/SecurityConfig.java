package com.intellitrack.config;

import com.intellitrack.security.JwtAuthenticationFilter;
import com.intellitrack.service.CustomOAuth2UserService;
import com.intellitrack.security.OAuth2AuthenticationSuccessHandler;
import com.intellitrack.security.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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

        @Autowired
        private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

        @Bean
        public OidcIdTokenDecoderFactory oidcIdTokenDecoderFactory() {
                OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
                factory.setJwsAlgorithmResolver(clientRegistration -> SignatureAlgorithm.RS256);
                factory.setJwtValidatorFactory(clientRegistration -> new DelegatingOAuth2TokenValidator<>(
                                new JwtTimestampValidator(Duration.ofMinutes(5)),
                                new OidcIdTokenValidator(clientRegistration)));
                return factory;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(customAuthenticationEntryPoint))
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/auth/login",
                                                                "/api/auth/refresh-token",
                                                                "/api/auth/forgot-password", "/api/auth/reset-password",
                                                                "/login/**", "/oauth2/**")
                                                .permitAll()
                                                .requestMatchers("/api/google/callback").permitAll()
                                                .requestMatchers(HttpMethod.POST,"/api/automation/deadlines/run").permitAll()
                                                .requestMatchers("/actuator/health/readiness").permitAll()
                                                .requestMatchers("/api/dashboard/**", "/api/student/**", "/api/adviser/**", "/api/deliverables/**", "/api/deadlines/**", "/api/analytics/**", "/api/export/**", "/api/submission-summary/**", "/api/status-monitoring/**", "/api/submissions/**").denyAll()
                                                .requestMatchers("/api/student/workspace/generate-ai-insight", "/api/student/feedback/**").denyAll()
                                                .requestMatchers("/api/tracking/**", "/api/google/**").hasAnyRole("student", "adviser", "coordinator", "administrator")
                                                .requestMatchers("/api/comments/**", "/api/feedback/**", "/api/submissions/upload", "/api/submissions/google-doc", "/api/ai/**").denyAll()
                                                .requestMatchers("/api/auth/register", "/api/system-config/**", "/api/admin/**").hasRole("administrator")
                                                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("administrator")
                                                .requestMatchers(HttpMethod.PUT, "/api/users/*/account").hasRole("administrator")
                                                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("administrator")
                                                .requestMatchers("/api/users/**", "/api/notifications/**").authenticated()
                                                .requestMatchers("/api/student/**").hasRole("student")
                                                .requestMatchers("/api/adviser/**", "/api/dashboard/**").hasAnyRole("coordinator", "administrator")
                                                .requestMatchers("/api/academic/**", "/api/subjects/**", "/api/student-enrollments/**", "/api/classlist/**", "/api/deliverables/**", "/api/groups/**", "/api/deadlines/**", "/api/analytics/**", "/api/audit/**", "/api/export/**", "/api/submission-summary/**", "/api/status-monitoring/**", "/api/submissions/**").hasAnyRole("coordinator", "administrator")
                                                .anyRequest().denyAll())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oauth2AuthenticationSuccessHandler)
                                                .failureHandler(oauth2AuthenticationFailureHandler))
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.sameOrigin())
                                                .xssProtection(
                                                                xss -> xss.headerValue(
                                                                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                .contentTypeOptions(ct -> {
                                                }));

                return http.build();
        }
}
