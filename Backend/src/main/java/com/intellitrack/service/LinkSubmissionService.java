package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.dto.GoogleDocSubmissionRequest;
import com.intellitrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;

@Service
public class LinkSubmissionService {
    @org.springframework.beans.factory.annotation.Autowired private NotificationService notifications;
    @org.springframework.beans.factory.annotation.Autowired private com.intellitrack.service.SemesterService semesters;
    private final SubmissionRepository submissions;
    private final SubmissionVersionRepository versions;
    private final ProjectGroupRepository groups;
    private final DeliverableRepository deliverables;
    private final UserRepository users;
    private final ParticipationTrackingService participation;
    private final AuditService audit;
    public LinkSubmissionService(SubmissionRepository s,SubmissionVersionRepository v,ProjectGroupRepository g,DeliverableRepository d,UserRepository u,ParticipationTrackingService p,AuditService a){
        submissions=s; versions=v; groups=g; deliverables=d; users=u; participation=p; audit=a;
    }
    @Transactional
    public Long submit(Long userId, GoogleDocSubmissionRequest request, String verifiedUrl) {
        // Serializes a student's writes, including first submission, across server instances.
        var user=users.lockForSubmission(userId).orElseThrow();
        if(!"student".equals(user.getRole())) throw new AccessDeniedException("Only students can submit");
        var group=groups.findByIdWithStudents(request.groupId()).orElseThrow();
        if(group.getStudents().stream().noneMatch(e->e.getStudent()!=null && e.getStudent().getId().equals(userId)))
            throw new AccessDeniedException("You are not enrolled in this group");
        semesters.requireActive(group.getAcademicTerm());
        var d=deliverables.findById(request.deliverableId()).orElseThrow();
        if(!d.isActive()||d.isArchived()||d.getSubject()==null||group.getSubject()==null
            ||!d.getSubject().getId().equals(group.getSubject().getId())||!Objects.equals(d.getAcademicTerm(),group.getAcademicTerm()))
            throw new IllegalArgumentException("This requirement is not assigned to your group and academic term");
        var now=LocalDateTime.now(ZoneId.of("Asia/Manila"));
        var s=submissions.findByStudentIdAndGroupIdAndDeliverableId(userId,group.getId(),d.getId()).orElseGet(Submission::new);
        boolean fresh=s.getId()==null;
        int previousVersion=fresh?0:Math.max(1,Optional.ofNullable(s.getVersionNumber()).orElse(1));
        if(!fresh && s.getSubmittedAt()!=null) {
            // Preserve the last legacy link before its first replacement in this workflow.
            int previous=previousVersion;
            var snapshot=versions.findBySubmissionIdAndVersionNumber(s.getId(),previous).orElseGet(SubmissionVersion::new);
            snapshot.setSubmission(s);snapshot.setVersionNumber(previous);
            if(snapshot.getSubmittedAt()==null)snapshot.setSubmittedAt(s.getLastModifiedAt()==null?s.getSubmittedAt():s.getLastModifiedAt());
            if(snapshot.getGoogleDocUrl()==null)snapshot.setGoogleDocUrl(s.getGoogleDocUrl());
            if(snapshot.getStatus()==null)snapshot.setStatus(TrackingPolicy.status(snapshot.getSubmittedAt(),d.getDueAt(),now));
            if(snapshot.getNotes()==null)snapshot.setNotes(s.getNotes());
            versions.save(snapshot);
        }
        s.setStudent(user); s.setGroup(group); s.setDeliverable(d);
        if(s.getSubmittedAt()==null) s.setSubmittedAt(now);
        s.setLastModifiedAt(now); s.setGoogleDocUrl(verifiedUrl); s.setNotes(request.notes());
        s.setVersionNumber(previousVersion+1);
        s.setStatus(SubmissionStatus.valueOf(TrackingPolicy.status(s.getSubmittedAt(),d.getDueAt(),now)));
        s.setAiStatus(AiAnalysisStatus.PENDING); s.setAiSummary(null); s.setAiHighlights(null); s.setAiGeneratedAt(null);
        submissions.save(s);
        var version=new SubmissionVersion(); version.setSubmission(s); version.setVersionNumber(s.getVersionNumber());
        version.setSubmittedAt(now); version.setGoogleDocUrl(verifiedUrl); version.setNotes(request.notes());
        version.setStatus(TrackingPolicy.status(now,d.getDueAt(),now)); versions.save(version);
        participation.createParticipationsForSubmission(s,group,userId);
        if(group.getAdviser()!=null)notifications.createNotification(group.getAdviser().getId(),user.getFirstName()+" "+user.getLastName()+ (fresh?" submitted ":" replaced ")+d.getName()+" for "+group.getTitle()+".");
        audit.log("DOC_LINK_SUBMITTED",userId.toString(),"Submission","Submission "+s.getId()+", version "+s.getVersionNumber(),"WEB");
        return s.getId();
    }
}
