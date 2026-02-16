-- Issues table (stories, tasks, bugs)
CREATE TABLE issue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    epic_id UUID REFERENCES epic(id) ON DELETE SET NULL,
    key VARCHAR(50) NOT NULL, -- e.g., "PLAN-123"
    type issue_type NOT NULL DEFAULT 'TASK',
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status issue_status NOT NULL DEFAULT 'BACKLOG',
    priority issue_priority NOT NULL DEFAULT 'MEDIUM',
    story_points INTEGER,
    assignee_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    reporter_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    labels TEXT[], -- Array of labels
    original_estimate_hours DECIMAL(10, 2),
    remaining_estimate_hours DECIMAL(10, 2),
    time_spent_hours DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(project_id, key)
);

CREATE INDEX idx_issue_project ON issue(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issue_epic ON issue(epic_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issue_status ON issue(project_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_issue_assignee ON issue(assignee_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issue_reporter ON issue(reporter_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issue_type ON issue(project_id, type) WHERE deleted_at IS NULL;
CREATE INDEX idx_issue_priority ON issue(project_id, priority) WHERE deleted_at IS NULL;
CREATE INDEX idx_issue_labels ON issue USING GIN(labels) WHERE deleted_at IS NULL;

-- Issue dependencies (for tracking blocked relationships)
CREATE TABLE issue_dependency (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
    blocks_issue_id UUID NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(issue_id, blocks_issue_id),
    CHECK(issue_id != blocks_issue_id)
);

CREATE INDEX idx_issue_dependency_issue ON issue_dependency(issue_id);
CREATE INDEX idx_issue_dependency_blocks ON issue_dependency(blocks_issue_id);
