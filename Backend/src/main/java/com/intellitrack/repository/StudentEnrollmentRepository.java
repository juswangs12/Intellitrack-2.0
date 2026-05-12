package com.intellitrack.repository;

import com.intellitrack.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {
    List<StudentEnrollment> findByClassSectionId(Long classSectionId);
    Optional<StudentEnrollment> findByClassSectionIdAndStudentId(Long classSectionId, String studentId);

    List<StudentEnrollment> findByEmail(String email);
    
    @Query("SELECT se FROM StudentEnrollment se WHERE LOWER(se.email) = LOWER(:email)")
    List<StudentEnrollment> findByEmailIgnoreCase(String email);
    
    List<StudentEnrollment> findByStudentId(String studentId);
    List<StudentEnrollment> findByStudent_Id(Long userId);

    @Query("SELECT se FROM StudentEnrollment se LEFT JOIN FETCH se.classSection cs LEFT JOIN FETCH cs.subject")
    List<StudentEnrollment> findAllWithSectionAndSubject();

    @Query("SELECT se FROM StudentEnrollment se LEFT JOIN FETCH se.classSection cs LEFT JOIN FETCH cs.subject WHERE se.classSection.id = :classSectionId")
    List<StudentEnrollment> findByClassSectionIdWithSectionAndSubject(Long classSectionId);
}
