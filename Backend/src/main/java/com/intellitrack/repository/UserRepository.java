package com.intellitrack.repository;

import com.intellitrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"group"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    Optional<User> findByStudentId(String studentId);

    List<User> findByAdvisorId(Long advisorId);

    List<User> findByRole(String role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.studentId) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<User> findByRoleAndQuery(@Param("role") String role, @Param("q") String q);

    @EntityGraph(attributePaths = {"group"})
    Optional<User> findById(Long id);

    Optional<User> findByPasswordResetToken(String passwordResetToken);
}