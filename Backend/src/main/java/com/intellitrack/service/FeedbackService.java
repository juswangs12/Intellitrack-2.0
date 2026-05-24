package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("null")
@Service
@Transactional
public class FeedbackService {

    @Autowired
    private AdviserFeedbackRepository feedbackRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    public AdviserFeedback addComment(Long submissionId, Long adviserId, String comments) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        User adviser = userRepository.findById(adviserId)
                .orElseThrow(() -> new ResourceNotFoundException("Adviser not found"));

        AdviserFeedback feedback = feedbackRepository.findBySubmissionId(submissionId)
                .orElse(new AdviserFeedback());

        feedback.setSubmission(submission);
        feedback.setAdviser(adviser);
        feedback.setGeneralComments(comments);
        feedback.setEvaluatedAt(LocalDateTime.now());
        feedback.setTotalScore(0.0);

        submission.setStatus(SubmissionStatus.COMMENTED);
        submissionRepository.save(submission);

        AdviserFeedback savedFeedback = feedbackRepository.save(feedback);

        ProjectGroup group = submission.getGroup();
        group.getStudents().forEach(enrollment -> {
            if (enrollment.getStudent() != null) {
                notificationService.createNotification(enrollment.getStudent().getId(),
                        "New comment available for " + submission.getDeliverable().getName());
            }
        });

        auditService.log("COMMENT_ADDED", String.valueOf(adviserId), "FEEDBACK",
                "Added comment to submission " + submissionId, "SYSTEM");

        return savedFeedback;
    }
}
