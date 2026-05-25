package com.intellitrack.dto;

public class GroupRequest {
    private String code;
    private String title;
    private String subjectCode;

    public GroupRequest() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
}
