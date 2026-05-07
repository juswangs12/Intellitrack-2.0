package com.intellitrack.service;

import com.intellitrack.dto.PasswordChangeRequest;
import com.intellitrack.dto.UpdateProfileRequest;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Get user by ID
     */
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        throw new RuntimeException("User not found");
    }

    /**
     * Create a new user (admin functionality). Password will be hashed.
     */
    public User createUser(User newUser) {
        if (newUser.getEmail() == null || newUser.getPassword() == null) {
            throw new RuntimeException("Email and password are required");
        }

        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        String hashed = passwordEncoder.encode(newUser.getPassword());
        newUser.setPassword(hashed);
        newUser.setCreatedAt(LocalDateTime.now());
        return userRepository.save(newUser);
    }

    /**
     * List users; if role provided, filter by role.
     */
    public List<User> listUsers(String role) {
        if (role != null && !role.isEmpty()) {
            return userRepository.findByRole(role);
        }
        return userRepository.findAll();
    }

    /**
     * Save avatar image for user and return updated user.
     */
    public User saveAvatar(Long id, MultipartFile file) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        try {
            String uploadsDir = "uploads/avatars";
            Path uploadPath = Paths.get(uploadsDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }

            String filename = "user-" + id + "-" + System.currentTimeMillis() + ext;
            Path target = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            user.setAvatarFilename(filename);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store avatar: " + e.getMessage());
        }
    }

    /**
     * Return avatar file as Resource for streaming.
     */
    public Resource getAvatarResource(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOpt.get();
        if (user.getAvatarFilename() == null) {
            throw new RuntimeException("Avatar not set");
        }

        try {
            Path file = Paths.get("uploads/avatars").resolve(user.getAvatarFilename()).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Avatar not found");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to load avatar: " + e.getMessage());
        }
    }

    /**
     * Update user profile with validated request
     */
    public User updateUserProfile(Long id, UpdateProfileRequest updateRequest) {
        Optional<User> existingUserOpt = userRepository.findById(id);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Update only allowed profile fields
            if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().isEmpty()) {
                existingUser.setFirstName(updateRequest.getFirstName());
            }
            if (updateRequest.getLastName() != null && !updateRequest.getLastName().isEmpty()) {
                existingUser.setLastName(updateRequest.getLastName());
            }
            if (updateRequest.getPhone() != null) {
                existingUser.setPhone(updateRequest.getPhone());
            }
            if (updateRequest.getDepartment() != null) {
                existingUser.setDepartment(updateRequest.getDepartment());
            }
            if (updateRequest.getYear() != null) {
                existingUser.setYear(updateRequest.getYear());
            }

            existingUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(existingUser);
        }

        throw new RuntimeException("User not found");
    }

    /**
     * Update full user object
     */
    public User updateUser(Long id, User updatedUser) {
        Optional<User> existingUserOpt = userRepository.findById(id);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Update fields (avoid updating email and password here)
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());
            existingUser.setStudentId(updatedUser.getStudentId());
            existingUser.setDepartment(updatedUser.getDepartment());
            existingUser.setYear(updatedUser.getYear());
            existingUser.setPhone(updatedUser.getPhone());
            if (updatedUser.getRole() != null) {
                existingUser.setRole(updatedUser.getRole());
            }
            existingUser.setUpdatedAt(LocalDateTime.now());

            return userRepository.save(existingUser);
        }

        throw new RuntimeException("User not found");
    }

    /**
     * Change user password
     */
    public void changePassword(Long id, PasswordChangeRequest passwordChangeRequest) {
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify current password
            if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }

            // Verify new passwords match
            if (!passwordChangeRequest.isPasswordMatch()) {
                throw new RuntimeException("New passwords do not match");
            }

            // Check if new password is same as current
            if (passwordChangeRequest.getCurrentPassword().equals(passwordChangeRequest.getNewPassword())) {
                throw new RuntimeException("New password must be different from current password");
            }

            // Hash and set new password
            String hashedPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
            user.setPassword(hashedPassword);
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);
            return;
        }

        throw new RuntimeException("User not found");
    }

    /**
     * Delete user
     */
    public void deleteUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isPresent()) {
            userRepository.deleteById(id);
            return;
        }

        throw new RuntimeException("User not found");
    }
}