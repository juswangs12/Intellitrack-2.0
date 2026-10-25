package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.ClasslistImportResultDto;
import com.intellitrack.dto.ManualEnrollmentRequest;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.service.ClasslistImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/classlist")
public class ClasslistImportController {

    @Autowired
    private ClasslistImportService classlistImportService;

    @Autowired
    private UserRepository userRepository;
    @Autowired private com.intellitrack.service.GoogleWorkspaceService google;
    public record GoogleImport(String url,String worksheet,String term) {}
    @PostMapping("/import-google")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('coordinator','administrator')")
    public ResponseEntity<ApiResponse<ClasslistImportResultDto>> importGoogle(@RequestBody GoogleImport input,HttpServletRequest request,Authentication authentication)throws Exception{
        var user=userRepository.findById((Long)authentication.getPrincipal()).orElseThrow();
        var file=new com.intellitrack.service.SpreadsheetImportFile(google.classlistRows(user,input.url(),input.worksheet()));
        return ResponseEntity.ok(ApiResponse.success("Google classlist imported",classlistImportService.importClasslist(file,user.getEmail(),request.getRemoteAddr(),input.term())));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ClasslistImportResultDto>> importClasslist(
            @RequestParam("file") MultipartFile file,
            @RequestParam String term,
            HttpServletRequest request,
            Authentication authentication) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            String ipAddress = request.getRemoteAddr();
            User currentUser = userRepository.findById(userId).orElse(null);
            String performedBy = currentUser != null ? currentUser.getEmail() : String.valueOf(userId);
            
            ClasslistImportResultDto result = classlistImportService.importClasslist(file, performedBy, ipAddress, term);
            return ResponseEntity.ok(ApiResponse.success("Classlist imported successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to import classlist: " + e.getMessage()));
        }
    }

    @PostMapping("/add-student")
    public ResponseEntity<ApiResponse<Void>> addStudent(
            @RequestBody ManualEnrollmentRequest request) {
        try {
            classlistImportService.addStudent(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Student enrolled successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(
                    "[" + e.getClass().getSimpleName() + "] " + e.getMessage()));
        }
    }
}
