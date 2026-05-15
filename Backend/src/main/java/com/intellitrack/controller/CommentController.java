package com.intellitrack.controller;

import com.intellitrack.dto.ApiResponse;
import com.intellitrack.dto.SubmissionCommentDto;
import com.intellitrack.entity.SubmissionComment;
import com.intellitrack.entity.User;
import com.intellitrack.repository.SubmissionCommentRepository;
import com.intellitrack.repository.SubmissionRepository;
import com.intellitrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "http://localhost:3000")
public class CommentController {

    @Autowired
    private SubmissionCommentRepository commentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<List<SubmissionCommentDto>>> getComments(@PathVariable Long submissionId) {
        List<SubmissionCommentDto> dtos = commentRepository.findBySubmission_IdOrderByCreatedAtAsc(submissionId)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<SubmissionCommentDto>> addComment(
            @PathVariable Long submissionId,
            @RequestBody SubmissionComment comment,
            Authentication authentication) {

        // Derive userId from the verified JWT principal — never trust a client-supplied
        // value
        Long userId = (Long) authentication.getPrincipal();
        comment.setSubmission(submissionRepository.findById(submissionId).get());
        comment.setAuthor(userRepository.findById(userId).get());
        comment.setCreatedAt(LocalDateTime.now());

        SubmissionComment saved = commentRepository.save(comment);
        return ResponseEntity.ok(ApiResponse.success("Comment added", toDto(saved)));
    }

    private SubmissionCommentDto toDto(SubmissionComment comment) {
        User author = comment.getAuthor();
        return new SubmissionCommentDto(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                author == null ? null : author.getId(),
                author == null ? null : author.getFirstName(),
                author == null ? null : author.getLastName());
    }
}
