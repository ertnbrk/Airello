package ai.planmate.shared.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictErrorResponse {

    private Instant timestamp;
    private Integer status;
    private String errorCode;
    private String message;
    private String path;

    public static ConflictErrorResponse of(
            String errorCode, String message, String path, Integer status) {
        return ConflictErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .build();
    }
}
