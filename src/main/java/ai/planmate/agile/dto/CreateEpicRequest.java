package ai.planmate.agile.dto;

import java.util.UUID;

import ai.planmate.agile.entity.IssuePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEpicRequest {
    @NotNull private UUID projectId;

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 5000)
    private String description;

    private IssuePriority priority = IssuePriority.MEDIUM;
}
