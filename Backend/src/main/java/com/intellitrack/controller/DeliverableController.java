package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.repository.DeliverableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliverables")
@CrossOrigin(origins = "http://localhost:3000")
public class DeliverableController {

    @Autowired
    private DeliverableRepository deliverableRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Deliverable>>> getAllDeliverables() {
        return ResponseEntity.ok(ApiResponse.success(deliverableRepository.findAll()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Deliverable>>> getActiveDeliverables() {
        return ResponseEntity.ok(ApiResponse.success(deliverableRepository.findByActiveTrue()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Deliverable>> createDeliverable(@RequestBody Deliverable deliverable) {
        if (deliverableRepository.existsByName(deliverable.getName())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Deliverable with this name already exists"));
        }
        return ResponseEntity.ok(ApiResponse.success("Deliverable created", deliverableRepository.save(deliverable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Deliverable>> updateDeliverable(@PathVariable Long id, @RequestBody Deliverable deliverable) {
        return deliverableRepository.findById(id)
                .map(existing -> {
                    existing.setName(deliverable.getName());
                    existing.setStage(deliverable.getStage());
                    existing.setActive(deliverable.isActive());
                    return ResponseEntity.ok(ApiResponse.success("Deliverable updated", deliverableRepository.save(existing)));
                })
                .orElse(ResponseEntity.notFound().build());
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
