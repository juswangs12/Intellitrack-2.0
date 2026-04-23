package com.intellitrack.controller;

import com.intellitrack.dto.PasswordChangeRequest;
import com.intellitrack.dto.UpdateProfileRequest;
import com.intellitrack.dto.UserDTO;
import com.intellitrack.entity.User;
import com.intellitrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            UserDTO userDTO = new UserDTO(user);
            return ResponseEntity.ok(userDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user profile (same as getUserById but explicitly for profile)
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            UserDTO userDTO = new UserDTO(user);
            return ResponseEntity.ok(userDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List users (admin UI). Optional role filter.
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> listUsers(@RequestParam(required = false) String role) {
        try {
            List<User> users = userService.listUsers(role);
            List<UserDTO> dtos = users.stream().map(UserDTO::new).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new user (admin)
     */
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody User newUser) {
        try {
            User created = userService.createUser(newUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(new UserDTO(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update user profile
     */
    @PutMapping("/{id}/profile")
    public ResponseEntity<UserDTO> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().build();
            }

            User updatedUser = userService.updateUserProfile(id, updateRequest);
            UserDTO userDTO = new UserDTO(updatedUser);
            return ResponseEntity.ok(userDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update user (full user update)
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            UserDTO userDTO = new UserDTO(updatedUser);
            return ResponseEntity.ok(userDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Change user password
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest passwordChangeRequest,
            BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body("Validation failed");
            }

            if (!passwordChangeRequest.isPasswordMatch()) {
                return ResponseEntity.badRequest().body("Passwords do not match");
            }

            userService.changePassword(id, passwordChangeRequest);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Delete user account
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Upload avatar image for user
     */
    @PostMapping(path = "/{id}/avatar", consumes = {"multipart/form-data"})
    public ResponseEntity<UserDTO> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            User updated = userService.saveAvatar(id, file);
            return ResponseEntity.ok(new UserDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Serve avatar image file
     */
    @GetMapping(path = "/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id) {
        try {
            Resource resource = userService.getAvatarResource(id);
            Path filePath = Path.of(resource.getURI());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}