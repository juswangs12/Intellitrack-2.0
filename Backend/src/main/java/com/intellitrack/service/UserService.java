package com.intellitrack.service;

import com.intellitrack.dto.PasswordChangeRequest;
import com.intellitrack.dto.UpdateProfileRequest;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.User;
import com.intellitrack.repository.StudentEnrollmentRepository;
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

@SuppressWarnings("null")
@Service
@Transactional
public class UserService {
    @Autowired private org.springframework.context.ApplicationEventPublisher events;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;
    @Autowired private AccessService access;
    @Autowired private AuditService audit;
    @Autowired private com.intellitrack.repository.GoogleConnectionRepository googleConnections;
    @Autowired private com.intellitrack.repository.NotificationRepository notifications;
    @Autowired private com.intellitrack.repository.ProjectGroupRepository groups;
    @Autowired private com.intellitrack.repository.SubmissionRepository submissions;

    public User updateAccount(Long id, String firstName, String lastName, String department, String role) {
        var user=getUserById(id);
        if(!java.util.Set.of("student","adviser","coordinator","administrator").contains(role))
            throw new IllegalArgumentException("Choose a supported account role");
        if(!role.equals(user.getRole())) {
            if(access.current().getId().equals(id)) throw new IllegalArgumentException("You cannot change your own administrator role");
            if(!studentEnrollmentRepository.findByStudent_Id(id).isEmpty() || !groups.findByAdviserId(id).isEmpty()
                    || !submissions.findByStudentId(id).isEmpty())
                throw new IllegalArgumentException("This account has academic assignments or submission history. Keep its role or use a separate staff account.");
        }
        user.setFirstName(firstName.trim());user.setLastName(lastName.trim());user.setDepartment(department);
        user.setRole(role);user.setUpdatedAt(LocalDateTime.now());
        var saved=userRepository.save(user);
        audit.log("ACCOUNT_UPDATED",access.current().getId().toString(),"Accounts","Account "+id+" updated; role: "+role,"WEB");
        return saved;
    }

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
    public User createUser(User request) {
        User newUser=new User();
        newUser.setEmail(request.getEmail());newUser.setFirstName(request.getFirstName());newUser.setLastName(request.getLastName());
        newUser.setPassword(request.getPassword());newUser.setRole(request.getRole());newUser.setDepartment(request.getDepartment());
        newUser.setId(null);
        if (newUser.getRole()==null || !java.util.Set.of("student","adviser","coordinator","administrator").contains(newUser.getRole()))
            throw new IllegalArgumentException("Choose a supported account role");
        if(newUser.getEmail()==null || !newUser.getEmail().trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("A valid email address is required");
        if(newUser.getFirstName()==null || newUser.getFirstName().isBlank() || newUser.getLastName()==null || newUser.getLastName().isBlank())
            throw new IllegalArgumentException("First and last name are required");
        if(newUser.getPassword()==null || newUser.getPassword().length()<8 || newUser.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Password must contain at least 8 characters and at most 72 UTF-8 bytes");
        newUser.setEmail(newUser.getEmail().trim().toLowerCase(java.util.Locale.ROOT));

        if (userRepository.existsByEmailIgnoreCase(newUser.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        if(newUser.getFirstName().trim().length()>50||newUser.getLastName().trim().length()>50||newUser.getEmail().length()>100||(newUser.getDepartment()!=null&&newUser.getDepartment().length()>100))
            throw new IllegalArgumentException("Name, email or department exceeds the allowed length");
        newUser.setFirstName(newUser.getFirstName().trim());newUser.setLastName(newUser.getLastName().trim());
        String hashed = passwordEncoder.encode(newUser.getPassword());
        newUser.setPassword(hashed);
        newUser.setCreatedAt(LocalDateTime.now());
        var saved=userRepository.save(newUser);
        if("adviser".equals(saved.getRole()))events.publishEvent(new ActivityEmailService.AdviserCreated(saved.getId()));
        audit.log("ACCOUNT_CREATED",access.current().getId().toString(),"Accounts","Account "+saved.getId()+" created; role: "+saved.getRole(),"WEB");
        return saved;
    }

    /**
     * List users; if role provided, filter by role.
     */
    public List<User> listUsers(String role, String q) {
        if (q != null && !q.isBlank()) {
            return userRepository.findByRoleAndQuery(
                    (role != null && !role.isBlank()) ? role : null, q.trim());
        }
        if (role != null && !role.isBlank()) {
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

            // Update studentId and cascade to any linked enrollment records
            if (updateRequest.getStudentId() != null) {
                String newStudentId = updateRequest.getStudentId().trim();
                String oldStudentId = existingUser.getStudentId();
                if (!newStudentId.equals(oldStudentId)) {
                    // Reject if another user already owns this student ID
                    if (!newStudentId.isBlank() && userRepository.existsByStudentId(newStudentId)
                            && userRepository.findByStudentId(newStudentId)
                                    .map(u -> !u.getId().equals(id))
                                    .orElse(false)) {
                        throw new RuntimeException(
                                "Student ID '" + newStudentId + "' is already in use by another account.");
                    }
                    existingUser.setStudentId(newStudentId.isBlank() ? null : newStudentId);
                    // Cascade: update the studentId column on every linked enrollment row
                    // so the coordinator classlist reflects the new ID immediately.
                    List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_Id(id);
                    for (StudentEnrollment enrollment : enrollments) {
                        enrollment.setStudentId(
                                newStudentId.isBlank() ? (oldStudentId != null ? oldStudentId : id.toString())
                                        : newStudentId);
                        studentEnrollmentRepository.save(enrollment);
                    }
                }
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
            existingUser.setUpdatedAt(LocalDateTime.now());

            return userRepository.save(existingUser);
        }

        throw new RuntimeException("User not found");
    }

    /**
     * Change user password
     */
    public void changePassword(Long id, PasswordChangeRequest passwordChangeRequest) {
        AuthService.validatePassword(passwordChangeRequest.getNewPassword());
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
            user.revokeSessions();
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);
            return;
        }

        throw new RuntimeException("User not found");
    }

    /**
     * Delete user - first unlink all StudentEnrollments to avoid foreign key constraints
     */
    public void deleteUser(Long id) {
        if(access.current().getId().equals(id))throw new IllegalArgumentException("You cannot delete your own account");
        if(!studentEnrollmentRepository.findByStudent_Id(id).isEmpty() || !groups.findByAdviserId(id).isEmpty() || !submissions.findByStudentId(id).isEmpty())
            throw new IllegalArgumentException("This account has academic assignments or submission history and cannot be deleted");
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("Deleting user: " + user.getEmail() + ", first unlinking enrollments...");
            
            List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByStudent_Id(id);
            for (StudentEnrollment enrollment : enrollments) {
                System.out.println("  Unlinking enrollment: " + enrollment.getFullName());
                enrollment.setStudent(null);
                studentEnrollmentRepository.save(enrollment);
            }
            System.out.println("  Unlinked " + enrollments.size() + " enrollments");
            
            googleConnections.deleteByUserId(id);
            notifications.deleteByUserId(id);
            userRepository.deleteById(id);
            audit.log("ACCOUNT_DELETED",access.current().getId().toString(),"Accounts","Account "+id+" deleted","WEB");
            System.out.println("User deleted successfully");
            return;
        }

        throw new RuntimeException("User not found");
    }
}
