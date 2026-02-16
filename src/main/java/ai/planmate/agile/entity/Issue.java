package ai.planmate.agile.entity;

import java.math.BigDecimal;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.entity.Project;
import ai.planmate.shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "issue", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "key"}))
@Getter
@Setter
public class Issue extends BaseEntity {

    @NotNull(message = "Project is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id")
    private Epic epic;

    @NotBlank(message = "Issue key is required")
    @Size(max = 50, message = "Issue key must not exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String key;

    @NotNull(message = "Issue type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueType type = IssueType.TASK;

    @NotBlank(message = "Issue title is required")
    @Size(max = 500, message = "Issue title must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String title;

    @Size(max = 5000, message = "Issue description must not exceed 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Issue status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.BACKLOG;

    @NotNull(message = "Issue priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Min(value = 0, message = "Story points must be at least 0")
    @Max(value = 100, message = "Story points must not exceed 100")
    @Column(name = "story_points")
    private Integer storyPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private AppUser reporter;

    @Column(columnDefinition = "text[]")
    private String[] labels;

    @DecimalMin(value = "0.0", message = "Original estimate hours must be at least 0")
    @Column(name = "original_estimate_hours", precision = 10, scale = 2)
    private BigDecimal originalEstimateHours;

    @DecimalMin(value = "0.0", message = "Remaining estimate hours must be at least 0")
    @Column(name = "remaining_estimate_hours", precision = 10, scale = 2)
    private BigDecimal remainingEstimateHours;

    @NotNull(message = "Time spent hours is required")
    @DecimalMin(value = "0.0", message = "Time spent hours must be at least 0")
    @Column(name = "time_spent_hours", precision = 10, scale = 2)
    private BigDecimal timeSpentHours = BigDecimal.ZERO;

    @NotNull(message = "Order index is required")
    @DecimalMin(value = "0.0", message = "Order index must be at least 0")
    @Column(name = "order_index", precision = 15, scale = 2)
    private BigDecimal orderIndex = BigDecimal.valueOf(1000);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_column_id")
    private BoardColumn boardColumn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_issue_id")
    private Issue parentIssue;

    @Version
    @Column(nullable = false)
    private Integer version = 0;
}
