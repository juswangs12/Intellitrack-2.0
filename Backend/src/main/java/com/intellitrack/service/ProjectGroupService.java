package com.intellitrack.service;

import com.intellitrack.dto.GroupRequest;
import com.intellitrack.dto.ProjectGroupDTO;
import com.intellitrack.entity.*;
import com.intellitrack.exception.ResourceNotFoundException;
import com.intellitrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class ProjectGroupService {
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    @org.springframework.beans.factory.annotation.Autowired private com.intellitrack.service.SemesterService semesters;
    @org.springframework.beans.factory.annotation.Autowired private NotificationService notifications;
    private final ProjectGroupRepository groups;
    private final UserRepository users;
    private final StudentEnrollmentRepository enrollments;
    private final SubjectRepository subjects;
    private final SubmissionRepository submissions;
    @org.springframework.beans.factory.annotation.Autowired private SubmissionVersionRepository versions;
    @org.springframework.beans.factory.annotation.Autowired private SubmissionCommentRepository comments;
    @org.springframework.beans.factory.annotation.Autowired private StudentSubmissionParticipationRepository participations;
    @org.springframework.beans.factory.annotation.Autowired private AdviserFeedbackRepository feedback;
    @org.springframework.beans.factory.annotation.Autowired private ReminderLogRepository reminders;
    @org.springframework.beans.factory.annotation.Autowired private RiskAssessmentLogRepository risks;
    @org.springframework.beans.factory.annotation.Autowired private DeadlineEmailRepository deadlineEmails;
    @org.springframework.beans.factory.annotation.Autowired private AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired private AccessService access;

    public ProjectGroupService(ProjectGroupRepository groups, UserRepository users,
            StudentEnrollmentRepository enrollments, SubjectRepository subjects, SubmissionRepository submissions) {
        this.groups=groups; this.users=users; this.enrollments=enrollments;
        this.subjects=subjects; this.submissions=submissions;
    }

    public List<ProjectGroupDTO> getAllGroups() {
        var archived=semesters.archivedTerms();
        return groups.findAllWithAllRelations().stream().map(g->new ProjectGroupDTO(g).withSemesterArchived(archived.contains(g.getAcademicTerm()))).toList();
    }

    public ProjectGroupDTO createGroup(GroupRequest request) {
        var group=new ProjectGroup();
        group.setCode(required(request.getCode(), "Group code"));
        group.setTitle(required(request.getTitle(), "Group title"));
        group.setAcademicTerm(TrackingPolicy.term(request.getAcademicTerm()));
        if(group.getAcademicTerm()==null) throw new IllegalArgumentException("Choose a school year and semester");
        semesters.requireActive(group.getAcademicTerm());
        group.setSubject(subject(request.getSubjectCode()));
        checkCode(group);
        return new ProjectGroupDTO(groups.save(group));
    }

    public ProjectGroupDTO updateGroup(Long id, GroupRequest request) {
        var group=group(id);semesters.requireActive(group.getAcademicTerm());
        if(request.getAcademicTerm()!=null && !Objects.equals(group.getAcademicTerm(),TrackingPolicy.term(request.getAcademicTerm())))
            throw new IllegalArgumentException("Use Academic setup to assign a legacy term, or carry the team to a new term");
        if(request.getCode()!=null) group.setCode(required(request.getCode(),"Group code"));
        if(request.getTitle()!=null) group.setTitle(required(request.getTitle(),"Group title"));
        if(request.getSubjectCode()!=null) {
            var target=subject(request.getSubjectCode());
            if(group.getSubject()!=null && !group.getSubject().getId().equals(target.getId())
                    && (!group.getStudents().isEmpty() || !submissions.findByGroupId(id).isEmpty()))
                throw new IllegalArgumentException("Carry the team forward to change subjects without losing history");
            group.setSubject(target);
        }
        checkCode(group);
        return new ProjectGroupDTO(groups.save(group));
    }

    public ProjectGroupDTO assignAdviser(Long groupId, Long adviserId) {
        var group=group(groupId);semesters.requireActive(group.getAcademicTerm());
        var adviser=users.findById(adviserId).orElseThrow(()->new ResourceNotFoundException("Adviser not found"));
        if(!"adviser".equals(adviser.getRole())) throw new IllegalArgumentException("Choose an adviser account");
        if(group.getAdviser()==null||!group.getAdviser().getId().equals(adviserId))
            group.getStudents().stream().filter(e->e.getStudent()!=null).map(e->e.getStudent().getId()).distinct().forEach(id->notifications.createNotification(id,"Your adviser for "+group.getTitle()+" ? "+group.getAcademicTerm()+" is now "+adviser.getFirstName()+" "+adviser.getLastName()+"."));
        if(group.getAdviser()==null||!group.getAdviser().getId().equals(adviserId)){
            notifications.createNotification(adviserId,"You were assigned to "+group.getTitle()+" — "+group.getAcademicTerm()+".");
            events.publishEvent(new ActivityEmailService.AdviserAssigned(groupId,adviserId,UUID.randomUUID().toString()));
        }
        group.setAdviser(adviser);
        return new ProjectGroupDTO(groups.save(group));
    }

    public ProjectGroupDTO assignStudents(Long groupId, List<Long> ids) {
        var group=group(groupId);semesters.requireActive(group.getAcademicTerm());
        if(group.getAcademicTerm()==null || group.getSubject()==null)
            throw new IllegalArgumentException("Assign the group's subject and academic term first");
        var selected=selected(ids);
        for(var enrollment:selected) {
            var section=enrollment.getClassSection();
            if(!Objects.equals(group.getAcademicTerm(),section.getAcademicTerm())
                    || !group.getSubject().getId().equals(section.getSubject().getId()))
                throw new IllegalArgumentException("Students and group must have the same subject and academic term");
            var other=groups.findByStudents_Id(enrollment.getId());
            if(other.isPresent() && !other.get().getId().equals(groupId))
                throw new IllegalArgumentException(enrollment.getFullName()+" already belongs to "+other.get().getTitle());
            if(group.getStudents().stream().noneMatch(e->e.getId().equals(enrollment.getId()))) {
                group.getStudents().add(enrollment);
                if(enrollment.getStudent()!=null) notifications.createNotification(enrollment.getStudent().getId(),"You were assigned to "+group.getTitle()+" ? "+group.getAcademicTerm()+". View My groups & enrollment.");
            }
        }
        return new ProjectGroupDTO(groups.save(group));
    }

    public ProjectGroupDTO removeStudents(Long groupId, List<Long> ids) {
        var group=group(groupId);semesters.requireActive(group.getAcademicTerm());
        var selected=selected(ids);
        for(var enrollment:selected) {
            if(group.getStudents().stream().noneMatch(e->e.getId().equals(enrollment.getId())))
                throw new IllegalArgumentException("Student is not a current member of this group");
            if(enrollment.getStudent()!=null) notifications.createNotification(enrollment.getStudent().getId(),"You were removed from "+group.getTitle()+" ? "+group.getAcademicTerm()+". Contact your coordinator about your group assignment. Your submission history is preserved.");
            if(!enrollment.getFormerGroupIds().contains(groupId)) enrollment.getFormerGroupIds().add(groupId);
            if(enrollment.getStudent()!=null && enrollment.getStudent().getGroup()!=null && groupId.equals(enrollment.getStudent().getGroup().getId()))
                enrollment.getStudent().setGroup(null);
            audit.log("GROUP_MEMBER_REMOVED",access.current().getId().toString(),"Enrollment","Enrollment "+enrollment.getId()+": removed from group "+groupId+"; progression unchanged","WEB");
        }
        group.getStudents().removeIf(e->ids.contains(e.getId()));
        return new ProjectGroupDTO(groups.save(group));
    }

    public Map<String,Object> groupDeletionReview(Long id) { return deletionImpact(id); }

    /** Deletion is only available after the coordinator reviews this exact impact snapshot. */
    public void deleteGroup(Long id, String reviewedVersion) {
        var impact=deletionImpact(id);
        if(reviewedVersion==null||!reviewedVersion.equals(impact.get("reviewVersion")))
            throw new IllegalArgumentException("The group changed after review. Review the updated deletion impact and try again.");
        var group=group(id);semesters.requireActive(group.getAcademicTerm());
        var groupSubmissions=submissions.findByGroupId(id);
        for(var submission:groupSubmissions){
            comments.deleteAll(comments.findBySubmission_Id(submission.getId()));
            versions.deleteAll(versions.findBySubmission_Id(submission.getId()));
            feedback.findBySubmissionId(submission.getId()).ifPresent(feedback::delete);
        }
        participations.deleteBySubmission_Group_Id(id);
        submissions.deleteAll(groupSubmissions);
        reminders.deleteAll(reminders.findByGroup_Id(id));
        risks.deleteAll(risks.findByGroup_Id(id));
        deadlineEmails.cancelPendingByGroupId(id);
        for(var enrollment:enrollments.findAll())
            if(enrollment.getFormerGroupIds().remove(id)) enrollments.save(enrollment);
        for(var successor:groups.findAll()) if(Objects.equals(successor.getPreviousGroupId(),id)) {
            successor.setPreviousGroupId(null);
            groups.save(successor);
        }
        for(var user:users.findByGroup_Id(id)) {user.setGroup(null); users.save(user);}
        group.getStudents().clear();
        audit.log("GROUP_DELETED",access.current().getId().toString(),"Group",
                "Group "+id+" ("+group.getCode()+") deleted after review; submissions="+groupSubmissions.size(),"WEB");
        groups.delete(group);
    }

    /** Legacy callers cannot bypass the review step. */
    @Deprecated public void deleteGroup(Long id) {
        throw new IllegalArgumentException("Review the group deletion impact before deleting it.");
    }

    private Map<String,Object> deletionImpact(Long id) {
        var group=group(id);
        var groupSubmissions=submissions.findByGroupId(id);
        var groupVersions=versions.findBySubmission_Group_Id(id);
        var groupComments=comments.findBySubmission_Group_Id(id);
        var groupFeedback=feedback.findBySubmission_Group_Id(id);
        var groupParticipations=groupSubmissions.stream().flatMap(s->participations.findBySubmission_Id(s.getId()).stream()).toList();
        var groupReminders=reminders.findByGroup_Id(id);
        var groupRisks=risks.findByGroup_Id(id);
        var pendingEmails=deadlineEmails.findByGroupIdAndStatus(id,"PENDING");
        var successors=groups.findAll().stream().filter(g->Objects.equals(g.getPreviousGroupId(),id)).toList();
        String ids=group.getId()+"|"+group.getCode()+"|"+group.getAcademicTerm()+"|"+group.getPreviousGroupId()
                +"|"+group.getStudents().stream().map(StudentEnrollment::getId).sorted().toList()
                +"|"+groupSubmissions.stream().map(Submission::getId).sorted().toList()
                +"|"+groupParticipations.stream().map(StudentSubmissionParticipation::getId).sorted().toList()
                +"|"+groupVersions.stream().map(SubmissionVersion::getId).sorted().toList()
                +"|"+groupComments.stream().map(SubmissionComment::getId).sorted().toList()
                +"|"+groupFeedback.stream().map(AdviserFeedback::getId).sorted().toList()
                +"|"+groupReminders.stream().map(ReminderLog::getId).sorted().toList()
                +"|"+groupRisks.stream().map(RiskAssessmentLog::getId).sorted().toList()
                +"|"+pendingEmails.stream().map(DeadlineEmail::getId).sorted().toList()
                +"|"+successors.stream().map(ProjectGroup::getId).sorted().toList();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",id);result.put("code",group.getCode());result.put("title",group.getTitle());
        result.put("subjectCode",group.getSubject()==null?null:group.getSubject().getCode());
        result.put("academicTerm",group.getAcademicTerm());
        result.put("members",group.getStudents().stream().map(StudentEnrollment::getFullName).toList());
        result.put("submissionCount",groupSubmissions.size());result.put("versionCount",groupVersions.size());
        result.put("commentCount",groupComments.size());result.put("feedbackCount",groupFeedback.size());
        result.put("participationCount",groupParticipations.size());result.put("reminderCount",groupReminders.size());
        result.put("riskLogCount",groupRisks.size());result.put("pendingEmailCount",pendingEmails.size());
        result.put("previousGroupId",group.getPreviousGroupId());result.put("successorCount",successors.size());
        result.put("reviewVersion",hash(ids));
        return result;
    }

    private String hash(String value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @Deprecated public void addStudentToGroup(Long groupId,Long studentId) {
        throw new IllegalArgumentException("Assign students using enrollment IDs");
    }
    @Deprecated public void removeStudentFromGroup(Long studentId) {
        throw new IllegalArgumentException("Remove students using enrollment IDs");
    }
    private ProjectGroup group(Long id) {
        return groups.findByIdWithStudents(id).orElseThrow(()->new ResourceNotFoundException("Group not found"));
    }
    private Subject subject(String code) {
        return subjects.findByCode(required(code,"Subject")).orElseThrow(()->new ResourceNotFoundException("Subject not found"));
    }
    private void checkCode(ProjectGroup group) {
        groups.findByCode(group.getCode()).filter(g->!Objects.equals(g.getId(),group.getId())).ifPresent(g->{throw new IllegalArgumentException("Group code is already used; choose a unique code");});
    }
    private List<StudentEnrollment> selected(List<Long> ids) {
        if(ids==null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Choose at least one enrollment");
        var found=enrollments.findAllById(new HashSet<>(ids));
        if(found.size()!=new HashSet<>(ids).size()) throw new ResourceNotFoundException("An enrollment no longer exists; refresh the classlist");
        return found;
    }
    private String required(String value,String name) {
        if(value==null || value.isBlank() || value.trim().length()>255) throw new IllegalArgumentException(name+" is required and must be at most 255 characters");
        return value.trim();
    }
}
