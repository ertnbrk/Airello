package ai.planmate.projects.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArtifactUploadResponse {
    private UUID artifactId;
    private String name;
    private Long sizeBytes;
    private String contentType;
    private String downloadUrl;
}
