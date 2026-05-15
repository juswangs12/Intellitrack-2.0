package com.intellitrack.service;

import com.intellitrack.dto.LoginRequest;
import com.intellitrack.dto.LoginResponse;
import com.intellitrack.dto.UserDTO;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.allowed-email-domains:}")
    private String allowedEmailDomains;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Authenticate user with email and password (Staff login)
     */
    public LoginResponse authenticate(LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Check if password matches
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                // Update last login timestamp
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);

                // Generate JWT tokens
                String accessToken = jwtTokenProvider.generateToken(user);
                String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

                UserDTO userDTO = new UserDTO(user);
                System.out.println("User authenticated successfully: " + loginRequest.getEmail() + " with role: "
                        + user.getRole());
                return new LoginResponse(accessToken, refreshToken, userDTO, user.getRole());
            } else {
                System.err.println("Password mismatch for user: " + loginRequest.getEmail());
            }
        } else {
            System.err.println("User not found: " + loginRequest.getEmail());
        }

        throw new RuntimeException("Invalid credentials");
    }

    /**
     * Register a new user (admin-only — enforced at the controller layer by SecurityConfig).
     * The role is validated against a whitelist to prevent privilege escalation.
     */
    public LoginResponse register(User user) {
        // Validate email domain
        if (!isEmailDomainAllowed(user.getEmail())) {
            throw new RuntimeException("Email domain not allowed. Please use your institutional email.");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Whitelist the allowed roles — never trust a caller-supplied role string blindly
        String requestedRole = user.getRole();
        java.util.Set<String> allowedRoles = java.util.Set.of("student", "adviser", "coordinator", "administrator");
        if (requestedRole == null || !allowedRoles.contains(requestedRole.toLowerCase())) {
            user.setRole("student");
        } else {
            user.setRole(requestedRole.toLowerCase());
        }

        // Hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(savedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId());

        UserDTO userDTO = new UserDTO(savedUser);
        return new LoginResponse(accessToken, refreshToken, userDTO, savedUser.getRole());
    }

    /**
     * Handle OAuth2 user (Google login - Student)
     */
    public LoginResponse handleOAuth2User(User user) {
        // Validate email domain for OAuth users
        if (!isEmailDomainAllowed(user.getEmail())) {
            throw new RuntimeException("Email domain not allowed for OAuth login");
        }

        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        User userToSave;

        if (existingUser.isPresent()) {
            userToSave = existingUser.get();
            // Update last login timestamp
            userToSave.setUpdatedAt(LocalDateTime.now());
        } else {
            userToSave = user;
            userToSave.setRole("student");
            userToSave.setCreatedAt(LocalDateTime.now());
        }

        User savedUser = userRepository.save(userToSave);

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(savedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId());

        UserDTO userDTO = new UserDTO(savedUser);
        return new LoginResponse(accessToken, refreshToken, userDTO, savedUser.getRole());
    }

    /**
     * Validate email domain
     */
    private boolean isEmailDomainAllowed(String email) {
        if (allowedEmailDomains == null || allowedEmailDomains.trim().isEmpty()) {
            return true;
        }

        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.indexOf("@"));
        String[] domains = allowedEmailDomains.split(",");

        for (String allowedDomain : domains) {
            if (domain.equalsIgnoreCase(allowedDomain.trim())) {
                return true;
            }
        }

        System.err.println("Email domain not allowed: " + domain);
        return false;
    }

    /**
     * Refresh access token using refresh token
     */
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String newAccessToken = jwtTokenProvider.generateToken(user);
            UserDTO userDTO = new UserDTO(user);
            return new LoginResponse(newAccessToken, refreshToken, userDTO, user.getRole());
        }

        throw new RuntimeException("User not found");
    }

    /**
     * Initiate password reset: generate a time-limited token and email it.
     */
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Return silently — never reveal whether an email is registered
            return;
        }
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + token;
        emailService.sendEmail(
                user.getEmail(),
                "IntelliTrack – Password Reset",
                "Hello " + user.getFirstName() + ",\n\n" +
                "You requested a password reset. Click the link below to choose a new password.\n" +
                "This link expires in 1 hour.\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.\n\nIntelliTrack"
        );
    }

    /**
     * Complete password reset: validate token, set new password, clear token.
     */
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        if (user.getPasswordResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new RuntimeException("Invalid or expired password reset token");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }
}