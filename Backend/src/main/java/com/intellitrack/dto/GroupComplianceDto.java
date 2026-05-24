package com.intellitrack.dto;

public class GroupComplianceDto {
    private Long groupId;
    private String groupCode;
    private String groupTitle;
    private long totalDeliverables;
    private long submittedDeliverables;
    private long lateDeliverables;
    private long missingDeliverables;
    private double complianceRate;
    private long onTimeStudents;
    private long lateStudents;
    private long missingStudents;

    public GroupComplianceDto() {
    }

    public GroupComplianceDto(Long groupId, String groupCode, String groupTitle,
            long totalDeliverables, long submittedDeliverables, long lateDeliverables, long missingDeliverables,
            double complianceRate, long onTimeStudents, long lateStudents, long missingStudents) {
        this.groupId = groupId;
        this.groupCode = groupCode;
        this.groupTitle = groupTitle;
        this.totalDeliverables = totalDeliverables;
        this.submittedDeliverables = submittedDeliverables;
        this.lateDeliverables = lateDeliverables;
        this.missingDeliverables = missingDeliverables;
        this.complianceRate = complianceRate;
        this.onTimeStudents = onTimeStudents;
        this.lateStudents = lateStudents;
        this.missingStudents = missingStudents;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public long getTotalDeliverables() {
        return totalDeliverables;
    }

    public void setTotalDeliverables(long totalDeliverables) {
        this.totalDeliverables = totalDeliverables;
    }

    public long getSubmittedDeliverables() {
        return submittedDeliverables;
    }

    public void setSubmittedDeliverables(long submittedDeliverables) {
        this.submittedDeliverables = submittedDeliverables;
    }

    public long getLateDeliverables() {
        return lateDeliverables;
    }

    public void setLateDeliverables(long lateDeliverables) {
        this.lateDeliverables = lateDeliverables;
    }

    public long getMissingDeliverables() {
        return missingDeliverables;
    }

    public void setMissingDeliverables(long missingDeliverables) {
        this.missingDeliverables = missingDeliverables;
    }

    public double getComplianceRate() {
        return complianceRate;
    }

    public void setComplianceRate(double complianceRate) {
        this.complianceRate = complianceRate;
    }

    public long getOnTimeStudents() {
        return onTimeStudents;
    }

    public void setOnTimeStudents(long onTimeStudents) {
        this.onTimeStudents = onTimeStudents;
    }

    public long getLateStudents() {
        return lateStudents;
    }

    public void setLateStudents(long lateStudents) {
        this.lateStudents = lateStudents;
    }

    public long getMissingStudents() {
        return missingStudents;
    }

    public void setMissingStudents(long missingStudents) {
        this.missingStudents = missingStudents;
    }
}
