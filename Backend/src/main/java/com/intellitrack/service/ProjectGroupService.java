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

    public void deleteGroup(Long id) {
        var group=group(id);semesters.requireActive(group.getAcademicTerm());
        boolean hasSubmissions=!submissions.findByGroupId(id).isEmpty();
        boolean hasSuccessor=groups.findAll().stream().anyMatch(g->Objects.equals(g.getPreviousGroupId(),id));
        if(!group.getStudents().isEmpty() || hasSubmissions || group.getPreviousGroupId()!=null || hasSuccessor
                )
            throw new IllegalArgumentException(hasSubmissions||group.getPreviousGroupId()!=null||hasSuccessor
                    ? "This group has submission or carry-forward history and cannot be deleted"
                    : "Remove all current members before deleting this group");
        // Former membership markers alone should not make an unused, empty group undeletable.
        enrollments.findAll().stream().filter(e->e.getFormerGroupIds().remove(id)).forEach(enrollments::save);
        // Only unused groups can be removed; never cascade through academic records.
        for(var user:users.findByGroup_Id(id)) {user.setGroup(null); users.save(user);}
        group.getStudents().clear();
        groups.delete(group);
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
