package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import com.intellitrack.dto.GoogleDocSubmissionRequest;
import com.intellitrack.controller.AcademicController;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql=false, properties={"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.jpa.show-sql=false"})
@ActiveProfiles("test")
@Import({SemesterService.class,ProgressionService.class,TrackingService.class,AccessService.class,LinkSubmissionService.class,AcademicController.class,ParticipationTrackingService.class,ProjectGroupService.class,ClasslistImportService.class,com.intellitrack.controller.SubjectController.class})
class TrackingIntegrationTest {
    @Autowired TestEntityManager em;
    @Autowired TrackingService tracking;
    @Autowired LinkSubmissionService links;
    @Autowired SubmissionRepository submissions;
    @Autowired SubmissionVersionRepository versions;
    @Autowired AcademicController academic;
    @Autowired ProjectGroupService groupService;
    @Autowired ClasslistImportService imports;
    @Autowired StudentEnrollmentRepository enrollments;
    @Autowired com.intellitrack.controller.SubjectController subjectController;
    @Autowired AccessService access;
    @Autowired SemesterService semesters;
    @MockBean AuditService audit;
    @MockBean NotificationService notifications;
    User alice,bob,adviser,otherAdviser,coordinator;
    ProjectGroup group;
    Deliverable requirement;
    static final String TERM="2026-2027 / 1";
    @BeforeEach void setup() {
        alice=user("alice","student");bob=user("bob","student");adviser=user("adviser","adviser");otherAdviser=user("other","adviser");coordinator=user("coordinator","coordinator");
        var subject=new Subject();subject.setCode("CAP1");subject.setName("Capstone 1");em.persist(subject);
        var section=new ClassSection();section.setSubject(subject);section.setSection("A");section.setAcademicTerm(TERM);em.persist(section);
        var sectionB=new ClassSection();sectionB.setSubject(subject);sectionB.setSection("B");sectionB.setAcademicTerm(TERM);em.persist(sectionB);
        group=new ProjectGroup();group.setTitle("Alpha");group.setCode("ALPHA");group.setSubject(subject);group.setAcademicTerm(TERM);group.setAdviser(adviser);
        group.getStudents().add(enrollment(alice,section));group.getStudents().add(enrollment(bob,sectionB));em.persist(group);
        requirement=new Deliverable();requirement.setName("Proposal");requirement.setStage("Submission");requirement.setSubject(subject);requirement.setAcademicTerm(TERM);requirement.setDueAt(LocalDateTime.now(ZoneId.of("Asia/Manila")).minusDays(1));em.persist(requirement);
        var next=new Deliverable();next.setName("Proposal");next.setStage("Submission");next.setSubject(subject);next.setAcademicTerm("2026-2027 / 2");em.persist(next);
        em.flush();
    }
    User user(String name,String role){return em.persist(new User(name,"Test",name+"@example.test","test-hash",role));}
    @Test void googleEmailLinksUnclaimedImportsWithoutTakingAnotherAccountsEnrollment(){
        var imported=group.getStudents().get(0);imported.setStudent(null);imported.setEmail(alice.getEmail().toUpperCase());
        var owned=group.getStudents().get(1);owned.setEmail(alice.getEmail());em.flush();
        var oauth=new CustomOAuth2UserService();
        org.springframework.test.util.ReflectionTestUtils.setField(oauth,"studentEnrollmentRepository",enrollments);
        org.springframework.test.util.ReflectionTestUtils.setField(oauth,"userRepository",org.mockito.Mockito.mock(UserRepository.class));
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(oauth,"autoLinkEnrollments",alice,alice.getEmail(),"google-id","Alice");em.flush();
        assertEquals(alice.getId(),imported.getStudent().getId());
        assertEquals(bob.getId(),owned.getStudent().getId());
    }
    StudentEnrollment enrollment(User user,ClassSection section){var e=new StudentEnrollment();e.setProgression(StudentEnrollment.Progression.ELIGIBLE);e.setStudent(user);e.setStudentId(user.getFirstName());e.setFullName(user.getFirstName());e.setEmail(user.getEmail());e.setClassSection(section);return em.persist(e);}
    GoogleDocSubmissionRequest request(){return new GoogleDocSubmissionRequest("https://docs.google.com/document/d/abcdefghijk/edit",group.getId(),requirement.getId(),"test");}
    @Test void finishingSemesterPreservesRecordsAndBlocksChangesUntilReopened() throws Exception {
        asCoordinator();var submission=links.submit(alice.getId(),request(),request().googleDocUrl());
        var before=enrollments.count();
        academic.semester(new AcademicController.SemesterInput(TERM,true));em.flush();
        assertTrue(semesters.archived(TERM));assertEquals(before,enrollments.count());
        assertEquals(2,group.getStudents().size());assertTrue(submissions.existsById(submission));
        assertTrue(tracking.filter(tracking.rows(coordinator),Map.of()).isEmpty());
        assertEquals(2,tracking.filter(tracking.rows(coordinator),Map.of("term",TERM)).size());
        assertFalse(tracking.filter(tracking.rows(alice),Map.of("term",TERM)).get(0).canSubmit());
        assertTrue(groupService.getAllGroups().stream().filter(g->g.getId().equals(group.getId())).findFirst().orElseThrow().isSemesterArchived());
        assertThrows(IllegalArgumentException.class,()->links.submit(alice.getId(),request(),request().googleDocUrl()));
        assertThrows(IllegalArgumentException.class,()->academic.groupProgression(group.getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.REPEAT,"Blocked")));
        assertThrows(IllegalArgumentException.class,()->groupService.removeStudents(group.getId(),List.of(group.getStudents().get(0).getId())));
        assertThrows(IllegalArgumentException.class,()->academic.section(new AcademicController.Offering(group.getSubject().getId(),"NEW",TERM)));
        assertThrows(IllegalArgumentException.class,()->imports.importClasslist(null,"test","test",TERM));
        academic.semester(new AcademicController.SemesterInput(TERM,false));
        assertFalse(semesters.archived(TERM));assertEquals(2,tracking.filter(tracking.rows(coordinator),Map.of()).size());
        academic.groupProgression(group.getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.ELIGIBLE,"INC cleared"));
        assertEquals(2,group.getStudents().size());
    }
    @Test void requirementNotificationsOnlyReachEnrolledStudentsAndSkipUnchangedSaves(){
        asCoordinator();
        var input=new AcademicController.Requirement(requirement.getId(),group.getSubject().getId(),TERM,"Proposal",requirement.getDueAt().plusDays(1),"Extension",true);
        academic.requirement(input);
        org.mockito.Mockito.verify(notifications).createNotification(org.mockito.ArgumentMatchers.eq(alice.getId()),org.mockito.ArgumentMatchers.contains("Requirement updated"));
        org.mockito.Mockito.verify(notifications).createNotification(org.mockito.ArgumentMatchers.eq(bob.getId()),org.mockito.ArgumentMatchers.contains("Requirement updated"));
        org.mockito.Mockito.verifyNoMoreInteractions(notifications);
        org.mockito.Mockito.clearInvocations(notifications);
        academic.requirement(input);
        org.mockito.Mockito.verifyNoInteractions(notifications);
        groupService.removeStudents(group.getId(),List.of(group.getStudents().get(0).getId()));
        org.mockito.Mockito.verify(notifications).createNotification(org.mockito.ArgumentMatchers.eq(alice.getId()),org.mockito.ArgumentMatchers.contains("removed"));
        org.mockito.Mockito.verifyNoMoreInteractions(notifications);
    }
    @Test void enrollmentOverviewWorksWithoutRequirementsAndScopesMembership(){
        requirement.setActive(false);em.flush();
        var own=tracking.studentEnrollments(alice);
        assertEquals(1,own.size());assertEquals("A",own.get(0).section());
        assertEquals(0,own.get(0).requirements());
        assertEquals(2,own.get(0).groups().get(0).members().size());
        assertEquals(1,own.get(0).groups().get(0).members().stream().filter(TrackingService.Member::self).count());
        assertTrue(tracking.studentEnrollments(user("newstudent","student")).isEmpty());
        assertThrows(AccessDeniedException.class,()->tracking.studentEnrollments(coordinator));
        asCoordinator();academic.semester(new AcademicController.SemesterInput(TERM,true));
        assertTrue(tracking.studentEnrollments(alice).get(0).archived());
        academic.semester(new AcademicController.SemesterInput(TERM,false));
        groupService.removeStudents(group.getId(),List.of(own.get(0).id()));em.flush();
        assertTrue(tracking.studentEnrollments(alice).get(0).groups().isEmpty());
    }
    @Test void eachStudentHasOwnRowAndAdviserOnlySeesAssignedGroups(){
        assertEquals(1,tracking.rows(alice).size());
        assertEquals(2,tracking.rows(adviser).size());
        assertTrue(tracking.rows(otherAdviser).isEmpty());
        assertEquals(2,tracking.rows(coordinator).size());
        assertEquals(Set.of("A","B"),new HashSet<>(tracking.rows(adviser).stream().map(r->r.section()).toList()));
        assertTrue(tracking.filter(tracking.rows(coordinator),Map.of("term","2026-2027 / 2")).isEmpty());
    }
    @Test void formerMemberCanReadOwnHistoryButCannotSubmitOrReadAnotherStudentsHistory(){
        var id=links.submit(alice.getId(),request(),request().googleDocUrl());
        var bobId=links.submit(bob.getId(),request(),request().googleDocUrl());
        asCoordinator();
        var enrollment=group.getStudents().stream().filter(e->e.getStudent().getId().equals(alice.getId())).findFirst().orElseThrow();
        groupService.removeStudents(group.getId(),List.of(enrollment.getId()));em.flush();
        var row=tracking.rows(alice).get(0);
        assertFalse(row.canSubmit());assertNotNull(row.submissionBlockReason());assertEquals(id,row.submissionId());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(alice.getId(),null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_student"))));
        assertDoesNotThrow(()->access.submission(submissions.findById(id).orElseThrow()));
        assertThrows(AccessDeniedException.class,()->access.submission(submissions.findById(bobId).orElseThrow()));
        assertThrows(AccessDeniedException.class,()->links.submit(alice.getId(),request(),request().googleDocUrl()));
    }
    @Test void transferredStudentHistoryDoesNotLeakToAnotherAdviser(){
        var id=links.submit(alice.getId(),request(),request().googleDocUrl());
        var enrollment=group.getStudents().get(0);
        asCoordinator();groupService.removeStudents(group.getId(),List.of(enrollment.getId()));
        var next=new ProjectGroup();next.setCode("TRANSFER");next.setTitle("Transfer");next.setSubject(group.getSubject());next.setAcademicTerm(TERM);next.setAdviser(otherAdviser);next.getStudents().add(enrollment);em.persist(next);em.flush();
        var row=tracking.rows(otherAdviser).get(0);
        assertNull(row.googleDocUrl());assertNull(row.submissionId());
        assertEquals(id,tracking.rows(alice).get(0).submissionId());
    }
    @Test void replacementKeepsFirstComplianceAndIndependentMemberHistory(){
        Long id=links.submit(alice.getId(),request(),request().googleDocUrl());
        var first=submissions.findById(id).orElseThrow();var onTime=requirement.getDueAt().minusDays(1);first.setSubmittedAt(onTime);submissions.saveAndFlush(first);
        links.submit(alice.getId(),request(),"https://docs.google.com/document/d/replacement12/edit");
        links.submit(bob.getId(),request(),request().googleDocUrl());em.flush();
        var rows=tracking.rows(adviser);
        assertEquals(2,rows.size());
        var aliceRow=rows.stream().filter(r->r.studentId().equals(alice.getId())).findFirst().orElseThrow();
        assertEquals(onTime,aliceRow.firstSubmittedAt());
        assertEquals("SUBMITTED_ON_TIME",aliceRow.status());
        assertEquals("SUBMITTED_LATE",aliceRow.latestStatus());
        assertEquals(2,versions.findBySubmissionIdOrderByVersionNumberDesc(id).size());
        assertEquals(1L,tracking.summary(rows).get("lateReplacements"));
        assertEquals(2L,tracking.summary(rows).get("students"));
    }
    @Test void nonmemberAndWrongTermCannotSubmit(){
        var outsider=user("outsider","student");
        assertThrows(AccessDeniedException.class,()->links.submit(outsider.getId(),request(),request().googleDocUrl()));
        requirement.setAcademicTerm("2027-2028 / 1");em.flush();
        assertThrows(IllegalArgumentException.class,()->links.submit(alice.getId(),request(),request().googleDocUrl()));
    }
    void asCoordinator(){org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(coordinator.getId(),null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_coordinator"))));}
    @AfterEach void clearAuth(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    @Test void carryForwardPreservesTeamButSeparatesSubmissionScope(){
        asCoordinator();links.submit(alice.getId(),request(),request().googleDocUrl());
        var subject=new Subject();subject.setCode("CAP2");subject.setName("Capstone 2");em.persist(subject);
        var result=academic.carry(group.getId(),new AcademicController.Carry(subject.getId(),"2026-2027 / 2"));
        var target=em.find(ProjectGroup.class,(Long)result.get("id"));
        assertEquals(group.getId(),target.getPreviousGroupId());assertEquals(2,target.getStudents().size());
        assertEquals(adviser.getId(),target.getAdviser().getId());
        assertNotEquals(group.getStudents().get(0).getId(),target.getStudents().get(0).getId());
        assertEquals(1,submissions.findByGroupId(group.getId()).size());assertTrue(submissions.findByGroupId(target.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class,()->academic.carry(group.getId(),new AcademicController.Carry(subject.getId(),"2026-2027 / 2")));
    }
    @Test void extendingDeadlineRequiresReasonAndChangesAllSections(){
        asCoordinator();var due=requirement.getDueAt().plusDays(3);
        assertThrows(IllegalArgumentException.class,()->academic.requirement(new AcademicController.Requirement(requirement.getId(),group.getSubject().getId(),TERM,"Proposal",due,"",true)));
        academic.requirement(new AcademicController.Requirement(requirement.getId(),group.getSubject().getId(),TERM,"Proposal",due,"Presentation rescheduled",true));
        assertTrue(tracking.rows(adviser).stream().allMatch(r->due.equals(r.dueAt())&&"AWAITING_SUBMISSION".equals(r.status())));
        org.mockito.Mockito.verify(audit).log(org.mockito.ArgumentMatchers.eq("REQUIREMENT_UPDATED"),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.eq("Deadline"),org.mockito.ArgumentMatchers.contains("Presentation rescheduled"),org.mockito.ArgumentMatchers.eq("WEB"));
    }
    @Test void groupChangesCannotEraseSubmissionHistory(){
        asCoordinator();
        links.submit(alice.getId(),request(),request().googleDocUrl());
        Long enrollment=group.getStudents().get(0).getId();
        groupService.removeStudents(group.getId(),List.of(enrollment));
        assertEquals(StudentEnrollment.Progression.ELIGIBLE,enrollments.findById(enrollment).orElseThrow().getProgression());
        assertNotNull(tracking.rows(coordinator).stream().filter(r->r.studentId().equals(alice.getId())).findFirst().orElseThrow().firstSubmittedAt());
        assertThrows(AccessDeniedException.class,()->links.submit(alice.getId(),request(),request().googleDocUrl()));
        assertThrows(IllegalArgumentException.class,()->subjectController.unenrollStudent(enrollment));
        assertThrows(IllegalArgumentException.class,()->groupService.deleteGroup(group.getId()));
        assertEquals(1,group.getStudents().size());assertEquals(1,submissions.findByGroupId(group.getId()).size());
    }
    @Test void pendingMemberCanBeClearedAndAddedToExistingSuccessor(){
        asCoordinator();var pending=group.getStudents().get(1);
        academic.progression(pending.getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.PENDING,"Incomplete work"));
        var section=targetSection();var carry=new AcademicController.Carry(section.getSubject().getId(),section.getAcademicTerm());
        var id=(Long)academic.carry(group.getId(),carry).get("id");
        assertEquals(1,em.find(ProjectGroup.class,id).getStudents().size());
        assertEquals(TERM,pending.getClassSection().getAcademicTerm());
        academic.progression(pending.getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.ELIGIBLE,"Cleared by professor"));
        assertEquals(id,academic.carry(group.getId(),carry).get("id"));
        assertEquals(2,em.find(ProjectGroup.class,id).getStudents().size());
        assertTrue(em.find(ProjectGroup.class,id).getStudents().stream().allMatch(e->e.getProgression()==StudentEnrollment.Progression.PENDING));
    }
    @Test void repeatCreatesNewEnrollmentOnlyInSameSubjectAndLaterTerm(){
        asCoordinator();var old=group.getStudents().get(0);
        var later=new ClassSection();later.setSubject(group.getSubject());later.setSection("REPEAT");later.setAcademicTerm("2026-2027 / 2");em.persist(later);
        assertThrows(IllegalArgumentException.class,()->academic.repeat(old.getId(),new AcademicController.RepeatInput(later.getId())));
        academic.progression(old.getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.REPEAT,"Professor requires a repeat"));
        assertThrows(IllegalArgumentException.class,()->academic.repeat(old.getId(),new AcademicController.RepeatInput(old.getClassSection().getId())));
        assertThrows(IllegalArgumentException.class,()->academic.repeat(old.getId(),new AcademicController.RepeatInput(targetSection().getId())));
        var result=academic.repeat(old.getId(),new AcademicController.RepeatInput(later.getId()));
        var repeated=em.find(StudentEnrollment.class,(Long)result.get("id"));
        assertEquals(old.getId(),repeated.getPreviousEnrollmentId());assertEquals(old.getStudent(),repeated.getStudent());
        assertEquals(StudentEnrollment.Progression.PENDING,repeated.getProgression());assertTrue(repeated.getGroups().isEmpty());
        assertEquals(TERM,old.getClassSection().getAcademicTerm());assertTrue(group.getStudents().contains(old));
        assertThrows(IllegalArgumentException.class,()->academic.repeat(old.getId(),new AcademicController.RepeatInput(later.getId())));
    }
    @Test void groupDecisionUpdatesCurrentMembersButExcludesRemovedMembers(){
        asCoordinator();var removed=group.getStudents().get(0);
        groupService.removeStudents(group.getId(),List.of(removed.getId()));
        var response=academic.groupProgression(group.getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.REPEAT,"Group must repeat"));
        assertEquals(1,response.get("updated"));
        assertEquals(StudentEnrollment.Progression.ELIGIBLE,removed.getProgression());
        assertEquals(StudentEnrollment.Progression.REPEAT,group.getStudents().get(0).getProgression());
    }
    @Test void repeatGroupKeepsTeamAndSectionsInLaterTerm(){
        asCoordinator();academic.groupProgression(group.getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.REPEAT,"Professor decision"));
        var result=academic.repeatGroup(group.getId(),new AcademicController.TermAssignment("2026-2027 / 2"));
        var next=em.find(ProjectGroup.class,(Long)result.get("id"));
        assertEquals(2,next.getStudents().size());assertEquals(group.getSubject(),next.getSubject());
        assertEquals(group.getAdviser(),next.getAdviser());assertEquals(group.getId(),next.getPreviousGroupId());
        assertEquals(Set.of("A","B"),new HashSet<>(next.getStudents().stream().map(e->e.getClassSection().getSection()).toList()));
        assertTrue(next.getStudents().stream().allMatch(e->e.getProgression()==StudentEnrollment.Progression.PENDING));
        assertEquals(2,group.getStudents().size());
        assertThrows(IllegalArgumentException.class,()->academic.repeatGroup(group.getId(),new AcademicController.TermAssignment("2026-2027 / 2")));
    }
    @Test void defaultPendingAndRepeatCannotAdvance(){
        asCoordinator();assertEquals(StudentEnrollment.Progression.PENDING,new StudentEnrollment().getProgression());
        group.getStudents().get(0).setProgression(StudentEnrollment.Progression.PENDING);
        group.getStudents().get(1).setProgression(StudentEnrollment.Progression.REPEAT);
        var section=targetSection();
        assertThrows(IllegalArgumentException.class,()->academic.carry(group.getId(),new AcademicController.Carry(section.getSubject().getId(),section.getAcademicTerm())));
        assertThrows(IllegalArgumentException.class,()->academic.progression(group.getStudents().get(0).getId(),new AcademicController.ProgressionInput(StudentEnrollment.Progression.ELIGIBLE,"")));
    }
    @Test void groupCreationRequiresSubjectAndDuplicateMemberInputsAreDeduplicated(){
        var request=new com.intellitrack.dto.GroupRequest();request.setCode("NEW");request.setTitle("New group");request.setAcademicTerm(TERM);
        assertThrows(IllegalArgumentException.class,()->groupService.createGroup(request));
        Long enrollment=group.getStudents().get(0).getId();
        groupService.assignStudents(group.getId(),List.of(enrollment,enrollment));
        assertEquals(2,group.getStudents().size());
    }
    @Test void importDoesNotTransferOwnershipByStudentNumberAndRejectsCrossSectionDuplicates()throws Exception{
        alice.setStudentId("20-0000-001");em.flush();
        byte[] bytes;
        try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var output=new java.io.ByteArrayOutputStream()){
            var sheet=workbook.createSheet("Classlist");sheet.createRow(0);
            for(int i=1;i<=2;i++){
                var row=sheet.createRow(i);String[] values={"20-0000-001","Imported Student","new"+i+"@example.test","Capstone 1","CAP1",i==1?"C":"D",""};
                for(int column=0;column<values.length;column++)row.createCell(column).setCellValue(values[column]);
            }
            workbook.write(output);bytes=output.toByteArray();
        }
        imports.importClasslist(new org.springframework.mock.web.MockMultipartFile("file","classlist.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes),"test","TEST",TERM);
        var imported=enrollments.findAll().stream().filter(e->"20-0000-001".equals(e.getStudentId())).toList();
        assertEquals(1,imported.size());assertNull(imported.get(0).getStudent());
    }

    ClassSection targetSection(){
        var subject=new Subject();subject.setCode("CAP2");subject.setName("Capstone 2");em.persist(subject);
        var section=new ClassSection();section.setSubject(subject);section.setSection("A");section.setAcademicTerm("2026-2027 / 2");return em.persist(section);
    }
    @Test void carryRejectsAnotherAccountWithTheSameStudentNumber(){
        asCoordinator();var section=targetSection();
        var wrong=enrollment(user("unrelated","student"),section);
        wrong.setStudentId(group.getStudents().get(0).getStudentId());em.flush();
        var error=assertThrows(IllegalArgumentException.class,()->academic.carry(group.getId(),new AcademicController.Carry(section.getSubject().getId(),section.getAcademicTerm())));
        assertTrue(error.getMessage().contains("Conflicting student identity"));
        assertEquals("unrelated",wrong.getStudent().getFirstName());
    }
    @Test void carryReusesSameAccountDespiteChangedEnrollmentDetails(){
        asCoordinator();var section=targetSection();var existing=enrollment(alice,section);
        existing.setStudentId("corrected-number");existing.setEmail("old-address@example.test");em.flush();
        var result=academic.carry(group.getId(),new AcademicController.Carry(section.getSubject().getId(),section.getAcademicTerm()));
        var target=em.find(ProjectGroup.class,(Long)result.get("id"));
        assertTrue(target.getStudents().stream().anyMatch(e->e.getId().equals(existing.getId())));
        assertEquals(1,enrollments.findAll().stream().filter(e->e.getClassSection().getId().equals(section.getId())&&e.getStudent()!=null&&e.getStudent().getId().equals(alice.getId())).count());
    }
    @Test void carryRejectsAmbiguousTargetEnrollments(){
        asCoordinator();var section=targetSection();enrollment(alice,section);enrollment(alice,section);em.flush();
        var error=assertThrows(IllegalArgumentException.class,()->academic.carry(group.getId(),new AcademicController.Carry(section.getSubject().getId(),section.getAcademicTerm())));
        assertTrue(error.getMessage().contains("duplicate target enrollments"));
    }
    @Test void manualEnrollmentRejectsUnlinkedImportsByEmailOrStudentNumber(){
        var section=targetSection();var imported=enrollment(alice,section);imported.setStudent(null);
        imported.setEmail(alice.getEmail().toUpperCase());
        var other=new ClassSection();other.setSubject(section.getSubject());other.setSection("B");other.setAcademicTerm(section.getAcademicTerm());em.persist(other);em.flush();
        assertEquals(409,subjectController.addStudentManually(other.getId(),Map.of("userId",alice.getId())).getStatusCode().value());
        imported.setEmail("different@example.test");alice.setStudentId(imported.getStudentId());em.flush();
        assertEquals(409,subjectController.addStudentManually(other.getId(),Map.of("userId",alice.getId())).getStatusCode().value());
        assertNull(imported.getStudent());
        // The same student can still enroll in a separate term.
        other.setAcademicTerm("2027-2028 / 1");em.flush();
        assertEquals(200,subjectController.addStudentManually(other.getId(),Map.of("userId",alice.getId())).getStatusCode().value());
    }
    @Test void extendedDeadlineUpdatesHistoryStatusWithoutRewritingOriginalRecord(){
        asCoordinator();Long id=links.submit(alice.getId(),request(),request().googleDocUrl());
        var controller=new com.intellitrack.controller.TrackingController(tracking,access,links,org.mockito.Mockito.mock(GoogleDocLinkService.class),submissions,versions);
        assertEquals("SUBMITTED_LATE",controller.history(id).get(0).get("status"));
        var due=LocalDateTime.now(ZoneId.of("Asia/Manila")).plusDays(1);
        academic.requirement(new AcademicController.Requirement(requirement.getId(),group.getSubject().getId(),TERM,"Proposal",due,"Extension",true));
        var history=controller.history(id).get(0);
        assertEquals(tracking.rows(alice).get(0).status(),history.get("status"));
        assertEquals("SUBMITTED_ON_TIME",history.get("status"));
        assertEquals("SUBMITTED_LATE",history.get("recordedStatus"));
        assertEquals("SUBMITTED_LATE",versions.findBySubmissionIdOrderByVersionNumberDesc(id).get(0).getStatus());
    }
}
