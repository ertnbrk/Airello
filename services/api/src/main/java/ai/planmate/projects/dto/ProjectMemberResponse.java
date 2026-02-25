package ai.planmate.projects.dto;

import java.time.Instant;
import java.util.UUID;

import ai.planmate.projects.ProjectRole;

import lombok.Data;

@Data
public class ProjectMemberResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private ProjectRole role;
    private UUID addedById;
    private String addedByName;
    private Instant joinedAt;
}
