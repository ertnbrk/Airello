package ai.planmate.agile.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.agile.dto.CreateIssueRequest;
import ai.planmate.agile.dto.UpdateIssueRequest;
import ai.planmate.agile.entity.Epic;
import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.entity.IssueStatus;
import ai.planmate.agile.repository.EpicRepository;
import ai.planmate.agile.repository.IssueRepository;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.outbox.entity.OutboxEvent;
import ai.planmate.outbox.events.IssueCreatedEvent;
import ai.planmate.outbox.events.IssueUpdatedEvent;
import ai.planmate.outbox.repository.OutboxEventRepository;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.shared.exception.BadRequestException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Issue Service with Transactional Outbox Pattern.
 *
 * <p><b>KEY ARCHITECTURAL CHANGE:</b> All state-changing operations (create, update) now follow the
 * Transactional Outbox Pattern to ensure data consistency between DB writes and event publishing.
 *
 * <p><b>PATTERN FLOW:</b>
 *
 * <pre>
 * 1. Start DB transaction
 * 2. Save Issue to DB
 * 3. Create OutboxEvent in SAME transaction
 * 4. Commit (or rollback atomically)
 * 5. Background publisher polls outbox_events
 * 6. Publisher sends events to RabbitMQ
 * </pre>
 *
 * <p><b>BENEFITS:</b>
 *
 * <ul>
 *   <li>No "dual write" problem (DB + RabbitMQ inconsistency)
 *   <li>At-least-once delivery guarantee
 *   <li>Events survive crashes and restarts
 *   <li>Automatic retry with exponential backoff
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final EpicRepository epicRepository;
    private final AppUserRepository appUserRepository;
    private final OutboxEventRepository outboxEventRepository;

    /**
     * Creates a new issue and publishes IssueCreatedEvent to outbox.
     *
     * <p><b>TRANSACTIONAL OUTBOX:</b> Both DB write and event creation happen in SAME transaction.
     *
     * @param request Issue creation request
     * @return Created issue
     */
    @Transactional
    public Issue createIssue(CreateIssueRequest request) {
        AppUser reporter = null;

        Project project =
                projectRepository
                        .findByIdAndNotDeleted(request.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        var issue = new Issue();
        issue.setProject(project);
        issue.setKey(generateIssueKey(project));
        issue.setType(request.getType());
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setPriority(request.getPriority());
        issue.setStoryPoints(request.getStoryPoints());
        issue.setReporter(reporter);
        issue.setOriginalEstimateHours(request.getOriginalEstimateHours());
        issue.setRemainingEstimateHours(request.getOriginalEstimateHours());

        if (request.getEpicId() != null) {
            Epic epic =
                    epicRepository
                            .findByIdAndNotDeleted(request.getEpicId())
                            .orElseThrow(() -> new ResourceNotFoundException("Epic not found"));
            issue.setEpic(epic);
        }

        if (request.getAssigneeId() != null) {
            AppUser assignee =
                    appUserRepository
                            .findById(request.getAssigneeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            issue.setAssignee(assignee);
        }

        if (request.getLabels() != null) {
            issue.setLabels(request.getLabels().toArray(new String[0]));
        }

        // STEP 1: Save issue to DB
        var savedIssue = issueRepository.save(issue);

        // STEP 2: Create outbox event in SAME transaction
        var event = IssueCreatedEvent.fromIssue(savedIssue);
        var outboxEvent =
                OutboxEvent.builder()
                        .aggregateType("Issue")
                        .aggregateId(savedIssue.getId())
                        .eventType("IssueCreated")
                        .payload(event)
                        .correlationId(MDC.get("correlationId"))
                        .traceId(MDC.get("traceId"))
                        .build();

        outboxEventRepository.save(outboxEvent);

        log.info(
                "✅ Issue created with Outbox pattern: key={ }, outboxEventId={ }",
                savedIssue.getKey(),
                outboxEvent.getId());

        return savedIssue;
    }

    /**
     * Updates an issue and publishes IssueUpdatedEvent to outbox.
     *
     * <p><b>TRANSACTIONAL OUTBOX:</b> Both DB write and event creation happen in SAME transaction.
     *
     * <p><b>CHANGE TRACKING:</b> Captures which fields changed for optimistic UI updates.
     *
     * @param issueId Issue UUID
     * @param request Update request
     * @return Updated issue
     */
    @Transactional
    public Issue updateIssue(UUID issueId, UpdateIssueRequest request) {
        var issue =
                issueRepository
                        .findByIdAndNotDeleted(issueId)
                        .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));

        // Track changed fields for delta updates
        Map<String, Object> changedFields = new HashMap<>();

        if (request.getTitle() != null && !request.getTitle().equals(issue.getTitle())) {
            changedFields.put("title", request.getTitle());
            issue.setTitle(request.getTitle());
        }
        if (request.getDescription() != null
                && !request.getDescription().equals(issue.getDescription())) {
            changedFields.put("description", request.getDescription());
            issue.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && request.getStatus() != issue.getStatus()) {
            validateStatusTransition(issue.getStatus(), request.getStatus());
            changedFields.put("status", request.getStatus());
            issue.setStatus(request.getStatus());
        }
        if (request.getPriority() != null && request.getPriority() != issue.getPriority()) {
            changedFields.put("priority", request.getPriority());
            issue.setPriority(request.getPriority());
        }
        if (request.getStoryPoints() != null
                && !request.getStoryPoints().equals(issue.getStoryPoints())) {
            changedFields.put("storyPoints", request.getStoryPoints());
            issue.setStoryPoints(request.getStoryPoints());
        }
        if (request.getAssigneeId() != null) {
            AppUser assignee =
                    appUserRepository
                            .findById(request.getAssigneeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            changedFields.put("assigneeId", assignee.getId());
            issue.setAssignee(assignee);
        }
        if (request.getLabels() != null) {
            changedFields.put("labels", request.getLabels());
            issue.setLabels(request.getLabels().toArray(new String[0]));
        }
        if (request.getRemainingEstimateHours() != null
                && !request.getRemainingEstimateHours().equals(issue.getRemainingEstimateHours())) {
            changedFields.put("remainingEstimateHours", request.getRemainingEstimateHours());
            issue.setRemainingEstimateHours(request.getRemainingEstimateHours());
        }
        if (request.getOrderIndex() != null
                && !request.getOrderIndex().equals(issue.getOrderIndex())) {
            changedFields.put("orderIndex", request.getOrderIndex());
            issue.setOrderIndex(request.getOrderIndex());
        }

        // STEP 1: Save issue to DB
        var updatedIssue = issueRepository.save(issue);

        // STEP 2: Create outbox event in SAME transaction (only if something changed)
        if (!changedFields.isEmpty()) {
            var event = IssueUpdatedEvent.fromIssue(updatedIssue, changedFields);
            var outboxEvent =
                    OutboxEvent.builder()
                            .aggregateType("Issue")
                            .aggregateId(updatedIssue.getId())
                            .eventType("IssueUpdated")
                            .payload(event)
                            .correlationId(MDC.get("correlationId"))
                            .traceId(MDC.get("traceId"))
                            .build();

            outboxEventRepository.save(outboxEvent);

            log.info(
                    "✅ Issue updated with Outbox pattern: key={ }, changedFields={ },"
                            + " outboxEventId={ }",
                    updatedIssue.getKey(),
                    changedFields.keySet(),
                    outboxEvent.getId());
        } else {
            log.debug("No fields changed for issue: key={ }", updatedIssue.getKey());
        }

        return updatedIssue;
    }

    @Transactional(readOnly = true)
    public List<Issue> getProjectIssues(UUID projectId, IssueStatus status) {
        if (status != null) {
            return issueRepository.findByProjectIdAndStatus(projectId, status);
        }
        return issueRepository.findByProjectId(projectId);
    }

    private String generateIssueKey(Project project) {
        long count = issueRepository.findByProjectId(project.getId()).size();
        return String.format("%s-%d", project.getKey(), count + 1);
    }

    private void validateStatusTransition(IssueStatus current, IssueStatus next) {
        if (current == IssueStatus.DONE && next != IssueStatus.DONE) {
            throw new BadRequestException("Cannot move issue out of DONE status");
        }
    }
}
