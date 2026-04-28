package com.intellitrack.repository;

import com.intellitrack.entity.RiskAssessmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskAssessmentLogRepository extends JpaRepository<RiskAssessmentLog, Long> {
}