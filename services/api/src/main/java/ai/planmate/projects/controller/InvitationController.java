package ai.planmate.projects.controller;

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
import ai.planmate.projects.dto.AcceptInvitationRequest;
import ai.planmate.projects.dto.CreateInvitationRequest;
import ai.planmate.projects.dto.InvitationResponse;
import ai.planmate.projects.dto.ProjectMemberResponse;
import ai.planmate.projects.service.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/projects/{projectId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse createInvitation(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal AppUser user) {
        return invitationService.createInvitation(projectId, request, user.getId());
    }

    @GetMapping("/invitations/{token}")
    public InvitationResponse getInvitation(@PathVariable String token) {
        return invitationService.getInvitation(token);
    }

    @PostMapping("/invitations/{token}/accept")
    public ProjectMemberResponse acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request,
            @AuthenticationPrincipal AppUser user) {
        return invitationService.acceptInvitation(token, user.getId());
    }
}
