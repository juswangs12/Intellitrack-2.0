package com.intellitrack.service;

import com.intellitrack.dto.ReminderDto;
import com.intellitrack.dto.RiskAssessmentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmartReminderService {

    @Autowired
    private AiService aiService;

    public ReminderDto buildReminder(Long groupId, Long deliverableId, String deliverableName, RiskAssessmentDto risk,
            long hoursRemaining) {
        
        String prompt = String.format(
            "Create a short, encouraging, and professional reminder message for a student group. " +
            "Deliverable: '%s', Risk Level: %s, Hours remaining: %d. " +
            "If risk is AT_RISK, be firm but helpful. If ON_TRACK, be positive and motivating. Max 15 words.",
            deliverableName, risk.riskLevel(), hoursRemaining
        );

        String message;
        try {
            message = aiService.getCompletion(prompt).block();
        } catch (Exception e) {
            message = risk.riskLevel().equals("AT_RISK")
                ? "Action needed: " + deliverableName + " is approaching and current progress indicates delay risk."
                : "Stay on pace: " + deliverableName + " is on track, but keep the current momentum going.";
        }

        return new ReminderDto(groupId, deliverableId, deliverableName, message, risk.riskLevel(), hoursRemaining);
    }
}