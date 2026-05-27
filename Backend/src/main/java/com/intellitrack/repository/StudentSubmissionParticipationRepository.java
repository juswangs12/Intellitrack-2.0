package com.intellitrack.repository;

import com.intellitrack.entity.StudentSubmissionParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSubmissionParticipationRepository extends JpaRepository<StudentSubmissionParticipation, Long> {
    List<StudentSubmissionParticipation> findBySubmission_Id(Long submissionId);
    List<StudentSubmissionParticipation> findByStudent_Id(Long studentId);
    List<StudentSubmissionParticipation> findByEnrollment_Id(Long enrollmentId);
    
    @Query("SELECT p FROM StudentSubmissionParticipation p " +
           "LEFT JOIN FETCH p.submission s " +
           "LEFT JOIN FETCH s.group g " +
           "LEFT JOIN FETCH s.deliverable d " +
           "LEFT JOIN FETCH p.enrollment e " +
           "LEFT JOIN FETCH e.classSection cs " +
           "LEFT JOIN FETCH cs.subject subj " +
           "LEFT JOIN FETCH p.student st")
    List<StudentSubmissionParticipation> findAllWithAllRelations();
    
    @Query("SELECT p FROM StudentSubmissionParticipation p " +
           "LEFT JOIN FETCH p.submission s " +
           "LEFT JOIN FETCH s.group g " +
           "LEFT JOIN FETCH s.deliverable d " +
           "LEFT JOIN FETCH p.enrollment e " +
           "LEFT JOIN FETCH e.classSection cs " +
           "LEFT JOIN FETCH cs.subject subj " +
           "LEFT JOIN FETCH p.student st " +
           "WHERE g.adviser.id = :adviserId")
    List<StudentSubmissionParticipation> findByAdviserIdWithAllRelations(Long adviserId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM StudentSubmissionParticipation p WHERE p.submission.group.id = :groupId")
    void deleteBySubmission_Group_Id(Long groupId);
}
