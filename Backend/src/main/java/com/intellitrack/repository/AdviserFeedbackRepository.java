package com.intellitrack.repository;

import com.intellitrack.entity.AdviserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdviserFeedbackRepository extends JpaRepository<AdviserFeedback, Long> {
    @EntityGraph(attributePaths = {"adviser", "submission", "submission.deliverable", "criterionEvaluations", "criterionEvaluations.criterion"})
    Optional<AdviserFeedback> findBySubmissionId(Long submissionId);
    
    @EntityGraph(attributePaths = {"adviser", "submission", "submission.deliverable"})
    List<AdviserFeedback> findBySubmission_Group_IdOrderByEvaluatedAtDesc(Long groupId);
}
