package ai.planmate.diagram.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.ai.service.AiProvider;
import ai.planmate.ai.service.AiRouterService;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.diagram.dto.DiagramResponse;
import ai.planmate.diagram.dto.GenerateDiagramRequest;
import ai.planmate.diagram.entity.Diagram;
import ai.planmate.diagram.repository.DiagramRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramService {

    private final DiagramRepository diagramRepository;
    private final ProjectRepository projectRepository;
    private final AiRouterService aiRouterService;

    @Transactional(readOnly = true)
    public List<DiagramResponse> getDiagrams(UUID projectId) {
        return diagramRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DiagramResponse generateDiagram(
            UUID projectId, AppUser user, GenerateDiagramRequest request) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        String prompt =
                buildPrompt(request.getType(), request.getContext(), project.getDescription());

        Map<String, Object> context = new HashMap<>();
        context.put("operation", "DIAGRAM_GENERATION");
        context.put("diagramType", request.getType());

        AiProvider.AiResult result = aiRouterService.route(user, prompt, null, context);

        if (!result.success()) {
            throw new RuntimeException("AI diagram generation failed: " + result.error());
        }

        Diagram diagram = new Diagram();
        diagram.setProject(project);
        diagram.setType(request.getType());
        diagram.setFormat(request.getFormat());
        diagram.setTitle(request.getTitle());
        diagram.setContent(result.content());
        diagram.setCreatedBy(user);

        // Check for existing diagrams of same type and increment version
        List<Diagram> existing =
                diagramRepository.findByProjectIdAndType(projectId, request.getType());
        diagram.setVersion(existing.size() + 1);

        diagram = diagramRepository.save(diagram);
        return toResponse(diagram);
    }

    private String buildPrompt(String type, String context, String projectDescription) {
        String basePrompt =
                switch (type.toLowerCase()) {
                    case "er" -> "Generate an Entity-Relationship diagram in Mermaid format showing"
                            + " the database schema.";
                    case "sequence" -> "Generate a sequence diagram in Mermaid format showing the"
                            + " main user flows.";
                    case "use_case" -> "Generate a use case diagram in Mermaid flowchart format"
                            + " showing user interactions.";
                    case "system" -> "Generate a system architecture diagram in Mermaid format.";
                    default -> "Generate a flowchart diagram in Mermaid format.";
                };

        StringBuilder prompt = new StringBuilder(basePrompt);
        if (projectDescription != null && !projectDescription.isEmpty()) {
            prompt.append("\n\nProject context: ").append(projectDescription);
        }
        if (context != null && !context.isEmpty()) {
            prompt.append("\n\nAdditional context: ").append(context);
        }
        prompt.append("\n\nReturn only the Mermaid diagram code, no explanations.");

        return prompt.toString();
    }

    private DiagramResponse toResponse(Diagram diagram) {
        return DiagramResponse.builder()
                .id(diagram.getId())
                .projectId(diagram.getProject().getId())
                .type(diagram.getType())
                .format(diagram.getFormat())
                .title(diagram.getTitle())
                .content(diagram.getContent())
                .version(diagram.getVersion())
                .createdAt(diagram.getCreatedAt())
                .build();
    }
}
