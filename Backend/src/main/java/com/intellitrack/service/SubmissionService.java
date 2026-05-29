package com.intellitrack.service;

import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.entity.SubmissionVersion;
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
    private final NotificationService notificationService;
    private final ParticipationTrackingService participationTrackingService;

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
            NotificationService notificationService,
            ParticipationTrackingService participationTrackingService) {
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
        this.notificationService = notificationService;
        this.participationTrackingService = participationTrackingService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Submission submitFile(
            MultipartFile file,
            String groupIdStr,
            String deliverableIdStr,
            String notes,
            Long userId,
            String ipAddress) throws Exception {

        final Long groupId;
        final Long deliverableId;

        if (groupIdStr != null && !"null".equals(groupIdStr) && !groupIdStr.trim().isEmpty()) {
            try {
                groupId = Long.parseLong(groupIdStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid groupId");
            }
        } else {
            throw new RuntimeException("groupId is required");
        }

        if (deliverableIdStr != null && !"null".equals(deliverableIdStr) && !deliverableIdStr.trim().isEmpty()) {
            try {
                deliverableId = Long.parseLong(deliverableIdStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid deliverableId");
            }
        } else {
            throw new RuntimeException("deliverableId is required");
        }

        evictGroupCache(groupId);

        // Validate user exists before proceeding
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        final ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        final var deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new RuntimeException("Deliverable not found with id: " + deliverableId));

        // Validate that group and deliverable belong to the same subject
        if (group.getSubject() == null) {
            throw new RuntimeException("Group has no subject assigned");
        }
        if (deliverable.getSubject() == null) {
            throw new RuntimeException("Deliverable has no subject assigned");
        }
        if (!group.getSubject().getId().equals(deliverable.getSubject().getId())) {
            throw new RuntimeException(
                String.format("Group %s is for subject %s, but deliverable %s is for subject %s",
                    group.getCode(),
                    group.getSubject().getCode(),
                    deliverable.getName(),
                    deliverable.getSubject().getCode()));
        }

        var groupRef = group;
        var deliverableRef = deliverable;

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

        LocalDateTime dueAt = deliverableRef.getDueAt();
        if (dueAt != null && submission.getSubmittedAt().isAfter(dueAt)) {
            submission.setStatus(SubmissionStatus.SUBMITTED_LATE);
        } else {
            submission.setStatus(SubmissionStatus.SUBMITTED_ON_TIME);
        }

        Submission savedSubmission = submissionRepository.save(submission);
        
        ProjectGroup hydratedGroup = projectGroupRepository.findByIdWithStudents(groupId).orElse(groupRef);
        participationTrackingService.createParticipationsForSubmission(savedSubmission, hydratedGroup, userId);

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
                ipAddress);

        ProjectGroup notificationGroup = projectGroupRepository.findById(groupId).orElse(null);
        if (notificationGroup != null) {
            String statusText = savedSubmission.getStatus() == SubmissionStatus.SUBMITTED_ON_TIME ? "on time" : "late";
            String message = "New submission received: " + deliverableRef.getName() + " (submitted " + statusText
                    + ") by Group " + notificationGroup.getCode();

            if (notificationGroup.getAdviser() != null) {
                notificationService.createNotification(notificationGroup.getAdviser().getId(), message);
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
