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
import org.springframework.security.core.Authentication;
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
        System.out.println("=== getSubmissionsByGroup called for groupId: " + groupId + " ===");
        try {
            List<SubmissionDto> dtos = submissionRepository.findByGroupId(groupId).stream()
                    .map(this::toDto)
                    .toList();
            System.out.println("=== getSubmissionsByGroup returning " + dtos.size() + " submissions ===");
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            System.err.println("=== ERROR in getSubmissionsByGroup ===");
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/deliverable/{deliverableId}")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getSubmissionsByDeliverable(
            @PathVariable Long deliverableId) {
        System.out.println("=== getSubmissionsByDeliverable called for deliverableId: " + deliverableId + " ===");
        try {
            List<SubmissionDto> dtos = submissionRepository.findByDeliverableId(deliverableId).stream()
                    .map(this::toDto)
                    .toList();
            System.out.println("=== getSubmissionsByDeliverable returning " + dtos.size() + " submissions ===");
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            System.err.println("=== ERROR in getSubmissionsByDeliverable ===");
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getPendingSubmissions() {
        System.out.println("=== getPendingSubmissions called ===");
        try {
            // Return submissions that are SUBMITTED or UNDER_REVIEW as "pending" for the
            // coordinator
            List<SubmissionDto> dtos = submissionRepository.findByStatusIn(
                    List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.UPDATED)).stream().map(this::toDto).toList();
            System.out.println("=== getPendingSubmissions returning " + dtos.size() + " submissions ===");
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            System.err.println("=== ERROR in getPendingSubmissions ===");
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/adviser/{adviserId}/pending")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getAdviserPendingSubmissions(@PathVariable Long adviserId) {
        System.out.println("=== getAdviserPendingSubmissions called for adviserId: " + adviserId + " ===");
        try {
            // Return submissions assigned to this adviser that are SUBMITTED or UPDATED
            List<SubmissionDto> dtos = submissionRepository.findByGroupAdviserId(adviserId).stream()
                    .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.UPDATED)
                    .map(this::toDto)
                    .toList();
            System.out.println("=== getAdviserPendingSubmissions returning " + dtos.size() + " submissions ===");
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            System.err.println("=== ERROR in getAdviserPendingSubmissions ===");
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
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
            @RequestParam(value = "groupId", required = false) String groupIdStr,
            @RequestParam(value = "deliverableId", required = false) String deliverableIdStr,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            HttpServletRequest request,
            Authentication authentication) {

        // Parse groupId and deliverableId safely
        Long groupId = null;
        Long deliverableId = null;
        
        try {
            if (groupIdStr != null && !groupIdStr.trim().isEmpty() && !"null".equals(groupIdStr.trim())) {
                groupId = Long.parseLong(groupIdStr.trim());
            }
            if (deliverableIdStr != null && !deliverableIdStr.trim().isEmpty() && !"null".equals(deliverableIdStr.trim())) {
                deliverableId = Long.parseLong(deliverableIdStr.trim());
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing groupId or deliverableId: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid group or deliverable ID"));
        }
        
        if (groupId == null || deliverableId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Group ID and Deliverable ID are required"));
        }

        System.out.println("=== UPLOAD METHOD REACHED! ===");

        // Derive userId from the verified JWT principal — never trust a client-supplied
        // value
        Long userId = (Long) authentication.getPrincipal();

        System.out.println("=== Starting submission upload ===");
        System.out.println("  [1] GroupId: " + groupId);
        System.out.println("  [2] DeliverableId: " + deliverableId);
        System.out.println("  [3] UserId: " + userId);
        System.out.println("  [4] File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");

        try {
            System.out.println("  [5] Getting IP address...");
            String ipAddress = request.getRemoteAddr();
            System.out.println("  [5] IP Address: " + ipAddress);

            // Validate user exists first
            System.out.println("  [6] Validating user...");
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            System.out.println("  ✅ [6] User found: id=" + user.getId() + ", email=" + user.getEmail() + ", role=" + user.getRole());

            // Validate group exists
            System.out.println("  [7] Validating group...");
            boolean groupExists = projectGroupRepository.existsById(groupId);
            if (!groupExists) {
                throw new RuntimeException("Group not found with id: " + groupId);
            }
            ProjectGroup groupRef = projectGroupRepository.getReferenceById(groupId);
            System.out.println("  ✅ [7] Group found: id=" + groupId);

            // Validate deliverable exists
            System.out.println("  [8] Validating deliverable...");
            boolean deliverableExists = deliverableRepository.existsById(deliverableId);
            if (!deliverableExists) {
                throw new RuntimeException("Deliverable not found with id: " + deliverableId);
            }
            var deliverableRef = deliverableRepository.getReferenceById(deliverableId);
            System.out.println("  ✅ [8] Deliverable found: id=" + deliverableId);

            // Anomaly detection
            System.out.println("  [9] Running anomaly detection...");
            anomalyDetectionService.detectSubmissionAnomaly(groupId, ipAddress);
            System.out.println("  ✅ [9] Anomaly detection passed");

            // Store file
            System.out.println("  [10] Storing file...");
            String fileUrl = fileStorageService.storeFile(file);
            System.out.println("  ✅ [10] File stored at: " + fileUrl);

            // Find existing submission or create new
            System.out.println("  [11] Looking for existing submission...");
            Optional<Submission> existingSub = submissionRepository.findByGroupIdAndDeliverableId(groupId, deliverableId);
            System.out.println("  ✅ [11] Existing submission found: " + existingSub.isPresent());

            Submission submission;
            if (existingSub.isPresent()) {
                submission = existingSub.get();
                submission.setVersionNumber(submission.getVersionNumber() + 1);
                submission.setRevisionCount(submission.getRevisionCount() + 1);
                System.out.println("  ✅ [12] Updating existing submission - new version: " + submission.getVersionNumber());
            } else {
                submission = new Submission();
                System.out.println("  ✅ [12] Creating new submission");
                submission.setGroup(groupRef);
                System.out.println("  ✅ [13] Group set to: " + groupId);
                submission.setDeliverable(deliverableRef);
                System.out.println("  ✅ [14] Deliverable set");
                submission.setVersionNumber(1);
                submission.setRevisionCount(0);
            }

            submission.setStatus(SubmissionStatus.SUBMITTED);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setFileUrl(fileUrl);
            submission.setNotes(notes);
            System.out.println("  ✅ [15] Submission details set - status: " + submission.getStatus());

            System.out.println("  [16] Saving submission...");
            Submission saved = submissionRepository.save(submission);
            System.out.println("  ✅ [16] Submission saved with id: " + saved.getId());

            SubmissionVersion version = new SubmissionVersion();
            version.setSubmission(saved);
            version.setVersionNumber(saved.getVersionNumber());
            version.setSubmittedAt(saved.getSubmittedAt());
            version.setStatus(saved.getStatus().name());
            version.setNotes(saved.getNotes());
            version.setFileUrl(saved.getFileUrl());
            System.out.println("  [17] Saving submission version...");
            SubmissionVersion savedVersion = submissionVersionRepository.save(version);
            System.out.println("  ✅ [17] Submission version saved with id: " + savedVersion.getId());

            // Trigger AI Summary Generation (stores both latest + per-version)
            System.out.println("  [18] Triggering AI summary...");
            try {
                aiSummaryEngine.processDocument(saved, savedVersion.getId());
                System.out.println("  ✅ [18] AI summary processing triggered");
            } catch (Exception aiEx) {
                System.err.println("  ⚠️ [18] AI summary failed (continuing): " + aiEx.getMessage());
                aiEx.printStackTrace();
            }

            // Audit log
            System.out.println("  [19] Logging audit...");
            auditService.log("FILE_UPLOAD", String.valueOf(userId), "SUBMISSION", 
                "Uploaded version " + submission.getVersionNumber() + " for deliverable " + deliverableId, ipAddress);

            System.out.println("  [20] Converting to DTO...");
            SubmissionDto dto = toDto(saved);
            System.out.println("  ✅ [20] DTO conversion complete");

            System.out.println("=== Submission upload complete ===");
            return ResponseEntity.ok(ApiResponse.success(dto));
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
        Long groupId = submission.getGroup() != null ? submission.getGroup().getId() : null;
        Long deliverableId = submission.getDeliverable() != null ? submission.getDeliverable().getId() : null;
        String groupCode = null;
        String groupTitle = null;
        String deliverableName = null;
        String deliverableStage = null;

        // Load group details if needed
        List<StudentEnrollmentDto> students = Collections.emptyList();
        if (groupId != null) {
            Optional<ProjectGroup> hydratedGroupOpt = projectGroupRepository.findByIdWithStudents(groupId);
            if (hydratedGroupOpt.isPresent()) {
                ProjectGroup hydratedGroup = hydratedGroupOpt.get();
                groupCode = hydratedGroup.getCode();
                groupTitle = hydratedGroup.getTitle();
                if (hydratedGroup.getStudents() != null) {
                    students = hydratedGroup.getStudents().stream()
                            .map(e -> new StudentEnrollmentDto(
                                    e.getId(),
                                    e.getStudentId(),
                                    e.getFullName(),
                                    e.getEmail(),
                                    e.getStudent() != null ? e.getStudent().getId() : null,
                                    null,
                                    null,
                                    null))
                            .toList();
                }
            }
        }
        
        // Load deliverable details if needed
        if (deliverableId != null) {
            var deliverableOpt = deliverableRepository.findById(deliverableId);
            if (deliverableOpt.isPresent()) {
                deliverableName = deliverableOpt.get().getName();
                deliverableStage = deliverableOpt.get().getStage();
            }
        }

        return new SubmissionDto(
                submission.getId(),
                groupId,
                groupCode,
                groupTitle,
                students,
                deliverableId,
                deliverableName,
                deliverableStage,
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
