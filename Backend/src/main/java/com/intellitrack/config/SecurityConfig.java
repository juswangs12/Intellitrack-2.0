package com.intellitrack.config;

import com.intellitrack.security.JwtAuthenticationFilter;
import com.intellitrack.service.CustomOAuth2UserService;
import com.intellitrack.security.OAuth2AuthenticationSuccessHandler;
import com.intellitrack.security.CustomAuthenticationEntryPoint;
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

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/login/**", "/oauth2/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/student/**").hasRole("student")
                        .requestMatchers(HttpMethod.GET, "/api/deliverables/active").permitAll()
                        .requestMatchers("/api/notifications/**").hasAnyRole("adviser", "coordinator", "administrator", "student")
                        .requestMatchers(HttpMethod.GET, "/api/submissions/*/download").hasAnyRole("adviser", "coordinator", "administrator", "student")
                        .requestMatchers(HttpMethod.GET, "/api/submissions/pending").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.GET, "/api/submissions/group/**").hasAnyRole("adviser", "coordinator", "administrator", "student")
                        .requestMatchers(HttpMethod.GET, "/api/submissions/deliverable/**").hasAnyRole("adviser", "coordinator", "administrator")
                        .requestMatchers(HttpMethod.POST, "/api/feedback/**").hasAnyRole("adviser", "coordinator", "administrator")
                        .requestMatchers(HttpMethod.GET, "/api/feedback/**").hasAnyRole("adviser", "coordinator", "administrator")
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").hasAnyRole("adviser", "coordinator", "administrator", "student")
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").hasAnyRole("adviser", "coordinator", "administrator", "student")
                        .requestMatchers(HttpMethod.POST, "/api/deliverables/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.PUT, "/api/deliverables/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.DELETE, "/api/deliverables/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers("/api/system-config/**").hasRole("administrator")
                        .requestMatchers("/api/audit/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.GET, "/api/deadlines/admin").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.POST, "/api/deadlines/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.PUT, "/api/deadlines/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.DELETE, "/api/deadlines/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.GET, "/api/groups/**").hasAnyRole("adviser", "coordinator", "administrator")
                        .requestMatchers(HttpMethod.POST, "/api/groups/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(HttpMethod.DELETE, "/api/groups/**").hasAnyRole("coordinator", "administrator")
                        .requestMatchers(
                                "/api/users/**",
                                "/api/dashboard/**",
                                "/api/analytics/**",
                                "/api/deadlines/**",
                                "/api/deliverables/**",
                                "/api/groups/**",
                                "/api/status-monitoring/**",
                                "/api/submission-summary/**",
                                "/api/submissions/**",
                                "/api/feedback/**",
                                "/api/comments/**")
                        .authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oauth2AuthenticationSuccessHandler)
                        .failureHandler(oauth2AuthenticationFailureHandler))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .xssProtection(
                                xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentTypeOptions(ct -> {
                        }));

        return http.build();
    }
}
