package com.intellitrack.dto;

import com.intellitrack.dto.coordinator.GroupRiskDto;
import java.util.List;

public class SubjectAnalyticsDto {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private long totalGroups;
    private long totalDeliverables;
    private long totalStudents;
    private double complianceRate;
    private double lateSubmissionRate;
    private double missingSubmissionRate;
    private List<GroupComplianceDto> groupCompliance;
    private List<GroupRiskDto> riskGroups;
    private String aiSummary;

    public SubjectAnalyticsDto() {
    }

    public SubjectAnalyticsDto(Long subjectId, String subjectCode, String subjectName,
            long totalGroups, long totalDeliverables, long totalStudents,
            double complianceRate, double lateSubmissionRate, double missingSubmissionRate,
            List<GroupComplianceDto> groupCompliance, List<GroupRiskDto> riskGroups) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.totalGroups = totalGroups;
        this.totalDeliverables = totalDeliverables;
        this.totalStudents = totalStudents;
        this.complianceRate = complianceRate;
        this.lateSubmissionRate = lateSubmissionRate;
        this.missingSubmissionRate = missingSubmissionRate;
        this.groupCompliance = groupCompliance;
        this.riskGroups = riskGroups;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public long getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(long totalGroups) {
        this.totalGroups = totalGroups;
    }

    public long getTotalDeliverables() {
        return totalDeliverables;
    }

    public void setTotalDeliverables(long totalDeliverables) {
        this.totalDeliverables = totalDeliverables;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public double getComplianceRate() {
        return complianceRate;
    }

    public void setComplianceRate(double complianceRate) {
        this.complianceRate = complianceRate;
    }

    public double getLateSubmissionRate() {
        return lateSubmissionRate;
    }

    public void setLateSubmissionRate(double lateSubmissionRate) {
        this.lateSubmissionRate = lateSubmissionRate;
    }

    public double getMissingSubmissionRate() {
        return missingSubmissionRate;
    }

    public void setMissingSubmissionRate(double missingSubmissionRate) {
        this.missingSubmissionRate = missingSubmissionRate;
    }

    public List<GroupComplianceDto> getGroupCompliance() {
        return groupCompliance;
    }

    public void setGroupCompliance(List<GroupComplianceDto> groupCompliance) {
        this.groupCompliance = groupCompliance;
    }

    public List<GroupRiskDto> getRiskGroups() {
        return riskGroups;
    }

    public void setRiskGroups(List<GroupRiskDto> riskGroups) {
        this.riskGroups = riskGroups;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
