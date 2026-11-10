package com.intellitrack.controller;

import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import com.intellitrack.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/academic")
@PreAuthorize("hasAnyRole('coordinator','administrator')")
@Transactional
public class AcademicController {
    private final SubjectRepository subjects; private final ClassSectionRepository sections;
    private final ProjectGroupRepository groups; private final StudentEnrollmentRepository enrollments;
    private final DeliverableRepository deliverables; private final AccessService access; private final AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired private AuditLogRepository auditLogs;
    @org.springframework.beans.factory.annotation.Autowired private ProgressionService progression;
    @org.springframework.beans.factory.annotation.Autowired private SemesterService semesters;
    @org.springframework.beans.factory.annotation.Autowired private NotificationService notifications;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    @org.springframework.beans.factory.annotation.Autowired private SubmissionRepository submissions;
    @org.springframework.beans.factory.annotation.Autowired private DeadlineRepository deadlines;
    @org.springframework.beans.factory.annotation.Autowired private SubmissionCommentRepository submissionComments;
    @org.springframework.beans.factory.annotation.Autowired private ReminderLogRepository reminders;
    @org.springframework.beans.factory.annotation.Autowired private RiskAssessmentLogRepository risks;
    @org.springframework.beans.factory.annotation.Autowired private DeadlineEmailRepository deadlineEmails;
    public record SemesterInput(String term,boolean archived) {}
    @PostMapping({"/groups/{id}/carry","/groups/{id}/repeat","/enrollments/{id}/repeat"})
    public org.springframework.http.ResponseEntity<Map<String,String>> retiredEnrollmentCopy(){
        return org.springframework.http.ResponseEntity.status(410).body(Map.of("message","Import the new semester classlist to create enrollments. Carry-forward and repeat-copy actions have been retired."));
    }
    @PutMapping("/semesters")
    public void semester(@RequestBody SemesterInput req) {
        boolean known=sections.findAll().stream().anyMatch(s->Objects.equals(s.getAcademicTerm(),req.term()))
            ||groups.findAll().stream().anyMatch(g->Objects.equals(g.getAcademicTerm(),req.term()))
            ||deliverables.findAll().stream().anyMatch(d->Objects.equals(d.getAcademicTerm(),req.term()));
        if(!known)throw new IllegalArgumentException("No records exist for this semester");
        semesters.setArchived(req.term(),req.archived());
    }
    public record ProgressionInput(StudentEnrollment.Progression status,String reason) {}
    @PutMapping("/groups/{id}/progression")
    public Map<String,Object> groupProgression(@PathVariable Long id,@RequestBody ProgressionInput req) {
        var group=groups.findByIdWithStudents(id).orElseThrow();
        if(group.getStudents().isEmpty()) throw new IllegalArgumentException("Assign members to this group first");
        for(var e:group.getStudents()) progression.status(e.getId(),req.status(),req.reason());
        return map("updated",group.getStudents().size());
    }
    // Enrollment creation now uses classlist import.
    public Map<String,Object> repeatGroup(@PathVariable Long id,@RequestBody TermAssignment req) {
        var source=groups.findByIdWithStudents(id).orElseThrow();
        String term=requiredTerm(req.term());semesters.requireActive(term);ProgressionService.laterTerm(source.getAcademicTerm(),term);
        var members=source.getStudents().stream().filter(e->e.getProgression()==StudentEnrollment.Progression.REPEAT).toList();
        if(members.isEmpty()) throw new IllegalArgumentException("Mark the group as Repeat subject first");
        if(groups.findAll().stream().anyMatch(g->Objects.equals(g.getPreviousGroupId(),id)&&Objects.equals(g.getAcademicTerm(),term)&&Objects.equals(g.getSubject().getId(),source.getSubject().getId())))
            throw new IllegalArgumentException("This group already has a repeat enrollment in that term");
        var target=new ProjectGroup();target.setCode(source.getCode()+"-"+UUID.randomUUID().toString().substring(0,8));
        target.setTitle(source.getTitle());target.setAdviser(source.getAdviser());target.setSubject(source.getSubject());target.setAcademicTerm(term);target.setPreviousGroupId(id);
        for(var old:members) {
            var cs=sections.findAll().stream().filter(s->s.getSubject().getId().equals(source.getSubject().getId())&&Objects.equals(s.getAcademicTerm(),term)&&s.getSection().equals(old.getClassSection().getSection())).findFirst().orElseGet(()->{
                var s=new ClassSection();s.setSubject(source.getSubject());s.setAcademicTerm(term);s.setSection(old.getClassSection().getSection());return sections.save(s);
            });
            target.getStudents().add(progression.repeat(old.getId(),cs.getId()));
        }
        groups.save(target);return map("id",target.getId(),"enrolled",members.size());
    }
    @PutMapping("/enrollments/{id}/progression")
    public void progression(@PathVariable Long id,@RequestBody ProgressionInput req) { progression.status(id,req.status(),req.reason()); }
    public record RepeatInput(Long sectionId) {}
    // Enrollment creation now uses classlist import.
    public Map<String,Object> repeat(@PathVariable Long id,@RequestBody RepeatInput req) { return map("id",progression.repeat(id,req.sectionId()).getId()); }
    @GetMapping("/enrollments/{id}/history")
    public List<Map<String,Object>> enrollmentHistory(@PathVariable Long id) {
        return auditLogs.findByTargetModuleAndDetailsStartingWithOrderByTimestampDesc("Enrollment","Enrollment "+id+":").stream()
            .map(log->map("details",log.getDetails(),"timestamp",log.getTimestamp())).toList();
    }
    @GetMapping("/requirements/{id}/history")
    public List<Map<String,Object>> requirementHistory(@PathVariable Long id){
        deliverables.findById(id).orElseThrow();
        return auditLogs.findByTargetModuleAndDetailsStartingWithOrderByTimestampDesc("Deadline","Requirement "+id+":").stream()
            .map(log->map("id",log.getId(),"details",log.getDetails(),"timestamp",log.getTimestamp(),"performedBy",log.getPerformedBy())).toList();
    }
    public AcademicController(SubjectRepository s,ClassSectionRepository c,ProjectGroupRepository g,StudentEnrollmentRepository e,DeliverableRepository d,AccessService a,AuditService log){
        subjects=s; sections=c; groups=g; enrollments=e; deliverables=d; access=a; audit=log;
    }
    private Map<String,Object> map(Object... pairs){ Map<String,Object> m=new LinkedHashMap<>(); for(int i=0;i<pairs.length;i+=2)m.put(pairs[i].toString(),pairs[i+1]);return m; }
    @GetMapping
    public Map<String,Object> catalog(){
        return map("archivedTerms",semesters.archivedTerms(),"subjects",subjects.findAll().stream().map(s->map("id",s.getId(),"code",s.getCode(),"name",s.getName())).toList(),
            "sections",sections.findAll().stream().map(s->map("id",s.getId(),"name",s.getSection(),"subjectId",s.getSubject().getId(),"term",s.getAcademicTerm())).toList(),
            "groups",groups.findAllWithAllRelations().stream().map(g->map("id",g.getId(),"name",g.getTitle(),"subjectId",g.getSubject()==null?null:g.getSubject().getId(),"term",g.getAcademicTerm(),"previousGroupId",g.getPreviousGroupId(),"members",g.getStudents().size(),"eligible",g.getStudents().stream().filter(e->e.getProgression()==StudentEnrollment.Progression.ELIGIBLE).count())).toList(),
            "deliverables",deliverables.findAllWithSubjectAndSection().stream().map(d->map("id",d.getId(),"name",d.getName(),"subjectId",d.getSubject()==null?null:d.getSubject().getId(),"term",d.getAcademicTerm(),"dueAt",d.getDueAt(),"active",d.isActive()&&!d.isArchived())).toList());
    }
    public record Offering(Long subjectId,String section,String term) {}
    public record SubjectInput(String code,String name) {}
    @PostMapping("/subjects")
    public Map<String,Object> createSubject(@RequestBody SubjectInput req){
        if(req.code()==null||req.code().isBlank()||req.code().trim().length()>255
            ||req.name()==null||req.name().isBlank()||req.name().trim().length()>255)
            throw new IllegalArgumentException("Enter a subject code and name (up to 255 characters each)");
        String code=com.intellitrack.service.CapstoneCoursePolicy.code(req.code());
        String name=com.intellitrack.service.CapstoneCoursePolicy.name(code,req.name());
        if(subjects.findAll().stream().anyMatch(s->s.getCode().equalsIgnoreCase(code)))
            throw new IllegalArgumentException("This subject code already exists. Select it from the subject list.");
        var subject=new Subject();subject.setCode(code);subject.setName(name);subjects.save(subject);
        audit.log("SUBJECT_CREATED",access.current().getId().toString(),"Academic","Subject "+code+" created","WEB");
        return map("id",subject.getId());
    }
    @PostMapping("/sections")
    public Map<String,Object> section(@RequestBody Offering req){
        String term=requiredTerm(req.term());semesters.requireActive(term);
        if(req.section()==null||req.section().isBlank())throw new IllegalArgumentException("Section name is required");
        var existing=sections.findAll().stream().filter(s->s.getSubject().getId().equals(req.subjectId())&&s.getSection().equals(req.section().trim())&&Objects.equals(s.getAcademicTerm(),term)).findFirst();
        if(existing.isPresent()){CapstoneCoursePolicy.section(existing.get().getSubject().getCode(),term,existing.get().getSection());return map("id",existing.get().getId());}
        var section=new ClassSection();section.setSubject(subjects.findById(req.subjectId()).orElseThrow());CapstoneCoursePolicy.section(section.getSubject().getCode(),term,req.section().trim());section.setSection(req.section().trim());section.setAcademicTerm(term);
        return map("id",sections.save(section).getId());
    }
    @DeleteMapping("/sections/{id}")
    public void deleteEmptySection(@PathVariable Long id) {
        var section = sections.findById(id).orElseThrow(() -> new com.intellitrack.exception.ResourceNotFoundException("Section not found"));
        semesters.requireActive(section.getAcademicTerm());
        if (!enrollments.findByClassSectionId(id).isEmpty())
            throw new IllegalArgumentException("This section still has students. Move or unenroll them before deleting it.");
        sections.delete(section);
        audit.log("SECTION_DELETED", access.current().getId().toString(), "Academic", "Empty section " + id + " (" + section.getSection() + ") deleted", "WEB");
    }
    public record TermAssignment(String term) {}
    @PutMapping("/sections/{id}/term")
    public void assignTerm(@PathVariable Long id,@RequestBody TermAssignment req){
        var section=sections.findById(id).orElseThrow(); String term=requiredTerm(req.term());semesters.requireActive(term);
        if(section.getAcademicTerm()!=null&&!section.getAcademicTerm().equals(term))throw new IllegalArgumentException("Existing terms are immutable. Create a new section for the new term.");
        // Follow all connected sections, including other groups in a cross-section team.
        var allGroups=groups.findAllWithAllRelations();Set<Long> affected=new HashSet<>();affected.add(id);
        boolean expanded;
        do {expanded=false;for(var g:allGroups)if(g.getStudents().stream().anyMatch(e->affected.contains(e.getClassSection().getId())))
            for(var e:g.getStudents()) if(affected.add(e.getClassSection().getId()))expanded=true;
        } while(expanded);
        for(var g:allGroups) if(g.getStudents().stream().anyMatch(e->affected.contains(e.getClassSection().getId()))){
            if(g.getAcademicTerm()!=null&&!g.getAcademicTerm().equals(term))throw new IllegalArgumentException("Group is already assigned to a different term");
            for(var e:g.getStudents()) {var cs=e.getClassSection();if(cs.getAcademicTerm()!=null&&!cs.getAcademicTerm().equals(term))throw new IllegalArgumentException("Group members have conflicting terms");CapstoneCoursePolicy.section(cs.getSubject().getCode(),term,cs.getSection());cs.setAcademicTerm(term);sections.save(cs);}
            g.setAcademicTerm(term);groups.save(g);
        }
        CapstoneCoursePolicy.section(section.getSubject().getCode(),term,section.getSection());section.setAcademicTerm(term);sections.save(section);audit.log("TERM_ASSIGNED",access.current().getId().toString(),"Academic","Section "+id+" assigned to "+term,"WEB");
    }
    public record Requirement(Long id,Long subjectId,String term,String name,LocalDateTime dueAt,String reason,boolean active) {}
    @PostMapping("/requirements")
    public Map<String,Object> requirement(@RequestBody Requirement req){
        String term=requiredTerm(req.term());semesters.requireActive(term);
        if(req.name()==null||req.name().isBlank()||req.name().length()>255||req.dueAt()==null||req.subjectId()==null)throw new IllegalArgumentException("Choose a subject, name (up to 255 characters) and deadline");
        var d=req.id()==null?new Deliverable():deliverables.findById(req.id()).orElseThrow();
        if(d.getId()!=null&&d.getAcademicTerm()!=null&&(!Objects.equals(d.getAcademicTerm(),term)||d.getSubject()==null||!d.getSubject().getId().equals(req.subjectId())))throw new IllegalArgumentException("Create a new requirement for another subject or term");
        semesters.requireActive(d.getAcademicTerm());
        var old=d.getDueAt();
        boolean changed=req.id()==null||!Objects.equals(old,req.dueAt())||!Objects.equals(d.getName(),req.name().trim())||d.isActive()!=req.active();
        if(old!=null&&!old.equals(req.dueAt())&&(req.reason()==null||req.reason().isBlank()))throw new IllegalArgumentException("Explain the deadline change or extension");
        if(deliverables.findAll().stream().anyMatch(x->!Objects.equals(x.getId(),req.id())&&x.getName().equalsIgnoreCase(req.name().trim())&&x.getSubject()!=null&&x.getSubject().getId().equals(req.subjectId())&&Objects.equals(x.getAcademicTerm(),term)))throw new IllegalArgumentException("A requirement with this name already exists in this subject and term");
        d.setName(req.name().trim());d.setSubject(subjects.findById(req.subjectId()).orElseThrow());d.setAcademicTerm(term);d.setSection(null);d.setDueAt(req.dueAt());d.setStage("Submission");d.setActive(req.active());d.setArchived(!req.active());
        deliverables.save(d);
        if(changed) events.publishEvent(new com.intellitrack.service.DeadlineAutomationService.RequirementChanged(d.getId(),req.active()?(req.id()==null?"New requirement":"Deadline updated"):"Requirement archived"));
        if(changed) enrollments.findAllWithSectionAndSubject().stream().filter(e->e.getStudent()!=null
            &&e.getClassSection().getSubject().getId().equals(req.subjectId())&&Objects.equals(e.getClassSection().getAcademicTerm(),term))
            .map(e->e.getStudent().getId()).distinct().forEach(id->notifications.createNotification(id,
                (req.active()?(req.id()==null?"New requirement: ":"Requirement updated: "):"Requirement archived: ")+d.getName()+" ? "+d.getSubject().getCode()+" ? "+term+". Deadline: "+req.dueAt()+" (Asia/Manila). Check My submissions."));
        audit.log("REQUIREMENT_UPDATED",access.current().getId().toString(),"Deadline","Requirement "+d.getId()+": "+old+" -> "+req.dueAt()+"; "+Objects.toString(req.reason(),"Created/updated"),"WEB");
        return map("id",d.getId());
    }
    @DeleteMapping("/requirements/{id}")
    public void deleteRequirement(@PathVariable Long id){
        var d=deliverables.findById(id).orElseThrow(()->new com.intellitrack.exception.ResourceNotFoundException("Requirement not found"));
        semesters.requireActive(d.getAcademicTerm());
        if(submissions.existsByDeliverableId(id)||reminders.existsByDeliverable_Id(id)||risks.existsByDeliverable_Id(id))throw new IllegalArgumentException("This requirement has submission or activity history and cannot be deleted. Archive it to preserve that history.");
        deadlines.deleteByDeliverableId(id);
        submissionComments.deleteByDeliverableId(id);
        deadlineEmails.cancelPendingByRequirementId(id);
        deliverables.delete(d);
        audit.log("REQUIREMENT_DELETED",access.current().getId().toString(),"Deadline","Requirement "+id+" ("+d.getName()+") deleted before submissions existed","WEB");
        events.publishEvent(new com.intellitrack.service.DeadlineAutomationService.RequirementChanged(id,"Requirement deleted"));
    }
    public record Carry(Long subjectId,String term) {}
    // Enrollment creation now uses classlist import.
    public Map<String,Object> carry(@PathVariable Long id,@RequestBody Carry req){
        String term=requiredTerm(req.term());semesters.requireActive(term);var source=groups.findByIdWithStudents(id).orElseThrow();
        ProgressionService.laterTerm(source.getAcademicTerm(),term);
        if(source.getSubject()!=null && source.getSubject().getId().equals(req.subjectId())) throw new IllegalArgumentException("Use Repeat subject in Students & sections for the same subject");
        var eligible=source.getStudents().stream().filter(e->e.getProgression()==StudentEnrollment.Progression.ELIGIBLE).toList();
        if(eligible.isEmpty()) throw new IllegalArgumentException("No eligible members. Review progression in Students & sections first");
        if(source.getStudents().isEmpty())throw new IllegalArgumentException("Assign team members before carrying the team forward");
        if(source.getSubject()!=null&&source.getSubject().getId().equals(req.subjectId())&&Objects.equals(source.getAcademicTerm(),term))throw new IllegalArgumentException("Choose a different subject or term");
        var successor=groups.findAllWithAllRelations().stream().filter(g->Objects.equals(g.getPreviousGroupId(),id)&&Objects.equals(g.getAcademicTerm(),term)&&g.getSubject().getId().equals(req.subjectId())).findFirst();
        var target=successor.orElseGet(ProjectGroup::new);
        if(successor.isEmpty()) {target.setCode(source.getCode()+"-"+UUID.randomUUID().toString().substring(0,8));target.setTitle(source.getTitle());target.setAdviser(source.getAdviser());target.setSubject(subjects.findById(req.subjectId()).orElseThrow());target.setAcademicTerm(term);target.setPreviousGroupId(id);}
        int added=0;
        for(var old:eligible){
            if(target.getStudents().stream().anyMatch(e->Objects.equals(e.getPreviousEnrollmentId(),old.getId()))) continue;
            var cs=sections.findAll().stream().filter(s->s.getSubject().getId().equals(req.subjectId())&&Objects.equals(s.getAcademicTerm(),term)&&s.getSection().equals(old.getClassSection().getSection())).findFirst().orElseGet(()->{var s=new ClassSection();s.setSubject(target.getSubject());s.setAcademicTerm(term);s.setSection(old.getClassSection().getSection());return sections.save(s);});
            var candidates=enrollments.findAll().stream().filter(e->e.getClassSection().getSubject().getId().equals(req.subjectId())&&Objects.equals(e.getClassSection().getAcademicTerm(),term)
                && ((old.getStudent()!=null&&e.getStudent()!=null&&e.getStudent().getId().equals(old.getStudent().getId()))
                    || (old.getStudentId()!=null&&!old.getStudentId().isBlank()&&old.getStudentId().equals(e.getStudentId()))
                    || (old.getEmail()!=null&&!old.getEmail().isBlank()&&old.getEmail().equalsIgnoreCase(e.getEmail())))).toList();
            if(candidates.size()>1)throw new IllegalArgumentException("Resolve duplicate target enrollments for "+old.getFullName()+" before carrying the team");
            var matching=candidates.stream().findFirst();
            StudentEnrollment e;
            if(matching.isPresent()){
                e=matching.get();
                boolean sameAccount=old.getStudent()!=null&&e.getStudent()!=null&&old.getStudent().getId().equals(e.getStudent().getId());
                boolean sameEmail=old.getEmail()!=null&&!old.getEmail().isBlank()&&old.getEmail().equalsIgnoreCase(e.getEmail());
                boolean conflictingAccounts=old.getStudent()!=null&&e.getStudent()!=null&&!sameAccount;
                if(conflictingAccounts||(!sameAccount&&!sameEmail))throw new IllegalArgumentException("Conflicting student identity for "+old.getFullName()+". Verify and link the target enrollment before carrying the team.");
                if(groups.existsByStudents_Id(e.getId()))throw new IllegalArgumentException(old.getFullName()+" already belongs to a group in the target subject and term");
            }
            else {e=new StudentEnrollment();e.setStudent(old.getStudent());e.setStudentId(old.getStudentId());e.setFullName(old.getFullName());e.setEmail(old.getEmail());e.setClassSection(cs);enrollments.save(e);}
            e.setPreviousEnrollmentId(old.getId());
            target.getStudents().add(e);added++;
        }
        if(added==0) throw new IllegalArgumentException("All eligible members have already been carried to this destination");
        groups.save(target);audit.log("GROUP_CARRIED",access.current().getId().toString(),"Group","Group "+id+" carried to "+target.getId()+" / "+term,"WEB");
        return map("id",target.getId());
    }
    private String requiredTerm(String input){var term=TrackingPolicy.term(input);if(term==null)throw new IllegalArgumentException("Select a school year and semester");return term;}
}
