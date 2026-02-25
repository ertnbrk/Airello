package ai.planmate.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.dto.CreateProjectRequest;
import ai.planmate.projects.dto.ProjectResponse;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.entity.Workspace;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.projects.repository.WorkspaceRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;

    @Mock private WorkspaceRepository workspaceRepository;

    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private ProjectService projectService;

    private UUID userId;
    private UUID workspaceId;
    private AppUser user;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();

        user = new AppUser();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFullName("Test User");

        workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setName("Test Workspace");
        workspace.setOwner(user);
    }

    @Test
    void createProjectSuccess() {
        // Given
        CreateProjectRequest request = new CreateProjectRequest();
        request.setWorkspaceId(workspaceId);
        request.setName("Test Project");
        request.setKey("TEST");
        request.setDescription("Test description");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        Project savedProject = new Project();
        savedProject.setId(UUID.randomUUID());
        savedProject.setName(request.getName());
        savedProject.setKey(request.getKey());
        savedProject.setWorkspace(workspace);
        savedProject.setOwner(null); // No authentication, so owner is null

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        // When
        ProjectResponse response = projectService.createProject(request);

        // Then
        assertNotNull(response);
        assertEquals("Test Project", response.getName());
        assertEquals("TEST", response.getKey());
        verify(projectRepository, times(1)).save(any(Project.class));
    }
}
