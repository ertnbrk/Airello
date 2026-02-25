-- Add index for workspace member queries
CREATE INDEX idx_workspace_member_workspace_role
    ON workspace_member(workspace_id, role);

-- Add function to check if user is workspace member
CREATE OR REPLACE FUNCTION is_workspace_member(p_workspace_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
RETURN EXISTS (
    SELECT 1
    FROM workspace_member wm
             JOIN app_user au ON wm.user_id = au.id
    WHERE wm.workspace_id = p_workspace_id
      AND wm.user_id = p_user_id
      AND au.deleted_at IS NULL
);
END;
$$ LANGUAGE plpgsql;

-- Add function to check if user can access project
CREATE OR REPLACE FUNCTION can_access_project(p_project_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
RETURN EXISTS (
    SELECT 1
    FROM project p
             JOIN workspace_member wm ON p.workspace_id = wm.workspace_id
             JOIN app_user au ON wm.user_id = au.id
    WHERE p.id = p_project_id
      AND wm.user_id = p_user_id
      AND p.deleted_at IS NULL
      AND au.deleted_at IS NULL
);
END;
$$ LANGUAGE plpgsql;
