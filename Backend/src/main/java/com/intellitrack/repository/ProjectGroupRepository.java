package com.intellitrack.repository;

import com.intellitrack.entity.ProjectGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, Long> {
    Optional<ProjectGroup> findByCode(String code);

    List<ProjectGroup> findByAdviserId(Long adviserId);
}