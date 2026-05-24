package com.intellitrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrack.dto.AIInsightResponseDto;
import com.intellitrack.dto.DeliverableSummaryRowDto;
import com.intellitrack.dto.RawSubmissionSummaryData;
import com.intellitrack.dto.SummaryInsightDto;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionVersion;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class AISummaryEngine {

    @Autowired
    private AiService aiService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionVersionRepository submissionVersionRepository;
    
    @Autowired
    private ProjectGroupRepository projectGroupRepository;
    
    @Autowired
    private DeliverableRepository deliverableRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processDocument(Submission submission) {
        processDocument(submission, null);
    }

    public void generateSummaryAsync(Submission submission, SubmissionVersion version) {
        final Long submissionId = submission.getId();
        final Long submissionVersionId = version != null ? version.getId() : null;
        final Long groupId = submission.getGroup() != null ? submission.getGroup().getId() : null;
        final Long deliverableId = submission.getDeliverable() != null ? submission.getDeliverable().getId() : null;
        
        System.out.println("=== AiSummaryEngine.generateSummaryAsync called ===");
        System.out.println("Submission ID: " + submissionId);
        System.out.println("Submission version ID: " + submissionVersionId);
        
        processDocument(submission, submissionVersionId);
    }

    public void processDocument(Submission submission, Long submissionVersionId) {
        System.out.println("=== AiSummaryEngine.processDocument called ===");
        System.out.println("Submission ID: " + submission.getId());
        System.out.println("Submission version ID: " + submissionVersionId);
        System.out.println("Submission submittedAt: " + submission.getSubmittedAt());
        
        // Load actual group and deliverable to avoid LazyInitializationException
        final Long groupId = submission.getGroup() != null ? submission.getGroup().getId() : null;
        final Long deliverableId = submission.getDeliverable() != null ? submission.getDeliverable().getId() : null;
        
        final String groupTitle = (groupId != null)
                ? projectGroupRepository.findById(groupId).map(ProjectGroup::getTitle).orElse(null)
                : null;
        
        final Optional<Deliverable> deliverableOpt = (deliverableId != null)
                ? deliverableRepository.findById(deliverableId)
                : Optional.empty();
        
        final String deliverableName = deliverableOpt.map(Deliverable::getName).orElse(null);
        final String deliverableStage = deliverableOpt.map(Deliverable::getStage).orElse(null);
        final String dueAt = deliverableOpt.map(Deliverable::getDueAt).map(Object::toString).orElse(null);
        
        System.out.println("Deliverable dueAt: " + dueAt);
        
        try {
            Map<String, Object> inputData = new java.util.HashMap<>();
            inputData.put("submissionId", submission.getId());
            inputData.put("groupTitle", groupTitle);
            inputData.put("deliverableName", deliverableName);
            inputData.put("deliverableStage", deliverableStage);
            inputData.put("submittedAt", submission.getSubmittedAt() != null ? submission.getSubmittedAt().toString() : null);
            inputData.put("dueAt", dueAt);
            inputData.put("versionNumber", submission.getVersionNumber());
            inputData.put("notes", submission.getNotes() != null ? submission.getNotes() : "No notes provided");
            inputData.put("fileUrl", submission.getFileUrl());
            
            String inputJson = objectMapper.writeValueAsString(inputData);
            System.out.println("=== Sending structured input to AI ===");
            System.out.println(inputJson);
            
            final Long finalSubmissionId = submission.getId();
            
            aiService.getCompletion(inputJson).subscribe(response -> {
                System.out.println("=== Received AI response for summary ===");
                System.out.println("Response: " + response);
                try {
                    if (response == null || response.trim().isEmpty()) {
                        System.out.println("AI response is null or empty, generating metadata analysis");
                        updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle, deliverableName, deliverableStage);
                        return;
                    }
                    AIInsightResponseDto aiResult = objectMapper.readValue(response, AIInsightResponseDto.class);
                    updateSubmissionWithAi(finalSubmissionId, submissionVersionId, aiResult);
                } catch (Exception e) {
                    System.err.println("Error processing AI summary: " + e.getMessage());
                    e.printStackTrace();
                    updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle, deliverableName, deliverableStage);
                }
            }, error -> {
                System.err.println("Error receiving AI response: " + error.getMessage());
                error.printStackTrace();
                updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle, deliverableName, deliverableStage);
            }, () -> {
                System.out.println("=== AI service returned empty response, generating metadata analysis ===");
                updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle, deliverableName, deliverableStage);
            });
        } catch (Exception e) {
            System.err.println("Error preparing AI input: " + e.getMessage());
            e.printStackTrace();
            generateBasicMetadataAnalysis(submission, submissionVersionId, groupTitle, deliverableName, deliverableStage);
        }
    }

    @Transactional
    public void updateSubmissionWithAi(Long submissionId, Long submissionVersionId, AIInsightResponseDto aiResult) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        
        submission.setAiSummary(aiResult.submissionAnalysis().summary());
        
        String highlights = "";
        if (aiResult.issuesFound() != null && !aiResult.issuesFound().isEmpty()) {
            try {
                highlights = objectMapper.writeValueAsString(aiResult.issuesFound());
            } catch (Exception e) {
                highlights = "";
            }
        }
        submission.setAiHighlights(highlights);
        
        submissionRepository.save(submission);
        
        if (submissionVersionId != null) {
            SubmissionVersion version = submissionVersionRepository.findById(submissionVersionId).orElse(null);
            if (version != null) {
                version.setAiSummary(submission.getAiSummary());
                version.setAiHighlights(submission.getAiHighlights());
                submissionVersionRepository.save(version);
            }
        }
    }

    @Transactional
    public void updateSubmissionWithBasicMetadata(Long submissionId, Long submissionVersionId, String groupTitle, String deliverableName, String deliverableStage) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        
        String status = submission.getStatus() != null ? submission.getStatus().name().replace("_", " ") : "Pending";
        int version = submission.getVersionNumber() != null ? submission.getVersionNumber() : 1;
        String safeDeliverableName = deliverableName != null ? deliverableName : "Deliverable";
        String safeDeliverableStage = deliverableStage != null ? deliverableStage : "Unknown";

        String summary = String.format(
            "%s (Version %d) has been submitted successfully. Stage: %s. Current status: %s. The submission is queued for adviser review and AI analysis.",
            safeDeliverableName,
            version,
            safeDeliverableStage,
            status
        );

        submission.setAiSummary(summary);
        submission.setAiHighlights("Deliverable submitted; awaiting adviser review and AI analysis.");
        
        submissionRepository.save(submission);
        
        if (submissionVersionId != null) {
            SubmissionVersion versionObj = submissionVersionRepository.findById(submissionVersionId).orElse(null);
            if (versionObj != null) {
                versionObj.setAiSummary(submission.getAiSummary());
                versionObj.setAiHighlights(submission.getAiHighlights());
                submissionVersionRepository.save(versionObj);
            }
        }
    }

    private void generateBasicMetadataAnalysis(Submission submission, Long submissionVersionId, String groupTitle, String deliverableName, String deliverableStage) {
        updateSubmissionWithBasicMetadata(submission.getId(), submissionVersionId, groupTitle, deliverableName, deliverableStage);
    }

    public SummaryInsightDto generate(RawSubmissionSummaryData rawData) {
        long submitted = rawData.deliverables().stream()
                .filter(item -> "SUBMITTED".equals(item.status()) || "UPDATED".equals(item.status()))
                .count();
        long late = rawData.deliverables().stream()
                .filter(item -> "LATE".equals(item.status()))
                .count();
        long pending = rawData.deliverables().stream()
                .filter(item -> "PENDING".equals(item.status()))
                .count();
        int revisions = rawData.deliverables().stream().mapToInt(DeliverableSummaryRowDto::revisionCount).sum();

        String headline = rawData.groupTitle() + " has completed " + submitted + " deliverable(s), with " + pending
                + " pending and " + late + " late item(s).";

        String detail = revisions > 0
                ? "Revision activity is active with " + revisions + " tracked update(s) across deliverables."
                : "No revision activity has been recorded yet, so the current submission pattern is mostly first-pass work.";

        return new SummaryInsightDto(headline, detail);
    }
}
