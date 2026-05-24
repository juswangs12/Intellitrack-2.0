package com.intellitrack.service;

import com.intellitrack.dto.StudentSubmissionParticipationDto;
import com.intellitrack.entity.*;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.StudentSubmissionParticipationRepository;
import com.intellitrack.repository.StudentEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
