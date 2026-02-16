package ai.planmate.projects.dto;

import java.time.Instant;
import java.util.UUID;

import ai.planmate.projects.InvitationType;
import ai.planmate.projects.ProjectRole;

import lombok.Data;

@Data
public class InvitationResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private InvitationType type;
    private String token;
    private String email;
    private ProjectRole role;
    private UUID invitedById;
    private String invitedByName;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant acceptedAt;
    private UUID acceptedById;
    private String acceptedByName;
    private Integer maxUses;
    private Integer currentUses;
    private boolean expired;
    private boolean revoked;
    private boolean valid;
}
