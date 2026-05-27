package com.intellitrack.service;

import com.intellitrack.dto.IndividualStudentSubmissionDto;
import com.intellitrack.dto.StudentSubmissionParticipationDto;
import com.intellitrack.entity.*;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.StudentSubmissionParticipationRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParticipationTrackingService {

    private final StudentSubmissionParticipationRepository participationRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final DeadlineRepository deadlineRepository;

    public ParticipationTrackingService(
            StudentSubmissionParticipationRepository participationRepository,
            StudentEnrollmentRepository enrollmentRepository,
            DeadlineRepository deadlineRepository) {
        this.participationRepository = participationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.deadlineRepository = deadlineRepository;
    }

    @Transactional
    public List<StudentSubmissionParticipation> createParticipationsForSubmission(Submission submission, ProjectGroup group) {
        List<StudentSubmissionParticipation> participations = new ArrayList<>();
        
        for (StudentEnrollment enrollment : group.getStudents()) {
            if (enrollment.getStudent() == null) {
                System.out.println("Skipping participation for unlinked student: " + enrollment.getFullName());
                continue;
            }
            
            StudentSubmissionParticipation participation = new StudentSubmissionParticipation();
            participation.setSubmission(submission);
            participation.setStudent(enrollment.getStudent());
            participation.setEnrollment(enrollment);
            participation.setContributed(true);
            
            LocalDateTime submittedAt = submission.getSubmittedAt();
            if (submittedAt != null) {
                participation.setSubmittedAt(submittedAt);
                ParticipationStatus status = evaluateParticipationStatus(submittedAt, submission.getDeliverable());
                participation.setStatus(status);
            } else {
                participation.setStatus(ParticipationStatus.NOT_SUBMITTED);
            }
            
            participations.add(participationRepository.save(participation));
        }
        
        return participations;
    }

    public ParticipationStatus evaluateParticipationStatus(LocalDateTime submittedAt, Deliverable deliverable) {
        if (submittedAt == null) {
            return ParticipationStatus.NOT_SUBMITTED;
        }
        
        LocalDateTime deadline = null;
        var deadlineOpt = deadlineRepository.findByDeliverableId(deliverable.getId());
        if (deadlineOpt.isPresent()) {
            deadline = deadlineOpt.get().getDueAt();
        }
        
        if (deadline != null && submittedAt.isAfter(deadline)) {
            return ParticipationStatus.LATE;
        }
        
        return ParticipationStatus.ON_TIME;
    }

    public List<StudentSubmissionParticipationDto> getParticipationsBySubmission(Long submissionId) {
        return participationRepository.findBySubmission_Id(submissionId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<StudentSubmissionParticipationDto> getParticipationsByStudent(Long studentId) {
        return participationRepository.findByStudent_Id(studentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IndividualStudentSubmissionDto> getAllIndividualStudentSubmissions() {
        return participationRepository.findAllWithAllRelations()
                .stream()
                .map(this::toIndividualDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IndividualStudentSubmissionDto> getIndividualStudentSubmissionsByAdviser(Long adviserId) {
        return participationRepository.findByAdviserIdWithAllRelations(adviserId)
                .stream()
                .map(this::toIndividualDto)
                .toList();
    }

    private StudentSubmissionParticipationDto toDto(StudentSubmissionParticipation participation) {
        String subjectCode = null;
        String sectionName = null;
        
        if (participation.getEnrollment() != null && participation.getEnrollment().getClassSection() != null) {
            sectionName = participation.getEnrollment().getClassSection().getSection();
            if (participation.getEnrollment().getClassSection().getSubject() != null) {
                subjectCode = participation.getEnrollment().getClassSection().getSubject().getCode();
            }
        }
        
        return new StudentSubmissionParticipationDto(
                participation.getId(),
                participation.getSubmission().getId(),
                participation.getStudent().getId(),
                participation.getStudent().getFirstName() + " " + participation.getStudent().getLastName(),
                participation.getEnrollment().getId(),
                subjectCode,
                sectionName,
                participation.getStatus(),
                participation.isContributed(),
                participation.getSubmittedAt()
        );
    }

    private IndividualStudentSubmissionDto toIndividualDto(StudentSubmissionParticipation participation) {
        Long participationId = participation.getId();
        Long submissionId = null;
        Long studentId = null;
        String studentName = null;
        Long groupId = null;
        String groupCode = null;
        String groupTitle = null;
        Long deliverableId = null;
        String deliverableName = null;
        String stage = null;
        String submissionStatus = null;
        ParticipationStatus participationStatus = participation.getStatus();
        LocalDateTime submittedAt = participation.getSubmittedAt();
        String subjectCode = null;
        String sectionName = null;
        Integer lateDays = null;
        String fileUrl = null;
        String aiSummary = null;
        String aiHighlights = null;

        if (participation.getStudent() != null) {
            studentId = participation.getStudent().getId();
            studentName = participation.getStudent().getFirstName() + " " + participation.getStudent().getLastName();
        }

        if (participation.getSubmission() != null) {
            Submission submission = participation.getSubmission();
            submissionId = submission.getId();
            submissionStatus = submission.getStatus() != null ? submission.getStatus().name() : null;
            fileUrl = submission.getFileUrl();
            aiSummary = submission.getAiSummary();
            aiHighlights = submission.getAiHighlights();

            if (submission.getGroup() != null) {
                ProjectGroup group = submission.getGroup();
                groupId = group.getId();
                groupCode = group.getCode();
                groupTitle = group.getTitle();
            }

            if (submission.getDeliverable() != null) {
                Deliverable deliverable = submission.getDeliverable();
                deliverableId = deliverable.getId();
                deliverableName = deliverable.getName();
                stage = deliverable.getStage();

                if (deliverable.getDueAt() != null && submission.getSubmittedAt() != null) {
                    Duration duration = Duration.between(deliverable.getDueAt(), submission.getSubmittedAt());
                    long daysLate = duration.toDays();
                    if (daysLate > 0) {
                        lateDays = (int) daysLate;
                    }
                }
            }
        }

        if (participation.getEnrollment() != null && participation.getEnrollment().getClassSection() != null) {
            sectionName = participation.getEnrollment().getClassSection().getSection();
            if (participation.getEnrollment().getClassSection().getSubject() != null) {
                subjectCode = participation.getEnrollment().getClassSection().getSubject().getCode();
            }
        }

        return new IndividualStudentSubmissionDto(
                participationId,
                submissionId,
                studentId,
                studentName,
                groupId,
                groupCode,
                groupTitle,
                deliverableId,
                deliverableName,
                stage,
                submissionStatus,
                participationStatus,
                submittedAt,
                subjectCode,
                sectionName,
                lateDays,
                fileUrl,
                aiSummary,
                aiHighlights
        );
    }
}
