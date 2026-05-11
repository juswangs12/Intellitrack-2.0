package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private RubricRepository rubricRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    public AdviserFeedback evaluateSubmission(Long submissionId, Long adviserId, String comments, SubmissionStatus status, List<CriterionEvaluation> evaluations) {
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

        double totalScore = 0;
        feedback.getCriterionEvaluations().clear();
        for (CriterionEvaluation eval : evaluations) {
            feedback.addCriterionEvaluation(eval);
            totalScore += eval.getScore();
        }
        feedback.setTotalScore(totalScore);

        // Update submission status (Approval/Rejection stamping)
        submission.setStatus(status);
        submissionRepository.save(submission);

        AdviserFeedback savedFeedback = feedbackRepository.save(feedback);

        // Notify students in the group
        ProjectGroup group = submission.getGroup();
        group.getStudents().forEach(enrollment -> {
            if (enrollment.getStudent() != null) {
                notificationService.createNotification(enrollment.getStudent().getId(), 
                    "New feedback available for " + submission.getDeliverable().getName() + ". Status: " + status);
            }
        });

        auditService.log("EVALUATION_COMPLETED", String.valueOf(adviserId), "FEEDBACK", 
            "Evaluated submission " + submissionId + " with status " + status, "SYSTEM");

        return savedFeedback;
    }

    public List<Rubric> getAllRubrics() {
        return rubricRepository.findAll();
    }

    public Rubric createRubric(Rubric rubric) {
        rubric.setCreatedAt(LocalDateTime.now());
        return rubricRepository.save(rubric);
    }
}
