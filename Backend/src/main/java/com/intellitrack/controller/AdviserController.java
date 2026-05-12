package com.intellitrack.controller;

import com.intellitrack.dto.ProjectGroupDTO;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/adviser")
@CrossOrigin(origins = "http://localhost:3000")
@Transactional(readOnly = true)
public class AdviserController {

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @GetMapping("/{adviserId}/groups")
    public ResponseEntity<ApiResponse<List<ProjectGroupDTO>>> getAdviserGroups(@PathVariable Long adviserId) {
        List<ProjectGroup> groups = projectGroupRepository.findByAdviserId(adviserId);
        List<ProjectGroupDTO> dtos = groups.stream().map(ProjectGroupDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<ProjectGroupDTO>> getGroupDetails(@PathVariable Long groupId) {
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new com.intellitrack.exception.ResourceNotFoundException("Group not found"));
        return ResponseEntity.ok(ApiResponse.success(new ProjectGroupDTO(group)));
    }
}
