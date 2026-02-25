package ai.planmate.diagram.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateDiagramRequest {
    @NotBlank(message = "Diagram type is required")
    private String type; // use_case, er, system, flowchart, sequence

    private String format = "mermaid"; // mermaid or plantuml
    private String title;
    private String context; // additional context for AI generation
}
