package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import java.time.*;
import java.util.*;

/** Uses the same claimed, deduplicated outbox as deadline reminders. No email on dashboard reads. */
@Service
public class ActivityEmailService {
    public static final String DIGEST = "Daily digest", ASSIGNMENT = "Adviser assignment";
    public static final String ENROLLMENT="Enrollment welcome", ADVISER_CREATED="Adviser welcome";
    public static final String COORDINATOR_CREATED="Coordinator welcome";
    @org.springframework.beans.factory.annotation.Autowired private StudentEnrollmentRepository enrollments;
    @Value("${app.frontend.base-url:http://localhost:3000}") private String frontend="http://localhost:3000";
    private final DeadlineEmailRepository emails;
    private final UserRepository users;
    private final ProjectGroupRepository groups;
    private final TrackingService tracking;
    private final SemesterService semesters;
    @Value("${app.deadline-automation.enabled:false}") private boolean enabled;
    public ActivityEmailService(DeadlineEmailRepository emails,UserRepository users,ProjectGroupRepository groups,TrackingService tracking,SemesterService semesters){
        this.emails=emails;this.users=users;this.groups=groups;this.tracking=tracking;this.semesters=semesters;
    }
    public record AdviserAssigned(Long groupId,Long adviserId,String eventId) {}
    public record Message(String subject,String body,String recipient) {
        public Message(String subject,String body){this(subject,body,null);}
    }
    public record StudentImported(Long enrollmentId) {}
    public record AdviserCreated(Long userId) {}
    public record CoordinatorCreated(Long userId) {}
    private String link(String path){return frontend.replaceAll("/+$","")+path;}
    private String enrollmentRevision(StudentEnrollment e){return DeadlineAutomationService.hash(e.getEmail().trim().toLowerCase(Locale.ROOT)+"|"+e.getClassSection().getId());}
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void imported(StudentImported event){
        if(!enabled)return;
        var e=enrollments.findById(event.enrollmentId()).orElse(null);
        if(e==null||e.getEmail()==null||e.getEmail().isBlank())return;
        String id=DeadlineAutomationService.hash(ENROLLMENT+"|"+e.getId());
        if(emails.existsById(id))return;
        var job=new DeadlineEmail();job.setId(id);job.setKind(ENROLLMENT);job.setEnrollmentId(e.getId());job.setRevision(enrollmentRevision(e));emails.save(job);
    }
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void created(AdviserCreated event){if(enabled)queue(event.userId(),ADVISER_CREATED,"adviser",null,event.userId().toString());}
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void created(CoordinatorCreated event){if(enabled)queue(event.userId(),COORDINATOR_CREATED,"coordinator",null,event.userId().toString());}
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void assigned(AdviserAssigned event){
        if(!enabled)return;
        queue(event.adviserId(),ASSIGNMENT,"adviser",event.groupId(),event.eventId());
    }
    private void queue(Long user,String kind,String revision,Long group,String key){
        String id=DeadlineAutomationService.hash(user+"|"+kind+"|"+key);
        if(emails.existsById(id))return;
        var job=new DeadlineEmail();job.setId(id);job.setUserId(user);job.setKind(kind);job.setRevision(revision);job.setGroupId(group);emails.save(job);
    }
    public void prepareDigests(LocalDateTime now){
        if(!enabled||now.getHour()<8)return;
        for(var user:users.findAll())if(Set.of("coordinator","adviser").contains(Objects.toString(user.getRole(),"")))
            queue(user.getId(),DIGEST,user.getRole()+"|"+now.toLocalDate(),null,now.toLocalDate().toString());
    }
    public boolean handles(DeadlineEmail job){return Set.of(DIGEST,ASSIGNMENT,ENROLLMENT,ADVISER_CREATED,COORDINATOR_CREATED).contains(job.getKind());}
    public Optional<Message> message(DeadlineEmail job,User user,LocalDateTime now){
        if(ENROLLMENT.equals(job.getKind())){
            var e=job.getEnrollmentId()==null?null:enrollments.findById(job.getEnrollmentId()).orElse(null);
            if(e==null||e.getEmail()==null||e.getEmail().isBlank()||e.getClassSection()==null||!Objects.equals(job.getRevision(),enrollmentRevision(e))||semesters.archived(e.getClassSection().getAcademicTerm()))return Optional.empty();
            var section=e.getClassSection();var subject=section.getSubject();
            return Optional.of(new Message("IntelliTrack: You are enrolled in "+subject.getCode(),
                "Hello "+e.getFullName()+",\n\nYour coordinator has enrolled you in "+subject.getCode()+" — "+subject.getName()+".\nSection: "+section.getSection()+"\nAcademic term: "+section.getAcademicTerm()+
                "\n\nOpen IntelliTrack: "+link("/student/login")+"\nChoose Continue with Google using this email address to access your enrollment and assigned deliverables.\nIf this enrollment is incorrect, contact your coordinator.",e.getEmail()));
        }
        if(user==null)return Optional.empty();
        if(COORDINATOR_CREATED.equals(job.getKind())){
            if(!"coordinator".equals(user.getRole()))return Optional.empty();
            return Optional.of(new Message("Welcome to IntelliTrack - Coordinator account",
                "Hello "+user.getFirstName()+",\n\nYour IntelliTrack coordinator account has been created.\nOpen your staff workspace: "+link("/staff/login")+
                "\nUse Continue with Google with this email address, or the sign-in details supplied separately by your administrator. Your coordinator workspace lets you import classlists, manage groups and deadlines, and monitor submissions."));
        }
        if(ADVISER_CREATED.equals(job.getKind())){
            if(!"adviser".equals(user.getRole()))return Optional.empty();
            return Optional.of(new Message("Welcome to IntelliTrack — Adviser account",
                "Hello "+user.getFirstName()+",\n\nYour IntelliTrack adviser account has been created.\nOpen your staff workspace: "+link("/staff/login")+
                "\nUse Continue with Google with this email address, or the sign-in details supplied separately by your administrator. Assigned groups will appear in your adviser dashboard."));
        }
        if(ASSIGNMENT.equals(job.getKind())){
            if(!"adviser".equals(user.getRole()))return Optional.empty();
            var group=groups.findByIdWithStudents(job.getGroupId()).orElse(null);
            if(group==null||group.getAdviser()==null||!Objects.equals(group.getAdviser().getId(),user.getId())||semesters.archived(group.getAcademicTerm()))return Optional.empty();
            return Optional.of(new Message("IntelliTrack: Group assigned", "You have been assigned to "+group.getTitle()+".\nAcademic term: "+group.getAcademicTerm()+"\nSign in to your adviser workspace to review this group: "+link("/staff/login")));
        }
        if(!Set.of("coordinator","adviser").contains(Objects.toString(user.getRole(),""))||!Objects.equals(job.getRevision(),user.getRole()+"|"+now.toLocalDate()))return Optional.empty();
        var rows=tracking.filter(tracking.rows(user),Map.of());
        if(rows.isEmpty())return Optional.empty();
        var yesterday=now.toLocalDate().minusDays(1);
        long updates=rows.stream().filter(r->r.latestSubmittedAt()!=null&&r.latestSubmittedAt().toLocalDate().equals(yesterday)).count();
        long submitted=rows.stream().filter(r->r.firstSubmittedAt()!=null).count();
        long overdue=rows.stream().filter(r->r.firstSubmittedAt()==null&&r.dueAt()!=null&&r.dueAt().isBefore(now)).count();
        long due=rows.stream().filter(r->r.firstSubmittedAt()==null&&r.dueAt()!=null&&!r.dueAt().isBefore(now)&&r.dueAt().isBefore(now.toLocalDate().plusDays(1).atStartOfDay())).count();
        String scope="adviser".equals(user.getRole())?"Your assigned groups":"Active academic work";
        return Optional.of(new Message("IntelliTrack: Daily submission digest — "+now.toLocalDate(),scope+"\nSnapshot: "+now+" (Asia/Manila)\n\nStudent-requirement records: "+rows.size()+"\nSubmitted: "+submitted+"\nSubmitted or replaced yesterday ("+yesterday+"): "+updates+"\nUnsubmitted and due today: "+due+"\nUnsubmitted and overdue: "+overdue+"\n\nEach count represents a student and requirement, not a whole group's submission. Sign in for the current analytics and submission details."));
    }
}
