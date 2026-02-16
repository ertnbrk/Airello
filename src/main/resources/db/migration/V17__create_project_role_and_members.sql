-- Add project_role enum
CREATE TYPE project_role AS ENUM ('ADMIN', 'MEMBER', 'VIEWER');

-- Project members (many-to-many with roles)
CREATE TABLE project_member (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role project_role NOT NULL DEFAULT 'MEMBER',
    added_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(project_id, user_id)
);

CREATE INDEX idx_project_member_project ON project_member(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_project_member_user ON project_member(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_project_member_role ON project_member(project_id, role) WHERE deleted_at IS NULL;

-- Automatically add project owner as ADMIN member when project is created
CREATE OR REPLACE FUNCTION add_project_owner_as_admin()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO project_member (project_id, user_id, role, added_by, joined_at)
    VALUES (NEW.id, NEW.owner_id, 'ADMIN'::project_role, NEW.owner_id, NOW());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_add_project_owner_as_admin
AFTER INSERT ON project
FOR EACH ROW
EXECUTE FUNCTION add_project_owner_as_admin();
