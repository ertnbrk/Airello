-- Artifacts table (files uploaded to S3)
CREATE TABLE artifact (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    upload_completed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_artifact_project ON artifact(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_artifact_uploaded_by ON artifact(uploaded_by) WHERE deleted_at IS NULL;
CREATE INDEX idx_artifact_created_at ON artifact(created_at DESC) WHERE deleted_at IS NULL;

-- Artifact chunks with embeddings (pgvector)
CREATE TABLE artifact_chunk (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    artifact_id UUID NOT NULL REFERENCES artifact(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536), -- OpenAI ada-002 dimensions
    metadata JSONB, -- Additional metadata like page number, section, etc.
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(artifact_id, chunk_index)
);

CREATE INDEX idx_artifact_chunk_artifact ON artifact_chunk(artifact_id);
-- HNSW index for fast similarity search
CREATE INDEX idx_artifact_chunk_embedding ON artifact_chunk USING hnsw (embedding vector_cosine_ops);
