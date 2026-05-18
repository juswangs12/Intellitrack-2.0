package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.SubmissionDto;
import com.intellitrack.dto.SubmissionVersionDto;
import com.intellitrack.dto.StudentEnrollmentDto;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.StudentEnrollment;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.SubmissionVersion;
import com.intellitrack.entity.User;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.service.AnomalyDetectionService;
import com.intellitrack.service.AuditService;
import com.intellitrack.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "http://localhost:3000")
public class SubmissionController {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Autowired
    private com.intellitrack.service.AISummaryEngine aiSummaryEngine;

    @Autowired
    private SubmissionVersionRepository submissionVersionRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getSubmissionsByGroup(@PathVariable Long groupId) {
        List<SubmissionDto> dtos = submissionRepository.findByGroupId(groupId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/deliverable/{deliverableId}")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getSubmissionsByDeliverable(@PathVariable Long deliverableId) {
        List<SubmissionDto> dtos = submissionRepository.findByDeliverableId(deliverableId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getPendingSubmissions() {
        // Return submissions that are SUBMITTED or UNDER_REVIEW as "pending" for the coordinator
        List<SubmissionDto> dtos = submissionRepository.findByStatusIn(
            List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.UPDATED)
        ).stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/adviser/{adviserId}/pending")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getAdviserPendingSubmissions(@PathVariable Long adviserId) {
        // Return submissions assigned to this adviser that are SUBMITTED or UPDATED
        List<SubmissionDto> dtos = submissionRepository.findByGroupAdviserId(adviserId).stream()
            .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.UPDATED)
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id) throws IOException {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        String fileName = extractFileName(submission.getFileUrl());

        byte[] data = fileStorageService.getFile(fileName);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<SubmissionVersionDto>>> listVersions(@PathVariable Long id) {
        List<SubmissionVersionDto> versions = submissionVersionRepository.findBySubmissionIdOrderByVersionNumberDesc(id)
                .stream()
                .map(version -> new SubmissionVersionDto(
                        version.getId(),
                        id,
                        version.getVersionNumber(),
                        version.getStatus(),
                        version.getSubmittedAt(),
                        version.getNotes(),
                        extractFileName(version.getFileUrl()),
                        version.getAiSummary(),
                        version.getAiHighlights()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadVersion(@PathVariable Long versionId) throws IOException {
        SubmissionVersion version = submissionVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission version not found"));

        String fileName = extractFileName(version.getFileUrl());
        byte[] data = fileStorageService.getFile(fileName);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

    @PostMapping("/upload")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<SubmissionDto>> uploadSubmission(
            @RequestParam("groupId") Long groupId,
            @RequestParam("deliverableId") Long deliverableId,
            @RequestParam("userId") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            HttpServletRequest request) {

        System.out.println("=== Starting submission upload ===");
        System.out.println("  GroupId: " + groupId);
        System.out.println("  DeliverableId: " + deliverableId);
        System.out.println("  UserId: " + userId);
        System.out.println("  File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");

        try {
            String ipAddress = request.getRemoteAddr();
            System.out.println("  IP Address: " + ipAddress);

            // Validate user exists first
            System.out.println("  Validating user...");
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            System.out.println("  ✅ User found: id=" + user.getId() + ", email=" + user.getEmail() + ", role=" + user.getRole());

            // Validate group exists
            System.out.println("  Validating group...");
            ProjectGroup hydratedGroup = projectGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));
            System.out.println("  ✅ Group found: id=" + hydratedGroup.getId() + ", name=" + hydratedGroup.getTitle());

            // Validate deliverable exists
            System.out.println("  Validating deliverable...");
            var deliverable = deliverableRepository.findById(deliverableId)
                    .orElseThrow(() -> new RuntimeException("Deliverable not found with id: " + deliverableId));
            System.out.println("  ✅ Deliverable found: id=" + deliverable.getId() + ", name=" + deliverable.getName());

            // Anomaly detection
            System.out.println("  Running anomaly detection...");
            anomalyDetectionService.detectSubmissionAnomaly(groupId, ipAddress);
            System.out.println("  ✅ Anomaly detection passed");

            // Store file
            System.out.println("  Storing file...");
            String fileUrl = fileStorageService.storeFile(file);
            System.out.println("  ✅ File stored at: " + fileUrl);

            // Find existing submission or create new
            System.out.println("  Looking for existing submission...");
            Optional<Submission> existingSub = submissionRepository.findByGroupIdAndDeliverableId(groupId, deliverableId);
            System.out.println("  Existing submission found: " + existingSub.isPresent());
            
            Submission submission;
            if (existingSub.isPresent()) {
                submission = existingSub.get();
                submission.setVersionNumber(submission.getVersionNumber() + 1);
                submission.setRevisionCount(submission.getRevisionCount() + 1);
                System.out.println("  Updating existing submission - new version: " + submission.getVersionNumber());
            } else {
                submission = new Submission();
                System.out.println("  Creating new submission");
                submission.setGroup(hydratedGroup);
                System.out.println("  Group set to: " + hydratedGroup.getId());
                submission.setDeliverable(deliverable);
                System.out.println("  Deliverable set");
                submission.setVersionNumber(1);
                submission.setRevisionCount(0);
            }

            submission.setStatus(SubmissionStatus.SUBMITTED);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setFileUrl(fileUrl);
            submission.setNotes(notes);
            System.out.println("  Submission details set - status: " + submission.getStatus());

            System.out.println("  Saving submission...");
            Submission saved = submissionRepository.save(submission);
            System.out.println("  ✅ Submission saved with id: " + saved.getId());

            SubmissionVersion version = new SubmissionVersion();
            version.setSubmission(saved);
            version.setVersionNumber(saved.getVersionNumber());
            version.setSubmittedAt(saved.getSubmittedAt());
            version.setStatus(saved.getStatus().name());
            version.setNotes(saved.getNotes());
            version.setFileUrl(saved.getFileUrl());
            System.out.println("  Saving submission version...");
            SubmissionVersion savedVersion = submissionVersionRepository.save(version);
            System.out.println("  ✅ Submission version saved with id: " + savedVersion.getId());

            // Trigger AI Summary Generation (stores both latest + per-version)
            System.out.println("  Triggering AI summary...");
            aiSummaryEngine.processDocument(saved, savedVersion.getId());
            System.out.println("  ✅ AI summary processing triggered");

            // Audit log
            System.out.println("  Logging audit...");
            auditService.log("FILE_UPLOAD", String.valueOf(userId), "SUBMISSION", 
                "Uploaded version " + submission.getVersionNumber() + " for deliverable " + deliverableId, ipAddress);
            
            System.out.println("=== Submission upload complete ===");
            return ResponseEntity.ok(ApiResponse.success(toDto(saved)));
        } catch (Exception e) {
            System.err.println("=== ERROR IN SUBMISSION UPLOAD ===");
            System.err.println("  Error type: " + e.getClass().getName());
            System.err.println("  Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return ResponseEntity.badRequest().body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    private SubmissionDto toDto(Submission submission) {
        var group = submission.getGroup();
        var deliverable = submission.getDeliverable();
        
        List<StudentEnrollmentDto> students = group != null && group.getStudents() != null ? 
            group.getStudents().stream()
                .map(e -> new StudentEnrollmentDto(
                        e.getId(),
                        e.getStudentId(),
                        e.getFullName(),
                        e.getEmail(),
                        e.getStudent() != null ? e.getStudent().getId() : null,
                        e.getClassSection() != null && e.getClassSection().getSubject() != null ? e.getClassSection().getSubject().getName() : null,
                        e.getClassSection() != null && e.getClassSection().getSubject() != null ? e.getClassSection().getSubject().getCode() : null,
                        e.getClassSection() != null ? e.getClassSection().getSection() : null))
                .toList() : 
            Collections.emptyList();

        return new SubmissionDto(
                submission.getId(),
                group == null ? null : group.getId(),
                group == null ? null : group.getCode(),
                group == null ? null : group.getTitle(),
                students,
                deliverable == null ? null : deliverable.getId(),
                deliverable == null ? null : deliverable.getName(),
                deliverable == null ? null : deliverable.getStage(),
                submission.getStatus() == null ? null : submission.getStatus().name(),
                submission.getSubmittedAt(),
                submission.getVersionNumber(),
                submission.getRevisionCount(),
                submission.getNotes(),
                submission.getAiSummary(),
                submission.getAiHighlights());
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null) {
            return "file";
        }
        int lastSlash = fileUrl.lastIndexOf('/');
        int lastBackslash = fileUrl.lastIndexOf('\\');
        int idx = Math.max(lastSlash, lastBackslash);
        return idx >= 0 ? fileUrl.substring(idx + 1) : fileUrl;
    }
}
