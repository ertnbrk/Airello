package ai.planmate.projects.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.ProjectRole;
import ai.planmate.projects.dto.AddProjectMemberRequest;
import ai.planmate.projects.dto.ProjectMemberResponse;
import ai.planmate.projects.dto.UpdateProjectMemberRequest;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.entity.ProjectMember;
import ai.planmate.projects.repository.ProjectMemberRepository;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.projects.repository.WorkspaceMemberRepository;
import ai.planmate.shared.exception.BadRequestException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(UUID projectId, UUID requesterId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Verify requester has access to workspace
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                project.getWorkspace().getId(), requesterId)) {
            throw new ResourceNotFoundException("Project not found");
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectIdWithUser(projectId);
        return members.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ProjectMemberResponse addProjectMember(
            UUID projectId, AddProjectMemberRequest request, UUID adderId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        AppUser userToAdd =
                appUserRepository
                        .findById(request.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AppUser adder =
                appUserRepository
                        .findById(adderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Adder not found"));

        // Check if user is already a project member
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new BadRequestException("User is already a member of this project");
        }

        // Verify adder has access to project (must be workspace member at minimum)
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                project.getWorkspace().getId(), adderId)) {
            throw new BadRequestException("You don't have permission to add members");
        }

        // User to add must be a workspace member
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                project.getWorkspace().getId(), request.getUserId())) {
            throw new BadRequestException(
                    "User must be a workspace member before being added to project");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(userToAdd);
        member.setRole(request.getRole());
        member.setAddedBy(adder);

        member = projectMemberRepository.save(member);

        return toResponse(member);
    }

    @Transactional
    public ProjectMemberResponse updateProjectMember(
            UUID projectId, UUID userId, UpdateProjectMemberRequest request, UUID updaterId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        ProjectMember member =
                projectMemberRepository
                        .findByProjectIdAndUserId(projectId, userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Member not found in project"));

        // Verify updater has access (should be ADMIN in real app)
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, updaterId)) {
            throw new BadRequestException("You don't have permission to update members");
        }

        // Don't allow changing project owner's role
        if (project.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Cannot change project owner's role");
        }

        member.setRole(request.getRole());
        member = projectMemberRepository.save(member);

        return toResponse(member);
    }

    @Transactional
    public void removeProjectMember(UUID projectId, UUID userId, UUID removerId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        ProjectMember member =
                projectMemberRepository
                        .findByProjectIdAndUserId(projectId, userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Member not found in project"));

        // Verify remover has access
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, removerId)) {
            throw new BadRequestException("You don't have permission to remove members");
        }

        // Don't allow removing project owner
        if (project.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Cannot remove project owner");
        }

        member.softDelete();
        projectMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(UUID projectId, UUID userId, ProjectRole role) {
        return projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .map(pm -> pm.getRole() == role)
                .orElse(false);
    }

    private ProjectMemberResponse toResponse(ProjectMember member) {
        ProjectMemberResponse response = new ProjectMemberResponse();
        response.setId(member.getId());
        response.setUserId(member.getUser().getId());
        response.setUserName(member.getUser().getFullName());
        response.setUserEmail(member.getUser().getEmail());
        response.setRole(member.getRole());
        if (member.getAddedBy() != null) {
            response.setAddedById(member.getAddedBy().getId());
            response.setAddedByName(member.getAddedBy().getFullName());
        }
        response.setJoinedAt(member.getJoinedAt());
        return response;
    }
}
