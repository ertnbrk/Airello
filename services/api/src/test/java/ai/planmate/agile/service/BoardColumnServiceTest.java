package ai.planmate.agile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.agile.entity.BoardColumn;
import ai.planmate.agile.entity.ColumnCategory;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.repository.BoardColumnRepository;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.AuthProvider;
import ai.planmate.auth.entity.UserType;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.entity.Workspace;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.projects.repository.WorkspaceRepository;
import ai.planmate.shared.exception.ConflictException;
import ai.planmate.shared.exception.ResourceNotFoundException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BoardColumnServiceTest {

    @Autowired private BoardService boardService;

    @Autowired private BoardColumnRepository boardColumnRepository;

    @Autowired private ProjectRepository projectRepository;

    @Autowired private WorkspaceRepository workspaceRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private IssueRepository issueRepository;

    private Project testProject;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setUserType(UserType.REGISTERED);
        testUser.setProvider(AuthProvider.LOCAL);
        testUser.setEmailVerified(true);
        testUser = appUserRepository.save(testUser);

        Workspace workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspace.setKey("TEST");
        workspace = workspaceRepository.save(workspace);

        testProject = new Project();
        testProject.setWorkspace(workspace);
        testProject.setName("Test Project");
        testProject.setKey("TP");
        testProject.setOwner(testUser);
        testProject = projectRepository.save(testProject);
    }

    @Test
    void softDeletedColumnsNotReturnedInList() {
        BoardColumn column =
                boardService.createColumn(testProject.getId(), "Test Column", null, null, null);
        UUID columnId = column.getId();

        List<BoardColumn> columnsBeforeDelete =
                boardColumnRepository.findByProjectIdOrderByPosition(testProject.getId());
        assertThat(columnsBeforeDelete).hasSize(1);

        boardColumnRepository.delete(column);
        boardColumnRepository.flush();

        List<BoardColumn> columnsAfterDelete =
                boardColumnRepository.findByProjectIdOrderByPosition(testProject.getId());
        assertThat(columnsAfterDelete).isEmpty();
    }

    @Test
    void cannotCreateSecondDefaultColumn() {
        boardService.createColumn(testProject.getId(), "First Column", null, null, true);

        assertThatThrownBy(
                        () ->
                                boardService.createColumn(
                                        testProject.getId(), "Second Column", null, null, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("default column already exists");
    }

    @Test
    void cannotDeleteDefaultColumn() {
        BoardColumn defaultColumn =
                boardService.createColumn(testProject.getId(), "Default Column", null, null, true);

        assertThatThrownBy(() -> boardService.deleteColumn(defaultColumn.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete the default column");
    }

    @Test
    void cannotDeleteColumnWithIssues() {
        BoardColumn column =
                boardService.createColumn(testProject.getId(), "Column with Issues", null, null, null);

        Issue issue = new Issue();
        issue.setProject(testProject);
        issue.setTitle("Test Issue");
        issue.setReporter(testUser);
        issue.setBoardColumn(column);
        issueRepository.save(issue);

        assertThatThrownBy(() -> boardService.deleteColumn(column.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete column with");
    }

    @Test
    void reorderIgnoresDeletedColumns() {
        BoardColumn column1 =
                boardService.createColumn(testProject.getId(), "Column 1", null, null, null);
        BoardColumn column2 =
                boardService.createColumn(testProject.getId(), "Column 2", null, null, null);
        BoardColumn column3 =
                boardService.createColumn(testProject.getId(), "Column 3", null, null, null);

        column2.setDeletedAt(java.time.Instant.now());
        boardColumnRepository.save(column2);

        List<UUID> reorderedIds = List.of(column3.getId(), column1.getId());
        boardService.reorderColumns(testProject.getId(), reorderedIds);

        List<BoardColumn> columns =
                boardColumnRepository.findByProjectIdOrderByPosition(testProject.getId());
        assertThat(columns).hasSize(2);
        assertThat(columns.get(0).getId()).isEqualTo(column3.getId());
        assertThat(columns.get(1).getId()).isEqualTo(column1.getId());
    }

    @Test
    void wipLimitValidation() {
        assertThatThrownBy(
                        () ->
                                boardService.createColumn(
                                        testProject.getId(),
                                        "Invalid WIP Column",
                                        null,
                                        0,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WIP limit must be at least 1");

        BoardColumn validColumn =
                boardService.createColumn(testProject.getId(), "Valid WIP Column", null, 5, null);
        assertThat(validColumn.getWipLimit()).isEqualTo(5);

        BoardColumn unlimitedColumn =
                boardService.createColumn(testProject.getId(), "Unlimited Column", null, null, null);
        assertThat(unlimitedColumn.getWipLimit()).isNull();
    }

    @Test
    void categoryDetermination() {
        BoardColumn backlogColumn =
                boardService.createColumn(
                        testProject.getId(), "Product Backlog", ColumnCategory.BACKLOG, null, null);
        assertThat(backlogColumn.getCategory()).isEqualTo(ColumnCategory.BACKLOG);

        BoardColumn doneColumn =
                boardService.createColumn(
                        testProject.getId(), "Done", ColumnCategory.DONE, null, null);
        assertThat(doneColumn.getCategory()).isEqualTo(ColumnCategory.DONE);

        BoardColumn customColumn =
                boardService.createColumn(testProject.getId(), "Custom Status", null, null, null);
        assertThat(customColumn.getCategory()).isEqualTo(ColumnCategory.CUSTOM);
    }

    @Test
    void columnNameTrimming() {
        BoardColumn column =
                boardService.createColumn(
                        testProject.getId(), "  Trimmed Column  ", null, null, null);
        assertThat(column.getName()).isEqualTo("Trimmed Column");
    }

    @Test
    void columnNameLengthValidation() {
        String tooLong = "a".repeat(61);
        assertThatThrownBy(
                        () -> boardService.createColumn(testProject.getId(), tooLong, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Column name must be between 1 and 60 characters");

        String emptyName = "   ";
        assertThatThrownBy(
                        () ->
                                boardService.createColumn(
                                        testProject.getId(), emptyName, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void positionCalculation() {
        BoardColumn column1 =
                boardService.createColumn(testProject.getId(), "Column 1", null, null, null);
        BoardColumn column2 =
                boardService.createColumn(testProject.getId(), "Column 2", null, null, null);
        BoardColumn column3 =
                boardService.createColumn(testProject.getId(), "Column 3", null, null, null);

        assertThat(column1.getPosition()).isEqualTo(0);
        assertThat(column2.getPosition()).isEqualTo(1);
        assertThat(column3.getPosition()).isEqualTo(2);
    }

    @Test
    void updateColumnSetsIsDefaultCorrectly() {
        BoardColumn column1 =
                boardService.createColumn(testProject.getId(), "Column 1", null, null, true);
        BoardColumn column2 =
                boardService.createColumn(testProject.getId(), "Column 2", null, null, false);

        assertThatThrownBy(
                        () ->
                                boardService.updateColumn(
                                        column2.getId(), null, null, null, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("default column already exists");
    }

    @Test
    void findByIdAndNotDeletedReturnsEmptyForDeletedColumn() {
        BoardColumn column =
                boardService.createColumn(testProject.getId(), "Test Column", null, null, null);
        UUID columnId = column.getId();

        column.setDeletedAt(java.time.Instant.now());
        boardColumnRepository.save(column);

        assertThatThrownBy(() -> boardService.updateColumn(columnId, "New Name", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
