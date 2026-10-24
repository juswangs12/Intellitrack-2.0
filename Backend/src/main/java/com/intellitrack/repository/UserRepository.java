package com.intellitrack.repository;

import com.intellitrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.lastActiveAt = :now WHERE u.id = :id AND (u.lastActiveAt IS NULL OR u.lastActiveAt < :cutoff)")
    int recordActivity(@Param("id") Long id, @Param("now") java.time.Instant now, @Param("cutoff") java.time.Instant cutoff);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> lockForSubmission(Long id);

    @EntityGraph(attributePaths = { "group" })
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStudentId(String studentId);

    Optional<User> findByStudentId(String studentId);

    List<User> findByAdvisorId(Long advisorId);

    List<User> findByRole(String role);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND (" +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(u.studentId) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<User> findByRoleAndQuery(@Param("role") String role, @Param("q") String q);

    @Override
    @NonNull
    @EntityGraph(attributePaths = { "group" })
    Optional<User> findById(@NonNull Long id);

    Optional<User> findByPasswordResetToken(String passwordResetToken);
    
    List<User> findByGroup_Id(Long groupId);
    @Query("SELECT u FROM User u WHERE u.role = 'student' " +
        "AND ((u.googleId IS NOT NULL AND u.googleId <> '') OR (u.googleSub IS NOT NULL AND u.googleSub <> '')) " +
        "AND NOT EXISTS (SELECT e.id FROM StudentEnrollment e WHERE e.student.id = u.id) ORDER BY u.createdAt DESC, u.id DESC")
    List<User> findGoogleStudentsAwaitingEnrollment();
}
