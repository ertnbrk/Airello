-- Workspace table for organizing projects
CREATE TABLE workspace (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_workspace_owner ON workspace(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_workspace_slug ON workspace(slug) WHERE deleted_at IS NULL;

-- Workspace members (many-to-many with roles)
CREATE TABLE workspace_member (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role workspace_role NOT NULL DEFAULT 'VIEWER',
    invited_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(workspace_id, user_id)
);

CREATE INDEX idx_workspace_member_workspace ON workspace_member(workspace_id);
CREATE INDEX idx_workspace_member_user ON workspace_member(user_id);
CREATE INDEX idx_workspace_member_role ON workspace_member(workspace_id, role);
