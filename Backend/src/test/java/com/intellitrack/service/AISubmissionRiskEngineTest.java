package com.intellitrack.service;

import com.intellitrack.dto.RiskAssessmentDto;
import com.intellitrack.entity.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AISubmissionRiskEngineTest {

    @Autowired
    private AISubmissionRiskEngine riskEngine;

    @MockBean
    private AiService aiService;

    @Test
    public void testAssessAtRisk() {
        when(aiService.getCompletion(anyString())).thenReturn(Mono.just("Risk is high due to remaining time."));

        RiskAssessmentDto result = riskEngine.assess(12, SubmissionStatus.PENDING, 0, 1);

        assertEquals("AT_RISK", result.riskLevel());
        assertTrue(result.riskScore() >= 60);
        assertEquals("Risk is high due to remaining time.", result.explanation());
    }

    @Test
    public void testAssessOnTrack() {
        when(aiService.getCompletion(anyString())).thenReturn(Mono.just("Progress is on track."));

        RiskAssessmentDto result = riskEngine.assess(120, SubmissionStatus.SUBMITTED, 2, 0);

        assertEquals("ON_TRACK", result.riskLevel());
        assertTrue(result.riskScore() < 60);
        assertEquals("Progress is on track.", result.explanation());
    }
}
