package com.intellitrack.service;
import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ActivityEmailServiceTest {
    @Test void coordinatorWelcomeIsDeduplicatedAndCancelledAfterRoleChange(){
        var emails=mock(DeadlineEmailRepository.class);
        var service=new ActivityEmailService(emails,mock(UserRepository.class),mock(ProjectGroupRepository.class),mock(TrackingService.class),mock(SemesterService.class));
        ReflectionTestUtils.setField(service,"enabled",true);
        ReflectionTestUtils.setField(service,"frontend","https://intellitrack-pied.vercel.app/");
        Map<String,DeadlineEmail> jobs=new HashMap<>();
        when(emails.existsById(anyString())).thenAnswer(i->jobs.containsKey(i.getArgument(0)));
        when(emails.save(any())).thenAnswer(i->{DeadlineEmail job=i.getArgument(0);jobs.put(job.getId(),job);return job;});
        service.created(new ActivityEmailService.CoordinatorCreated(7L));
        service.created(new ActivityEmailService.CoordinatorCreated(7L));
        assertEquals(1,jobs.size());verify(emails,times(1)).save(any());
        var job=jobs.values().iterator().next();assertEquals(7L,job.getUserId());assertTrue(service.handles(job));
        var user=new User();user.setId(7L);user.setRole("coordinator");user.setFirstName("Test");user.setPassword("SECRET-PASSWORD");
        var message=service.message(job,user,LocalDateTime.now()).orElseThrow();
        assertTrue(message.subject().contains("Coordinator"));
        assertTrue(message.body().contains("https://intellitrack-pied.vercel.app/staff/login"));
        assertFalse(message.body().contains(user.getPassword()));
        user.setRole("adviser");assertTrue(service.message(job,user,LocalDateTime.now()).isEmpty());
        assertTrue(service.message(job,null,LocalDateTime.now()).isEmpty());
    }
    @Test void importedStudentWithoutAccountGetsOneWelcomeAndStaleEnrollmentIsCancelled(){
        var emails=mock(DeadlineEmailRepository.class);var enrollments=mock(StudentEnrollmentRepository.class);var semesters=mock(SemesterService.class);
        var service=new ActivityEmailService(emails,mock(UserRepository.class),mock(ProjectGroupRepository.class),mock(TrackingService.class),semesters);
        ReflectionTestUtils.setField(service,"enabled",true);ReflectionTestUtils.setField(service,"enrollments",enrollments);ReflectionTestUtils.setField(service,"frontend","https://intellitrack-pied.vercel.app/");
        var subject=new Subject();subject.setCode("IT332");subject.setName("Capstone 1");var section=new ClassSection();section.setId(5L);section.setSubject(subject);section.setSection("G01");section.setAcademicTerm("2026-2027 / 1");
        var enrollment=new StudentEnrollment();enrollment.setId(9L);enrollment.setEmail("student@example.test");enrollment.setFullName("Test Student");enrollment.setClassSection(section);when(enrollments.findById(9L)).thenReturn(Optional.of(enrollment));
        Map<String,DeadlineEmail> jobs=new HashMap<>();when(emails.existsById(anyString())).thenAnswer(i->jobs.containsKey(i.getArgument(0)));when(emails.save(any())).thenAnswer(i->{DeadlineEmail job=i.getArgument(0);jobs.put(job.getId(),job);return job;});
        service.imported(new ActivityEmailService.StudentImported(9L));service.imported(new ActivityEmailService.StudentImported(9L));assertEquals(1,jobs.size());
        var job=jobs.values().iterator().next();assertNull(job.getUserId());var message=service.message(job,null,LocalDateTime.now()).orElseThrow();assertEquals("student@example.test",message.recipient());assertTrue(message.body().contains("IT332 — Capstone 1"));assertTrue(message.body().contains("G01"));assertTrue(message.body().contains("https://intellitrack-pied.vercel.app/student/login"));
        when(semesters.archived(section.getAcademicTerm())).thenReturn(true);assertTrue(service.message(job,null,LocalDateTime.now()).isEmpty());when(semesters.archived(section.getAcademicTerm())).thenReturn(false);
        enrollment.setEmail("changed@example.test");assertTrue(service.message(job,null,LocalDateTime.now()).isEmpty());
        when(enrollments.findById(9L)).thenReturn(Optional.empty());assertTrue(service.message(job,null,LocalDateTime.now()).isEmpty());
    }
    @Test void adviserWelcomeContainsStaffLinkAndRequiresCurrentAdviserRole(){
        var emails=mock(DeadlineEmailRepository.class);var service=new ActivityEmailService(emails,mock(UserRepository.class),mock(ProjectGroupRepository.class),mock(TrackingService.class),mock(SemesterService.class));
        ReflectionTestUtils.setField(service,"enabled",true);ReflectionTestUtils.setField(service,"frontend","https://intellitrack-pied.vercel.app");
        service.created(new ActivityEmailService.AdviserCreated(1L));verify(emails).save(any());
        var user=new User();user.setId(1L);user.setRole("adviser");user.setFirstName("Test");var job=new DeadlineEmail();job.setKind(ActivityEmailService.ADVISER_CREATED);
        assertTrue(service.message(job,user,LocalDateTime.now()).orElseThrow().body().contains("https://intellitrack-pied.vercel.app/staff/login"));
        user.setRole("student");assertTrue(service.message(job,user,LocalDateTime.now()).isEmpty());
    }
    @Test void dailyDigestIsOncePerStaffAccountPerDayAndNotBeforeEight(){
        var emails=mock(DeadlineEmailRepository.class);var users=mock(UserRepository.class);
        var service=new ActivityEmailService(emails,users,mock(ProjectGroupRepository.class),mock(TrackingService.class),mock(SemesterService.class));
        ReflectionTestUtils.setField(service,"enabled",true);
        var coord=new User();coord.setId(1L);coord.setRole("coordinator");var student=new User();student.setId(2L);student.setRole("student");
        when(users.findAll()).thenReturn(List.of(coord,student));
        Set<String> ids=new HashSet<>();when(emails.existsById(anyString())).thenAnswer(i->ids.contains(i.getArgument(0)));when(emails.save(any())).thenAnswer(i->{DeadlineEmail e=i.getArgument(0);ids.add(e.getId());return e;});
        var morning=LocalDateTime.of(2026,11,2,8,0);service.prepareDigests(morning.minusMinutes(1));verifyNoInteractions(emails);
        service.prepareDigests(morning);service.prepareDigests(morning.plusHours(1));verify(emails,times(1)).save(any());
        service.prepareDigests(morning.plusDays(1));verify(emails,times(2)).save(any());
    }
    @Test void disabledAutomationDoesNotQueueAnything(){
        var emails=mock(DeadlineEmailRepository.class);var users=mock(UserRepository.class);
        var service=new ActivityEmailService(emails,users,mock(ProjectGroupRepository.class),mock(TrackingService.class),mock(SemesterService.class));
        service.created(new ActivityEmailService.CoordinatorCreated(1L));
        service.assigned(new ActivityEmailService.AdviserAssigned(1L,2L,"event"));service.imported(new ActivityEmailService.StudentImported(1L));service.created(new ActivityEmailService.AdviserCreated(1L));service.prepareDigests(LocalDateTime.now());verifyNoInteractions(emails,users);
    }
    @Test void reassignedOrArchivedGroupDoesNotEmailFormerAdviser(){
        var groups=mock(ProjectGroupRepository.class);var semesters=mock(SemesterService.class);
        var service=new ActivityEmailService(mock(DeadlineEmailRepository.class),mock(UserRepository.class),groups,mock(TrackingService.class),semesters);
        var user=new User();user.setId(1L);user.setRole("adviser");var other=new User();other.setId(2L);
        var group=new ProjectGroup();group.setAdviser(other);group.setAcademicTerm("2026-2027 / 1");when(groups.findByIdWithStudents(3L)).thenReturn(Optional.of(group));
        var job=new DeadlineEmail();job.setKind(ActivityEmailService.ASSIGNMENT);job.setGroupId(3L);
        assertTrue(service.message(job,user,LocalDateTime.now()).isEmpty());
        group.setAdviser(user);when(semesters.archived(group.getAcademicTerm())).thenReturn(true);assertTrue(service.message(job,user,LocalDateTime.now()).isEmpty());
    }
}
