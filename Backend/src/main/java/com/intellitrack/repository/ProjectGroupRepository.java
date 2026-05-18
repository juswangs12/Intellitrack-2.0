package com.intellitrack.repository;

import com.intellitrack.entity.ProjectGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, Long> {
    Optional<ProjectGroup> findByCode(String code);

    List<ProjectGroup> findByAdviserId(Long adviserId);

    @Query("SELECT pg FROM ProjectGroup pg JOIN pg.students s WHERE s.id = :enrollmentId")
    Optional<ProjectGroup> findByStudents_Id(Long enrollmentId);

    @Query("SELECT CASE WHEN COUNT(pg) > 0 THEN true ELSE false END FROM ProjectGroup pg JOIN pg.students s WHERE s.id = :enrollmentId")
    boolean existsByStudents_Id(Long enrollmentId);
}