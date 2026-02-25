package ai.planmate.projects.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class ProjectResponse {
    private UUID id;
    private UUID workspaceId;
    private String name;
    private String key;
    private String description;
    private UUID ownerId;
    private String ownerName;
    private Integer defaultVelocity;
    private Integer sprintLengthDays;
    private Instant createdAt;
    private Instant updatedAt;
}
