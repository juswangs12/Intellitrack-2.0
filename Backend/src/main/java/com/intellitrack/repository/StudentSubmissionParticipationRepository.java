package com.intellitrack.repository;

import com.intellitrack.entity.StudentSubmissionParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSubmissionParticipationRepository extends JpaRepository<StudentSubmissionParticipation, Long> {
    List<StudentSubmissionParticipation> findBySubmission_Id(Long submissionId);
    List<StudentSubmissionParticipation> findByStudent_Id(Long studentId);
    List<StudentSubmissionParticipation> findByEnrollment_Id(Long enrollmentId);
}
