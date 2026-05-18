package com.intellitrack.controller;

import com.intellitrack.dto.CalendarDeadlineDto;
import com.intellitrack.dto.DeadlineAdminDto;
import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.service.DeadlineMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deadlines")
@CrossOrigin(origins = "http://localhost:3000")
public class DeadlineMonitoringController {

    private final DeadlineMonitoringService deadlineMonitoringService;

    public DeadlineMonitoringController(DeadlineMonitoringService deadlineMonitoringService) {
        this.deadlineMonitoringService = deadlineMonitoringService;
    }

    @GetMapping("/active")
    public ResponseEntity<List<DeadlineCardDto>> getActiveDeadlines(@RequestParam Long groupId) {
        return ResponseEntity.ok(deadlineMonitoringService.getActiveDeadlines(groupId));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarDeadlineDto>> getCalendar(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(deadlineMonitoringService.getCalendar(year, month));
    }

    @GetMapping("/reminders")
    public ResponseEntity<List<ReminderDto>> getReminders(@RequestParam Long userId) {
        return ResponseEntity.ok(deadlineMonitoringService.getReminders(userId));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<DeadlineAdminDto>> getAdminDeadlines() {
        return ResponseEntity.ok(deadlineMonitoringService.getAdminDeadlines());
    }

    @PostMapping("/deliverables")
    public ResponseEntity<Deliverable> createDeliverable(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String stage = payload.get("stage");
        return ResponseEntity.ok(deadlineMonitoringService.createDeliverable(name, stage));
    }

    @PostMapping
    public ResponseEntity<Deadline> createDeadline(@RequestBody Map<String, Object> payload) {
        Long deliverableId = Long.valueOf(payload.get("deliverableId").toString());
        LocalDateTime dueAt = LocalDateTime.parse(payload.get("dueAt").toString());
        String academicTerm = payload.get("academicTerm").toString();
        return ResponseEntity.ok(deadlineMonitoringService.createDeadline(deliverableId, dueAt, academicTerm));
    }

    @PutMapping("/{deadlineId}")
    public ResponseEntity<Deadline> updateDeadline(
            @PathVariable Long deadlineId,
            @RequestBody Map<String, Object> payload) {
        LocalDateTime dueAt = LocalDateTime.parse(payload.get("dueAt").toString());
        String academicTerm = payload.get("academicTerm").toString();
        return ResponseEntity.ok(deadlineMonitoringService.updateDeadline(deadlineId, dueAt, academicTerm));
    }

    @DeleteMapping("/{deadlineId}")
    public ResponseEntity<Void> deleteDeadline(@PathVariable Long deadlineId) {
        deadlineMonitoringService.deleteDeadline(deadlineId);
        return ResponseEntity.noContent().build();
    }
}
