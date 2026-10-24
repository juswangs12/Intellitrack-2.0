package com.intellitrack.controller;

import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.service.AccessService;
import com.intellitrack.service.GoogleWorkspaceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/google/calendar/coordinator")
public class CoordinatorCalendarController {
    private final com.intellitrack.service.SemesterService semesters;
    private final DeliverableRepository deliverables;
    private final AccessService access;
    private final GoogleWorkspaceService google;
    public CoordinatorCalendarController(DeliverableRepository d,AccessService a,GoogleWorkspaceService g,com.intellitrack.service.SemesterService semesters){deliverables=d;access=a;google=g;this.semesters=semesters;}

    @PostMapping("/sync")
    @PreAuthorize("hasRole('coordinator')")
    public Map<String,Object> sync(){
        // Always reconcile the entire catalog, never a filtered view that could remove other terms' events.
        var deadlines=deliverables.findAllWithSubjectAndSection().stream()
            .filter(d->!semesters.archived(d.getAcademicTerm())).filter(d->d.isActive()&&!d.isArchived()&&d.getDueAt()!=null)
            .map(d->new GoogleWorkspaceService.CalendarDeadline(d.getId(),d.getSubject()==null?"Unassigned subject":d.getSubject().getCode(),d.getName(),d.getAcademicTerm(),d.getDueAt())).toList();
        return google.syncDeadlines(access.current(),deadlines);
    }
}
