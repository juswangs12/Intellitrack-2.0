package com.intellitrack.controller;

import com.intellitrack.repository.*;
import com.intellitrack.service.SemesterService;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/overview")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('administrator')")
public class AdminOverviewController {
    private final UserRepository users;
    private final ClassSectionRepository sections;
    private final ProjectGroupRepository groups;
    private final SubmissionRepository submissions;
    private final GoogleConnectionRepository connections;
    private final SemesterService semesters;
    public AdminOverviewController(UserRepository u, ClassSectionRepository s, ProjectGroupRepository g,
            SubmissionRepository sub, GoogleConnectionRepository c, SemesterService sem) {
        users=u;sections=s;groups=g;submissions=sub;connections=c;semesters=sem;
    }
    @GetMapping @Transactional(readOnly=true)
    public Map<String,Object> overview() {
        var now=Instant.now();var zone=ZoneId.of("Asia/Manila");
        var today=LocalDate.now(zone);var start=today.atStartOfDay(zone).toInstant();
        var all=users.findAll();var archived=semesters.archivedTerms();
        var active=sections.findAll().stream().filter(s->s.getAcademicTerm()!=null&&!archived.contains(s.getAcademicTerm())).toList();
        Map<String,Object> result=new LinkedHashMap<>();
        Map<String,Long> roles=new LinkedHashMap<>();
        for(var role:List.of("student","adviser","coordinator","administrator"))roles.put(role,all.stream().filter(u->role.equals(u.getRole())).count());
        result.put("roles",roles);result.put("totalUsers",all.size());
        result.put("awaitingEnrollment",all.stream().filter(u->"student".equals(u.getRole())&&(u.getGoogleId()!=null||u.getGoogleSub()!=null)&&(u.getEnrollments()==null||u.getEnrollments().isEmpty())).count());
        result.put("activeToday",all.stream().filter(u->u.getLastActiveAt()!=null&&!u.getLastActiveAt().isBefore(start)).count());
        result.put("activeWeek",all.stream().filter(u->u.getLastActiveAt()!=null&&!u.getLastActiveAt().isBefore(now.minusSeconds(7*86400))).count());
        List<Map<String,Object>> growth=new ArrayList<>();
        for(int i=6;i>=0;i--){var day=today.minusDays(i);growth.add(Map.of("date",day.toString(),"count",all.stream().filter(u->u.getCreatedAt()!=null&&u.getCreatedAt().toLocalDate().equals(day)).count()));}
        result.put("registrations",growth);
        result.put("recentSignIns",all.stream().filter(u->u.getLastSignInAt()!=null).sorted(Comparator.comparing(com.intellitrack.entity.User::getLastSignInAt).reversed()).limit(10).map(u->Map.of("name",u.getFirstName()+" "+u.getLastName(),"role",u.getRole(),"at",u.getLastSignInAt())).toList());
        result.put("academic",Map.of("terms",active.stream().map(s->s.getAcademicTerm()).distinct().count(),"subjects",active.stream().map(s->s.getSubject().getId()).distinct().count(),"groups",groups.findAll().stream().filter(g->g.getAcademicTerm()!=null&&!archived.contains(g.getAcademicTerm())).count(),"submissions",submissions.count()));
        List<Map<String,Object>> integrations=new ArrayList<>();
        var savedConnections=connections.findAll();
        for(var purpose:List.of("calendar","reports")){
            var matching=savedConnections.stream().filter(c->purpose.equals(c.getPurpose())&&c.getEncryptedRefreshToken()!=null).toList();
            Map<String,Object> item=new LinkedHashMap<>();item.put("purpose",purpose);item.put("connected",matching.size());
            item.put("lastSuccess",matching.stream().map(c->c.getLastSuccessAt()).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));integrations.add(item);
        }
        result.put("integrations",integrations);result.put("checkedAt",now);
        result.put("database","Reachable");
        return result;
    }
}
