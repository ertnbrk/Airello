package ai.planmate.agile.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.agile.dto.CreateEpicRequest;
import ai.planmate.agile.dto.CreateIssueRequest;
import ai.planmate.agile.dto.CreateSprintRequest;
import ai.planmate.agile.dto.EpicDto;
import ai.planmate.agile.dto.IssueResponse;
import ai.planmate.agile.dto.SprintResponse;
import ai.planmate.agile.dto.UpdateIssueRequest;
import ai.planmate.agile.mapper.EnumMapper;
import ai.planmate.agile.mapper.IssueMapper;
import ai.planmate.agile.mapper.SprintMapper;
import ai.planmate.agile.repository.EpicRepository;
import ai.planmate.agile.service.IssueService;
import ai.planmate.agile.service.SprintService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/projects/{projectId}")
@RequiredArgsConstructor
public class AgileController {

    private final IssueService issueService;
    private final SprintService sprintService;
    private final EpicRepository epicRepository;

    @PostMapping("/epics")
    @ResponseStatus(HttpStatus.CREATED)
    public EpicDto createEpic(
            @PathVariable UUID projectId, @Valid @RequestBody CreateEpicRequest request) {
        request.setProjectId(projectId);
        throw new UnsupportedOperationException("Epic creation not yet implemented");
    }

    @GetMapping("/epics")
    public List<EpicDto> listEpics(@PathVariable UUID projectId) {
        return epicRepository.findByProjectId(projectId).stream().map(EpicDto::fromEntity).toList();
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse createIssue(
            @PathVariable UUID projectId, @Valid @RequestBody CreateIssueRequest request) {
        request.setProjectId(projectId);
        return IssueMapper.toResponse(issueService.createIssue(request));
    }

    @PatchMapping("/issues/{issueId}")
    public IssueResponse updateIssue(
            @PathVariable UUID projectId,
            @PathVariable UUID issueId,
            @Valid @RequestBody UpdateIssueRequest request) {
        return IssueMapper.toResponse(issueService.updateIssue(issueId, request));
    }

    @GetMapping("/issues")
    public List<IssueResponse> listIssues(
            @PathVariable UUID projectId, @RequestParam(required = false) String status) {
        var issueStatus = status != null ? EnumMapper.toBackendStatus(status) : null;
        return IssueMapper.toResponseList(issueService.getProjectIssues(projectId, issueStatus));
    }

    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse createSprint(
            @PathVariable UUID projectId, @Valid @RequestBody CreateSprintRequest request) {
        request.setProjectId(projectId);
        return SprintMapper.toResponse(sprintService.createSprint(request));
    }

    @PostMapping("/sprints/{sprintId}/issues/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addIssueToSprint(
            @PathVariable UUID projectId, @PathVariable UUID sprintId, @PathVariable UUID issueId) {
        sprintService.addIssueToSprint(sprintId, issueId);
    }

    @GetMapping("/sprints")
    public List<SprintResponse> listSprints(@PathVariable UUID projectId) {
        return SprintMapper.toResponseList(sprintService.getProjectSprints(projectId));
    }
}
