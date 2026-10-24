package com.intellitrack.controller;
import com.intellitrack.service.DeadlineAutomationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
@RestController
public class DeadlineAutomationController {
    private final DeadlineAutomationService service;
    @Value("${app.deadline-automation.trigger-secret:}") private String secret="";
    public DeadlineAutomationController(DeadlineAutomationService service){this.service=service;}
    @GetMapping("/api/admin/automation") public Map<String,Object> status(){return service.status();}
    @PostMapping("/api/automation/deadlines/run") public Map<String,String> trigger(@RequestHeader(value="X-Automation-Key",defaultValue="") String supplied){
        if(secret.length()<32||!MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        service.run();return Map.of("status","completed");
    }
}
