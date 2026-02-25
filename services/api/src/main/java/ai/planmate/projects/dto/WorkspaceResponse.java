package ai.planmate.projects.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class WorkspaceResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private UUID ownerId;
    private String ownerName;
    private String ownerEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
