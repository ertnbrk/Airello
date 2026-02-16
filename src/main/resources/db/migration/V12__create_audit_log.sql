-- Audit log for critical operations
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL, -- e.g., "USER_LOGIN", "PROJECT_CREATED", "ARTIFACT_UPLOADED"
    entity_type VARCHAR(100), -- e.g., "PROJECT", "ISSUE", "USER"
    entity_id UUID,
    workspace_id UUID REFERENCES workspace(id) ON DELETE SET NULL,
    project_id UUID REFERENCES project(id) ON DELETE SET NULL,
    ip_address INET,
    user_agent TEXT,
    metadata JSONB, -- Additional contextual data
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_workspace ON audit_log(workspace_id);
CREATE INDEX idx_audit_log_project ON audit_log(project_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);
