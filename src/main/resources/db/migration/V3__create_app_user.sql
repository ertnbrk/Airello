-- Application users table
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255), -- Nullable for OAuth-only users
    full_name VARCHAR(255) NOT NULL,
    keycloak_subject VARCHAR(255) UNIQUE, -- Keycloak user ID
    plan user_plan NOT NULL DEFAULT 'FREE',
    stripe_customer_id VARCHAR(255) UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE -- Soft delete
);

CREATE INDEX idx_app_user_email ON app_user(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_app_user_keycloak_subject ON app_user(keycloak_subject) WHERE deleted_at IS NULL;
CREATE INDEX idx_app_user_stripe_customer ON app_user(stripe_customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_app_user_active ON app_user(active) WHERE deleted_at IS NULL;
