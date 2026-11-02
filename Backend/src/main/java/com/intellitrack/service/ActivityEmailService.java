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
    public record Message(String subject,String body) {}
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
    public boolean handles(DeadlineEmail job){return Set.of(DIGEST,ASSIGNMENT).contains(job.getKind());}
    public Optional<Message> message(DeadlineEmail job,User user,LocalDateTime now){
        if(user==null)return Optional.empty();
        if(ASSIGNMENT.equals(job.getKind())){
            if(!"adviser".equals(user.getRole()))return Optional.empty();
            var group=groups.findByIdWithStudents(job.getGroupId()).orElse(null);
            if(group==null||group.getAdviser()==null||!Objects.equals(group.getAdviser().getId(),user.getId())||semesters.archived(group.getAcademicTerm()))return Optional.empty();
            return Optional.of(new Message("IntelliTrack: Group assigned", "You have been assigned to "+group.getTitle()+".\nAcademic term: "+group.getAcademicTerm()+"\nSign in to your adviser workspace to review this group."));
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
