package ai.planmate.diagram.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.diagram.dto.DiagramResponse;
import ai.planmate.diagram.dto.GenerateDiagramRequest;
import ai.planmate.diagram.service.DiagramService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/projects/{projectId}/diagrams")
@RequiredArgsConstructor
public class DiagramController {

    private final DiagramService diagramService;

    @GetMapping
    public List<DiagramResponse> getDiagrams(@PathVariable UUID projectId) {
        return diagramService.getDiagrams(projectId);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public DiagramResponse generateDiagram(
            @PathVariable UUID projectId,
            @Valid @RequestBody GenerateDiagramRequest request,
            @AuthenticationPrincipal AppUser user) {
        return diagramService.generateDiagram(projectId, user, request);
    }
}
