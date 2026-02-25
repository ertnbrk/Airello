package ai.planmate.agile.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {
    private String id;
    private String projectId;
    private String epicId;
    private String key;
    private String type; // Frontend format: 'feature', 'task', 'bug', 'improvement'
    private String title;
    private String description;
    private String status; // Frontend format: 'backlog', 'todo', 'in-progress', 'review', 'done'
    private String priority; // Frontend format: 'low', 'medium', 'high', 'urgent'
    private Integer storyPoints;
    private String assigneeId;
    private String reporterId;
    private List<String> labels;
    private BigDecimal originalEstimateHours;
    private BigDecimal remainingEstimateHours;
    private BigDecimal timeSpentHours;
    private BigDecimal orderIndex;
    private Instant createdAt;
    private Instant updatedAt;
}
