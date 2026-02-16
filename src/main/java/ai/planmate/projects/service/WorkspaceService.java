package ai.planmate.projects.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.dto.AddWorkspaceMemberRequest;
import ai.planmate.projects.dto.CreateWorkspaceRequest;
import ai.planmate.projects.dto.WorkspaceMemberResponse;
import ai.planmate.projects.dto.WorkspaceResponse;
import ai.planmate.projects.entity.Workspace;
import ai.planmate.projects.entity.WorkspaceMember;
import ai.planmate.projects.repository.WorkspaceMemberRepository;
import ai.planmate.projects.repository.WorkspaceRepository;
import ai.planmate.shared.exception.BadRequestException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getUserWorkspaces(UUID userId) {
        AppUser user =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<WorkspaceMember> memberships =
                workspaceMemberRepository.findByUserIdWithWorkspace(userId);

        return memberships.stream()
                .map(wm -> toResponse(wm.getWorkspace()))
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, UUID ownerId) {
        AppUser owner =
                appUserRepository
                        .findById(ownerId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if slug is already taken
        workspaceRepository
                .findBySlug(request.getSlug())
                .ifPresent(
                        w -> {
                            throw new BadRequestException(
                                    "Workspace with slug '"
                                            + request.getSlug()
                                            + "' already exists");
                        });

        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        workspace.setSlug(request.getSlug());
        workspace.setDescription(request.getDescription());
        workspace.setOwner(owner);

        workspace = workspaceRepository.save(workspace);

        // Add owner as workspace member with OWNER role
        WorkspaceMember ownerMembership = new WorkspaceMember();
        ownerMembership.setWorkspace(workspace);
        ownerMembership.setUser(owner);
        ownerMembership.setRole(ai.planmate.auth.WorkspaceRole.OWNER);
        ownerMembership.setInvitedBy(owner);
        workspaceMemberRepository.save(ownerMembership);

        return toResponse(workspace);
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(UUID workspaceId, UUID userId) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .filter(w -> !w.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // Verify user has access
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ResourceNotFoundException("Workspace not found");
        }

        return toResponse(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getWorkspaceMembers(UUID workspaceId, UUID userId) {
        // Verify workspace exists
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .filter(w -> !w.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // Verify user has access
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ResourceNotFoundException("Workspace not found");
        }

        List<WorkspaceMember> members =
                workspaceMemberRepository.findByWorkspaceIdWithUser(workspaceId);

        return members.stream().map(this::toMemberResponse).collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceMemberResponse addWorkspaceMember(
            UUID workspaceId, AddWorkspaceMemberRequest request, UUID inviterId) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .filter(w -> !w.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        AppUser userToAdd =
                appUserRepository
                        .findById(request.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AppUser inviter =
                appUserRepository
                        .findById(inviterId)
                        .orElseThrow(() -> new ResourceNotFoundException("Inviter not found"));

        // Check if user is already a member
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId, request.getUserId())) {
            throw new BadRequestException("User is already a member of this workspace");
        }

        // Verify inviter has access (at least MANAGER role would be checked in real app)
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, inviterId)) {
            throw new BadRequestException("You don't have permission to add members");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(userToAdd);
        member.setRole(request.getRole());
        member.setInvitedBy(inviter);

        member = workspaceMemberRepository.save(member);

        return toMemberResponse(member);
    }

    @Transactional
    public void removeWorkspaceMember(UUID workspaceId, UUID userIdToRemove, UUID removerId) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .filter(w -> !w.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        WorkspaceMember memberToRemove =
                workspaceMemberRepository
                        .findByWorkspaceIdAndUserId(workspaceId, userIdToRemove)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Member not found in workspace"));

        // Verify remover has access
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, removerId)) {
            throw new BadRequestException("You don't have permission to remove members");
        }

        // Don't allow removing the workspace owner
        if (workspace.getOwner().getId().equals(userIdToRemove)) {
            throw new BadRequestException("Cannot remove workspace owner");
        }

        workspaceMemberRepository.delete(memberToRemove);
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        WorkspaceResponse response = new WorkspaceResponse();
        response.setId(workspace.getId());
        response.setName(workspace.getName());
        response.setSlug(workspace.getSlug());
        response.setDescription(workspace.getDescription());
        response.setOwnerId(workspace.getOwner().getId());
        response.setOwnerName(workspace.getOwner().getFullName());
        response.setOwnerEmail(workspace.getOwner().getEmail());
        response.setCreatedAt(workspace.getCreatedAt());
        response.setUpdatedAt(workspace.getUpdatedAt());
        return response;
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember member) {
        WorkspaceMemberResponse response = new WorkspaceMemberResponse();
        response.setId(member.getId());
        response.setUserId(member.getUser().getId());
        response.setUserName(member.getUser().getFullName());
        response.setUserEmail(member.getUser().getEmail());
        response.setRole(member.getRole());
        if (member.getInvitedBy() != null) {
            response.setInvitedById(member.getInvitedBy().getId());
            response.setInvitedByName(member.getInvitedBy().getFullName());
        }
        response.setJoinedAt(member.getJoinedAt());
        return response;
    }
}
