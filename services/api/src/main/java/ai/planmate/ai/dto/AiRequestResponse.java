package ai.planmate.ai.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestResponse {
    private String id;
    private String correlationId;
    private String projectId;
    private String status; // Frontend format: lowercase
    private String requestType;
    private Map<String, Object> parameters; // requestPayload
    private Map<String, Object> result; // responsePayload
    private Instant createdAt;
    private Instant completedAt;
}
