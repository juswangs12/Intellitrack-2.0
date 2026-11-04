package com.intellitrack.controller;

import com.intellitrack.dto.*;
import com.intellitrack.service.*;
import com.intellitrack.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {
    private final TrackingService tracking; private final AccessService access;
    private final LinkSubmissionService links; private final GoogleDocLinkService docs;
    private final SubmissionRepository submissions; private final SubmissionVersionRepository versions;
    @org.springframework.beans.factory.annotation.Autowired private TrackingSummaryService summaries;
    @org.springframework.beans.factory.annotation.Autowired private WorkspaceHistoryService history;
    @GetMapping("/summary/history") public List<Map<String,Object>> summaryHistory(){var u=access.current();return history.list(u,"AI_SUMMARY",tracking.rows(u));}
    @GetMapping("/import/history") @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('coordinator','administrator')") public List<Map<String,Object>> importHistory(){return history.imports(access.current());}
    @PostMapping("/summary")
    public Map<String,Object> summary(@RequestParam Map<String,String> filters){var u=access.current();var rows=tracking.filter(tracking.rows(u),filters);var result=summaries.summarize(u.getId(),u.getRole(),rows);history.save(u,"AI_SUMMARY",filters,rows,result);return result;}
    public TrackingController(TrackingService t,AccessService a,LinkSubmissionService l,GoogleDocLinkService d,SubmissionRepository s,SubmissionVersionRepository v) {
        tracking=t; access=a; links=l; docs=d; submissions=s; versions=v;
    }
    @GetMapping
    public Map<String,Object> list(@RequestParam Map<String,String> filters) {
        var user=access.current();
        var all=tracking.rows(user);
        if ("true".equals(filters.get("bootstrap"))) {
            var effective=new LinkedHashMap<>(filters);
            effective.remove("bootstrap");effective.remove("defaultTerm");effective.remove("defaultSubject");
            var catalogSet=new LinkedHashSet<>(tracking.filter(all,Map.of()));
            if (filters.containsKey("term")) catalogSet.addAll(tracking.filter(all,Map.of("term",filters.get("term"))));
            var catalog=new ArrayList<>(catalogSet);
            if ("true".equals(filters.get("defaultTerm"))) effective.put("term",catalog.stream().map(TrackingRow::academicTerm).filter(Objects::nonNull).sorted(Comparator.reverseOrder()).findFirst().orElse(""));
            if (!"student".equals(user.getRole()) && "true".equals(filters.get("defaultSubject"))) effective.put("subject",catalog.stream().filter(r->effective.getOrDefault("term","").isEmpty() || Objects.equals(r.academicTerm(),effective.get("term"))).map(r->String.valueOf(r.subjectId())).findFirst().orElse(""));
            var rows=tracking.filter(all,effective);
            return Map.of("rows",rows,"summary",tracking.summary(rows),"catalog",catalog,"filters",effective,"terms",all.stream().map(TrackingRow::academicTerm).filter(Objects::nonNull).distinct().sorted(Comparator.reverseOrder()).toList());
        }
        var rows=tracking.filter(all,filters);
        return Map.of("rows",rows,"summary",tracking.summary(rows),"terms",all.stream().map(TrackingRow::academicTerm).filter(Objects::nonNull).distinct().sorted(Comparator.reverseOrder()).toList());
    }
    @GetMapping("/enrollments")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('student')")
    public List<TrackingService.EnrollmentView> enrollments() {
        return tracking.studentEnrollments(access.current());
    }
    @PostMapping("/submit")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('student')")
    public Map<String,Long> submit(@RequestBody GoogleDocSubmissionRequest request) {
        if(request.groupId()==null||request.deliverableId()==null) throw new IllegalArgumentException("Choose a group and requirement");
        if(request.notes()!=null&&request.notes().length()>2000) throw new IllegalArgumentException("Notes must be at most 2000 characters");
        return Map.of("id",links.submit(access.current().getId(),request,docs.verify(request.googleDocUrl())));
    }
    @GetMapping("/{id}/history")
    @Transactional(readOnly=true)
    public List<Map<String,Object>> history(@PathVariable Long id) {
        var s=submissions.findById(id).orElseThrow(); access.submission(s);
        return versions.findBySubmissionIdOrderByVersionNumberDesc(id).stream().map(v->{
            Map<String,Object> row=new LinkedHashMap<>(); row.put("version",v.getVersionNumber()); row.put("submittedAt",v.getSubmittedAt());
            row.put("status",TrackingPolicy.status(v.getSubmittedAt(),s.getDeliverable().getDueAt(),java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Manila"))));
            row.put("recordedStatus",v.getStatus()); row.put("googleDocUrl",v.getGoogleDocUrl()); return row;
        }).toList();
    }
}
