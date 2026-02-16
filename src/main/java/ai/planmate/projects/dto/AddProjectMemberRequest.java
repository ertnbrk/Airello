package ai.planmate.projects.dto;

import java.util.UUID;

import ai.planmate.projects.ProjectRole;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddProjectMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role is required")
    private ProjectRole role;
}
