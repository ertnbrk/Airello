package ai.planmate.projects.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.dto.AddProjectMemberRequest;
import ai.planmate.projects.dto.ProjectMemberResponse;
import ai.planmate.projects.dto.UpdateProjectMemberRequest;
import ai.planmate.projects.service.ProjectMemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @GetMapping
    public List<ProjectMemberResponse> getProjectMembers(
            @PathVariable UUID projectId, @AuthenticationPrincipal AppUser user) {
        return projectMemberService.getProjectMembers(projectId, user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addProjectMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberRequest request,
            @AuthenticationPrincipal AppUser user) {
        return projectMemberService.addProjectMember(projectId, request, user.getId());
    }

    @PatchMapping("/{userId}")
    public ProjectMemberResponse updateProjectMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProjectMemberRequest request,
            @AuthenticationPrincipal AppUser user) {
        return projectMemberService.updateProjectMember(projectId, userId, request, user.getId());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProjectMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal AppUser user) {
        projectMemberService.removeProjectMember(projectId, userId, user.getId());
    }
}
