package ai.planmate.agile.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.agile.dto.BoardColumnDto;
import ai.planmate.agile.dto.BoardColumnResponse;
import ai.planmate.agile.dto.BoardViewResponse;
import ai.planmate.agile.dto.IssueSummaryDto;
import ai.planmate.agile.entity.BoardColumn;
import ai.planmate.agile.entity.ColumnCategory;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.mapper.IssueMapper;
import ai.planmate.agile.repository.BoardColumnRepository;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.realtime.RealtimeEvent;
import ai.planmate.realtime.RealtimeEventService;
import ai.planmate.shared.exception.ConflictException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final IssueRepository issueRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectRepository projectRepository;
    private final RealtimeEventService realtimeEventService;

    private static final String[] DEFAULT_COLUMNS = {
        "Backlog", "To Do", "In Progress", "Review", "Done"
    };

    @Transactional
    public List<BoardColumn> createDefaultColumns(Project project) {
        log.info("Creating default columns for project: {}", project.getId());
        List<BoardColumn> columns = new ArrayList<>();
        UUID currentUserId = getCurrentUserId();

        for (int i = 0; i < DEFAULT_COLUMNS.length; i++) {
            BoardColumn col = new BoardColumn();
            col.setProject(project);
            col.setName(DEFAULT_COLUMNS[i]);
            col.setPosition(i);
            col.setIsDefault(i == 0);
            col.setCategory(determineCategoryFromName(DEFAULT_COLUMNS[i]));
            col.setCreatedBy(currentUserId);
            col.setUpdatedBy(currentUserId);
            columns.add(boardColumnRepository.save(col));
        }

        log.info(
                "Created {} default columns for project: {}",
                columns.size(),
                project.getId());
        return columns;
    }

    @Transactional(readOnly = true)
    public BoardViewResponse getBoard(UUID projectId, UUID assigneeId, String label) {
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderByPosition(projectId);

        if (columns.isEmpty()) {
            return getLegacyBoard(projectId, assigneeId, label);
        }

        List<Issue> allIssues = issueRepository.findByProjectId(projectId);

        if (assigneeId != null) {
            allIssues =
                    allIssues.stream()
                            .filter(
                                    i ->
                                            i.getAssignee() != null
                                                    && i.getAssignee().getId().equals(assigneeId))
                            .collect(Collectors.toList());
        }

        if (label != null && !label.isEmpty()) {
            allIssues =
                    allIssues.stream()
                            .filter(
                                    i ->
                                            i.getLabels() != null
                                                    && Arrays.asList(i.getLabels()).contains(label))
                            .collect(Collectors.toList());
        }

        List<BoardColumnResponse> columnResponses = new ArrayList<>();
        for (BoardColumn col : columns) {
            List<IssueSummaryDto> issuesInColumn =
                    allIssues.stream()
                            .filter(
                                    i ->
                                            i.getBoardColumn() != null
                                                    && i.getBoardColumn()
                                                            .getId()
                                                            .equals(col.getId()))
                            .sorted(Comparator.comparing(Issue::getOrderIndex))
                            .map(IssueMapper::toSummary)
                            .collect(Collectors.toList());

            columnResponses.add(
                    BoardColumnResponse.builder()
                            .id(col.getId())
                            .name(col.getName())
                            .position(col.getPosition())
                            .isDefault(col.getIsDefault())
                            .category(col.getCategory())
                            .wipLimit(col.getWipLimit())
                            .issues(issuesInColumn)
                            .build());
        }

        BoardViewResponse response = new BoardViewResponse();
        response.setDynamicColumns(columnResponses);
        return response;
    }

    private BoardViewResponse getLegacyBoard(UUID projectId, UUID assigneeId, String label) {
        List<Issue> issues = issueRepository.findByProjectId(projectId);

        if (assigneeId != null) {
            issues =
                    issues.stream()
                            .filter(
                                    i ->
                                            i.getAssignee() != null
                                                    && i.getAssignee().getId().equals(assigneeId))
                            .collect(Collectors.toList());
        }

        if (label != null && !label.isEmpty()) {
            issues =
                    issues.stream()
                            .filter(
                                    i ->
                                            i.getLabels() != null
                                                    && Arrays.asList(i.getLabels()).contains(label))
                            .collect(Collectors.toList());
        }

        java.util.Map<String, List<IssueSummaryDto>> columns = new java.util.LinkedHashMap<>();
        ai.planmate.agile.entity.IssueStatus[] statusValues =
                ai.planmate.agile.entity.IssueStatus.values();
        for (int idx = 0; idx < statusValues.length; idx++) {
            ai.planmate.agile.entity.IssueStatus status = statusValues[idx];
            String frontendStatus = ai.planmate.agile.mapper.EnumMapper.toFrontendStatus(status);
            List<IssueSummaryDto> columnIssues =
                    issues.stream()
                            .filter(i -> i.getStatus() == status)
                            .sorted(Comparator.comparing(Issue::getOrderIndex))
                            .map(IssueMapper::toSummary)
                            .collect(Collectors.toList());
            columns.put(frontendStatus, columnIssues);
        }

        BoardViewResponse response = new BoardViewResponse();
        response.setColumns(columns);
        return response;
    }

    @Transactional
    public BoardColumn createColumn(
            UUID projectId,
            String name,
            ColumnCategory category,
            Integer wipLimit,
            Boolean isDefault) {
        UUID userId = getCurrentUserId();

        log.info(
                "Creating column: projectId={}, name={}, category={}, wipLimit={}, isDefault={},"
                        + " userId={}",
                projectId,
                name,
                category,
                wipLimit,
                isDefault,
                userId);

        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        String trimmedName = name.trim();
        if (trimmedName.length() < 1 || trimmedName.length() > 60) {
            throw new IllegalArgumentException("Column name must be between 1 and 60 characters");
        }

        if (wipLimit != null && wipLimit < 1) {
            throw new IllegalArgumentException("WIP limit must be at least 1 if specified");
        }

        Boolean isDefaultValue = isDefault != null ? isDefault : false;

        if (isDefaultValue && boardColumnRepository.existsDefaultByProjectId(projectId)) {
            throw new ConflictException(
                    "A default column already exists for this project", "DEFAULT_COLUMN_EXISTS");
        }

        Integer maxPosition = boardColumnRepository.findMaxPositionByProjectId(projectId);
        int nextPosition = (maxPosition != null && maxPosition >= 0) ? maxPosition + 1 : 0;

        ColumnCategory columnCategory = category != null ? category : ColumnCategory.CUSTOM;

        BoardColumn column = new BoardColumn();
        column.setProject(project);
        column.setName(trimmedName);
        column.setPosition(nextPosition);
        column.setIsDefault(isDefaultValue);
        column.setCategory(columnCategory);
        column.setWipLimit(wipLimit);
        column.setCreatedBy(userId);
        column.setUpdatedBy(userId);

        column = boardColumnRepository.save(column);

        log.info(
                "Column created: id={}, projectId={}, name={}, position={}, category={}, userId={}",
                column.getId(),
                projectId,
                trimmedName,
                nextPosition,
                columnCategory,
                userId);

        realtimeEventService.broadcastBoardUpdate(
                projectId,
                RealtimeEvent.of("BOARD_COLUMN_CREATED", BoardColumnDto.fromEntity(column)));

        return column;
    }

    @Transactional
    public BoardColumn createColumn(UUID projectId, String name) {
        return createColumn(projectId, name, null, null, null);
    }

    @Transactional
    public BoardColumn updateColumn(
            UUID columnId, String name, Integer wipLimit, ColumnCategory category, Boolean isDefault) {
        UUID userId = getCurrentUserId();

        log.info(
                "Updating column: id={}, name={}, wipLimit={}, category={}, isDefault={}, userId={}",
                columnId,
                name,
                wipLimit,
                category,
                isDefault,
                userId);

        BoardColumn column =
                boardColumnRepository
                        .findByIdAndNotDeleted(columnId)
                        .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        if (name != null) {
            String trimmedName = name.trim();
            if (trimmedName.length() < 1 || trimmedName.length() > 60) {
                throw new IllegalArgumentException(
                        "Column name must be between 1 and 60 characters");
            }
            column.setName(trimmedName);
        }

        if (wipLimit != null && wipLimit < 1) {
            throw new IllegalArgumentException("WIP limit must be at least 1 if specified");
        }

        if (wipLimit != null) {
            column.setWipLimit(wipLimit);
        }

        if (category != null) {
            column.setCategory(category);
        }

        if (isDefault != null) {
            if (isDefault && !column.getIsDefault()) {
                if (boardColumnRepository.existsDefaultByProjectId(
                        column.getProject().getId())) {
                    throw new ConflictException(
                            "A default column already exists for this project",
                            "DEFAULT_COLUMN_EXISTS");
                }
            }
            column.setIsDefault(isDefault);
        }

        column.setUpdatedBy(userId);
        column = boardColumnRepository.save(column);

        log.info("Column updated: id={}, userId={}", columnId, userId);

        realtimeEventService.broadcastBoardUpdate(
                column.getProject().getId(),
                RealtimeEvent.of("BOARD_COLUMN_UPDATED", BoardColumnDto.fromEntity(column)));

        return column;
    }

    @Transactional
    public BoardColumn renameColumn(UUID columnId, String newName) {
        return updateColumn(columnId, newName, null, null, null);
    }

    @Transactional
    public void deleteColumn(UUID columnId) {
        UUID userId = getCurrentUserId();

        log.info("Deleting column: id={}, userId={}", columnId, userId);

        BoardColumn column =
                boardColumnRepository
                        .findByIdAndNotDeleted(columnId)
                        .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        UUID projectId = column.getProject().getId();

        if (column.getIsDefault()) {
            throw new ConflictException(
                    "Cannot delete the default column", "CANNOT_DELETE_DEFAULT_COLUMN");
        }

        List<Issue> issuesInColumn =
                issueRepository.findByProjectId(projectId).stream()
                        .filter(
                                i ->
                                        i.getBoardColumn() != null
                                                && i.getBoardColumn().getId().equals(columnId))
                        .toList();

        if (!issuesInColumn.isEmpty()) {
            throw new ConflictException(
                    String.format(
                            "Cannot delete column with %d issue(s). Move issues to another column"
                                    + " first.",
                            issuesInColumn.size()),
                    "COLUMN_IN_USE");
        }

        column.setDeletedAt(Instant.now());
        column.setUpdatedBy(userId);
        boardColumnRepository.save(column);

        log.info(
                "Column soft-deleted: id={}, projectId={}, issuesCount={}, userId={}",
                columnId,
                projectId,
                issuesInColumn.size(),
                userId);

        realtimeEventService.broadcastBoardUpdate(
                projectId,
                RealtimeEvent.of("BOARD_COLUMN_DELETED", java.util.Map.of("columnId", columnId)));
    }

    @Transactional
    public void reorderColumns(UUID projectId, List<UUID> columnIds) {
        UUID userId = getCurrentUserId();

        log.info(
                "Reordering columns: projectId={}, columnCount={}, userId={}",
                projectId,
                columnIds.size(),
                userId);

        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderByPosition(projectId);
        java.util.Map<UUID, BoardColumn> columnMap =
                columns.stream().collect(Collectors.toMap(BoardColumn::getId, c -> c));

        for (UUID columnId : columnIds) {
            if (!columnMap.containsKey(columnId)) {
                throw new ResourceNotFoundException(
                        "Column not found or belongs to different project: " + columnId);
            }
        }

        for (int i = 0; i < columnIds.size(); i++) {
            BoardColumn col = columnMap.get(columnIds.get(i));
            if (col != null) {
                col.setPosition(i);
                col.setUpdatedBy(userId);
                boardColumnRepository.save(col);
            }
        }

        log.info(
                "Columns reordered: projectId={}, count={}, userId={}",
                projectId,
                columnIds.size(),
                userId);

        realtimeEventService.broadcastBoardUpdate(
                projectId, RealtimeEvent.of("BOARD_COLUMNS_REORDERED", columnIds));
    }

    @Transactional
    public Issue moveIssue(
            UUID projectId,
            UUID issueId,
            UUID targetColumnId,
            UUID afterIssueId,
            UUID beforeIssueId) {
        Issue issue =
                issueRepository
                        .findByIdAndNotDeleted(issueId)
                        .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        BoardColumn targetColumn =
                boardColumnRepository
                        .findByIdAndNotDeleted(targetColumnId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Target column not found"));

        if (!targetColumn.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Column does not belong to this project");
        }

        issue.setBoardColumn(targetColumn);

        BigDecimal newOrder = computeOrderIndex(targetColumnId, afterIssueId, beforeIssueId);
        issue.setOrderIndex(newOrder);
        issue = issueRepository.save(issue);

        realtimeEventService.broadcastBoardUpdate(
                projectId,
                RealtimeEvent.of(
                        "ISSUE_MOVED",
                        java.util.Map.of(
                                "issueId", issueId,
                                "columnId", targetColumnId,
                                "orderIndex", newOrder)));

        return issue;
    }

    private BigDecimal computeOrderIndex(UUID columnId, UUID afterIssueId, UUID beforeIssueId) {
        if (afterIssueId != null && beforeIssueId != null) {
            Issue after = issueRepository.findByIdAndNotDeleted(afterIssueId).orElse(null);
            Issue before = issueRepository.findByIdAndNotDeleted(beforeIssueId).orElse(null);
            if (after != null && before != null) {
                BigDecimal avg =
                        after.getOrderIndex()
                                .add(before.getOrderIndex())
                                .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
                if (avg.subtract(after.getOrderIndex()).abs().compareTo(new BigDecimal("0.001"))
                        < 0) {
                    reindexColumn(columnId);
                    return after.getOrderIndex().add(BigDecimal.valueOf(500));
                }
                return avg;
            }
        }

        if (afterIssueId != null) {
            Issue after = issueRepository.findByIdAndNotDeleted(afterIssueId).orElse(null);
            if (after != null) {
                return after.getOrderIndex().add(BigDecimal.valueOf(1000));
            }
        }

        if (beforeIssueId != null) {
            Issue before = issueRepository.findByIdAndNotDeleted(beforeIssueId).orElse(null);
            if (before != null) {
                BigDecimal half =
                        before.getOrderIndex()
                                .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
                return half.max(BigDecimal.ONE);
            }
        }

        return BigDecimal.valueOf(1000);
    }

    private void reindexColumn(UUID columnId) {
        List<Issue> issues =
                issueRepository.findByProjectId(null).stream()
                        .filter(
                                i ->
                                        i.getBoardColumn() != null
                                                && i.getBoardColumn().getId().equals(columnId))
                        .sorted(Comparator.comparing(Issue::getOrderIndex))
                        .toList();

        BigDecimal index = BigDecimal.valueOf(1000);
        for (Issue issue : issues) {
            issue.setOrderIndex(index);
            issueRepository.save(issue);
            index = index.add(BigDecimal.valueOf(1000));
        }
    }

    public List<BoardColumn> getColumns(UUID projectId) {
        return boardColumnRepository.findByProjectIdOrderByPosition(projectId);
    }

    private UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AppUser user) {
                return user.getId();
            }
        } catch (Exception e) {
            log.warn("Could not extract current user ID: {}", e.getMessage());
        }
        return null;
    }

    private ColumnCategory determineCategoryFromName(String name) {
        String lowerName = name.toLowerCase();
        if (lowerName.contains("backlog")) {
            return ColumnCategory.BACKLOG;
        } else if (lowerName.contains("done")) {
            return ColumnCategory.DONE;
        } else if (lowerName.contains("progress")) {
            return ColumnCategory.IN_PROGRESS;
        } else if (lowerName.contains("to do")) {
            return ColumnCategory.TODO;
        }
        return ColumnCategory.CUSTOM;
    }
}
