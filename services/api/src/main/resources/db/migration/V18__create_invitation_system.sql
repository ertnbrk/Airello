-- Add invitation_type enum
CREATE TYPE invitation_type AS ENUM ('EMAIL', 'LINK');

-- Invitation table for project invitations
CREATE TABLE invitation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    type invitation_type NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(255),
    role project_role NOT NULL DEFAULT 'MEMBER',
    invited_by UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    accepted_at TIMESTAMP WITH TIME ZONE,
    accepted_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    max_uses INTEGER NOT NULL DEFAULT 1,
    current_uses INTEGER NOT NULL DEFAULT 0,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_email_required_for_email_type CHECK (
        type = 'LINK' OR (type = 'EMAIL' AND email IS NOT NULL)
    ),
    CONSTRAINT chk_max_uses_positive CHECK (max_uses > 0),
    CONSTRAINT chk_current_uses_non_negative CHECK (current_uses >= 0)
);

CREATE INDEX idx_invitation_project ON invitation(project_id);
CREATE INDEX idx_invitation_token ON invitation(token) WHERE revoked_at IS NULL;
CREATE INDEX idx_invitation_email ON invitation(email) WHERE email IS NOT NULL AND revoked_at IS NULL;
CREATE INDEX idx_invitation_expires_at ON invitation(expires_at) WHERE expires_at IS NOT NULL AND revoked_at IS NULL;
