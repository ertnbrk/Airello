package ai.planmate.agile.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import ai.planmate.agile.entity.IssuePriority;
import ai.planmate.agile.entity.IssueStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateIssueRequest {
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private IssueStatus status;
    private IssuePriority priority;

    @Min(value = 0, message = "Story points must be at least 0")
    @Max(value = 100, message = "Story points must not exceed 100")
    private Integer storyPoints;

    private UUID assigneeId;
    private List<String> labels;

    @DecimalMin(value = "0.0", message = "Remaining estimate hours must be at least 0")
    private BigDecimal remainingEstimateHours;

    @DecimalMin(value = "0.0", message = "Order index must be at least 0")
    private BigDecimal orderIndex;
}
