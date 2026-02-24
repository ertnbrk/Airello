package ai.planmate.agile.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.agile.dto.BoardColumnDto;
import ai.planmate.agile.dto.BoardViewResponse;
import ai.planmate.agile.dto.CreateColumnRequest;
import ai.planmate.agile.dto.MoveIssueRequest;
import ai.planmate.agile.dto.ReorderColumnsRequest;
import ai.planmate.agile.dto.UpdateColumnRequest;
import ai.planmate.agile.service.BoardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/projects/{projectId}/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public BoardViewResponse getBoard(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(required = false) String label) {
        return boardService.getBoard(projectId, assignee, label);
    }

    @PostMapping("/columns")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardColumnDto createColumn(
            @PathVariable UUID projectId, @Valid @RequestBody CreateColumnRequest request) {
        return BoardColumnDto.fromEntity(
                boardService.createColumn(
                        projectId,
                        request.getName(),
                        request.getCategory(),
                        request.getWipLimit(),
                        request.getIsDefault()));
    }

    @PutMapping("/columns/{columnId}")
    public BoardColumnDto updateColumn(
            @PathVariable UUID projectId,
            @PathVariable UUID columnId,
            @Valid @RequestBody UpdateColumnRequest request) {
        return BoardColumnDto.fromEntity(
                boardService.updateColumn(
                        columnId,
                        request.getName(),
                        request.getWipLimit(),
                        request.getCategory(),
                        request.getIsDefault()));
    }

    @DeleteMapping("/columns/{columnId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteColumn(@PathVariable UUID projectId, @PathVariable UUID columnId) {
        boardService.deleteColumn(columnId);
    }

    @PutMapping("/columns/reorder")
    public void reorderColumns(
            @PathVariable UUID projectId, @Valid @RequestBody ReorderColumnsRequest request) {
        boardService.reorderColumns(projectId, request.getColumnIds());
    }

    @PutMapping("/issues/{issueId}/move")
    public void moveIssue(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @Valid @RequestBody MoveIssueRequest request) {
        boardService.moveIssue(
                projectId,
                issueId,
                request.getTargetColumnId(),
                request.getAfterIssueId(),
                request.getBeforeIssueId());
    }
}
