package com.intellitrack.controller;

import com.intellitrack.dto.CalendarDeadlineDto;
import com.intellitrack.dto.DeadlineCardDto;
import com.intellitrack.dto.ReminderDto;
import com.intellitrack.service.DeadlineMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}