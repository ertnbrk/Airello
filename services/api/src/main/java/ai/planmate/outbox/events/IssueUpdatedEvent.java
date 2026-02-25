package ai.planmate.outbox.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event payload for Issue Updated event.
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
 *   <li>✅ Events can be consumed by non-Java services (Python, Node.js, etc.)
 *   <li>✅ Bounded context separation (DDD principle)
 * </ul>
 *
 * <p><b>USAGE:</b> Captures which fields changed (for optimistic UI updates).
 *
 * @param issueId The UUID of the updated issue
 * @param projectId The UUID of the project containing the issue
 * @param key The issue key (e.g., "PROJ-123")
 * @param changedFields Map of field names to new values (for delta updates)
 * @param title Current title
 * @param type Current type as String (e.g., "STORY", "TASK", "BUG")
 * @param status Current status as String (e.g., "BACKLOG", "IN_PROGRESS")
 * @param priority Current priority as String (e.g., "HIGH", "MEDIUM", "LOW")
 * @param description Current description
 * @param storyPoints Current story points
 * @param assigneeId Current assignee UUID
 * @param epicId Current epic UUID
 * @param labels Current labels
 * @param orderIndex Current order index
 * @param updatedAt Timestamp when issue was updated
 */
public record IssueUpdatedEvent(
        @JsonProperty("issueId") UUID issueId,
        @JsonProperty("projectId") UUID projectId,
        @JsonProperty("key") String key,
        @JsonProperty("changedFields") Map<String, Object> changedFields,
        @JsonProperty("title") String title,
        @JsonProperty("type") String type,
        @JsonProperty("status") String status,
        @JsonProperty("priority") String priority,
        @JsonProperty("description") String description,
        @JsonProperty("storyPoints") Integer storyPoints,
        @JsonProperty("assigneeId") UUID assigneeId,
        @JsonProperty("epicId") UUID epicId,
        @JsonProperty("labels") List<String> labels,
        @JsonProperty("orderIndex") BigDecimal orderIndex,
        @JsonProperty("updatedAt") Instant updatedAt) {

    /**
     * Creates an IssueUpdatedEvent from an Issue entity.
     *
     * <p><b>MAPPING STRATEGY:</b> Enum → String conversion happens here (domain layer).
     *
     * @param issue The issue entity
     * @param changedFields Map of changed fields
     * @return Event payload with primitive types
     */
    public static IssueUpdatedEvent fromIssue(
            ai.planmate.agile.entity.Issue issue, Map<String, Object> changedFields) {
        return new IssueUpdatedEvent(
                issue.getId(),
                issue.getProject().getId(),
                issue.getKey(),
                changedFields,
                issue.getTitle(),
                issue.getType() != null ? issue.getType().name() : null,
                issue.getStatus() != null ? issue.getStatus().name() : null,
                issue.getPriority() != null ? issue.getPriority().name() : null,
                issue.getDescription(),
                issue.getStoryPoints(),
                issue.getAssignee() != null ? issue.getAssignee().getId() : null,
                issue.getEpic() != null ? issue.getEpic().getId() : null,
                issue.getLabels() != null ? List.of(issue.getLabels()) : List.of(),
                issue.getOrderIndex(),
                issue.getUpdatedAt());
    }
}
