package com.intellitrack.repository;

import com.intellitrack.entity.SubmissionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionVersionRepository extends JpaRepository<SubmissionVersion, Long> {
    List<SubmissionVersion> findBySubmissionIdOrderByVersionNumberDesc(Long submissionId);
    Optional<SubmissionVersion> findBySubmissionIdAndVersionNumber(Long submissionId, Integer versionNumber);
    Optional<SubmissionVersion> findTopBySubmissionIdOrderByVersionNumberDesc(Long submissionId);
}

