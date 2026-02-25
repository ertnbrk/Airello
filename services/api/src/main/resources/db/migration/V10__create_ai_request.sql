-- AI request tracking
CREATE TABLE ai_request (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    correlation_id VARCHAR(100) NOT NULL UNIQUE,
    requested_by UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    status ai_request_status NOT NULL DEFAULT 'PENDING',
    request_payload JSONB, -- Original request data
    response_payload JSONB, -- AI response data
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_request_project ON ai_request(project_id);
CREATE INDEX idx_ai_request_correlation ON ai_request(correlation_id);
CREATE INDEX idx_ai_request_status ON ai_request(status);
CREATE INDEX idx_ai_request_requested_by ON ai_request(requested_by);
CREATE INDEX idx_ai_request_started_at ON ai_request(started_at DESC);
