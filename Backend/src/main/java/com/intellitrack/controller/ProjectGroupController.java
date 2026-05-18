package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.ProjectGroupDTO;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.service.ProjectGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:3000")
public class ProjectGroupController {

    @Autowired
    private ProjectGroupService projectGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectGroupDTO>>> getAllGroups() {
        return ResponseEntity.ok(ApiResponse.success(projectGroupService.getAllGroups()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectGroupDTO>> createGroup(@RequestBody ProjectGroup group) {
        return ResponseEntity.ok(ApiResponse.success("Group created successfully", projectGroupService.createGroup(group)));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<ProjectGroupDTO>> updateGroup(
            @PathVariable Long groupId,
            @RequestBody ProjectGroup groupDetails) {
        return ResponseEntity.ok(ApiResponse.success("Group updated successfully", projectGroupService.updateGroup(groupId, groupDetails)));
    }

    @PostMapping("/{groupId}/assign-adviser/{adviserId}")
    public ResponseEntity<ApiResponse<ProjectGroupDTO>> assignAdviser(@PathVariable Long groupId, @PathVariable Long adviserId) {
        return ResponseEntity.ok(ApiResponse.success("Adviser assigned", projectGroupService.assignAdviser(groupId, adviserId)));
    }



    @PostMapping("/{groupId}/students")
    public ResponseEntity<ApiResponse<ProjectGroupDTO>> assignStudents(
            @PathVariable Long groupId,
            @RequestBody List<Long> enrollmentIds) {
        return ResponseEntity.ok(ApiResponse.success("Students assigned successfully", projectGroupService.assignStudents(groupId, enrollmentIds)));
    }

    @DeleteMapping("/{groupId}/students")
    public ResponseEntity<ApiResponse<ProjectGroupDTO>> removeStudents(
            @PathVariable Long groupId,
            @RequestBody List<Long> studentIds) {
        return ResponseEntity.ok(ApiResponse.success("Students removed successfully", projectGroupService.removeStudents(groupId, studentIds)));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        projectGroupService.deleteGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success("Group deleted successfully", null));
    }
}
