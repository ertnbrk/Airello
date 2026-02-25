package ai.planmate.projects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(max = 255, message = "Workspace name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Workspace slug is required")
    @Pattern(
            regexp = "^[a-z0-9-]+$",
            message = "Workspace slug must contain only lowercase letters, numbers, and hyphens")
    @Size(max = 100, message = "Workspace slug must not exceed 100 characters")
    private String slug;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
}
