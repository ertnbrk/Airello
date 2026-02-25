-- ============================================================
-- V27: Enhance board_column with soft delete and Jira-like semantics
-- Adds: soft delete, category, WIP limit, audit fields, constraints
-- ============================================================

-- 1. Add new columns to board_column
ALTER TABLE board_column ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE board_column ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT 'CUSTOM';
ALTER TABLE board_column ADD COLUMN IF NOT EXISTS wip_limit INTEGER;
ALTER TABLE board_column ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE board_column ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES app_user(id) ON DELETE SET NULL;

-- 2. Add constraint for WIP limit (must be >= 1 if not null)
ALTER TABLE board_column ADD CONSTRAINT chk_board_column_wip_limit CHECK (wip_limit IS NULL OR wip_limit >= 1);

-- 3. Update name column max length from 100 to 60
ALTER TABLE board_column ALTER COLUMN name TYPE VARCHAR(60);

-- 4. Backfill category for existing rows based on name patterns
UPDATE board_column
SET category = CASE
    WHEN LOWER(name) LIKE '%backlog%' THEN 'BACKLOG'
    WHEN LOWER(name) LIKE '%done%' THEN 'DONE'
    WHEN LOWER(name) LIKE '%progress%' THEN 'IN_PROGRESS'
    WHEN LOWER(name) LIKE '%to do%' OR LOWER(name) LIKE '%todo%' THEN 'TODO'
    ELSE 'CUSTOM'
END
WHERE category = 'CUSTOM';

-- Special case: Set first (default) column to BACKLOG if currently CUSTOM
UPDATE board_column
SET category = 'BACKLOG'
WHERE is_default = true AND category = 'CUSTOM';

-- 5. Drop old indexes that don't account for soft delete
DROP INDEX IF EXISTS idx_board_column_position;

-- 6. Create partial unique index: (project_id, position) WHERE deleted_at IS NULL
-- This ensures position uniqueness only among non-deleted columns
CREATE UNIQUE INDEX IF NOT EXISTS uq_board_column_project_position
ON board_column(project_id, position)
WHERE deleted_at IS NULL;

-- 7. Create partial unique index: (project_id) WHERE is_default = true AND deleted_at IS NULL
-- This ensures at most one default column per project among non-deleted columns
CREATE UNIQUE INDEX IF NOT EXISTS uq_board_column_project_default
ON board_column(project_id)
WHERE is_default = true AND deleted_at IS NULL;

-- 8. Create composite index for fast board loading
-- Optimizes queries like: SELECT * FROM board_column WHERE project_id = ? AND deleted_at IS NULL ORDER BY position
CREATE INDEX IF NOT EXISTS idx_board_column_project_deleted_position
ON board_column(project_id, deleted_at, position);

-- 9. Create index on deleted_at for soft delete queries
CREATE INDEX IF NOT EXISTS idx_board_column_deleted_at
ON board_column(deleted_at)
WHERE deleted_at IS NOT NULL;

-- 10. Create index on category for analytics/filtering
CREATE INDEX IF NOT EXISTS idx_board_column_category
ON board_column(category)
WHERE deleted_at IS NULL;

-- 11. Create indexes for audit tracking
CREATE INDEX IF NOT EXISTS idx_board_column_created_by
ON board_column(created_by)
WHERE created_by IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_board_column_updated_by
ON board_column(updated_by)
WHERE updated_by IS NOT NULL;

-- 12. Add comments for documentation
COMMENT ON COLUMN board_column.deleted_at IS 'Soft delete timestamp. NULL = active, NOT NULL = deleted';
COMMENT ON COLUMN board_column.category IS 'Column semantic category: BACKLOG, TODO, IN_PROGRESS, DONE, or CUSTOM';
COMMENT ON COLUMN board_column.wip_limit IS 'Work-in-progress limit. NULL = unlimited, otherwise must be >= 1';
COMMENT ON COLUMN board_column.created_by IS 'User ID who created this column';
COMMENT ON COLUMN board_column.updated_by IS 'User ID who last updated this column';

COMMENT ON INDEX uq_board_column_project_position IS 'Ensures unique position per project among non-deleted columns';
COMMENT ON INDEX uq_board_column_project_default IS 'Ensures at most one default column per project among non-deleted columns';
