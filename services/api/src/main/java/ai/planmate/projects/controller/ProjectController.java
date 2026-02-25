package ai.planmate.projects.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.dto.ArtifactUploadResponse;
import ai.planmate.projects.dto.CreateProjectRequest;
import ai.planmate.projects.dto.ProjectResponse;
import ai.planmate.projects.service.ArtifactService;
import ai.planmate.projects.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Autowired(required = false)
    private ArtifactService artifactService;

    @GetMapping
    public List<ProjectResponse> listProjects(
            @RequestParam UUID workspaceId, @AuthenticationPrincipal AppUser user) {
        return projectService.getProjectsByWorkspace(workspaceId, user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        return projectService.createProject(request, currentUser);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable UUID id) {
        return projectService.getProject(id);
    }

    @PostMapping(value = "/{id}/artifacts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArtifactUploadResponse uploadArtifact(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        if (artifactService == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Artifact uploads are disabled");
        }
        return artifactService.uploadArtifact(id, file);
    }
}
