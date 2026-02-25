package ai.planmate.agile.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.agile.dto.PlanningResult;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.entity.IssueStatus;
import ai.planmate.agile.entity.Sprint;
import ai.planmate.agile.entity.SprintStatus;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.agile.repository.SprintRepository;
import ai.planmate.ai.dto.PlanningAutoResponse;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningEngineService {

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;

    @Transactional
    public PlanningResult autoPlanning(UUID projectId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<Issue> backlogIssues =
                issueRepository.findByProjectIdAndStatus(projectId, IssueStatus.BACKLOG);

        if (backlogIssues.isEmpty()) {
            return new PlanningResult(
                    Collections.emptyList(), "No backlog issues found", Collections.emptyList());
        }

        List<Issue> plannable =
                backlogIssues.stream()
                        .filter(i -> i.getStoryPoints() != null && i.getStoryPoints() > 0)
                        .sorted(
                                (a, b) -> {
                                    int priorityCompare =
                                            b.getPriority().compareTo(a.getPriority());
                                    if (priorityCompare != 0) {
                                        return priorityCompare;
                                    }
                                    return b.getStoryPoints().compareTo(a.getStoryPoints());
                                })
                        .collect(Collectors.toList());

        int velocity = project.getDefaultVelocity();
        int sprintLengthDays = project.getSprintLengthDays();

        List<SprintPlan> sprintPlans = new ArrayList<>();
        int currentSprintIndex = 0;
        int currentCapacity = velocity;
        LocalDate currentStartDate = LocalDate.now();

        List<Issue> sprintIssues = new ArrayList<>();

        for (Issue issue : plannable) {
            if (currentCapacity >= issue.getStoryPoints()) {
                sprintIssues.add(issue);
                currentCapacity -= issue.getStoryPoints();
            } else {
                if (!sprintIssues.isEmpty()) {
                    Sprint sprint =
                            createSprint(
                                    project,
                                    "Sprint " + (currentSprintIndex + 1),
                                    currentStartDate,
                                    currentStartDate.plusDays(sprintLengthDays),
                                    sprintIssues);
                    sprintPlans.add(
                            new SprintPlan(
                                    sprint.getId(),
                                    sprint.getName(),
                                    sprintIssues.stream()
                                            .map(Issue::getKey)
                                            .collect(Collectors.toList())));
                    currentSprintIndex++;
                    currentStartDate = currentStartDate.plusDays(sprintLengthDays);
                }

                sprintIssues = new ArrayList<>();
                sprintIssues.add(issue);
                currentCapacity = velocity - issue.getStoryPoints();
            }
        }

        if (!sprintIssues.isEmpty()) {
            Sprint sprint =
                    createSprint(
                            project,
                            "Sprint " + (currentSprintIndex + 1),
                            currentStartDate,
                            currentStartDate.plusDays(sprintLengthDays),
                            sprintIssues);
            sprintPlans.add(
                    new SprintPlan(
                            sprint.getId(),
                            sprint.getName(),
                            sprintIssues.stream().map(Issue::getKey).collect(Collectors.toList())));
        }

        String summary =
                String.format(
                        "Created %d sprints with %d issues allocated",
                        sprintPlans.size(), plannable.size());

        return new PlanningResult(sprintPlans, summary, findCriticalPath(plannable));
    }

    private Sprint createSprint(
            Project project,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            List<Issue> issues) {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(name);
        sprint.setStartDate(startDate);
        sprint.setEndDate(endDate);
        sprint.setStatus(SprintStatus.PLANNED);

        sprint = sprintRepository.save(sprint);

        for (Issue issue : issues) {
            issue.setStatus(IssueStatus.SELECTED);
            issueRepository.save(issue);
        }

        return sprint;
    }

    @Transactional(readOnly = true)
    public PlanningAutoResponse autoPlanningForFrontend(UUID projectId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<Issue> backlogIssues =
                issueRepository.findByProjectIdAndStatus(projectId, IssueStatus.BACKLOG);
        List<Issue> selectedIssues =
                issueRepository.findByProjectIdAndStatus(projectId, IssueStatus.SELECTED);

        List<Issue> plannableIssues = new ArrayList<>();
        plannableIssues.addAll(backlogIssues);
        plannableIssues.addAll(selectedIssues);

        if (plannableIssues.isEmpty()) {
            return PlanningAutoResponse.builder()
                    .sprints(Collections.emptyList())
                    .summary("No issues available for planning")
                    .criticalPath(Collections.emptyList())
                    .build();
        }

        List<Issue> plannable =
                plannableIssues.stream()
                        .filter(i -> i.getStoryPoints() != null && i.getStoryPoints() > 0)
                        .sorted(
                                (a, b) -> {
                                    int priorityCompare =
                                            b.getPriority().compareTo(a.getPriority());
                                    if (priorityCompare != 0) {
                                        return priorityCompare;
                                    }
                                    return b.getStoryPoints().compareTo(a.getStoryPoints());
                                })
                        .collect(Collectors.toList());

        if (plannable.isEmpty()) {
            return PlanningAutoResponse.builder()
                    .sprints(Collections.emptyList())
                    .summary("No issues with story points found")
                    .criticalPath(Collections.emptyList())
                    .build();
        }

        int velocity = project.getDefaultVelocity();
        int sprintLengthDays = project.getSprintLengthDays();
        LocalDate startDate = LocalDate.now();

        List<PlanningAutoResponse.PlannedSprint> plannedSprints = new ArrayList<>();
        List<Issue> currentSprintIssues = new ArrayList<>();
        int currentPoints = 0;
        int sprintNumber = 1;

        for (Issue issue : plannable) {
            if (currentPoints + issue.getStoryPoints() <= velocity) {
                currentSprintIssues.add(issue);
                currentPoints += issue.getStoryPoints();
            } else {
                if (!currentSprintIssues.isEmpty()) {
                    plannedSprints.add(
                            createPlannedSprint(
                                    sprintNumber,
                                    currentSprintIssues,
                                    currentPoints,
                                    startDate,
                                    sprintLengthDays));
                    sprintNumber++;
                    startDate = startDate.plusDays(sprintLengthDays);
                }

                currentSprintIssues = new ArrayList<>();
                currentSprintIssues.add(issue);
                currentPoints = issue.getStoryPoints();
            }
        }

        if (!currentSprintIssues.isEmpty()) {
            plannedSprints.add(
                    createPlannedSprint(
                            sprintNumber,
                            currentSprintIssues,
                            currentPoints,
                            startDate,
                            sprintLengthDays));
        }

        if (plannedSprints.size() > 3) {
            plannedSprints = plannedSprints.subList(0, 3);
        }

        String summary =
                String.format(
                        "Generated %d sprint%s with %d issues. Total story points: %d",
                        plannedSprints.size(),
                        plannedSprints.size() > 1 ? "s" : "",
                        plannable.size(),
                        plannable.stream().mapToInt(Issue::getStoryPoints).sum());

        List<String> criticalPath = findCriticalPathIds(plannable);

        return PlanningAutoResponse.builder()
                .sprints(plannedSprints)
                .summary(summary)
                .criticalPath(criticalPath)
                .build();
    }

    private PlanningAutoResponse.PlannedSprint createPlannedSprint(
            int sprintNumber,
            List<Issue> issues,
            int totalPoints,
            LocalDate startDate,
            int durationDays) {
        return PlanningAutoResponse.PlannedSprint.builder()
                .sprintNumber(sprintNumber)
                .issues(issues.stream().map(i -> i.getId().toString()).collect(Collectors.toList()))
                .totalPoints(totalPoints)
                .startDate(startDate)
                .endDate(startDate.plusDays(durationDays))
                .build();
    }

    private List<String> findCriticalPathIds(List<Issue> issues) {
        return issues.stream()
                .filter(
                        i ->
                                i.getPriority().name().equals("HIGH")
                                        || i.getPriority().name().equals("CRITICAL"))
                .map(i -> i.getId().toString())
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<String> findCriticalPath(List<Issue> issues) {
        return issues.stream()
                .filter(i -> i.getPriority().name().equals("CRITICAL"))
                .map(Issue::getKey)
                .limit(5)
                .collect(Collectors.toList());
    }

    public static class SprintPlan {
        private UUID sprintId;
        private String sprintName;
        private List<String> issueKeys;

        public SprintPlan(UUID sprintId, String sprintName, List<String> issueKeys) {
            this.sprintId = sprintId;
            this.sprintName = sprintName;
            this.issueKeys = issueKeys;
        }

        public UUID getSprintId() {
            return sprintId;
        }

        public String getSprintName() {
            return sprintName;
        }

        public List<String> getIssueKeys() {
            return issueKeys;
        }
    }
}
