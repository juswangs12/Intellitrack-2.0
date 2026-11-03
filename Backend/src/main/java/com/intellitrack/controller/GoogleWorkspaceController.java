package com.intellitrack.controller;

import com.intellitrack.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController @RequestMapping("/api/google")
public class GoogleWorkspaceController {
    private final GoogleWorkspaceService google; private final TrackingService tracking; private final AccessService access;
    @org.springframework.beans.factory.annotation.Autowired private WorkspaceHistoryService history;
    @Value("${app.calendar-automation.enabled:true}") private boolean calendarAutomation;
    @GetMapping("/reports/history") @PreAuthorize("hasAnyRole('coordinator','administrator')") public List<Map<String,Object>> reportHistory(){var u=access.current();return history.list(u,"REPORT_EXPORT",tracking.rows(u));}
    @Value("${app.frontend.base-url:http://localhost:3000}") private String frontend;
    private final TrackingSummaryService summaries;
    public GoogleWorkspaceController(GoogleWorkspaceService g,TrackingService t,AccessService a,TrackingSummaryService s){google=g;tracking=t;access=a;summaries=s;}
    @GetMapping("/status") public Map<String,Object> status(){var u=access.current();return Map.of("calendarAutomation",calendarAutomation,"configured",google.configured(),"calendar",google.connected(u.getId(),"calendar"),"reports",google.connected(u.getId(),"reports"),"imports",google.connected(u.getId(),"imports"));}
    @PostMapping("/connect/{purpose}") public Map<String,String> connect(@PathVariable String purpose){return google.start(access.current(),purpose);}
    @GetMapping("/imports/files") @PreAuthorize("hasAnyRole('coordinator','administrator')")
    public Map<String,Object> importFiles(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String page){return google.classlistFiles(access.current(),search,page);}
    // No token exchange here: completion also requires the initiating user's JWT and browser-held state.
    @GetMapping("/callback") public void callback(@RequestParam(required=false) String code,@RequestParam(required=false) String state,@RequestParam(required=false) String error,HttpServletResponse response)throws java.io.IOException{
        response.setHeader("Referrer-Policy","no-referrer");response.setHeader("Cache-Control","no-store");
        response.sendRedirect(frontend+"/google-connect#"+(error!=null?"error=access_denied":"code="+enc(Objects.toString(code,""))+"&state="+enc(Objects.toString(state,""))));
    }
    @PostMapping("/complete") public Map<String,String> complete(@RequestBody Map<String,String> body){
        String state=body.get("state"),code=body.get("code");if(state==null||code==null)throw new IllegalArgumentException("Missing Google authorization response");
        return Map.of("purpose",google.finish(access.current(),state,code));
    }
    @PostMapping("/calendar/sync") @PreAuthorize("hasRole('student')")
    public Map<String,Object> sync(@RequestParam String term){
        // Sync all terms to reconcile stale events; term is used to reject accidental unscoped requests.
        TrackingPolicy.term(term);if(term.isBlank())throw new IllegalArgumentException("Select an academic term");
        var u=access.current();return google.sync(u,tracking.filter(tracking.rows(u),Map.of()).stream().filter(r->r.academicTerm()!=null).toList());
    }
    @PostMapping("/reports/export") @PreAuthorize("hasAnyRole('coordinator','administrator')")
    public Map<String,String> export(@RequestParam Map<String,String> filters){var u=access.current();var rows=tracking.filter(tracking.rows(u),filters);var result=google.export(u,rows,filters,tracking.summary(rows),summaries.summarize(u.getId(),u.getRole(),rows));history.save(u,"REPORT_EXPORT",filters,rows,result);return result;}
    private String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
