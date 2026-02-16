package ai.planmate.auth.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.agile.entity.BoardColumn;
import ai.planmate.agile.entity.Epic;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.entity.IssuePriority;
import ai.planmate.agile.entity.IssueStatus;
import ai.planmate.agile.entity.IssueType;
import ai.planmate.agile.repository.EpicRepository;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.agile.service.BoardService;
import ai.planmate.auth.dto.AuthResponse;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.UserType;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.entity.Workspace;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.projects.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoService {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final EpicRepository epicRepository;
    private final IssueRepository issueRepository;
    private final BoardService boardService;

    @Value("${planmate.demo.expiration-hours:24}")
    private int expirationHours;

    @Transactional
    public AuthResponse createDemoSession() {
        // 1. Create anonymous user
        AppUser user = authService.createAnonymousUser();

        // 2. Create personal workspace
        Workspace workspace = new Workspace();
        workspace.setName("Personal");
        workspace.setSlug("personal-" + user.getId().toString().substring(0, 8));
        workspace.setOwner(user);
        workspace = workspaceRepository.save(workspace);

        // 3. Create demo project
        Project project = new Project();
        project.setWorkspace(workspace);
        project.setName("Demo Project");
        project.setKey("DEMO");
        project.setDescription("A sample project to explore Airello's features.");
        project.setOwner(user);
        project = projectRepository.save(project);

        // 4. Create board columns
        List<BoardColumn> columns = boardService.createDefaultColumns(project);

        // 5. Create an epic
        Epic epic = new Epic();
        epic.setProject(project);
        epic.setTitle("Getting Started");
        epic.setDescription("Sample epic with starter tasks");
        epic = epicRepository.save(epic);

        // 6. Create sample issues across columns
        createSampleIssues(project, epic, columns, user);

        // 7. Generate JWT
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, authService.mapToUserDto(user));
    }

    private void createSampleIssues(
            Project project, Epic epic, List<BoardColumn> columns, AppUser reporter) {
        String[][] issueData = {
            {"Set up project structure", "TASK", "0", "HIGH", "3"},
            {"Design database schema", "TASK", "0", "CRITICAL", "5"},
            {"Implement user authentication", "STORY", "1", "HIGH", "8"},
            {"Create board view UI", "STORY", "2", "MEDIUM", "5"},
            {"Write API documentation", "TASK", "3", "LOW", "2"},
            {"Fix login redirect bug", "BUG", "2", "HIGH", "3"}
        };

        int issueNum = 1;
        for (String[] data : issueData) {
            int colIdx = Integer.parseInt(data[2]);
            BoardColumn column = colIdx < columns.size() ? columns.get(colIdx) : columns.get(0);

            Issue issue = new Issue();
            issue.setProject(project);
            issue.setKey(project.getKey() + "-" + issueNum);
            issue.setTitle(data[0]);
            issue.setType(IssueType.valueOf(data[1]));
            issue.setStatus(mapColumnToStatus(colIdx));
            issue.setPriority(IssuePriority.valueOf(data[3]));
            issue.setStoryPoints(Integer.parseInt(data[4]));
            issue.setReporter(reporter);
            issue.setEpic(epic);
            issue.setBoardColumn(column);
            issue.setOrderIndex(BigDecimal.valueOf(issueNum * 1000));
            issueRepository.save(issue);
            issueNum++;
        }
    }

    private IssueStatus mapColumnToStatus(int colIdx) {
        return switch (colIdx) {
            case 0 -> IssueStatus.BACKLOG;
            case 1 -> IssueStatus.SELECTED;
            case 2 -> IssueStatus.IN_PROGRESS;
            case 3 -> IssueStatus.REVIEW;
            case 4 -> IssueStatus.DONE;
            default -> IssueStatus.BACKLOG;
        };
    }

    /** Cleanup expired anonymous users and their data. Runs every hour. */
    @Scheduled(fixedDelayString = "3600000")
    @Transactional
    public void cleanupExpiredDemoUsers() {
        Instant cutoff = Instant.now().minusSeconds(expirationHours * 3600L);
        List<AppUser> expiredUsers =
                appUserRepository.findAll().stream()
                        .filter(u -> u.getUserType() == UserType.ANONYMOUS)
                        .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isBefore(cutoff))
                        .toList();

        for (AppUser user : expiredUsers) {
            log.info("Cleaning up expired demo user: { }", user.getId());
            user.softDelete();
            appUserRepository.save(user);
        }

        if (!expiredUsers.isEmpty()) {
            log.info("Cleaned up { } expired demo users", expiredUsers.size());
        }
    }
}
