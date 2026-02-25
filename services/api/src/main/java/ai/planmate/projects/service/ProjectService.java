package ai.planmate.projects.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.agile.service.BoardService;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.chat.service.ChatService;
import ai.planmate.projects.dto.CreateProjectRequest;
import ai.planmate.projects.dto.ProjectResponse;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.entity.Workspace;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.projects.repository.WorkspaceMemberRepository;
import ai.planmate.projects.repository.WorkspaceRepository;
import ai.planmate.shared.exception.ConflictException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AppUserRepository appUserRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final BoardService boardService;
    private final ChatService chatService;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        return createProject(request, null);
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, AppUser owner) {
        Workspace workspace =
                workspaceRepository
                        .findById(request.getWorkspaceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (projectRepository.existsByWorkspaceIdAndKey(
                request.getWorkspaceId(), request.getKey())) {
            throw new ConflictException(
                    "Project key already exists in this workspace", "PROJECT_KEY_ALREADY_EXISTS");
        }

        Project project = new Project();
        project.setWorkspace(workspace);
        project.setName(request.getName());
        project.setKey(request.getKey());
        project.setDescription(request.getDescription());
        project.setOwner(owner);
        project.setDefaultVelocity(request.getDefaultVelocity());
        project.setSprintLengthDays(request.getSprintLengthDays());

        project = projectRepository.save(project);

        boardService.createDefaultColumns(project);

        if (owner != null) {
            chatService.createDefaultThread(project, owner);
        }

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByWorkspace(UUID workspaceId, UUID userId) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .filter(w -> !w.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ResourceNotFoundException("Workspace not found");
        }

        List<Project> projects = projectRepository.findByWorkspaceId(workspaceId);
        return projects.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setWorkspaceId(project.getWorkspace().getId());
        response.setName(project.getName());
        response.setKey(project.getKey());
        response.setDescription(project.getDescription());
        if (project.getOwner() != null) {
            response.setOwnerId(project.getOwner().getId());
            response.setOwnerName(project.getOwner().getFullName());
        }
        response.setDefaultVelocity(project.getDefaultVelocity());
        response.setSprintLengthDays(project.getSprintLengthDays());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
