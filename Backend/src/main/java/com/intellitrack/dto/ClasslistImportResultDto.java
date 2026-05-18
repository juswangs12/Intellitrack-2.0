package com.intellitrack.dto;

import java.util.List;

public class ClasslistImportResultDto {
    private int totalRows;
    private int successfullyImported;
    private int failedRows;
    private List<String> errors;
    private List<String> warnings;

    public ClasslistImportResultDto() {}

    public ClasslistImportResultDto(int totalRows, int successfullyImported, int failedRows, List<String> errors, List<String> warnings) {
        this.totalRows = totalRows;
        this.successfullyImported = successfullyImported;
        this.failedRows = failedRows;
        this.errors = errors;
        this.warnings = warnings;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessfullyImported() {
        return successfullyImported;
    }

    public void setSuccessfullyImported(int successfullyImported) {
        this.successfullyImported = successfullyImported;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}