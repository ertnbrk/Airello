-- ========================================
-- V23: Transactional Outbox Pattern
-- ========================================
-- PURPOSE: Ensures data consistency between database writes and message publishing.
--
-- PROBLEM: "Dual Write" Issue
--   1. Service saves Issue to DB (SUCCESS)
--   2. Service publishes event to RabbitMQ (FAILURE - network issue)
--   Result: Database updated but no event published = Inconsistent state
--
-- SOLUTION: Transactional Outbox Pattern
--   1. Service saves Issue AND Event to DB in SAME transaction
--   2. Background publisher polls outbox_events table
--   3. Publisher sends events to RabbitMQ
--   4. On success, marks event as published
--   5. On failure, retries with exponential backoff
--
-- GUARANTEES:
--   - At-least-once delivery (events may be published multiple times)
--   - Eventual consistency (events will eventually be published)
--   - No message loss (survives crashes and restarts)
--
-- ========================================

-- Create outbox_events table
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Event metadata
    aggregate_type VARCHAR(100) NOT NULL,  -- e.g., "Issue", "Project", "ChatMessage"
    aggregate_id UUID NOT NULL,             -- The ID of the entity that changed
    event_type VARCHAR(100) NOT NULL,       -- e.g., "IssueCreated", "IssueUpdated", "IssueMoved"

    -- Event payload (JSONB for efficient querying and indexing)
    payload JSONB NOT NULL,                 -- The actual event data

    -- Publishing state
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE,

    -- Retry mechanism
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP WITH TIME ZONE,

    -- Error tracking
    error_message TEXT,

    -- Correlation (for distributed tracing)
    correlation_id VARCHAR(255),
    trace_id VARCHAR(255),

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- Indexes for Performance
-- ========================================

-- IMPORTANT: PostgreSQL index predicates MUST use IMMUTABLE functions only.
-- Time-based logic (CURRENT_TIMESTAMP, NOW()) belongs in SELECT queries, NOT in indexes.
-- See worker query example at end of file.

-- Primary polling query: Find unpublished events
-- Note: Time comparison (next_retry_at <= NOW()) moved to SELECT query for correctness
CREATE INDEX idx_outbox_events_polling ON outbox_events (published, created_at)
WHERE published = FALSE;

-- Index for retry scheduling: Find events with scheduled retry times
CREATE INDEX idx_outbox_events_retry_schedule ON outbox_events (next_retry_at, created_at)
WHERE published = FALSE AND next_retry_at IS NOT NULL;

-- Query by aggregate (e.g., "find all events for Issue X")
CREATE INDEX idx_outbox_events_aggregate ON outbox_events (aggregate_type, aggregate_id, created_at);

-- Query by event type (analytics, monitoring)
CREATE INDEX idx_outbox_events_type ON outbox_events (event_type, created_at);

-- Cleanup query: Find old published events for archival
CREATE INDEX idx_outbox_events_cleanup ON outbox_events (published, created_at)
WHERE published = TRUE;

-- Distributed tracing lookup
CREATE INDEX idx_outbox_events_correlation ON outbox_events (correlation_id)
WHERE correlation_id IS NOT NULL;

-- ========================================
-- Trigger: Auto-update updated_at timestamp
-- ========================================
CREATE OR REPLACE FUNCTION update_outbox_events_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_outbox_events_updated_at
    BEFORE UPDATE ON outbox_events
    FOR EACH ROW
    EXECUTE FUNCTION update_outbox_events_updated_at();

-- ========================================
-- Comments for Documentation
-- ========================================
COMMENT ON TABLE outbox_events IS 'Transactional Outbox Pattern: Stores domain events for reliable message publishing';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Entity type (e.g., Issue, Project, ChatMessage)';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the entity that changed';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of event (e.g., IssueCreated, IssueUpdated)';
COMMENT ON COLUMN outbox_events.payload IS 'Event data as JSONB (includes all relevant fields)';
COMMENT ON COLUMN outbox_events.published IS 'Whether event has been successfully published to message broker';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of publish attempts (for exponential backoff)';
COMMENT ON COLUMN outbox_events.next_retry_at IS 'Timestamp for next retry attempt (NULL = ready now)';
COMMENT ON COLUMN outbox_events.correlation_id IS 'Correlation ID for tracing request across services';
COMMENT ON COLUMN outbox_events.trace_id IS 'OpenTelemetry Trace ID for distributed tracing';

-- ========================================
-- WORKER QUERY PATTERN (Reference)
-- ========================================
-- RULE: Time-based logic (NOW(), CURRENT_TIMESTAMP) MUST live in SELECT queries.
--       NEVER put time-based functions in index predicates (they are NOT IMMUTABLE).
--
-- CORRECT Worker Query Pattern (used by OutboxPublisherService.java):
--
-- SELECT *
-- FROM outbox_events
-- WHERE published = FALSE
--   AND retry_count < max_retries
--   AND (next_retry_at IS NULL OR next_retry_at <= NOW())  -- ✅ Time logic in SELECT
-- ORDER BY created_at ASC
-- LIMIT 100
-- FOR UPDATE SKIP LOCKED;
--
-- WHY FOR UPDATE SKIP LOCKED?
--   - FOR UPDATE: Locks rows for processing (prevents race conditions)
--   - SKIP LOCKED: Allows multiple workers to process different events concurrently
--   - PostgreSQL will automatically skip rows locked by other transactions
--
-- PERFORMANCE NOTES:
--   - Uses idx_outbox_events_polling for "published = FALSE"
--   - Uses idx_outbox_events_retry_schedule for "next_retry_at IS NOT NULL" filtering
--   - Time comparison (next_retry_at <= NOW()) is evaluated at query execution time
--   - LIMIT prevents overwhelming worker with too many events
--
-- ========================================
