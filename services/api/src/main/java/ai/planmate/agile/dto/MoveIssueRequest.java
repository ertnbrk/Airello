package ai.planmate.agile.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveIssueRequest {
    @NotNull(message = "Target column ID is required")
    private UUID targetColumnId;

    private UUID afterIssueId;
    private UUID beforeIssueId;
}
