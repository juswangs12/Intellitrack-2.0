package com.intellitrack.service;

import com.intellitrack.dto.DeliverableSummaryRowDto;
import com.intellitrack.dto.RawSubmissionSummaryData;
import com.intellitrack.dto.SummaryInsightDto;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionVersion;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AISummaryEngine {

    @Autowired
    private AiService aiService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionVersionRepository submissionVersionRepository;

    /**
     * Generates an AI summary and highlights for a specific document submission.
     * This is intended to be called after a file is uploaded.
     */
    public void processDocument(Submission submission) {
        processDocument(submission, null);
    }

    public void processDocument(Submission submission, Long submissionVersionId) {
        System.out.println("=== AiSummaryEngine.processDocument called ===");
        System.out.println("Submission ID: " + submission.getId());
        System.out.println("Submission version ID: " + submissionVersionId);
        
        String prompt = String.format(
            "You are an academic document analyzer for IntelliTrack. Analyze the following deliverable submission details:\n" +
            "Group: %s\n" +
            "Deliverable Category: %s\n" +
            "Academic Stage: %s\n" +
            "Student Notes: %s\n" +
            "Filename: %s\n\n" +
            "Tasks:\n" +
            "1. CONCISE SUMMARY: Generate a 2-3 sentence professional summary of this academic work.\n" +
            "2. KEY OBJECTIVES: Identify the main objectives or research goals mentioned.\n" +
            "3. CRITICAL HIGHLIGHTS: List 3 significant sections or findings.\n" +
            "4. MISSING ELEMENTS: Briefly note if any standard sections for this document type seem missing.\n\n" +
            "Format your response as:\n" +
            "SUMMARY: [Your summary]\n" +
            "OBJECTIVES: [Point 1]; [Point 2]\n" +
            "HIGHLIGHTS: [Point 1]; [Point 2]; [Point 3]\n" +
            "MISSING: [Note]",
            submission.getGroup().getTitle(),
            submission.getDeliverable().getName(),
            submission.getDeliverable().getStage(),
            submission.getNotes() != null ? submission.getNotes() : "No notes provided",
            submission.getFileUrl()
        );

        System.out.println("Sending prompt to AI service...");
        aiService.getCompletion(prompt).subscribe(response -> {
            System.out.println("=== Received AI response for summary ===");
            System.out.println("Response: " + response);
            try {
                String summary = "";
                String highlights = "";

                if (response.contains("SUMMARY:") && response.contains("HIGHLIGHTS:")) {
                    summary = response.substring(response.indexOf("SUMMARY:") + 8, response.indexOf("OBJECTIVES:")).trim();
                    highlights = response.substring(response.indexOf("HIGHLIGHTS:") + 11).trim();
                    
                    // Store the full AI output in the summary field for now, or split if needed
                    submission.setAiSummary(summary);
                    submission.setAiHighlights(highlights);
                } else {
                    submission.setAiSummary(response);
                    submission.setAiHighlights("Highlights extraction in progress...");
                }

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
            }
        });
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

        String prompt = String.format(
            "Generate a professional insight summary for a capstone group named '%s'. " +
            "Stats: %d submitted, %d pending, %d late, %d total revisions. " +
            "Provide a 1-2 sentence detailed observation about their progress.",
            rawData.groupTitle(), submitted, pending, late, revisions
        );

        String detail;
        try {
            detail = aiService.getCompletion(prompt).block();
        } catch (Exception e) {
            detail = revisions > 0
                ? "Revision activity is active with " + revisions + " tracked update(s) across deliverables."
                : "No revision activity has been recorded yet, so the current submission pattern is mostly first-pass work.";
        }

        return new SummaryInsightDto(headline, detail);
    }
}
