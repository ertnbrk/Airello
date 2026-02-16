-- Epics table
CREATE TABLE epic (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    key VARCHAR(50) NOT NULL, -- e.g., "PLAN-E1"
    priority issue_priority NOT NULL DEFAULT 'MEDIUM',
    status issue_status NOT NULL DEFAULT 'BACKLOG',
    created_by UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(project_id, key)
);

CREATE INDEX idx_epic_project ON epic(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_epic_status ON epic(project_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_epic_priority ON epic(project_id, priority) WHERE deleted_at IS NULL;
CREATE INDEX idx_epic_created_by ON epic(created_by) WHERE deleted_at IS NULL;
