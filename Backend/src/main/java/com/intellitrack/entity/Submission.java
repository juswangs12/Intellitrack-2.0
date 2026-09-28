package com.intellitrack.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ProjectGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deliverable_id")
    private Deliverable deliverable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column
    private LocalDateTime submittedAt;

    @Deprecated
    @Column(nullable = false)
    private Integer versionNumber = 1;

    @Deprecated
    @Column(nullable = false)
    private Integer revisionCount = 0;

    @Deprecated
    @Column
    private String fileUrl;

    @Column
    private String googleDocUrl;

    @Column
    private LocalDateTime lastModifiedAt;

    @Column
    private LocalDateTime lastAnalyzedAt;

    @Column(length = 2000)
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(columnDefinition = "TEXT")
    private String aiHighlights;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AiAnalysisStatus aiStatus = AiAnalysisStatus.PENDING;

    @Column
    private LocalDateTime aiGeneratedAt;

    @Column(length = 20)
    private String aiRiskLevel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectGroup getGroup() {
        return group;
    }

    public void setGroup(ProjectGroup group) {
        this.group = group;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Deliverable getDeliverable() {
        return deliverable;
    }

    public void setDeliverable(Deliverable deliverable) {
        this.deliverable = deliverable;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    @Deprecated
    public Integer getVersionNumber() {
        return versionNumber;
    }

    @Deprecated
    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    @Deprecated
    public Integer getRevisionCount() {
        return revisionCount;
    }

    @Deprecated
    public void setRevisionCount(Integer revisionCount) {
        this.revisionCount = revisionCount;
    }

    @Deprecated
    public String getFileUrl() {
        return fileUrl;
    }

    @Deprecated
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getGoogleDocUrl() {
        return googleDocUrl;
    }

    public void setGoogleDocUrl(String googleDocUrl) {
        this.googleDocUrl = googleDocUrl;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public LocalDateTime getLastAnalyzedAt() {
        return lastAnalyzedAt;
    }

    public void setLastAnalyzedAt(LocalDateTime lastAnalyzedAt) {
        this.lastAnalyzedAt = lastAnalyzedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiHighlights() {
        return aiHighlights;
    }

    public void setAiHighlights(String aiHighlights) {
        this.aiHighlights = aiHighlights;
    }

    public AiAnalysisStatus getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(AiAnalysisStatus aiStatus) {
        this.aiStatus = aiStatus;
    }

    public LocalDateTime getAiGeneratedAt() {
        return aiGeneratedAt;
    }

    public void setAiGeneratedAt(LocalDateTime aiGeneratedAt) {
        this.aiGeneratedAt = aiGeneratedAt;
    }

    public String getAiRiskLevel() {
        return aiRiskLevel;
    }

    public void setAiRiskLevel(String aiRiskLevel) {
        this.aiRiskLevel = aiRiskLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Submission that = (Submission) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
