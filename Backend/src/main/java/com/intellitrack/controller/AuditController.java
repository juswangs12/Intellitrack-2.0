package com.intellitrack.controller;

import com.intellitrack.dto.AuditLogDto;
import com.intellitrack.entity.AuditLog;
import com.intellitrack.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "http://localhost:3000")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogDto>> list(@RequestParam(defaultValue = "25") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<AuditLog> logs = auditLogRepository
                .findAll(PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent();

        return ResponseEntity.ok(logs.stream()
                .map(log -> new AuditLogDto(
                        log.getId(),
                        log.getAction(),
                        log.getPerformedBy(),
                        log.getTargetModule(),
                        log.getDetails(),
                        log.getTimestamp(),
                        log.getIpAddress()))
                .toList());
    }
}

