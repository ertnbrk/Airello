-- ============================================================
-- V20: MVP Schema Additions
-- Adds: user_type, ai_usage, board_column, chat, diagram,
--        subtask support, board_column_id on issue
-- ============================================================

-- 1. Add user_type to app_user (REGISTERED / ANONYMOUS)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) NOT NULL DEFAULT 'REGISTERED';
CREATE INDEX IF NOT EXISTS idx_app_user_user_type ON app_user(user_type);

-- 2. Add DEMO to plan values (plan is already VARCHAR from V16)
-- No schema change needed, just allow 'DEMO' as a value.

-- 3. AI Usage tracking
CREATE TABLE IF NOT EXISTS ai_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    calls_used INTEGER NOT NULL DEFAULT 0,
    tokens_used INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, usage_date)
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_user_date ON ai_usage(user_id, usage_date);

-- 4. Board columns (dynamic, per-project)
CREATE TABLE IF NOT EXISTS board_column (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_board_column_project ON board_column(project_id);
CREATE INDEX IF NOT EXISTS idx_board_column_position ON board_column(project_id, position);

-- 5. Add board_column_id to issue (nullable, will coexist with status enum)
ALTER TABLE issue ADD COLUMN IF NOT EXISTS board_column_id UUID REFERENCES board_column(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_issue_board_column ON issue(board_column_id) WHERE deleted_at IS NULL;

-- 6. Add version column to issue for optimistic locking
ALTER TABLE issue ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

-- 7. Add parent_issue_id for subtasks
ALTER TABLE issue ADD COLUMN IF NOT EXISTS parent_issue_id UUID REFERENCES issue(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_issue_parent ON issue(parent_issue_id) WHERE deleted_at IS NULL;

-- 8. Chat threads
CREATE TABLE IF NOT EXISTS chat_thread (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    created_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    title VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_chat_thread_project ON chat_thread(project_id) WHERE deleted_at IS NULL;

-- 9. Chat messages
CREATE TABLE IF NOT EXISTS chat_message (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id UUID NOT NULL REFERENCES chat_thread(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    sender_type VARCHAR(20) NOT NULL DEFAULT 'USER', -- USER, AI, SYSTEM
    content TEXT NOT NULL,
    tool_calls JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_chat_message_thread ON chat_message(thread_id, created_at);

-- 10. Diagrams
CREATE TABLE IF NOT EXISTS diagram (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- use_case, er, system, flowchart, sequence
    format VARCHAR(20) NOT NULL DEFAULT 'mermaid', -- mermaid, plantuml
    title VARCHAR(255),
    content TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_diagram_project ON diagram(project_id);

-- 11. Update project_role enum to add OWNER if not present
-- The existing enum has ADMIN, MEMBER, VIEWER. We'll use ADMIN as OWNER equivalent.
-- No schema change needed; we map ADMIN -> OWNER in Java.

-- 12. Allow nullable owner_id on project for anonymous/demo creation
-- Already nullable in practice (existing code sets null sometimes)

-- 13. Allow nullable reporter_id on issue for anonymous/demo creation
ALTER TABLE issue ALTER COLUMN reporter_id DROP NOT NULL;
