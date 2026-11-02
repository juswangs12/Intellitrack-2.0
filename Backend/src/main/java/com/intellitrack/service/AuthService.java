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
    @Autowired private UserService userService;

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
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(loginRequest.getEmail().trim());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if("student".equals(user.getRole()))
                throw new IllegalArgumentException("Students must use Continue with Google on the student sign-in page.");

            // Check if password matches
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                com.intellitrack.security.LoginAudience.requireRole(loginRequest.getAudience(), user.getRole());
                // Update last login timestamp
                user.setUpdatedAt(LocalDateTime.now());
                user.recordSignIn();
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
     * Register a new user (admin-only — enforced at the controller layer by
     * SecurityConfig).
     * The role is validated against a whitelist to prevent privilege escalation.
     */
    public LoginResponse register(User user) {
        if (!isEmailDomainAllowed(user.getEmail())) throw new IllegalArgumentException("Email domain not allowed");
        User saved=userService.createUser(user);
        return new LoginResponse(jwtTokenProvider.generateToken(saved),jwtTokenProvider.generateRefreshToken(saved.getId()),new UserDTO(saved),saved.getRole());
    }

    public static void validatePassword(String password) {
        if(password==null||password.length()<8||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Password must contain at least 8 characters and at most 72 UTF-8 bytes");
    }

    /**
     * Handle OAuth2 user (Google login - Student)
     */
    public LoginResponse handleOAuth2User(User user) {
        // Validate email domain for OAuth users
        if (!isEmailDomainAllowed(user.getEmail())) {
            throw new RuntimeException("Email domain not allowed for OAuth login");
        }

        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(user.getEmail());
        User userToSave;

        if (existingUser.isPresent()) {
            userToSave = existingUser.get();
            // Update last login timestamp
            userToSave.setUpdatedAt(LocalDateTime.now());
            // Update googleId if not set
            if (userToSave.getGoogleId() == null && user.getGoogleId() != null) {
                userToSave.setGoogleId(user.getGoogleId());
            }
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
    public boolean isEmailDomainAllowed(String email) {
        if (allowedEmailDomains == null || allowedEmailDomains.trim().isEmpty()) {
            return true;
        }

        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.indexOf("@"));
        String[] domains = allowedEmailDomains.split(",");

        for (String allowedDomain : domains) {
            if (domain.equalsIgnoreCase(allowedDomain.trim().startsWith("@") ? allowedDomain.trim() : "@" + allowedDomain.trim())) {
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
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        @SuppressWarnings("null")
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!jwtTokenProvider.matchesSession(refreshToken, user)) throw new RuntimeException("Session expired. Please sign in again.");
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
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
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
                        "If you did not request this, please ignore this email.\n\nIntelliTrack");
    }

    /**
     * Complete password reset: validate token, set new password, clear token.
     */
    public void resetPassword(String token, String newPassword) {
        validatePassword(newPassword);
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        if (user.getPasswordResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new RuntimeException("Invalid or expired password reset token");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.revokeSessions();
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }
}
