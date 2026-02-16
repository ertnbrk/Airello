-- User plan types
CREATE TYPE user_plan AS ENUM ('FREE', 'PRO', 'ENTERPRISE');

-- Workspace roles
CREATE TYPE workspace_role AS ENUM ('OWNER', 'MANAGER', 'EDITOR', 'VIEWER', 'COMMENTER');

-- Issue types
CREATE TYPE issue_type AS ENUM ('STORY', 'TASK', 'BUG');

-- Issue status
CREATE TYPE issue_status AS ENUM ('BACKLOG', 'SELECTED', 'IN_PROGRESS', 'REVIEW', 'DONE');

-- Issue priority
CREATE TYPE issue_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

-- AI request status
CREATE TYPE ai_request_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'TIMEOUT');

-- Subscription event types
CREATE TYPE subscription_event_type AS ENUM (
    'SUBSCRIPTION_CREATED',
    'SUBSCRIPTION_UPDATED',
    'SUBSCRIPTION_CANCELED',
    'PAYMENT_SUCCEEDED',
    'PAYMENT_FAILED'
);
