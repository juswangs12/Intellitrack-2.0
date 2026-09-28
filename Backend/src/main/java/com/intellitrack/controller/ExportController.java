package com.intellitrack.controller;

import com.intellitrack.entity.User;
import com.intellitrack.exception.GoogleSheetsExportException;
import com.intellitrack.service.GoogleSheetsService;
import com.intellitrack.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    private final GoogleSheetsService googleSheetsService;
    private final UserService userService;

    public ExportController(GoogleSheetsService googleSheetsService, UserService userService) {
        this.googleSheetsService = googleSheetsService;
        this.userService = userService;
    }

    @PostMapping("/analytics/google-sheets")
    @PreAuthorize("hasAnyRole('coordinator', 'administrator')")
    public ResponseEntity<?> generateInstitutionalReport(Authentication authentication) {
        try {
            if (!googleSheetsService.isConfigured()) {
                return ResponseEntity.status(503).body(java.util.Map.of(
                        "configured", false,
                        "message", "Google Sheets integration is not configured."
                ));
            }

            Long userId = (Long) authentication.getPrincipal();
            User user = userService.getUserById(userId);
            
            logger.info("Generating institutional report for user: {}", user.getEmail());
            
            String spreadsheetUrl = googleSheetsService.generateInstitutionalReport(user.getEmail());
            
            return ResponseEntity.ok(com.intellitrack.dto.ApiResponse.success(spreadsheetUrl));
        } catch (GoogleSheetsExportException e) {
            logger.error("Error generating institutional report", e);
            return ResponseEntity.internalServerError()
                    .body(com.intellitrack.dto.ApiResponse.error("Failed to generate institutional report: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error generating institutional report", e);
            return ResponseEntity.internalServerError()
                    .body(com.intellitrack.dto.ApiResponse.error("An unexpected error occurred while generating the report"));
        }
    }
}
