package ai.planmate.outbox.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.planmate.outbox.entity.OutboxEvent;

/**
 * Repository for Outbox Events (Transactional Outbox Pattern).
 *
 * <p><b>PRIMARY USE CASE:</b> Polling publisher to fetch unpublished events.
 *
 * <p><b>PERFORMANCE:</b> All queries use indexes defined in V23 migration.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find unpublished events ready for processing.
     *
     * <p><b>QUERY LOGIC:</b>
     *
     * <ul>
     *   <li>NOT published
     *   <li>Retry count < max retries
     *   <li>No retry scheduled OR retry time has passed
     * </ul>
     *
     * <p><b>PERFORMANCE:</b> Uses index: idx_outbox_events_polling
     *
     * <p><b>LIMIT:</b> Fetches up to maxResults events per poll (default: 100)
     *
     * @param now Current timestamp
     * @return List of events ready for publishing (ordered by created_at ASC)
     */
    @Query(
            """
        SELECT e FROM OutboxEvent e
        WHERE e.published = false
        AND e.retryCount < e.maxRetries
        AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
        ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findUnpublishedEvents(@Param("now") Instant now);

    /**
     * Find events by aggregate (for debugging, analytics).
     *
     * <p><b>EXAMPLE:</b> Find all events for Issue with ID "123"
     *
     * <p><b>PERFORMANCE:</b> Uses index: idx_outbox_events_aggregate
     *
     * @param aggregateType Type of aggregate (e.g., "Issue")
     * @param aggregateId ID of aggregate instance
     * @return List of events for this aggregate
     */
    @Query(
            """
        SELECT e FROM OutboxEvent e
        WHERE e.aggregateType = :aggregateType
        AND e.aggregateId = :aggregateId
        ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findByAggregate(
            @Param("aggregateType") String aggregateType, @Param("aggregateId") UUID aggregateId);

    /**
     * Find events by type (for analytics, monitoring).
     *
     * <p><b>EXAMPLE:</b> Count all "IssueCreated" events in last 24 hours
     *
     * <p><b>PERFORMANCE:</b> Uses index: idx_outbox_events_type
     *
     * @param eventType Type of event (e.g., "IssueCreated")
     * @return List of events of this type
     */
    List<OutboxEvent> findByEventType(String eventType);

    /**
     * Find old published events for cleanup/archival.
     *
     * <p><b>CLEANUP STRATEGY:</b> Delete published events older than 30 days.
     *
     * <p><b>PERFORMANCE:</b> Uses index: idx_outbox_events_cleanup
     *
     * @param threshold Timestamp threshold (e.g., now - 30 days)
     * @return List of old published events
     */
    @Query(
            """
        SELECT e FROM OutboxEvent e
        WHERE e.published = true
        AND e.createdAt < :threshold
        ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findOldPublishedEvents(@Param("threshold") Instant threshold);

    /**
     * Count unpublished events (for monitoring).
     *
     * @return Number of unpublished events
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.published = false")
    long countUnpublished();

    /**
     * Count failed events (retry count >= max retries).
     *
     * @return Number of permanently failed events (requires manual intervention)
     */
    @Query(
            "SELECT COUNT(e) FROM OutboxEvent e WHERE e.published = false AND e.retryCount >="
                    + " e.maxRetries")
    long countFailed();

    /**
     * Bulk delete old published events (for cleanup job).
     *
     * <p><b>CLEANUP STRATEGY:</b> Run daily to delete events older than 30 days.
     *
     * @param threshold Timestamp threshold
     * @return Number of deleted events
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.published = true AND e.createdAt < :threshold")
    int deleteOldPublishedEvents(@Param("threshold") Instant threshold);
}
