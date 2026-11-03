package com.intellitrack.repository;

import com.intellitrack.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByActionAndPerformedByOrderByTimestampDesc(String action,String performedBy);
    org.springframework.data.domain.Page<AuditLog> findByTargetModuleNot(String module,org.springframework.data.domain.Pageable pageable);
    List<AuditLog> findByTargetModule(String targetModule);
    List<AuditLog> findByPerformedBy(String performedBy);
    List<AuditLog> findByTargetModuleAndDetailsStartingWithOrderByTimestampDesc(String module,String prefix);
}
