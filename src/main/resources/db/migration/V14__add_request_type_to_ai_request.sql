-- Add request_type column to ai_request table
ALTER TABLE ai_request
ADD COLUMN request_type VARCHAR(100) NOT NULL DEFAULT 'GENERAL';

-- Remove default after adding the column
ALTER TABLE ai_request
ALTER COLUMN request_type DROP DEFAULT;

-- Add comment for documentation
COMMENT ON COLUMN ai_request.request_type IS 'Type of AI request: SPRINT_PLANNING, ISSUE_ESTIMATION, etc.';
