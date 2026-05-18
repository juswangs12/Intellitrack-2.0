package com.intellitrack.repository;

import com.intellitrack.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {
    List<ClassSection> findBySubjectId(Long subjectId);
    Optional<ClassSection> findBySubjectIdAndSection(Long subjectId, String section);
}
