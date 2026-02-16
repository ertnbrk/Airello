package ai.planmate.projects.dto;

import java.util.UUID;

import ai.planmate.auth.WorkspaceRole;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddWorkspaceMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role is required")
    private WorkspaceRole role;
}
