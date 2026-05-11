package com.intellitrack.service;

import com.intellitrack.entity.Submission;
import com.intellitrack.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnomalyDetectionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EmailService emailService;

    public void detectSubmissionAnomaly(Long groupId, String ipAddress) {
        // Check for rapid submissions (e.g., more than 5 in 1 minute)
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        List<Submission> recentSubmissions = submissionRepository.findByGroupId(groupId);
        
        long rapidCount = recentSubmissions.stream()
            .filter(s -> s.getSubmittedAt() != null && s.getSubmittedAt().isAfter(oneMinuteAgo))
            .count();

        if (rapidCount > 5) {
            String details = String.format("Rapid submission detected: %d submissions in 1 minute from group %d (IP: %s)", 
                rapidCount, groupId, ipAddress);
            
            auditService.log("ANOMALY_DETECTED", "SYSTEM", "SUBMISSION", details, ipAddress);
            
            // Alert coordinator
            emailService.sendEmail("coordinator@university.edu", "Security Alert: Rapid Submissions", details);
            
            System.err.println("Anomaly detected: " + details);
        }
    }
}
