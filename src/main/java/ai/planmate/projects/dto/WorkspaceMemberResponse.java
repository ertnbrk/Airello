package ai.planmate.projects.dto;

import java.time.Instant;
import java.util.UUID;

import ai.planmate.auth.WorkspaceRole;

import lombok.Data;

@Data
public class WorkspaceMemberResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private WorkspaceRole role;
    private UUID invitedById;
    private String invitedByName;
    private Instant joinedAt;
}
