package com.intellitrack.service;

import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.SubmissionVersion;
import com.intellitrack.entity.User;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.SubmissionVersionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final DeliverableRepository deliverableRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AISummaryEngine aiSummaryEngine;
    private final AuditService auditService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final CacheManager cacheManager;
    private final DeadlineRepository deadlineRepository;
    private final NotificationService notificationService;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            SubmissionVersionRepository submissionVersionRepository,
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            AISummaryEngine aiSummaryEngine,
            AuditService auditService,
            AnomalyDetectionService anomalyDetectionService,
            CacheManager cacheManager,
            DeadlineRepository deadlineRepository,
            NotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.submissionVersionRepository = submissionVersionRepository;
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.aiSummaryEngine = aiSummaryEngine;
        this.auditService = auditService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.cacheManager = cacheManager;
        this.deadlineRepository = deadlineRepository;
        this.notificationService = notificationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Submission submitFile(
            MultipartFile file,
            String groupIdStr,
            String deliverableIdStr,
            String notes,
            Long userId,
            String ipAddress) throws Exception {
        
        Long groupId = null;
        Long deliverableId = null;
        
        if (groupIdStr != null && !"null".equals(groupIdStr) && !groupIdStr.trim().isEmpty()) {
            try {
                groupId = Long.parseLong(groupIdStr);
            } catch (NumberFormatException e) {
                groupId = null;
            }
        }
        
        if (deliverableIdStr != null && !"null".equals(deliverableIdStr) && !deliverableIdStr.trim().isEmpty()) {
            try {
                deliverableId = Long.parseLong(deliverableIdStr);
            } catch (NumberFormatException e) {
                deliverableId = null;
            }
        }

        if (groupId == null) {
            throw new RuntimeException("groupId is required");
        }
        if (deliverableId == null) {
            throw new RuntimeException("deliverableId is required");
        }

        evictGroupCache(groupId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        boolean groupExists = projectGroupRepository.existsById(groupId);
        if (!groupExists) {
            throw new RuntimeException("Group not found with id: " + groupId);
        }
        var groupRef = projectGroupRepository.getReferenceById(groupId);

        boolean deliverableExists = deliverableRepository.existsById(deliverableId);
        if (!deliverableExists) {
            throw new RuntimeException("Deliverable not found with id: " + deliverableId);
        }
        var deliverableRef = deliverableRepository.getReferenceById(deliverableId);

        anomalyDetectionService.detectSubmissionAnomaly(groupId, ipAddress);

        Optional<Submission> existingSub = submissionRepository.findByGroupIdAndDeliverableId(groupId, deliverableId);

        Submission submission;
        if (existingSub.isPresent()) {
            submission = existingSub.get();
            submission.setVersionNumber(submission.getVersionNumber() + 1);
        } else {
            submission = new Submission();
            submission.setGroup(groupRef);
            submission.setDeliverable(deliverableRef);
            submission.setVersionNumber(1);
            submission.setAiSummary("");
            submission.setAiHighlights("");
        }

        String fileUrl = fileStorageService.storeFile(file);
        submission.setFileUrl(fileUrl);

        if (notes != null && !notes.trim().isEmpty()) {
            submission.setNotes(notes);
        }
        submission.setSubmittedAt(LocalDateTime.now());

        Optional<Deadline> deadlineOpt = deadlineRepository.findByDeliverableId(deliverableId);
        if (deadlineOpt.isPresent()) {
            Deadline deadline = deadlineOpt.get();
            if (submission.getSubmittedAt().isAfter(deadline.getDueAt())) {
                submission.setStatus(SubmissionStatus.SUBMITTED_LATE);
            } else {
                submission.setStatus(SubmissionStatus.SUBMITTED_ON_TIME);
            }
        } else {
            submission.setStatus(SubmissionStatus.SUBMITTED_ON_TIME);
        }

        Submission savedSubmission = submissionRepository.save(submission);

        SubmissionVersion version = new SubmissionVersion();
        version.setSubmission(savedSubmission);
        version.setVersionNumber(savedSubmission.getVersionNumber());
        version.setFileUrl(fileUrl);
        version.setNotes(notes);
        version.setSubmittedAt(LocalDateTime.now());
        version.setStatus(savedSubmission.getStatus().name());
        version.setAiSummary(savedSubmission.getAiSummary());
        version.setAiHighlights(savedSubmission.getAiHighlights());
        submissionVersionRepository.save(version);

        aiSummaryEngine.generateSummaryAsync(savedSubmission, version);

        auditService.log(
                "FILE_UPLOAD",
                String.valueOf(userId),
                "Submission",
                "Uploaded file: " + file.getOriginalFilename() + " for deliverable " + deliverableId,
                ipAddress
        );

        ProjectGroup group = projectGroupRepository.findById(groupId).orElse(null);
        if (group != null) {
            String statusText = savedSubmission.getStatus() == SubmissionStatus.SUBMITTED_ON_TIME ? "on time" : "late";
            String message = "New submission received: " + deliverableRef.getName() + " (submitted " + statusText + ") by Group " + group.getCode();
            
            if (group.getAdviser() != null) {
                notificationService.createNotification(group.getAdviser().getId(), message);
            }
            
            userRepository.findByRole("COORDINATOR").forEach(coordinator -> {
                notificationService.createNotification(coordinator.getId(), message);
            });
        }

        return savedSubmission;
    }

    private void evictGroupCache(Long groupId) {
        var cache = cacheManager.getCache("submissionSummaries");
        if (cache != null) {
            cache.evict(groupId);
        }
    }
}
