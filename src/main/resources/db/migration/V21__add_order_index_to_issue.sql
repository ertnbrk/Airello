ALTER TABLE issue
    ADD COLUMN IF NOT EXISTS order_index INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_issue_order_index
    ON issue(project_id, order_index)
    WHERE deleted_at IS NULL;
