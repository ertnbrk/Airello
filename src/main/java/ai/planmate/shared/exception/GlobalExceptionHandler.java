package ai.planmate.shared.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unexpected error [{ }]", traceId, ex);
        return ErrorResponse.of("PM-500-01", "An unexpected error occurred", traceId);
    }
}
