package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.ClasslistImportResultDto;
import com.intellitrack.service.ClasslistImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/classlist")
@CrossOrigin(origins = "http://localhost:3000")
public class ClasslistImportController {

    @Autowired
    private ClasslistImportService classlistImportService;

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ClasslistImportResultDto>> importClasslist(
            @RequestParam("file") MultipartFile file) {
        try {
            ClasslistImportResultDto result = classlistImportService.importClasslist(file);
            return ResponseEntity.ok(ApiResponse.success("Classlist imported successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to import classlist: " + e.getMessage()));
        }
    }
}