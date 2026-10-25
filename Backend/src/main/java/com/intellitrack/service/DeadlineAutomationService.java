package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class DeadlineAutomationService {
    private final GoogleWorkspaceService google;
    private final GoogleConnectionRepository connections;
    private final UserRepository users;
    private final DeliverableRepository requirements;
    private final StudentEnrollmentRepository enrollments;
    private final DeadlineEmailRepository emails;
    private final TrackingService tracking;
    private final SemesterService semesters;
    @Value("${app.deadline-automation.enabled:false}") private boolean enabled;
    public DeadlineAutomationService(GoogleWorkspaceService g,GoogleConnectionRepository c,UserRepository u,DeliverableRepository d,StudentEnrollmentRepository e,DeadlineEmailRepository m,TrackingService t,SemesterService s){google=g;connections=c;users=u;requirements=d;enrollments=e;emails=m;tracking=t;semesters=s;}
    public record RequirementChanged(Long id,String kind) {}
    static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    static String revision(Deliverable d){return hash(d.getName()+"|"+d.getAcademicTerm()+"|"+d.getDueAt()+"|"+d.isActive()+"|"+d.isArchived());}
    private void queue(Long user,Deliverable d,String kind){
        String revision=revision(d),id=hash(user+"|"+d.getId()+"|"+kind+"|"+revision);
        if(emails.existsById(id))return;
        var job=new DeadlineEmail();job.setId(id);job.setUserId(user);job.setRequirementId(d.getId());job.setKind(kind);job.setRevision(revision);emails.save(job);
    }
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT)
    public void changed(RequirementChanged event){
        if(!enabled)return;
        requirements.findById(event.id()).ifPresent(d->enrollments.findAllWithSectionAndSubject().stream()
            .filter(e->e.getStudent()!=null&&d.getSubject()!=null&&Objects.equals(e.getClassSection().getSubject().getId(),d.getSubject().getId())&&Objects.equals(e.getClassSection().getAcademicTerm(),d.getAcademicTerm()))
            .map(e->e.getStudent().getId()).distinct().forEach(id->queue(id,d,event.kind())));
    }
    public Map<String,Object> status(){return Map.of("enabled",enabled,"sender",google.deadlineSenderEmail(),"connected",google.deadlineMailConnected(),"configurationProblems",google.configurationProblems(),"pending",emails.countByStatus("PENDING"),"sent",emails.countByStatus("SENT"),"review",emails.countByStatus("REVIEW"));}
    static String reminder(LocalDateTime due,LocalDateTime now){long seconds=Duration.between(now,due).getSeconds();return seconds<0?"Overdue":seconds<=3600?"Due within one hour":seconds<=86400?"Due within 24 hours":null;}
    @Scheduled(fixedDelayString="${app.deadline-automation.interval-ms:300000}",initialDelay=60000)
    public synchronized void run(){
        if(!enabled)return;
        var all=requirements.findAllWithSubjectAndSection();
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Manila"));
        for(var user:users.findAll())if("student".equals(user.getRole())){
            for(var row:tracking.filter(tracking.rows(user),Map.of()))if(row.dueAt()!=null&&row.firstSubmittedAt()==null){
                String kind=reminder(row.dueAt(),now);
                if(kind!=null)all.stream().filter(d->Objects.equals(d.getId(),row.deliverableId())).findFirst().ifPresent(d->queue(user.getId(),d,kind));
            }
        }
        emails.recoverInterrupted(Instant.now().minusSeconds(900));
        if(google.deadlineMailConnected())for(var job:emails.findTop5ByStatusOrderByCreatedAtAsc("PENDING")){
            var user=users.findById(job.getUserId()).orElse(null);
            var d=all.stream().filter(x->Objects.equals(x.getId(),job.getRequirementId())).findFirst().orElse(null);
            boolean assigned=d!=null&&user!=null&&"student".equals(user.getRole())&&enrollments.findAllWithSectionAndSubject().stream().anyMatch(e->e.getStudent()!=null&&Objects.equals(e.getStudent().getId(),user.getId())&&d.getSubject()!=null&&Objects.equals(e.getClassSection().getSubject().getId(),d.getSubject().getId())&&Objects.equals(e.getClassSection().getAcademicTerm(),d.getAcademicTerm()));
            boolean reminder=job.getKind().startsWith("Due ")||job.getKind().equals("Overdue");
            boolean valid=assigned&&Objects.equals(job.getRevision(),revision(d))&&!semesters.archived(d.getAcademicTerm());
            if(valid&&reminder)valid=tracking.filter(tracking.rows(user),Map.of()).stream().anyMatch(r->Objects.equals(r.deliverableId(),d.getId())&&r.firstSubmittedAt()==null&&Objects.equals(job.getKind(),reminder(d.getDueAt(),now)));
            if(!valid){job.setStatus("CANCELLED");emails.save(job);continue;}
            if(emails.claim(job.getId(),Instant.now())!=1)continue;
            job.setAttemptedAt(Instant.now());
            try{google.sendDeadlineEmail(user.getEmail(),"IntelliTrack: "+job.getKind()+" - "+d.getName(),job.getKind()+"\n\n"+d.getName()+"\nSubject: "+d.getSubject().getCode()+"\nTerm: "+d.getAcademicTerm()+"\nDeadline: "+d.getDueAt()+" (Asia/Manila)\n\nSign in to IntelliTrack to view your requirements.");job.setStatus("SENT");job.setSentAt(Instant.now());}
            catch(Exception ex){job.setStatus("REVIEW");job.setFailure("Delivery could not be confirmed. Check sender Sent folder and Google connection before taking action.");}
            emails.save(job);
        }
        connections.findAll().stream().filter(c->"calendar".equals(c.getPurpose())&&c.getEncryptedRefreshToken()!=null&&(c.getNextAutoSyncAt()==null||c.getNextAutoSyncAt().isBefore(Instant.now())))
            .sorted(Comparator.comparing(GoogleConnection::getNextAutoSyncAt,Comparator.nullsFirst(Comparator.naturalOrder()))).limit(3).forEach(c->{
                var user=users.findById(c.getUserId()).orElse(null);if(user==null)return;
                List<GoogleWorkspaceService.CalendarDeadline> desired;
                if("student".equals(user.getRole()))desired=tracking.filter(tracking.rows(user),Map.of()).stream().map(r->new GoogleWorkspaceService.CalendarDeadline(r.deliverableId(),r.subject(),r.deliverable(),r.academicTerm(),r.dueAt())).distinct().toList();
                else if("coordinator".equals(user.getRole()))desired=all.stream().filter(d->d.isActive()&&!d.isArchived()&&!semesters.archived(d.getAcademicTerm())&&d.getDueAt()!=null).map(d->new GoogleWorkspaceService.CalendarDeadline(d.getId(),d.getSubject()==null?"Subject":d.getSubject().getCode(),d.getName(),d.getAcademicTerm(),d.getDueAt())).toList();
                else return;
                String fingerprint=hash(desired.stream().map(Object::toString).sorted().toList().toString());
                try{if(!fingerprint.equals(c.getCalendarFingerprint()))google.syncDeadlines(user,desired);
                    var fresh=connections.findById(c.getId()).orElseThrow();fresh.setCalendarFingerprint(fingerprint);fresh.setNextAutoSyncAt(Instant.now().plusSeconds(300));fresh.setAutoSyncError(null);connections.save(fresh);
                }catch(Exception ex){var fresh=connections.findById(c.getId()).orElseThrow();fresh.setNextAutoSyncAt(Instant.now().plusSeconds(600));fresh.setAutoSyncError("Automatic sync failed. Reconnect Google or try manual sync.");connections.save(fresh);}
            });
    }
}
