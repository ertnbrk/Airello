package ai.planmate.outbox.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Outbox Event Entity for the Transactional Outbox Pattern.
 *
 * <p><b>PURPOSE:</b> Ensures data consistency between database writes and message publishing.
 *
 * <p><b>PATTERN FLOW:</b>
 *
 * <pre>
 * 1. Service creates Issue (DB Write)
 * 2. Service creates OutboxEvent (SAME Transaction)
 * 3. Transaction commits (or rolls back atomically)
 * 4. Background publisher polls unpublished events
 * 5. Publisher sends to message broker (if enabled)
 * 6. On success: marks event as published
 * 7. On failure: schedules retry with exponential backoff
 * </pre>
 *
 * <p><b>GUARANTEES:</b>
 *
 * <ul>
 *   <li>At-least-once delivery (events may be delivered multiple times)
 *   <li>No message loss (events survive crashes and restarts)
 *   <li>Eventual consistency (events will eventually be published)
 * </ul>
 *
 * <p><b>EXAMPLE:</b>
 *
 * <pre>
 * var event = OutboxEvent.builder()
 *     .aggregateType("Issue")
 *     .aggregateId(issue.getId())
 *     .eventType("IssueCreated")
 *     .payload(new IssueCreatedEvent(issue.getId(), issue.getTitle(), ...))
 *     .correlationId(MDC.get("correlationId"))
 *     .traceId(MDC.get("traceId"))
 *     .build();
 * outboxEventRepository.save(event);
 * </pre>
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Type of aggregate (entity) that generated this event.
     *
     * <p>Examples: "Issue", "Project", "ChatMessage", "BoardColumn"
     */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /**
     * ID of the aggregate instance that generated this event.
     *
     * <p>Example: For "IssueCreated" event, this is the Issue UUID
     */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /**
     * Type of event (past tense verb).
     *
     * <p>Examples: "IssueCreated", "IssueUpdated", "IssueMoved", "IssueDeleted"
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Event payload as JSONB.
     *
     * <p>Store the entire event data as JSON for flexibility. The consumer can deserialize to the
     * appropriate event class.
     *
     * <p><b>BEST PRACTICE:</b> Use Java Records for event payload classes.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Object payload;

    /** Whether this event has been successfully published to the message broker. */
    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = false;

    /** Timestamp when event was published to message broker. */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Number of times publishing was attempted (for exponential backoff). */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /** Maximum number of retry attempts before giving up. */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 5;

    /**
     * Timestamp for next retry attempt (NULL = ready now).
     *
     * <p>Calculated using exponential backoff:
     *
     * <pre>
     * Attempt 1: Immediate
     * Attempt 2: 10 seconds
     * Attempt 3: 30 seconds
     * Attempt 4: 90 seconds
     * Attempt 5: 270 seconds
     * </pre>
     */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    /** Error message from last failed publish attempt. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Correlation ID for tracing this event across distributed services.
     *
     * <p>Propagated through HTTP headers, message broker headers, and logs.
     */
    @Column(name = "correlation_id")
    private String correlationId;

    /** OpenTelemetry Trace ID for distributed tracing. */
    @Column(name = "trace_id")
    private String traceId;

    /** Timestamp when event was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp when event was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Marks event as published and records timestamp. */
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * Records publish failure and schedules retry with exponential backoff.
     *
     * @param error Error message from failed publish attempt
     */
    public void recordFailure(String error) {
        this.retryCount++;
        this.errorMessage = error;

        if (this.retryCount < this.maxRetries) {
            // Exponential backoff: 10s, 30s, 90s, 270s, 810s
            long delaySeconds = (long) Math.pow(3, this.retryCount) * 10;
            this.nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        } else {
            // Max retries exceeded - mark for manual intervention
            this.nextRetryAt = null;
        }
    }

    /**
     * Checks if this event is ready for publishing.
     *
     * @return true if not published and (no retry scheduled OR retry time has passed)
     */
    public boolean isReadyForPublishing() {
        if (published || retryCount >= maxRetries) {
            return false;
        }
        return nextRetryAt == null || nextRetryAt.isBefore(Instant.now());
    }
}
