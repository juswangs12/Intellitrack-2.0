package com.intellitrack.dto;

import java.util.List;

public class SubjectDto {
    private Long id;
    private String name;
    private String code;
    private List<ClassSectionDto> sections;

    public SubjectDto() {}

    public SubjectDto(Long id, String name, String code, List<ClassSectionDto> sections) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.sections = sections;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<ClassSectionDto> getSections() {
        return sections;
    }

    public void setSections(List<ClassSectionDto> sections) {
        this.sections = sections;
    }
}
