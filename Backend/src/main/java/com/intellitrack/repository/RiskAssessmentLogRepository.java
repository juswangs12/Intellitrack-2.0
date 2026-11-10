package com.intellitrack.repository;

import com.intellitrack.entity.RiskAssessmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskAssessmentLogRepository extends JpaRepository<RiskAssessmentLog, Long> {
    List<RiskAssessmentLog> findByGroup_Id(Long groupId);
    boolean existsByDeliverable_Id(Long deliverableId);
}
