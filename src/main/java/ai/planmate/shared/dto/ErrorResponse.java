package ai.planmate.shared.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private ErrorDetail error;

    @Data
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
        private String traceId;
        private Instant timestamp;
    }

    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(new ErrorDetail(code, message, traceId, Instant.now()));
    }
}
