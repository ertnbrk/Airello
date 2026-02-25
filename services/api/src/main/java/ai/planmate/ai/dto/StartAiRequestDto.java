package ai.planmate.ai.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartAiRequestDto {
    @NotNull private UUID projectId;

    @NotBlank private String requestType; // e.g., "SPRINT_PLANNING", "ISSUE_ESTIMATION"

    private Map<String, Object> parameters;
}
