package ai.planmate.projects.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;

    @NotBlank(message = "Project name is required")
    @Size(max = 255, message = "Project name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Project key must be 2-10 uppercase letters")
    private String key;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Min(value = 1, message = "Default velocity must be at least 1")
    @Max(value = 200, message = "Default velocity must not exceed 200")
    private Integer defaultVelocity = 35;

    @Min(value = 1, message = "Sprint length must be at least 1 day")
    @Max(value = 30, message = "Sprint length must not exceed 30 days")
    private Integer sprintLengthDays = 14;
}
