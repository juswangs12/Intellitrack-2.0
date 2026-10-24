package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@Transactional
public class ProgressionService {
    @org.springframework.beans.factory.annotation.Autowired private com.intellitrack.service.SemesterService semesters;
    private final StudentEnrollmentRepository enrollments;
    private final ClassSectionRepository sections;
    private final AuditService audit;
    private final AccessService access;
    public ProgressionService(StudentEnrollmentRepository e, ClassSectionRepository s, AuditService a, AccessService u) {
        enrollments=e; sections=s; audit=a; access=u;
    }
    public void status(Long id, StudentEnrollment.Progression status, String reason) {
        if(status==null || reason==null || reason.isBlank() || reason.length()>255)
            throw new IllegalArgumentException("Choose a progression status and enter a reason (up to 255 characters)");
        var e=enrollments.findById(id).orElseThrow();
        semesters.requireActive(e.getClassSection().getAcademicTerm());
        var old=e.getProgression(); e.setProgression(status); e.setProgressionReason(reason.trim());
        audit.log("PROGRESSION_UPDATED",access.current().getId().toString(),"Enrollment","Enrollment "+id+": "+old+" -> "+status+"; "+reason.trim(),"WEB");
    }
    public static void laterTerm(String source, String target) {
        source=TrackingPolicy.term(source); target=TrackingPolicy.term(target);
        if(source==null || target==null || target.compareTo(source)<=0)
            throw new IllegalArgumentException("Choose a later academic term. Assign the original term first if needed.");
    }
    public StudentEnrollment repeat(Long id, Long sectionId) {
        var old=enrollments.findById(id).orElseThrow();
        if(old.getProgression()!=StudentEnrollment.Progression.REPEAT)
            throw new IllegalArgumentException("Mark this enrollment as Repeat subject first");
        var target=sections.findById(sectionId).orElseThrow();
        if(!target.getSubject().getId().equals(old.getClassSection().getSubject().getId()))
            throw new IllegalArgumentException("Repeat enrollment must use the same subject");
        laterTerm(old.getClassSection().getAcademicTerm(),target.getAcademicTerm());
        if(enrollments.findAllWithSectionAndSubject().stream().anyMatch(e->
            e.getClassSection().getSubject().getId().equals(target.getSubject().getId()) &&
            Objects.equals(e.getClassSection().getAcademicTerm(),target.getAcademicTerm()) &&
            (Objects.equals(e.getPreviousEnrollmentId(),id) || sameStudent(old,e))))
            throw new IllegalArgumentException("A matching enrollment already exists in that subject and term");
        var e=new StudentEnrollment();e.setStudent(old.getStudent());e.setStudentId(old.getStudentId());
        e.setFullName(old.getFullName());e.setEmail(old.getEmail());e.setClassSection(target);e.setPreviousEnrollmentId(id);
        enrollments.save(e);
        audit.log("SUBJECT_REPEATED",access.current().getId().toString(),"Enrollment","Enrollment "+id+": repeated as "+e.getId()+" / "+target.getAcademicTerm(),"WEB");
        return e;
    }
    private boolean sameStudent(StudentEnrollment a,StudentEnrollment b) {
        return (a.getStudent()!=null && b.getStudent()!=null && a.getStudent().getId().equals(b.getStudent().getId()))
            || (a.getEmail()!=null && !a.getEmail().isBlank() && a.getEmail().equalsIgnoreCase(b.getEmail()))
            || (a.getStudentId()!=null && !a.getStudentId().isBlank() && a.getStudentId().equals(b.getStudentId()));
    }
}
