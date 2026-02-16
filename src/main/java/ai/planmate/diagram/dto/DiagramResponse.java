package ai.planmate.diagram.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramResponse {
    private UUID id;
    private UUID projectId;
    private String type;
    private String format;
    private String title;
    private String content;
    private Integer version;
    private Instant createdAt;
}
