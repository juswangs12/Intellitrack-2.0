package com.intellitrack.exception;

public class GoogleSheetsExportException extends RuntimeException {
    public GoogleSheetsExportException(String message) {
        super(message);
    }

    public GoogleSheetsExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
