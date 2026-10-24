package com.intellitrack.exception;

import com.intellitrack.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<?> missing(java.util.NoSuchElementException ex){return ResponseEntity.status(404).body(Map.of("message","Record not found; refresh and try again"));}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(org.springframework.dao.DataIntegrityViolationException ex){return ResponseEntity.status(409).body(Map.of("message","This change conflicts with an existing record or academic history. Refresh the data and check for duplicates or linked records."));}
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> malformed(Exception ex){return ResponseEntity.badRequest().body(Map.of("message","Missing or invalid request fields"));}
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> method(Exception ex){return ResponseEntity.status(405).body(Map.of("message","This action is not supported"));}
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> denied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()==null?"Request failed":ex.getReason()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        System.err.println("=== GlobalExceptionHandler: ResourceNotFoundException ===");
        ex.printStackTrace();
        ErrorResponse errorDetails = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            "Resource Not Found",
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GoogleSheetsExportException.class)
    public ResponseEntity<ErrorResponse> handleGoogleSheetsExportException(GoogleSheetsExportException ex, WebRequest request) {
        System.err.println("=== GlobalExceptionHandler: GoogleSheetsExportException ===");
        ex.printStackTrace();
        ErrorResponse errorDetails = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage(),
            "Google Sheets Export Failed",
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        System.err.println("=== GlobalExceptionHandler: MethodArgumentNotValidException ===");
        ex.printStackTrace();
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError field ? field.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorDetails = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Bad Request",
            request.getDescription(false)
        );
        errorDetails.setValidationErrors(errors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        System.err.println("=== GlobalExceptionHandler: IllegalArgumentException ===");
        ex.printStackTrace();
        ErrorResponse errorDetails = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            "Bad Request",
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Unhandled request failure",ex);
        ErrorResponse errorDetails = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "The request could not be completed. Please try again or contact the coordinator.",
            "Internal Server Error",
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        System.err.println("=== GlobalExceptionHandler: Exception ===");
        ex.printStackTrace();
        ErrorResponse errorDetails = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            "Internal Server Error",
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
