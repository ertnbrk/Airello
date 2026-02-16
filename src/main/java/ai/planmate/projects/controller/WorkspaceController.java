package ai.planmate.projects.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.dto.AddWorkspaceMemberRequest;
import ai.planmate.projects.dto.CreateWorkspaceRequest;
import ai.planmate.projects.dto.WorkspaceMemberResponse;
import ai.planmate.projects.dto.WorkspaceResponse;
import ai.planmate.projects.service.WorkspaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    public List<WorkspaceResponse> listWorkspaces(@AuthenticationPrincipal AppUser user) {
        return workspaceService.getUserWorkspaces(user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal AppUser user) {
        return workspaceService.createWorkspace(request, user.getId());
    }

    @GetMapping("/{id}")
    public WorkspaceResponse getWorkspace(
            @PathVariable UUID id, @AuthenticationPrincipal AppUser user) {
        return workspaceService.getWorkspace(id, user.getId());
    }

    @GetMapping("/{id}/members")
    public List<WorkspaceMemberResponse> getWorkspaceMembers(
            @PathVariable UUID id, @AuthenticationPrincipal AppUser user) {
        return workspaceService.getWorkspaceMembers(id, user.getId());
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceMemberResponse addWorkspaceMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddWorkspaceMemberRequest request,
            @AuthenticationPrincipal AppUser user) {
        return workspaceService.addWorkspaceMember(id, request, user.getId());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWorkspaceMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal AppUser user) {
        workspaceService.removeWorkspaceMember(id, userId, user.getId());
    }
}
