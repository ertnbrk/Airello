-- ========================================
-- V24: ShedLock - Distributed Task Locking
-- ========================================
-- PURPOSE: Prevents duplicate execution of scheduled tasks in multi-instance deployments.
--
-- PROBLEM: Multiple instances running the same @Scheduled task
--   Instance 1: Polls outbox_events at 00:00:00
--   Instance 2: Polls outbox_events at 00:00:00
--   Result: Same events published twice (duplicate messages)
--
-- SOLUTION: ShedLock Database Locking
--   1. Instance 1 acquires lock "outbox-publisher" at 00:00:00
--   2. Instance 2 tries to acquire lock, fails, skips execution
--   3. Instance 1 processes events and releases lock
--   4. Next run: Instance 2 might win the race
--
-- GUARANTEES:
--   - Only ONE instance executes the scheduled task at a time
--   - Lock automatically expires (prevents deadlock if instance crashes)
--   - Lock is released immediately when task completes
--
-- ========================================

-- Create shedlock table (ShedLock provider for JDBC)
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,           -- Lock name (e.g., "outbox-publisher")
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,  -- Lock expiration time
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,   -- When lock was acquired
    locked_by VARCHAR(255) NOT NULL         -- Instance identifier (hostname)
);

-- ========================================
-- Indexes for Performance
-- ========================================

-- Query: Find expired locks (cleanup)
CREATE INDEX idx_shedlock_lock_until ON shedlock (lock_until);

-- ========================================
-- Comments for Documentation
-- ========================================
COMMENT ON TABLE shedlock IS 'ShedLock: Distributed lock table for scheduled tasks';
COMMENT ON COLUMN shedlock.name IS 'Unique lock name (identifies the scheduled task)';
COMMENT ON COLUMN shedlock.lock_until IS 'Lock expiration timestamp (auto-release if instance crashes)';
COMMENT ON COLUMN shedlock.locked_at IS 'Timestamp when lock was acquired';
COMMENT ON COLUMN shedlock.locked_by IS 'Hostname/instance ID that acquired the lock';
