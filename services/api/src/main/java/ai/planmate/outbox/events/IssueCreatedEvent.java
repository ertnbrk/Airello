package ai.planmate.outbox.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event payload for Issue Created event.
 *
 * <p><b>ARCHITECTURE PRINCIPLE:</b> Event payloads MUST NOT depend on domain entities.
 *
 * <p>Events use primitive types (String, UUID, etc.) for loose coupling.
 *
 * <p><b>WHY STRING INSTEAD OF ENUM?</b>
 *
 * <ul>
 *   <li>✅ Events are serialized to JSONB and sent across boundaries
 *   <li>✅ Domain enum changes don't break event consumers
 *   <li>✅ Events can be consumed by non-Java services
 *   <li>✅ Bounded context separation (DDD principle)
 * </ul>
 *
 * <p><b>USAGE:</b> Stored in outbox_events.payload as JSONB.
 *
 * <p><b>IMMUTABILITY:</b> Java Records are immutable by default (perfect for events).
 *
 * <p><b>SERIALIZATION:</b> Jackson automatically handles Java 8 Date/Time types.
 *
 * @param issueId The UUID of the created issue
 * @param projectId The UUID of the project containing the issue
 * @param key The issue key (e.g., "PROJ-123")
 * @param title The issue title
 * @param type The issue type as String (e.g., "STORY", "TASK", "BUG")
 * @param status The status as String (e.g., "BACKLOG", "IN_PROGRESS")
 * @param priority The priority as String (e.g., "HIGH", "MEDIUM", "LOW")
 * @param description Optional description
 * @param storyPoints Optional story points
 * @param assigneeId Optional assignee UUID
 * @param reporterId Optional reporter UUID
 * @param epicId Optional epic UUID
 * @param labels Optional list of labels
 * @param orderIndex The fractional order index for sorting
 * @param createdAt Timestamp when issue was created
 */
public record IssueCreatedEvent(
        @JsonProperty("issueId") UUID issueId,
        @JsonProperty("projectId") UUID projectId,
        @JsonProperty("key") String key,
        @JsonProperty("title") String title,
        @JsonProperty("type") String type,
        @JsonProperty("status") String status,
        @JsonProperty("priority") String priority,
        @JsonProperty("description") String description,
        @JsonProperty("storyPoints") Integer storyPoints,
        @JsonProperty("assigneeId") UUID assigneeId,
        @JsonProperty("reporterId") UUID reporterId,
        @JsonProperty("epicId") UUID epicId,
        @JsonProperty("labels") List<String> labels,
        @JsonProperty("orderIndex") BigDecimal orderIndex,
        @JsonProperty("createdAt") Instant createdAt) {

    /**
     * Creates an IssueCreatedEvent from an Issue entity.
     *
     * <p><b>MAPPING STRATEGY:</b> Enum → String conversion happens here (domain layer).
     *
     * @param issue The issue entity
     * @return Event payload with primitive types
     */
    public static IssueCreatedEvent fromIssue(ai.planmate.agile.entity.Issue issue) {
        return new IssueCreatedEvent(
                issue.getId(),
                issue.getProject().getId(),
                issue.getKey(),
                issue.getTitle(),
                issue.getType() != null ? issue.getType().name() : null,
                issue.getStatus() != null ? issue.getStatus().name() : null,
                issue.getPriority() != null ? issue.getPriority().name() : null,
                issue.getDescription(),
                issue.getStoryPoints(),
                issue.getAssignee() != null ? issue.getAssignee().getId() : null,
                issue.getReporter() != null ? issue.getReporter().getId() : null,
                issue.getEpic() != null ? issue.getEpic().getId() : null,
                issue.getLabels() != null ? List.of(issue.getLabels()) : List.of(),
                issue.getOrderIndex(),
                issue.getCreatedAt());
    }
}
