package ai.planmate.agile.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import ai.planmate.agile.entity.IssuePriority;
import ai.planmate.agile.entity.IssueType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateIssueRequest {
    @NotNull private UUID projectId;

    private UUID epicId;

    @NotNull private IssueType type;

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 5000)
    private String description;

    private IssuePriority priority = IssuePriority.MEDIUM;

    @Min(0)
    @Max(100)
    private Integer storyPoints;

    private UUID assigneeId;

    private List<String> labels;

    @DecimalMin("0.0")
    private BigDecimal originalEstimateHours;
}
