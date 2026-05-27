package com.intellitrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrack.dto.AIInsightResponseDto;
import com.intellitrack.dto.DeliverableSummaryRowDto;
import com.intellitrack.dto.RawSubmissionSummaryData;
import com.intellitrack.dto.SummaryInsightDto;
import com.intellitrack.entity.AiAnalysisStatus;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionVersion;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class AISummaryEngine {

    private static final Logger log = LoggerFactory.getLogger(AISummaryEngine.class);

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
        processDocument(submission, null, false);
    }

    public void processDocument(Submission submission, boolean forceRegenerate) {
        processDocument(submission, null, forceRegenerate);
    }

    public void generateSummaryAsync(Submission submission, SubmissionVersion version) {
        final Long submissionId = submission.getId();
        final Long submissionVersionId = version != null ? version.getId() : null;

        log.info("=== AISummaryEngine.generateSummaryAsync called ===");
        log.info("Submission ID: {}", submissionId);
        log.info("Submission version ID: {}", submissionVersionId);

        processDocument(submission, submissionVersionId, false);
    }

    public void processDocument(Submission submission, Long submissionVersionId, boolean forceRegenerate) {
        log.info("=== AISummaryEngine.processDocument called ===");
        log.info("Submission ID: {}", submission.getId());
        log.info("Submission version ID: {}", submissionVersionId);
        log.info("Force regenerate: {}", forceRegenerate);
        log.info("Submission submittedAt: {}", submission.getSubmittedAt());

        if (!forceRegenerate && submission.getAiStatus() == AiAnalysisStatus.COMPLETED 
                && submission.getAiGeneratedAt() != null
                && submission.getAiSummary() != null) {
            log.info("=== USING CACHED AI ANALYSIS - Skipping regeneration ===");
            return;
        }

        updateSubmissionStatus(submission.getId(), AiAnalysisStatus.PROCESSING);

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

        log.info("Deliverable dueAt: {}", dueAt);

        try {
            Map<String, Object> inputData = new java.util.HashMap<>();
            inputData.put("submissionId", submission.getId());
            inputData.put("groupTitle", groupTitle);
            inputData.put("deliverableName", deliverableName);
            inputData.put("deliverableStage", deliverableStage);
            inputData.put("submittedAt",
                    submission.getSubmittedAt() != null ? submission.getSubmittedAt().toString() : null);
            inputData.put("dueAt", dueAt);
            inputData.put("versionNumber", submission.getVersionNumber());
            inputData.put("notes", submission.getNotes() != null ? submission.getNotes() : "No notes provided");
            inputData.put("fileUrl", submission.getFileUrl());

            String inputJson = objectMapper.writeValueAsString(inputData);
            log.info("=== Sending structured input to AI ===");
            log.info(inputJson);

            final Long finalSubmissionId = submission.getId();

            log.info("=== GEMINI API CALLED ===");
            aiService.getCompletion(inputJson).subscribe(response -> {
                log.info("=== Received AI response for summary ===");
                log.info("Response: {}", response);
                try {
                    if (response == null || response.trim().isEmpty()) {
                        log.warn("AI response is null or empty, generating metadata analysis");
                        updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle,
                                deliverableName, deliverableStage);
                        return;
                    }
                    AIInsightResponseDto aiResult = objectMapper.readValue(response, AIInsightResponseDto.class);
                    updateSubmissionWithAi(finalSubmissionId, submissionVersionId, aiResult);
                } catch (Exception e) {
                    log.error("Error processing AI summary: {}", e.getMessage(), e);
                    updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle,
                            deliverableName, deliverableStage);
                }
            }, error -> {
                log.error("Error receiving AI response: {}", error.getMessage(), error);
                if (error.getMessage() != null && error.getMessage().contains("rate limit")) {
                    updateSubmissionStatus(finalSubmissionId, AiAnalysisStatus.RATE_LIMITED);
                } else {
                    updateSubmissionStatus(finalSubmissionId, AiAnalysisStatus.FAILED);
                }
                updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle, deliverableName,
                        deliverableStage);
            }, () -> {
                log.warn("=== AI service returned empty response, generating metadata analysis ===");
                updateSubmissionWithBasicMetadata(finalSubmissionId, submissionVersionId, groupTitle, deliverableName,
                        deliverableStage);
            });
        } catch (Exception e) {
            log.error("Error preparing AI input: {}", e.getMessage(), e);
            updateSubmissionStatus(submission.getId(), AiAnalysisStatus.FAILED);
            generateBasicMetadataAnalysis(submission, submissionVersionId, groupTitle, deliverableName,
                    deliverableStage);
        }
    }

    @Transactional
    public void updateSubmissionStatus(Long submissionId, AiAnalysisStatus status) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        submission.setAiStatus(status);
        submissionRepository.save(submission);
        log.info("=== AI ANALYSIS STATUS UPDATED: {} ===", status);
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
        submission.setAiStatus(AiAnalysisStatus.COMPLETED);
        submission.setAiGeneratedAt(LocalDateTime.now());
        
        if (aiResult.riskAssessment() != null) {
            submission.setAiRiskLevel(aiResult.riskAssessment().riskLevel());
        }

        submissionRepository.save(submission);
        log.info("=== AI ANALYSIS SAVED ===");

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
    public void updateSubmissionWithBasicMetadata(Long submissionId, Long submissionVersionId, String groupTitle,
            String deliverableName, String deliverableStage) {
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
                status);

        submission.setAiSummary(summary);
        submission.setAiHighlights("Deliverable submitted; awaiting adviser review and AI analysis.");
        
        if (submission.getAiStatus() == AiAnalysisStatus.PENDING) {
            submission.setAiStatus(AiAnalysisStatus.FAILED);
        }

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

    private void generateBasicMetadataAnalysis(Submission submission, Long submissionVersionId, String groupTitle,
            String deliverableName, String deliverableStage) {
        updateSubmissionWithBasicMetadata(submission.getId(), submissionVersionId, groupTitle, deliverableName,
                deliverableStage);
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
