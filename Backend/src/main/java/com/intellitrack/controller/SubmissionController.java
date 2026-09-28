package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.GoogleDocSubmissionRequest;
import com.intellitrack.dto.IndividualStudentSubmissionDto;
import com.intellitrack.dto.SubmissionDto;
import com.intellitrack.dto.SubmissionVersionDto;
import com.intellitrack.dto.StudentEnrollmentDto;
import com.intellitrack.entity.ProjectGroup;
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
import com.intellitrack.service.FileStorageService;
import com.intellitrack.service.ParticipationTrackingService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SubmissionVersionRepository submissionVersionRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private com.intellitrack.service.SubmissionService submissionService;

    @Autowired
    private ParticipationTrackingService participationTrackingService;

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getSubmissionsByGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        log.info("=== getSubmissionsByGroup called for groupId: {} ===", groupId);
        try {
            Long studentId = (Long) authentication.getPrincipal();
            List<SubmissionDto> dtos = submissionRepository.findByStudentIdAndGroupId(studentId, groupId).stream()
                    .map(this::toDto)
                    .toList();
            log.info("=== getSubmissionsByGroup returning {} submissions ===", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getSubmissionsByGroup ===", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/deliverable/{deliverableId}")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getSubmissionsByDeliverable(
            @PathVariable Long deliverableId) {
        log.info("=== getSubmissionsByDeliverable called for deliverableId: {} ===", deliverableId);
        try {
            List<SubmissionDto> dtos = submissionRepository.findByDeliverableId(deliverableId).stream()
                    .map(this::toDto)
                    .toList();
            log.info("=== getSubmissionsByDeliverable returning {} submissions ===", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getSubmissionsByDeliverable ===", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getPendingSubmissions() {
        log.info("=== getPendingSubmissions called ===");
        try {
            List<SubmissionDto> dtos = submissionRepository.findByStatusIn(
                    List.of(SubmissionStatus.PENDING)).stream().map(this::toDto).toList();
            log.info("=== getPendingSubmissions returning {} submissions ===", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getPendingSubmissions ===", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/adviser/{adviserId}/pending")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getAdviserPendingSubmissions(@PathVariable Long adviserId) {
        log.info("=== getAdviserPendingSubmissions called for adviserId: {} ===", adviserId);
        try {
            List<SubmissionDto> dtos = submissionRepository.findByGroupAdviserId(adviserId).stream()
                    .filter(s -> s.getStatus() == SubmissionStatus.PENDING)
                    .map(this::toDto)
                    .toList();
            log.info("=== getAdviserPendingSubmissions returning {} submissions ===", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getAdviserPendingSubmissions ===", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id) throws IOException {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAuthorized = false;
        String userRole = currentUser.getRole();

        if ("administrator".equals(userRole) || "coordinator".equals(userRole)) {
            isAuthorized = true;
        } else if ("adviser".equals(userRole)) {
            ProjectGroup group = submission.getGroup();
            if (group != null && group.getAdviser() != null
                    && group.getAdviser().getId().equals(currentUser.getId())) {
                isAuthorized = true;
            }
        } else if ("student".equals(userRole)) {
            ProjectGroup group = submission.getGroup();
            if (group != null && currentUser.getGroup() != null
                    && currentUser.getGroup().getId().equals(group.getId())) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to download this file");
        }

        String fileName = extractFileName(submission.getFileUrl());

        byte[] data = fileStorageService.getFile(fileName);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewSubmission(@PathVariable Long id) throws IOException {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAuthorized = false;
        String userRole = currentUser.getRole();
        if ("administrator".equals(userRole) || "coordinator".equals(userRole)) {
            isAuthorized = true;
        } else if ("adviser".equals(userRole)) {
            ProjectGroup group = submission.getGroup();
            if (group != null && group.getAdviser() != null
                    && group.getAdviser().getId().equals(currentUser.getId())) {
                isAuthorized = true;
            }
        } else if ("student".equals(userRole)) {
            ProjectGroup group = submission.getGroup();
            if (group != null && currentUser.getGroup() != null
                    && currentUser.getGroup().getId().equals(group.getId())) {
                isAuthorized = true;
            }
        }
        if (!isAuthorized) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to view this file");
        }

        String previewFileName = extractFileName(submission.getFileUrl());
        byte[] previewData = fileStorageService.getFile(previewFileName);
        ByteArrayResource previewResource = new ByteArrayResource(previewData);

        String lower = previewFileName.toLowerCase();
        MediaType mediaType;
        if (lower.endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
        } else if (lower.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + previewFileName + "\"")
                .contentType(mediaType)
                .contentLength(previewData.length)
                .body(previewResource);
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

    @PostMapping("/google-doc")
    public ResponseEntity<ApiResponse<SubmissionDto>> submitGoogleDoc(
            @RequestBody GoogleDocSubmissionRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        String ipAddress = httpServletRequest.getRemoteAddr();

        // Validate Google Doc URL
        String url = request.googleDocUrl();
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Google Doc URL is required"));
        }
        
        boolean isValid = url.matches("^https?://(docs\\.google\\.com/document/d/|drive\\.google\\.com/file/d/).*$");
        if (!isValid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid URL. Must be a Google Docs or Google Drive file URL."));
        }

        try {
            Submission submission = submissionService.submitGoogleDoc(
                    request.googleDocUrl(),
                    request.groupId(),
                    request.deliverableId(),
                    request.notes(),
                    userId,
                    ipAddress
            );
            return ResponseEntity.ok(ApiResponse.success(toDto(submission)));
        } catch (Exception e) {
            log.error("Error submitting Google Doc", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to submit Google Doc: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<SubmissionDto>> uploadSubmission(
            @RequestParam(value = "groupId", required = false) String groupIdStr,
            @RequestParam(value = "deliverableId", required = false) String deliverableIdStr,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            HttpServletRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        String ipAddress = request.getRemoteAddr();

        try {
            Submission submission = submissionService.submitFile(
                    file, groupIdStr, deliverableIdStr, notes, userId, ipAddress);
            return ResponseEntity.ok(ApiResponse.success(toDto(submission)));
        } catch (Exception e) {
            log.error("=== ERROR IN SUBMISSION UPLOAD ===", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getAllSubmissions() {
        log.info("=== getAllSubmissions called ===");
        try {
            List<SubmissionDto> dtos = submissionRepository.findAll().stream()
                    .map(this::toDto)
                    .toList();
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getAllSubmissions ===", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/individual-students")
    public ResponseEntity<ApiResponse<List<IndividualStudentSubmissionDto>>> getAllIndividualStudentSubmissions() {
        log.info("=== getAllIndividualStudentSubmissions called ===");
        try {
            List<IndividualStudentSubmissionDto> dtos = participationTrackingService.getAllIndividualStudentSubmissions();
            log.info("=== getAllIndividualStudentSubmissions returning {} submissions ===", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getAllIndividualStudentSubmissions ===", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to load individual student submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/adviser/{adviserId}/individual-students")
    public ResponseEntity<ApiResponse<List<IndividualStudentSubmissionDto>>> getIndividualStudentSubmissionsByAdviser(@PathVariable Long adviserId) {
        log.info("=== getIndividualStudentSubmissionsByAdviser called for adviserId: {} ===", adviserId);
        try {
            List<IndividualStudentSubmissionDto> dtos = participationTrackingService.getIndividualStudentSubmissionsByAdviser(adviserId);
            log.info("=== getIndividualStudentSubmissionsByAdviser returning {} submissions ===", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getIndividualStudentSubmissionsByAdviser ===", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to load individual student submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/adviser/{adviserId}/all")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> getAdviserAllSubmissions(@PathVariable Long adviserId) {
        log.info("=== getAdviserAllSubmissions called for adviserId: {} ===", adviserId);
        try {
            List<SubmissionDto> dtos = submissionRepository.findByGroupAdviserId(adviserId).stream()
                    .map(this::toDto)
                    .toList();
            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("=== ERROR in getAdviserAllSubmissions ===", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load submissions: " + e.getMessage()));
        }
    }

    @GetMapping("/tracking-summary")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTrackingSummary() {
        log.info("=== getTrackingSummary called ===");
        try {
            List<ProjectGroup> allGroups = projectGroupRepository.findAllWithAllRelations();
            List<Submission> allSubmissions = submissionRepository.findAll();

            Map<String, List<Map<String, Object>>> subjectGroups = new java.util.LinkedHashMap<>();

            for (ProjectGroup group : allGroups) {
                String subjectCode = "Unknown";

                var students = group.getStudents();
                if (students != null && !students.isEmpty()) {
                    var firstEnrollment = students.get(0);
                    var cs = firstEnrollment.getClassSection();
                    if (cs != null && cs.getSubject() != null) {
                        subjectCode = cs.getSubject().getCode() != null ? cs.getSubject().getCode() : "Unknown";
                    }
                }

                List<Submission> groupSubs = allSubmissions.stream()
                        .filter(s -> s.getGroup() != null && s.getGroup().getId().equals(group.getId()))
                        .toList();

                long onTime = groupSubs.stream()
                        .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED_ON_TIME)
                        .count();
                long late = groupSubs.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED_LATE).count();
                long pending = groupSubs.stream().filter(s -> s.getStatus() == SubmissionStatus.PENDING).count();

                Map<String, Object> groupSummary = new java.util.LinkedHashMap<>();
                groupSummary.put("groupId", group.getId());
                groupSummary.put("groupCode", group.getCode());
                groupSummary.put("groupTitle", group.getTitle());
                groupSummary.put("adviser", group.getAdviser() != null
                        ? group.getAdviser().getFirstName() + " " + group.getAdviser().getLastName()
                        : null);
                groupSummary.put("studentCount", students != null ? students.size() : 0);
                groupSummary.put("onTime", onTime);
                groupSummary.put("late", late);
                groupSummary.put("pending", pending);

                subjectGroups.computeIfAbsent(subjectCode, k -> new ArrayList<>()).add(groupSummary);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (var subjectEntry : subjectGroups.entrySet()) {
                Map<String, Object> subjectMap = new java.util.LinkedHashMap<>();
                subjectMap.put("subjectCode", subjectEntry.getKey());
                subjectMap.put("groups", subjectEntry.getValue());
                result.add(subjectMap);
            }

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("=== ERROR in getTrackingSummary ===", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to build tracking summary: " + e.getMessage()));
        }
    }

    private SubmissionDto toDto(Submission submission) {
        Long groupId = submission.getGroup() != null ? submission.getGroup().getId() : null;
        Long studentId = submission.getStudent() != null ? submission.getStudent().getId() : null;
        String studentName = null;
        if (submission.getStudent() != null) {
            studentName = (submission.getStudent().getFirstName() != null ? submission.getStudent().getFirstName() : "") + " " + (submission.getStudent().getLastName() != null ? submission.getStudent().getLastName() : "");
        }
        Long deliverableId = submission.getDeliverable() != null ? submission.getDeliverable().getId() : null;
        String groupCode = null;
        String groupTitle = null;
        String deliverableName = null;
        String deliverableStage = null;
        LocalDateTime dueAt = null;
        Integer lateDays = null;

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

        if (deliverableId != null) {
            var deliverableOpt = deliverableRepository.findById(deliverableId);
            if (deliverableOpt.isPresent()) {
                deliverableName = deliverableOpt.get().getName();
                deliverableStage = deliverableOpt.get().getStage();
                dueAt = deliverableOpt.get().getDueAt();
                if (dueAt != null && submission.getSubmittedAt() != null) {
                    java.time.Duration duration = java.time.Duration.between(dueAt, submission.getSubmittedAt());
                    long daysLate = duration.toDays();
                    if (daysLate > 0) {
                        lateDays = (int) daysLate;
                    }
                }
            }
        }

        String subjectCode = null;
        String sectionName = null;
        if (!students.isEmpty()) {
            Long firstEnrollmentId = students.get(0).getId();
            var enrollmentOpt = studentEnrollmentRepository.findByIdWithSectionAndSubject(firstEnrollmentId);
            if (enrollmentOpt.isPresent()) {
                var cs = enrollmentOpt.get().getClassSection();
                if (cs != null) {
                    sectionName = cs.getSection();
                    if (cs.getSubject() != null)
                        subjectCode = cs.getSubject().getCode();
                }
            }
        }

        return new SubmissionDto(
                submission.getId(),
                groupId,
                groupCode,
                groupTitle,
                students,
                studentId,
                studentName,
                deliverableId,
                deliverableName,
                deliverableStage,
                submission.getStatus() == null ? null : submission.getStatus().normalize().name(),
                submission.getSubmittedAt(),
                submission.getVersionNumber(),
                submission.getNotes(),
                submission.getAiSummary(),
                submission.getAiHighlights(),
                subjectCode,
                sectionName,
                lateDays,
                submission.getGoogleDocUrl(),
                submission.getLastModifiedAt(),
                submission.getLastAnalyzedAt());
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
