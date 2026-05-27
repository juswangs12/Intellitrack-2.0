package com.intellitrack.service;

import com.intellitrack.dto.RiskAssessmentDto;
import com.intellitrack.entity.SubmissionStatus;
import org.springframework.stereotype.Service;

@Service
public class RiskAssessmentService {

    public RiskAssessmentDto assess(long hoursRemaining, SubmissionStatus status,
            long previousLateCount) {
        int score = calculateBaseScore(hoursRemaining, status, previousLateCount);

        String level;
        if (score >= 80) level = "CRITICAL";
        else if (score >= 60) level = "AT_RISK";
        else if (score >= 40) level = "WARNING";
        else level = "ON_TRACK";
        
        String explanation = level.equals("AT_RISK")
            ? "Remaining time and current submission pattern suggest intervention is needed."
            : "Progress is aligned with the current deadline window.";

        return new RiskAssessmentDto(score, level, explanation);
    }

    private int calculateBaseScore(long hoursRemaining, SubmissionStatus status,
            long previousLateCount) {
        int score = 0;

        if (hoursRemaining <= 72) {
            score += 20;
        }
        if (hoursRemaining <= 24) {
            score += 25;
        }
        if (status == SubmissionStatus.PENDING) {
            score += 20;
        }
        if (status == SubmissionStatus.SUBMITTED_LATE) {
            score += 35;
        }
        if (hoursRemaining <= 48) {
            score += 10;
        }
        if (previousLateCount > 0) {
            score += 10;
        }
        return score;
    }
}
