-- ============================================================
-- V22: Fix Schema Validation Issues for Hibernate
--
-- Resolves conflicts between V13 and V21 regarding order_index
-- Ensures all entity-database type alignments are correct
-- ============================================================

-- 1. Ensure order_index is NUMERIC(15,2) for fractional ordering
--    V13 created it as NUMERIC(15,2), V21 tried to create as INTEGER
--    This ensures the correct type regardless of which migration ran
DO $$
DECLARE
    current_type TEXT;
BEGIN
    -- Get current data type
    SELECT data_type INTO current_type
    FROM information_schema.columns
    WHERE table_name = 'issue'
    AND column_name = 'order_index';

    IF current_type IS NULL THEN
        -- Column doesn't exist, create it
        ALTER TABLE issue
        ADD COLUMN order_index NUMERIC(15, 2) NOT NULL DEFAULT 1000;

        CREATE INDEX idx_issue_order_index
        ON issue(project_id, order_index)
        WHERE deleted_at IS NULL;

        RAISE NOTICE 'Created issue.order_index as NUMERIC(15,2)';

    ELSIF current_type = 'integer' THEN
        -- Wrong type, convert to NUMERIC
        ALTER TABLE issue
        ALTER COLUMN order_index TYPE NUMERIC(15, 2);

        RAISE NOTICE 'Converted issue.order_index from INTEGER to NUMERIC(15,2)';
    ELSIF current_type = 'numeric' THEN
        -- Correct type, verify precision/scale
        ALTER TABLE issue
        ALTER COLUMN order_index TYPE NUMERIC(15, 2);

        RAISE NOTICE 'Verified issue.order_index is NUMERIC(15,2)';
    END IF;

    -- Ensure DEFAULT is set to 1000 (matches Issue entity default)
    ALTER TABLE issue
    ALTER COLUMN order_index SET DEFAULT 1000;

    -- Ensure NOT NULL constraint
    ALTER TABLE issue
    ALTER COLUMN order_index SET NOT NULL;
END
$$;

-- 2. Verify index exists with correct predicate
CREATE INDEX IF NOT EXISTS idx_issue_order_index
ON issue(project_id, order_index)
WHERE deleted_at IS NULL;

-- 3. Update app_user.plan comment to include DEMO value
COMMENT ON COLUMN app_user.plan IS
    'User subscription plan: DEMO (anonymous trial), FREE, PRO, or ENTERPRISE';

-- 4. Add documentation comments for clarity
COMMENT ON COLUMN issue.order_index IS
    'Fractional order index for drag-and-drop positioning within board columns. '
    'Uses NUMERIC(15,2) to enable arbitrary precision reordering without gaps. '
    'Default 1000 allows prepending (0-999) and appending (1001+).';

COMMENT ON COLUMN issue.board_column_id IS
    'Dynamic board column assignment. Newer approach replacing fixed status enum. '
    'Allows projects to define custom workflow columns.';

COMMENT ON COLUMN issue.parent_issue_id IS
    'Parent issue for subtask relationships. NULL for top-level issues.';

-- 5. Verify reporter_id is nullable (for anonymous demo users)
--    V20 already dropped NOT NULL, this ensures idempotency
DO $$
BEGIN
    ALTER TABLE issue ALTER COLUMN reporter_id DROP NOT NULL;
EXCEPTION
    WHEN OTHERS THEN
        -- Constraint might already be dropped, ignore
        NULL;
END
$$;
