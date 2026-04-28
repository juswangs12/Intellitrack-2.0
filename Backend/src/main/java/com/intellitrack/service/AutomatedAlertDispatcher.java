package com.intellitrack.service;

import com.intellitrack.dto.ReminderDto;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.ReminderLog;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.ReminderLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutomatedAlertDispatcher {

    private final DeadlineMonitoringService deadlineMonitoringService;
    private final ReminderLogRepository reminderLogRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final DeliverableRepository deliverableRepository;

    public AutomatedAlertDispatcher(
            DeadlineMonitoringService deadlineMonitoringService,
            ReminderLogRepository reminderLogRepository,
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository) {
        this.deadlineMonitoringService = deadlineMonitoringService;
        this.reminderLogRepository = reminderLogRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void dispatchUpcomingAlerts() {
        List<ReminderDto> queue = deadlineMonitoringService.buildDispatchQueue();
        for (ReminderDto reminder : queue) {
            ReminderLog log = new ReminderLog();
            ProjectGroup group = projectGroupRepository.findById(reminder.groupId()).orElse(null);
            Deliverable deliverable = deliverableRepository.findById(reminder.deliverableId()).orElse(null);
            log.setGroup(group);
            log.setDeliverable(deliverable);
            log.setMessage(reminder.message());
            log.setChannel("in-app");
            reminderLogRepository.save(log);
        }
    }
}