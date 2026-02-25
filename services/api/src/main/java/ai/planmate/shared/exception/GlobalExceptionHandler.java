package ai.planmate.shared.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ai.planmate.shared.dto.ConflictErrorResponse;
import ai.planmate.shared.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Resource not found [{ }]: { }", traceId, ex.getMessage());
        return ErrorResponse.of("PM-404-01", ex.getMessage(), traceId);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Bad request [{ }]: { }", traceId, ex.getMessage());
        return ErrorResponse.of("PM-400-01", ex.getMessage(), traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String traceId = UUID.randomUUID().toString();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(
                        error -> {
                            String fieldName = ((FieldError) error).getField();
                            String errorMessage = error.getDefaultMessage();
                            fieldErrors.put(fieldName, errorMessage);
                        });

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        error.put("code", "PM-400-02");
        error.put("message", "Validation failed");
        error.put("traceId", traceId);
        error.put("fields", fieldErrors);

        response.put("error", error);
        return response;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Illegal argument [{ }]: { }", traceId, ex.getMessage());
        return ErrorResponse.of("PM-400-03", ex.getMessage(), traceId);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIllegalState(IllegalStateException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Illegal state [{ }]: { }", traceId, ex.getMessage());
        return ErrorResponse.of("PM-409-01", ex.getMessage(), traceId);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ConflictErrorResponse handleConflictException(
            ConflictException ex, HttpServletRequest request) {
        log.error("Conflict: [{}] {}", ex.getErrorCode(), ex.getMessage());
        return ConflictErrorResponse.of(
                ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), 409);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ConflictErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = ex.getMessage();
        String constraintName = extractConstraintName(ex);

        if (constraintName != null && constraintName.contains("project_workspace_id_key_key")) {
            log.error("Data integrity violation - project key conflict: {}", constraintName);
            ConflictErrorResponse response =
                    ConflictErrorResponse.builder()
                            .timestamp(java.time.Instant.now())
                            .status(409)
                            .errorCode("PROJECT_KEY_ALREADY_EXISTS")
                            .message("Project key already exists in this workspace")
                            .path(request.getRequestURI())
                            .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (constraintName != null
                && constraintName.contains("uq_board_column_project_position")) {
            log.error("Data integrity violation - board column position conflict: {}", constraintName);
            ConflictErrorResponse response =
                    ConflictErrorResponse.builder()
                            .timestamp(java.time.Instant.now())
                            .status(409)
                            .errorCode("COLUMN_POSITION_CONFLICT")
                            .message("Column position conflict in project")
                            .path(request.getRequestURI())
                            .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (constraintName != null
                && constraintName.contains("uq_board_column_project_default")) {
            log.error("Data integrity violation - multiple default columns: {}", constraintName);
            ConflictErrorResponse response =
                    ConflictErrorResponse.builder()
                            .timestamp(java.time.Instant.now())
                            .status(409)
                            .errorCode("DEFAULT_COLUMN_EXISTS")
                            .message("A default column already exists for this project")
                            .path(request.getRequestURI())
                            .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        log.error("Data integrity violation: {}", message);
        ConflictErrorResponse response =
                ConflictErrorResponse.builder()
                        .timestamp(java.time.Instant.now())
                        .status(400)
                        .errorCode("DATA_INTEGRITY_VIOLATION")
                        .message("Data integrity constraint violated")
                        .path(request.getRequestURI())
                        .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleNullPointerException(NullPointerException ex) {
        String traceId = UUID.randomUUID().toString();
        log.error(
                "NullPointerException occurred [traceId: {}] at: {}",
                traceId,
                ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : "unknown",
                ex);
        return ErrorResponse.of(
                "PM-500-02", "Null pointer exception - this is a bug", traceId);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error(
                "Unexpected error occurred [traceId: {}] - {}: {}",
                traceId,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex);
        return ErrorResponse.of("PM-500-01", "An unexpected error occurred", traceId);
    }

    private String extractConstraintName(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("constraint")) {
                return message;
            }
            cause = cause.getCause();
        }
        return ex.getMessage();
    }
}
