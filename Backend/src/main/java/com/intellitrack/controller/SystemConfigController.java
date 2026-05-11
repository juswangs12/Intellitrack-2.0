package com.intellitrack.controller;

import com.intellitrack.dto.SystemConfigDto;
import com.intellitrack.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system-config")
@CrossOrigin(origins = "http://localhost:3000")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ResponseEntity<SystemConfigDto> getConfig() {
        return ResponseEntity.ok(systemConfigService.getConfig());
    }

    @PutMapping
    public ResponseEntity<SystemConfigDto> update(@RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(systemConfigService.updateSettings(updates));
    }
}

