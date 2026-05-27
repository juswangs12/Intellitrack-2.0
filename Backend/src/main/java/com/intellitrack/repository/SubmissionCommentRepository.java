package com.intellitrack.repository;

import com.intellitrack.entity.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {
    List<SubmissionComment> findBySubmission_IdOrderByCreatedAtDesc(Long submissionId);
    List<SubmissionComment> findBySubmission_IdOrderByCreatedAtAsc(Long submissionId);
    List<SubmissionComment> findBySubmission_Id(Long submissionId);
    List<SubmissionComment> findBySubmission_Group_Id(Long groupId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SubmissionComment sc WHERE sc.submission.deliverable.id = :deliverableId")
    void deleteByDeliverableId(Long deliverableId);
}
