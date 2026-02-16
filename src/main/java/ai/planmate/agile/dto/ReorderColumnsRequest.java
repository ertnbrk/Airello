package ai.planmate.agile.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ReorderColumnsRequest {
    @NotEmpty(message = "Column IDs are required")
    private List<UUID> columnIds;
}
