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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.agile.dto.BoardColumnResponse;
import ai.planmate.agile.dto.BoardViewResponse;
import ai.planmate.agile.dto.IssueSummaryDto;
import ai.planmate.agile.entity.BoardColumn;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.mapper.IssueMapper;
import ai.planmate.agile.repository.BoardColumnRepository;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.realtime.RealtimeEvent;
import ai.planmate.realtime.RealtimeEventService;
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
        List<BoardColumn> columns = new ArrayList<>();
        for (int i = 0; i < DEFAULT_COLUMNS.length; i++) {
            BoardColumn col = new BoardColumn();
            col.setProject(project);
            col.setName(DEFAULT_COLUMNS[i]);
            col.setPosition(i);
            col.setIsDefault(i == 0);
            columns.add(boardColumnRepository.save(col));
        }
        return columns;
    }

    @Transactional(readOnly = true)
    public BoardViewResponse getBoard(UUID projectId, UUID assigneeId, String label) {
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderByPosition(projectId);

        // If no dynamic columns exist yet, return legacy view
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
                            .issues(issuesInColumn)
                            .build());
        }

        // Legacy compatibility: keep the old map too
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
    public BoardColumn createColumn(UUID projectId, String name) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        int nextPosition =
                boardColumnRepository
                        .findLastByProjectId(projectId)
                        .map(c -> c.getPosition() + 1)
                        .orElse(0);

        BoardColumn column = new BoardColumn();
        column.setProject(project);
        column.setName(name);
        column.setPosition(nextPosition);
        column = boardColumnRepository.save(column);

        realtimeEventService.broadcastBoardUpdate(
                projectId, RealtimeEvent.of("BOARD_COLUMN_CREATED", column));
        return column;
    }

    @Transactional
    public BoardColumn renameColumn(UUID columnId, String newName) {
        BoardColumn column =
                boardColumnRepository
                        .findById(columnId)
                        .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        column.setName(newName);
        column.setUpdatedAt(Instant.now());
        column = boardColumnRepository.save(column);

        realtimeEventService.broadcastBoardUpdate(
                column.getProject().getId(), RealtimeEvent.of("BOARD_COLUMN_UPDATED", column));
        return column;
    }

    @Transactional
    public void deleteColumn(UUID columnId) {
        BoardColumn column =
                boardColumnRepository
                        .findById(columnId)
                        .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

        UUID projectId = column.getProject().getId();

        // Move issues to default/first column
        BoardColumn fallback =
                boardColumnRepository
                        .findDefaultByProjectId(projectId)
                        .orElseGet(
                                () ->
                                        boardColumnRepository
                                                .findByProjectIdOrderByPosition(projectId)
                                                .stream()
                                                .filter(c -> !c.getId().equals(columnId))
                                                .findFirst()
                                                .orElse(null));

        if (fallback != null) {
            List<Issue> orphanedIssues =
                    issueRepository.findByProjectId(projectId).stream()
                            .filter(
                                    i ->
                                            i.getBoardColumn() != null
                                                    && i.getBoardColumn().getId().equals(columnId))
                            .toList();
            for (Issue issue : orphanedIssues) {
                issue.setBoardColumn(fallback);
                issueRepository.save(issue);
            }
        }

        boardColumnRepository.delete(column);

        realtimeEventService.broadcastBoardUpdate(
                projectId,
                RealtimeEvent.of("BOARD_COLUMN_DELETED", java.util.Map.of("columnId", columnId)));
    }

    @Transactional
    public void reorderColumns(UUID projectId, List<UUID> columnIds) {
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderByPosition(projectId);
        java.util.Map<UUID, BoardColumn> columnMap =
                columns.stream().collect(Collectors.toMap(BoardColumn::getId, c -> c));

        for (int i = 0; i < columnIds.size(); i++) {
            BoardColumn col = columnMap.get(columnIds.get(i));
            if (col != null) {
                col.setPosition(i);
                col.setUpdatedAt(Instant.now());
                boardColumnRepository.save(col);
            }
        }

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
                        .findById(targetColumnId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Target column not found"));

        if (!targetColumn.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Column does not belong to this project");
        }

        issue.setBoardColumn(targetColumn);

        // Compute new orderIndex
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

        // Default: place at end
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
}
