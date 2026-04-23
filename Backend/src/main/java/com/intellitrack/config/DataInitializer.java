package com.intellitrack.config;

import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        try {
            // Only initialize if no users exist
            if (userRepository.count() == 0) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                
                // Create test users
                User adviser = new User("Jane", "Smith", "adviser@university.edu", passwordEncoder.encode("password123"), "adviser");
                adviser.setDepartment("Computer Science");
                adviser.setPhone("+1234567891");
                adviser.setCreatedAt(LocalDateTime.now());
                adviser = userRepository.save(adviser);

                User student = new User("John", "Doe", "student@university.edu", passwordEncoder.encode("password123"), "student");
                student.setStudentId("STU001");
                student.setDepartment("Computer Science");
                student.setYear("3");
                student.setPhone("+1234567890");
                student.setAdvisorId(adviser.getId());
                student.setCreatedAt(LocalDateTime.now());
                userRepository.save(student);

                User coordinator = new User("Bob", "Johnson", "coordinator@university.edu", passwordEncoder.encode("password123"), "coordinator");
                coordinator.setDepartment("Computer Science");
                coordinator.setPhone("+1234567892");
                coordinator.setCreatedAt(LocalDateTime.now());
                userRepository.save(coordinator);

                User admin = new User("Alice", "Brown", "admin@university.edu", passwordEncoder.encode("password123"), "administrator");
                admin.setDepartment("Administration");
                admin.setPhone("+1234567893");
                admin.setCreatedAt(LocalDateTime.now());
                userRepository.save(admin);

                System.out.println("Test users initialized successfully!");
            }
        } catch (Exception e) {
            System.err.println("Error initializing test data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}