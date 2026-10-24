package com.intellitrack.service;

import com.intellitrack.entity.AuditLog;
import com.intellitrack.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String performedBy, String targetModule, String details, String ipAddress) {
        AuditLog log = new AuditLog(action, performedBy, targetModule, details, ipAddress);
        log.setTimestamp(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Manila")));
        auditLogRepository.save(log);
    }
}
