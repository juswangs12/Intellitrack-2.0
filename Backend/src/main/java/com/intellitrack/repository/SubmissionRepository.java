package com.intellitrack.repository;

import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByGroupId(Long groupId);

    List<Submission> findByDeliverableId(Long deliverableId);

    List<Submission> findByStatus(SubmissionStatus status);

    Optional<Submission> findByGroupIdAndDeliverableId(Long groupId, Long deliverableId);
}