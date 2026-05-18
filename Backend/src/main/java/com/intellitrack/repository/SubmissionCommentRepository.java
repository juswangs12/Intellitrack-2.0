package com.intellitrack.repository;

import com.intellitrack.entity.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {
    List<SubmissionComment> findBySubmission_IdOrderByCreatedAtDesc(Long submissionId);
    List<SubmissionComment> findBySubmission_IdOrderByCreatedAtAsc(Long submissionId);
}
