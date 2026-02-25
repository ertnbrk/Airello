-- Projects table
CREATE TABLE project (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    key VARCHAR(20) NOT NULL, -- Project key like "PLAN"
    description TEXT,
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    default_velocity INTEGER NOT NULL DEFAULT 35, -- Story points per sprint
    sprint_length_days INTEGER NOT NULL DEFAULT 14,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(workspace_id, key)
);

CREATE INDEX idx_project_workspace ON project(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_project_owner ON project(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_project_key ON project(workspace_id, key) WHERE deleted_at IS NULL;
