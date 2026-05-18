package com.intellitrack.repository;

import com.intellitrack.entity.RubricCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, Long> {
}

