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
        service.assigned(new ActivityEmailService.AdviserAssigned(1L,2L,"event"));service.prepareDigests(LocalDateTime.now());verifyNoInteractions(emails,users);
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
