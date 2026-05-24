package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.SubjectRepository;
import com.intellitrack.repository.ClassSectionRepository;
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
            if (deliverable.getSubject() != null && deliverable.getSubject().getId() != null) {
                deliverable.setSubject(subjectRepository.findById(deliverable.getSubject().getId()).orElse(null));
            }
            if (deliverable.getSection() != null && deliverable.getSection().getId() != null) {
                deliverable.setSection(classSectionRepository.findById(deliverable.getSection().getId()).orElse(null));
            }
            return ResponseEntity.ok(ApiResponse.success("Deliverable created", deliverableRepository.save(deliverable)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create deliverable: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Deliverable>> updateDeliverable(@PathVariable Long id,
            @RequestBody Deliverable deliverable) {
        try {
            return deliverableRepository.findById(id)
                    .map(existing -> {
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
                            existing.setSubject(subjectRepository.findById(deliverable.getSubject().getId()).orElse(null));
                        } else {
                            existing.setSubject(null);
                        }
                        if (deliverable.getSection() != null && deliverable.getSection().getId() != null) {
                            existing.setSection(classSectionRepository.findById(deliverable.getSection().getId()).orElse(null));
                        } else {
                            existing.setSection(null);
                        }
                        return ResponseEntity
                                .ok(ApiResponse.success("Deliverable updated", deliverableRepository.save(existing)));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update deliverable: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDeliverable(@PathVariable Long id) {
        if (!deliverableRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        deliverableRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Deliverable deleted", null));
    }
}
