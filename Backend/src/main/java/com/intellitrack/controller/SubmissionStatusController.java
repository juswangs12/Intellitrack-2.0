package com.intellitrack.controller;

import com.intellitrack.dto.DeliverableStatusDto;
import com.intellitrack.dto.GroupStatusSummaryDto;
import com.intellitrack.service.StatusEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/status-monitoring")
@CrossOrigin(origins = "http://localhost:3000")
public class SubmissionStatusController {

    private final StatusEvaluationService statusEvaluationService;

    public SubmissionStatusController(StatusEvaluationService statusEvaluationService) {
        this.statusEvaluationService = statusEvaluationService;
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<List<DeliverableStatusDto>> getGroupStatuses(@PathVariable Long groupId) {
        return ResponseEntity.ok(statusEvaluationService.getGroupStatuses(groupId));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<GroupStatusSummaryDto>> getClassStatuses(
            @RequestParam(required = false) Long adviserId) {
        return ResponseEntity.ok(statusEvaluationService.getClassStatuses(adviserId));
    }
}