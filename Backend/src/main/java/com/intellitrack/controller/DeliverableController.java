package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.SubjectRepository;
import com.intellitrack.repository.ClassSectionRepository;
import com.intellitrack.service.DeliverableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/deliverables")
@CrossOrigin(origins = "http://localhost:3000")
public class DeliverableController {

    @Autowired
    private DeliverableRepository deliverableRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private ClassSectionRepository classSectionRepository;

    @Autowired
    private DeliverableService deliverableService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Deliverable>>> getAllDeliverables() {
        return ResponseEntity.ok(ApiResponse.success(deliverableRepository.findAllWithSubjectAndSection()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Deliverable>>> getActiveDeliverables() {
        return ResponseEntity.ok(ApiResponse.success(deliverableRepository.findActiveWithSubjectAndSection()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Deliverable>> createDeliverable(@RequestBody Deliverable deliverable) {
        try {
            System.out.println("=== Creating new deliverable ===");
            System.out.println("  Name: " + deliverable.getName());
            System.out.println("  Stage: " + deliverable.getStage());
            if (deliverable.getSubject() != null && deliverable.getSubject().getId() != null) {
                System.out.println("  Subject id: " + deliverable.getSubject().getId());
                deliverable.setSubject(subjectRepository.findById(deliverable.getSubject().getId()).orElse(null));
            }
            if (deliverable.getSection() != null && deliverable.getSection().getId() != null) {
                System.out.println("  Section id: " + deliverable.getSection().getId());
                deliverable.setSection(classSectionRepository.findById(deliverable.getSection().getId()).orElse(null));
            }
            Deliverable saved = deliverableRepository.save(deliverable);
            System.out.println("✅ Deliverable created, id=" + saved.getId());
            return ResponseEntity.ok(ApiResponse.success("Deliverable created", saved));
        } catch (Exception e) {
            System.err.println("=== ERROR creating deliverable ===");
            System.err.println("  Error type: " + e.getClass().getName());
            System.err.println("  Error message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create deliverable: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Deliverable>> updateDeliverable(@PathVariable Long id,
            @RequestBody Deliverable deliverable) {
        try {
            System.out.println("=== Updating deliverable id=" + id + " ===");
            return deliverableRepository.findById(id)
                    .map(existing -> {
                        System.out.println("  Found existing deliverable: " + existing.getName());
                        existing.setName(deliverable.getName());
                        existing.setStage(deliverable.getStage());
                        existing.setActive(deliverable.isActive());
                        if (deliverable.getDueAt() != null) {
                            existing.setDueAt(deliverable.getDueAt());
                        }
                        if (deliverable.getAcademicTerm() != null) {
                            existing.setAcademicTerm(deliverable.getAcademicTerm());
                        }
                        if (deliverable.getSubject() != null && deliverable.getSubject().getId() != null) {
                            System.out.println("  Setting subject id=" + deliverable.getSubject().getId());
                            existing.setSubject(
                                    subjectRepository.findById(deliverable.getSubject().getId()).orElse(null));
                        } else {
                            System.out.println("  Setting subject to null");
                            existing.setSubject(null);
                        }
                        if (deliverable.getSection() != null && deliverable.getSection().getId() != null) {
                            System.out.println("  Setting section id=" + deliverable.getSection().getId());
                            existing.setSection(
                                    classSectionRepository.findById(deliverable.getSection().getId()).orElse(null));
                        } else {
                            System.out.println("  Setting section to null");
                            existing.setSection(null);
                        }
                        Deliverable saved = deliverableRepository.save(existing);
                        System.out.println("  Deliverable saved successfully, id=" + saved.getId());
                        return ResponseEntity
                                .ok(ApiResponse.success("Deliverable updated", saved));
                    })
                    .orElseGet(() -> {
                        System.out.println("  Deliverable not found for id=" + id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            System.err.println("=== ERROR updating deliverable id=" + id + " ===");
            System.err.println("  Error type: " + e.getClass().getName());
            System.err.println("  Error message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update deliverable: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDeliverable(@PathVariable Long id) {
        if (!deliverableRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        deliverableService.deleteDeliverable(id);
        return ResponseEntity.ok(ApiResponse.success("Deliverable deleted", null));
    }
}
