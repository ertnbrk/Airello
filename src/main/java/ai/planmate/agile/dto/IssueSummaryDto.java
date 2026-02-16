package ai.planmate.agile.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight Issue DTO for board views and listings */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSummaryDto {
    private String id;
    private String projectId;
    private String key;
    private String type; // Frontend format
    private String title;
    private String status; // Frontend format
    private String priority; // Frontend format
    private Integer storyPoints;
    private String assigneeId;
    private List<String> labels;
    private BigDecimal orderIndex;
    private Instant createdAt;
    private Instant updatedAt;
}
