-- Add authentication provider support for local and OAuth2 login

-- Add provider column as VARCHAR (defaults to LOCAL for existing users)
ALTER TABLE app_user
ADD COLUMN provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL';

-- Add provider_id column for OAuth2 provider user IDs (Google user ID, etc.)
ALTER TABLE app_user
ADD COLUMN provider_id VARCHAR(255) UNIQUE;

-- Create index for provider_id lookups
CREATE INDEX idx_app_user_provider_id ON app_user(provider_id) WHERE deleted_at IS NULL;

-- Create composite index for provider + email for faster queries
CREATE INDEX idx_app_user_provider_email ON app_user(provider, email) WHERE deleted_at IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN app_user.provider IS 'Authentication provider: LOCAL (email/password) or GOOGLE (OAuth2)';
COMMENT ON COLUMN app_user.provider_id IS 'External provider user ID (e.g., Google user ID)';
