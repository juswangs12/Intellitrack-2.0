package com.intellitrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrack.dto.DeliverableSummaryRowDto;
import com.intellitrack.dto.RawSubmissionSummaryData;
import com.intellitrack.dto.SummaryInsightDto;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionVersion;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AISummaryEngine {

    @Autowired
    private AiService aiService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionVersionRepository submissionVersionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processDocument(Submission submission) {
        processDocument(submission, null);
    }

    public void processDocument(Submission submission, Long submissionVersionId) {
        System.out.println("=== AiSummaryEngine.processDocument called ===");
        System.out.println("Submission ID: " + submission.getId());
        System.out.println("Submission version ID: " + submissionVersionId);
        
        try {
            Map<String, Object> inputData = Map.of(
                "submissionId", submission.getId(),
                "groupTitle", submission.getGroup().getTitle(),
                "deliverableName", submission.getDeliverable().getName(),
                "deliverableStage", submission.getDeliverable().getStage(),
                "submittedAt", submission.getSubmittedAt() != null ? submission.getSubmittedAt().toString() : null,
                "dueAt", submission.getDeliverable().getDueAt() != null ? submission.getDeliverable().getDueAt().toString() : null,
                "versionNumber", submission.getVersionNumber(),
                "notes", submission.getNotes() != null ? submission.getNotes() : "No notes provided",
                "fileUrl", submission.getFileUrl()
            );
            
            String inputJson = objectMapper.writeValueAsString(inputData);
            System.out.println("=== Sending structured input to AI ===");
            System.out.println(inputJson);
            
            aiService.getCompletion(inputJson).subscribe(response -> {
                System.out.println("=== Received AI response for summary ===");
                System.out.println("Response: " + response);
                try {
                    Map<String, Object> aiResult = objectMapper.readValue(response, Map.class);
                    String submissionAnalysis = (String) aiResult.getOrDefault("submissionAnalysis", "");
                    submission.setAiSummary(submissionAnalysis);
                    
                    String highlights = "";
                    Object issuesFound = aiResult.get("issuesFound");
                    if (issuesFound != null) {
                        highlights = objectMapper.writeValueAsString(issuesFound);
                    }
                    submission.setAiHighlights(highlights);

                    System.out.println("Saving submission with AI summary...");
                    submissionRepository.save(submission);
                    System.out.println("Submission saved with AI summary");

                    if (submissionVersionId != null) {
                        System.out.println("Saving submission version with AI summary...");
                        SubmissionVersion version = submissionVersionRepository.findById(submissionVersionId).orElse(null);
                        if (version != null) {
                            version.setAiSummary(submission.getAiSummary());
                            version.setAiHighlights(submission.getAiHighlights());
                            submissionVersionRepository.save(version);
                            System.out.println("Submission version saved with AI summary");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing AI summary: " + e.getMessage());
                    e.printStackTrace();
                    submission.setAiSummary(response);
                    submission.setAiHighlights("Highlights extraction in progress...");
                    submissionRepository.save(submission);
                }
            });
        } catch (Exception e) {
            System.err.println("Error preparing AI input: " + e.getMessage());
            e.printStackTrace();
        }
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
