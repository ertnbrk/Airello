-- ========================================
-- V25: AI Semantic Cache with pgvector
-- ========================================
-- PURPOSE: Reduce AI costs by caching similar prompts using vector embeddings.
--
-- PROBLEM: Every AI request costs money ($$$)
--   User 1: "Plan an e-commerce project" → OpenAI API ($0.002)
--   User 2: "Create an ecommerce website plan" → OpenAI API ($0.002)
--   Result: $0.004 for essentially the same request!
--
-- SOLUTION: Semantic Caching with Embeddings
--   1. Generate embedding for prompt (OpenAI text-embedding-3-small: $0.0001)
--   2. Query cache for similar embeddings (Cosine Similarity > 0.95)
--   3. If HIT: Return cached response (Cost: $0.0001 = 95% savings!)
--   4. If MISS: Call GPT-4, store response + embedding
--
-- COST ANALYSIS (1000 similar requests):
--   Without cache: 1000 × $0.002 = $2.00
--   With cache: 1 × $0.002 + 999 × $0.0001 = $0.10 (95% savings!)
--
-- ========================================

-- Create ai_semantic_cache table
CREATE TABLE IF NOT EXISTS ai_semantic_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Cache key components
    prompt TEXT NOT NULL,                   -- Original prompt text
    context TEXT,                           -- Optional context (e.g., project requirements)
    model VARCHAR(100) NOT NULL,            -- AI model used (e.g., "gpt-4o-mini")
    request_type VARCHAR(100) NOT NULL,     -- Type of request (e.g., "SPRINT_PLANNING")

    -- Vector embedding (1536 dimensions for OpenAI text-embedding-3-small)
    prompt_embedding vector(1536) NOT NULL, -- pgvector column

    -- Cached response
    response_payload JSONB NOT NULL,        -- The AI response (stored as JSON)
    response_text TEXT,                     -- Plain text response (for quick access)

    -- Metadata
    hit_count INTEGER NOT NULL DEFAULT 0,   -- Number of times this cache entry was used
    last_hit_at TIMESTAMP WITH TIME ZONE,   -- Last time cache was hit

    -- Expiration (TTL)
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- Cache expiration time

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- Indexes for Performance
-- ========================================

-- Vector similarity search using HNSW (Hierarchical Navigable Small World)
-- This is the MOST IMPORTANT index for semantic caching!
CREATE INDEX idx_ai_semantic_cache_vector_hnsw ON ai_semantic_cache
USING hnsw (prompt_embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Alternative: IVFFlat index (faster build, slower search)
-- Uncomment if HNSW is too slow to build:
-- CREATE INDEX idx_ai_semantic_cache_vector_ivfflat ON ai_semantic_cache
-- USING ivfflat (prompt_embedding vector_cosine_ops)
-- WITH (lists = 100);

-- Query by request type (analytics, debugging)
CREATE INDEX idx_ai_semantic_cache_request_type ON ai_semantic_cache (request_type, created_at);

-- IMPORTANT: PostgreSQL index predicates MUST use IMMUTABLE functions only.
-- Time-based logic (CURRENT_TIMESTAMP, NOW()) belongs in SELECT queries, NOT in indexes.

-- Cleanup query: Find expired cache entries
-- Note: Time comparison (expires_at <= NOW()) moved to SELECT query for correctness
CREATE INDEX idx_ai_semantic_cache_expiration ON ai_semantic_cache (expires_at);

-- Performance monitoring: Find most frequently used cache entries
CREATE INDEX idx_ai_semantic_cache_hit_count ON ai_semantic_cache (hit_count DESC)
WHERE hit_count > 0;

-- ========================================
-- Trigger: Auto-update updated_at timestamp
-- ========================================
CREATE OR REPLACE FUNCTION update_ai_semantic_cache_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_ai_semantic_cache_updated_at
    BEFORE UPDATE ON ai_semantic_cache
    FOR EACH ROW
    EXECUTE FUNCTION update_ai_semantic_cache_updated_at();

-- ========================================
-- Helper Function: Find Similar Prompts
-- ========================================
-- This function makes vector similarity queries easier.
--
-- USAGE:
-- SELECT * FROM find_similar_ai_prompts(
--     prompt_embedding := '[0.1, 0.2, ...]'::vector,
--     similarity_threshold := 0.95,
--     max_results := 5
-- );
--
CREATE OR REPLACE FUNCTION find_similar_ai_prompts(
    prompt_embedding vector(1536),
    similarity_threshold FLOAT DEFAULT 0.95,
    max_results INTEGER DEFAULT 5
)
RETURNS TABLE (
    id UUID,
    prompt TEXT,
    response_payload JSONB,
    cosine_similarity FLOAT,
    hit_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.prompt,
        c.response_payload,
        1 - (c.prompt_embedding <=> prompt_embedding) AS cosine_similarity,
        c.hit_count,
        c.created_at
    FROM ai_semantic_cache c
    WHERE c.expires_at > CURRENT_TIMESTAMP
    AND (1 - (c.prompt_embedding <=> prompt_embedding)) >= similarity_threshold
    ORDER BY c.prompt_embedding <=> prompt_embedding -- Cosine distance (lower = more similar)
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- Comments for Documentation
-- ========================================
COMMENT ON TABLE ai_semantic_cache IS 'Semantic cache for AI responses using pgvector embeddings';
COMMENT ON COLUMN ai_semantic_cache.prompt IS 'Original prompt text';
COMMENT ON COLUMN ai_semantic_cache.prompt_embedding IS 'Vector embedding of prompt (1536 dims, OpenAI text-embedding-3-small)';
COMMENT ON COLUMN ai_semantic_cache.response_payload IS 'Cached AI response as JSONB';
COMMENT ON COLUMN ai_semantic_cache.hit_count IS 'Number of times this cache entry was reused';
COMMENT ON COLUMN ai_semantic_cache.expires_at IS 'Cache expiration timestamp (default: 7 days)';

COMMENT ON INDEX idx_ai_semantic_cache_vector_hnsw IS 'HNSW index for fast vector similarity search (cosine distance)';

COMMENT ON FUNCTION find_similar_ai_prompts IS 'Find similar prompts using vector similarity search';

-- ========================================
-- Example Query: Vector Similarity Search
-- ========================================
-- Find the top 5 most similar prompts to a given embedding:
--
-- SELECT
--     prompt,
--     1 - (prompt_embedding <=> '[0.1, 0.2, ...]'::vector) AS similarity,
--     hit_count
-- FROM ai_semantic_cache
-- WHERE expires_at > CURRENT_TIMESTAMP
-- ORDER BY prompt_embedding <=> '[0.1, 0.2, ...]'::vector
-- LIMIT 5;
--
-- Operator explanation:
--   <=> = Cosine distance (0 = identical, 2 = opposite)
--   1 - <=> = Cosine similarity (1 = identical, -1 = opposite)
