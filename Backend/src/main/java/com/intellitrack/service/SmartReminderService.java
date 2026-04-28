package com.intellitrack.service;

import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.RiskAssessmentDto;
import org.springframework.stereotype.Service;

@Service
public class SmartReminderService {

    public ReminderDto buildReminder(Long groupId, Long deliverableId, String deliverableName, RiskAssessmentDto risk,
            long hoursRemaining) {
        String message = risk.riskLevel().equals("AT_RISK")
                ? "Action needed: " + deliverableName + " is approaching and current progress indicates delay risk."
                : "Stay on pace: " + deliverableName + " is on track, but keep the current momentum going.";

        return new ReminderDto(groupId, deliverableId, deliverableName, message, risk.riskLevel(), hoursRemaining);
    }
}