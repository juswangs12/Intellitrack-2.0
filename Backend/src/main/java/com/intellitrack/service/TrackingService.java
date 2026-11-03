package com.intellitrack.service;

import com.intellitrack.dto.TrackingRow;
import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class TrackingService {
    @org.springframework.beans.factory.annotation.Autowired private com.intellitrack.service.SemesterService semesters;
    private final StudentEnrollmentRepository enrollments;
    private final ProjectGroupRepository groups;
    private final DeliverableRepository deliverables;
    private final SubmissionRepository submissions;
    private final AccessService access;
    public TrackingService(StudentEnrollmentRepository e, ProjectGroupRepository g, DeliverableRepository d, SubmissionRepository s, AccessService a) {
        enrollments=e; groups=g; deliverables=d; submissions=s; access=a;
    }
    public record Member(String name, String section, boolean self) {}
    public record StudentGroup(Long id, String name, String adviser, List<Member> members) {}
    public record EnrollmentView(Long id, Long subjectId, String subject, String subjectName, String section,
                                 String term, boolean archived, long requirements, List<StudentGroup> groups) {}
    public List<EnrollmentView> studentEnrollments(User user) {
        if (!"student".equals(user.getRole())) throw new org.springframework.security.access.AccessDeniedException("Students only");
        var archived=semesters.archivedTerms();
        var requirements=deliverables.findAllWithSubjectAndSection();
        var currentGroups=groups.findAllWithAllRelations();
        return enrollments.findByStudent_Id(user.getId()).stream().map(e->{
            var section=e.getClassSection(); var subject=section.getSubject();
            var ownGroups=currentGroups.stream().filter(g->g.getStudents().stream().anyMatch(m->m.getId().equals(e.getId())))
                .map(g->new StudentGroup(g.getId(),g.getTitle(),g.getAdviser()==null?"Not assigned":
                    g.getAdviser().getFirstName()+" "+g.getAdviser().getLastName(),
                    g.getStudents().stream().map(m->new Member(m.getFullName(),m.getClassSection().getSection(),
                        m.getStudent()!=null&&user.getId().equals(m.getStudent().getId()))).toList())).toList();
            long count=requirements.stream().filter(d->d.isActive()&&!d.isArchived()&&d.getSubject()!=null&&
                subject.getId().equals(d.getSubject().getId())&&Objects.equals(section.getAcademicTerm(),d.getAcademicTerm())).count();
            return new EnrollmentView(e.getId(),subject.getId(),subject.getCode(),subject.getName(),section.getSection(),
                section.getAcademicTerm(),archived.contains(section.getAcademicTerm()),count,ownGroups);
        }).sorted(Comparator.comparing(EnrollmentView::term,Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(EnrollmentView::subject)).toList();
    }
    public List<TrackingRow> rows(User user) {
        var archivedTerms=semesters.archivedTerms();
        var allGroups=groups.findAllWithAllRelations();
        Map<Long,ProjectGroup> byEnrollment=new HashMap<>();
        for (var g:allGroups) for(var e:g.getStudents()) byEnrollment.put(e.getId(),g);
        Map<String,Submission> byStudent=new HashMap<>();
        for(var s:submissions.findAll()) {
            if(s.getStudent()==null) continue;
            String key=s.getStudent().getId()+":"+s.getGroup().getId()+":"+s.getDeliverable().getId();
            byStudent.merge(key,s,(a,b)->a.getId()>b.getId()?a:b);
        }
        var active=deliverables.findAllWithSubjectAndSection().stream().filter(d->d.isActive()&&!d.isArchived()).toList();
        List<TrackingRow> result=new ArrayList<>();
        var now=LocalDateTime.now(ZoneId.of("Asia/Manila"));
        for(var e:enrollments.findAllWithSectionAndSubject()) {
            var currentGroup=byEnrollment.get(e.getId()); var student=e.getStudent();
            var g=currentGroup;
            if(g==null && !e.getFormerGroupIds().isEmpty()) {var formerId=e.getFormerGroupIds().get(e.getFormerGroupIds().size()-1);g=allGroups.stream().filter(x->x.getId().equals(formerId)).findFirst().orElse(null);}
            if("student".equals(user.getRole()) && (student==null || !student.getId().equals(user.getId()))) continue;
            if("adviser".equals(user.getRole()) && (g==null || g.getAdviser()==null || !g.getAdviser().getId().equals(user.getId()))) continue;
            if(!access.staff(user) && !Set.of("student","adviser").contains(user.getRole())) continue;
            var section=e.getClassSection(); var subject=section.getSubject();
            for(var d:active) {
                if(d.getSubject()==null || !subject.getId().equals(d.getSubject().getId()) || !Objects.equals(section.getAcademicTerm(),d.getAcademicTerm())) continue;
                var s=student==null||g==null?null:byStudent.get(student.getId()+":"+g.getId()+":"+d.getId());
                if(s==null && student!=null) for(var former:e.getFormerGroupIds()) {var previous=byStudent.get(student.getId()+":"+former+":"+d.getId());if(previous!=null && (!"adviser".equals(user.getRole()) || (previous.getGroup().getAdviser()!=null && previous.getGroup().getAdviser().getId().equals(user.getId())))){s=previous;break;}}
                var first=s==null?null:s.getSubmittedAt();
                var latest=s==null?null:s.getLastModifiedAt()!=null?s.getLastModifiedAt():first;
                var adviser=g==null?null:g.getAdviser();
                result.add(new TrackingRow(e.getId(),student==null?null:student.getId(),e.getStudentId(),e.getFullName(),
                    section.getSection(),subject.getId(),subject.getCode(),section.getAcademicTerm(),g==null?null:g.getId(),
                    g==null?"Unassigned":g.getTitle(),adviser==null?null:adviser.getId(),adviser==null?"Unassigned":adviser.getFirstName()+" "+adviser.getLastName(),
                    d.getId(),d.getName(),d.getDueAt(),s==null?null:s.getId(),first,latest,
                    TrackingPolicy.status(first,d.getDueAt(),now),latest==null?null:TrackingPolicy.status(latest,d.getDueAt(),now),s==null?null:s.getGoogleDocUrl(),student!=null,
                    currentGroup!=null&&!archivedTerms.contains(section.getAcademicTerm()),
                    archivedTerms.contains(section.getAcademicTerm())?"This semester is archived. Your submissions are read-only.":currentGroup==null?"You are not currently assigned to a group. Contact your coordinator before submitting.":null));
            }
        }
        return result;
    }
    public List<TrackingRow> filter(List<TrackingRow> rows, Map<String,String> filters) {
        var archived=semesters.archivedTerms();
        return rows.stream().filter(r->!filters.getOrDefault("term","").isBlank()||!archived.contains(r.academicTerm())).filter(r -> matches(filters,"term",r.academicTerm()==null?"legacy":r.academicTerm())
            && matches(filters,"subject",r.subjectId()) && matches(filters,"group",r.groupId())
            && matches(filters,"section",r.section()) && matches(filters,"adviser",r.adviserId())
            && (!"true".equals(filters.get("submitted")) || r.submissionId()!=null)
            && matches(filters,"deliverable",r.deliverableId()) && matches(filters,"status",r.status())
            && (filters.getOrDefault("search","").isBlank() || (r.studentName()+" "+r.studentNumber()).toLowerCase().contains(filters.get("search").toLowerCase()))).toList();
    }
    private boolean matches(Map<String,String> f,String key,Object value) { return f.getOrDefault(key,"").isBlank() || Objects.equals(f.get(key),String.valueOf(value)); }
    public Map<String,Object> summary(List<TrackingRow> rows) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("students",rows.stream().map(r->r.studentId()!=null?"user:"+r.studentId():r.studentNumber()==null||r.studentNumber().isBlank()?"enrollment:"+r.enrollmentId():"number:"+r.studentNumber()).distinct().count());
        result.put("expected",rows.size());
        for(String status:List.of("SUBMITTED_ON_TIME","SUBMITTED_LATE","OVERDUE","DUE_TODAY","AWAITING_SUBMISSION"))
            result.put(status,rows.stream().filter(r->status.equals(r.status())).count());
        result.put("lateReplacements",rows.stream().filter(r->"SUBMITTED_LATE".equals(r.latestStatus()) && "SUBMITTED_ON_TIME".equals(r.status())).count());
        return result;
    }
}
