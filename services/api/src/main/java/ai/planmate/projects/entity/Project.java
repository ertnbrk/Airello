package ai.planmate.projects.entity;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "project",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "key"}))
@Getter
@Setter
public class Project extends BaseEntity {

    @NotNull(message = "Workspace is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @NotBlank(message = "Project name is required")
    @Size(max = 255, message = "Project name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Project key must be 2-10 uppercase letters")
    @Column(nullable = false, length = 20)
    private String key;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Owner is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @NotNull(message = "Default velocity is required")
    @Min(value = 1, message = "Default velocity must be at least 1")
    @Max(value = 200, message = "Default velocity must not exceed 200")
    @Column(name = "default_velocity", nullable = false)
    private Integer defaultVelocity = 35;

    @NotNull(message = "Sprint length is required")
    @Min(value = 1, message = "Sprint length must be at least 1 day")
    @Max(value = 30, message = "Sprint length must not exceed 30 days")
    @Column(name = "sprint_length_days", nullable = false)
    private Integer sprintLengthDays = 14;
}
