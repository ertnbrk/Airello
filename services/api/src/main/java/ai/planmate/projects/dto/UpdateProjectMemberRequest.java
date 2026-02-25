package ai.planmate.projects.dto;

import ai.planmate.projects.ProjectRole;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProjectMemberRequest {

    @NotNull(message = "Role is required")
    private ProjectRole role;
}
