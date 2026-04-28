package com.intellitrack.service;

import com.intellitrack.dto.RiskAssessmentDto;
import com.intellitrack.entity.SubmissionStatus;
import org.springframework.stereotype.Service;

@Service
public class AISubmissionRiskEngine {

    public RiskAssessmentDto assess(long hoursRemaining, SubmissionStatus status, int revisionCount,
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
        if (status == SubmissionStatus.LATE) {
            score += 35;
        }
        if (revisionCount == 0 && hoursRemaining <= 48) {
            score += 10;
        }
        if (previousLateCount > 0) {
            score += 10;
        }

        String level = score >= 60 ? "AT_RISK" : "ON_TRACK";
        String explanation = level.equals("AT_RISK")
                ? "Remaining time and current submission pattern suggest intervention is needed."
                : "Progress is aligned with the current deadline window.";

        return new RiskAssessmentDto(score, level, explanation);
    }
}