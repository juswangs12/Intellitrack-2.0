package com.intellitrack.service;

import com.intellitrack.controller.CoordinatorCalendarController;
import com.intellitrack.entity.*;
import com.intellitrack.repository.DeliverableRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;

class CoordinatorCalendarTest {
    @Test void syncUsesActiveCatalogAcrossTermsWithoutRequiringEnrollment(){
        var repo=mock(DeliverableRepository.class);var access=mock(AccessService.class);var google=mock(GoogleWorkspaceService.class);
        var user=new User("Test","Coordinator","test@example.test","hash","coordinator");
        when(access.current()).thenReturn(user);
        var subject=new Subject();subject.setCode("CAP1");
        var first=deadline(1L,subject,"2026-2027 / 1");var second=deadline(2L,subject,"2026-2027 / 2");
        var archived=deadline(3L,subject,"2026-2027 / 1");archived.setArchived(true);
        var inactive=deadline(4L,subject,"2026-2027 / 1");inactive.setActive(false);
        when(repo.findAllWithSubjectAndSection()).thenReturn(List.of(first,second,archived,inactive));
        new CoordinatorCalendarController(repo,access,google,mock(SemesterService.class)).sync();
        verify(google).syncDeadlines(user,List.of(
            new GoogleWorkspaceService.CalendarDeadline(1L,"CAP1","Proposal",first.getAcademicTerm(),first.getDueAt()),
            new GoogleWorkspaceService.CalendarDeadline(2L,"CAP1","Proposal",second.getAcademicTerm(),second.getDueAt())));
    }
    private Deliverable deadline(Long id,Subject subject,String term){
        var d=new Deliverable();d.setId(id);d.setSubject(subject);d.setName("Proposal");d.setAcademicTerm(term);d.setActive(true);d.setArchived(false);d.setDueAt(LocalDateTime.of(2026,10,25,23,59));return d;
    }
}
