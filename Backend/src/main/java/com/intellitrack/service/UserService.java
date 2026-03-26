package com.intellitrack.service;

import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        throw new RuntimeException("User not found");
    }

    public User updateUser(Long id, User updatedUser) {
        Optional<User> existingUserOpt = userRepository.findById(id);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Update fields
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setStudentId(updatedUser.getStudentId());
            existingUser.setDepartment(updatedUser.getDepartment());
            existingUser.setYear(updatedUser.getYear());
            existingUser.setPhone(updatedUser.getPhone());

            return userRepository.save(existingUser);
        }

        throw new RuntimeException("User not found");
    }
}