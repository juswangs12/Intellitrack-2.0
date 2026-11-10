package com.intellitrack.repository;
import com.intellitrack.entity.DeadlineEmail;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
public interface DeadlineEmailRepository extends JpaRepository<DeadlineEmail,String>{
 List<DeadlineEmail> findTop5ByStatusOrderByCreatedAtAsc(String status);
 List<DeadlineEmail> findByGroupIdAndStatus(Long groupId, String status);
 long countByStatus(String status);
 @org.springframework.transaction.annotation.Transactional @Modifying
 @Query("UPDATE DeadlineEmail e SET e.status='SENDING',e.attemptedAt=:now WHERE e.id=:id AND e.status='PENDING'")
 int claim(@Param("id") String id,@Param("now") Instant now);
 @org.springframework.transaction.annotation.Transactional @Modifying
 @Query("UPDATE DeadlineEmail e SET e.status='CANCELLED',e.failure='Group deleted after coordinator review' WHERE e.groupId=:groupId AND e.status='PENDING'")
 int cancelPendingByGroupId(@Param("groupId") Long groupId);
 @org.springframework.transaction.annotation.Transactional @Modifying
 @Query("UPDATE DeadlineEmail e SET e.status='CANCELLED',e.failure='Requirement deleted by coordinator' WHERE e.requirementId=:requirementId AND e.status='PENDING'")
 int cancelPendingByRequirementId(@Param("requirementId") Long requirementId);
 @org.springframework.transaction.annotation.Transactional @Modifying
 @Query("UPDATE DeadlineEmail e SET e.status='REVIEW',e.failure='Interrupted delivery; verify the sender Sent folder before retrying' WHERE e.status='SENDING' AND e.attemptedAt<:cutoff")
 int recoverInterrupted(@Param("cutoff") Instant cutoff);
}
