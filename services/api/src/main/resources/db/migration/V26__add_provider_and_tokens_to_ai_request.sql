-- ========================================
-- V26: Add provider and tokens_used to ai_request
-- ========================================
-- PURPOSE: Track which LLM provider handled the request and token usage

ALTER TABLE ai_request
ADD COLUMN IF NOT EXISTS provider VARCHAR(50),
ADD COLUMN IF NOT EXISTS tokens_used INTEGER;

COMMENT ON COLUMN ai_request.provider IS 'LLM provider that processed the request (openai, ollama, mock)';
COMMENT ON COLUMN ai_request.tokens_used IS 'Number of tokens consumed by the request';
