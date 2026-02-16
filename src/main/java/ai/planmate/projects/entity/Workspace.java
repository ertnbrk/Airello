package ai.planmate.projects.entity;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workspace")
@Getter
@Setter
@NoArgsConstructor
public class Workspace extends BaseEntity {

    @NotBlank(message = "Workspace name is required")
    @Size(max = 255, message = "Workspace name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Workspace slug is required")
    @Pattern(
            regexp = "^[a-z0-9-]+$",
            message = "Workspace slug must contain only lowercase letters, numbers, and hyphens")
    @Size(max = 100, message = "Workspace slug must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Owner is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;
}
