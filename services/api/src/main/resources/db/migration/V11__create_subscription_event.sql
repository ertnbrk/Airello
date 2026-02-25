-- Subscription events from Stripe webhooks
CREATE TABLE subscription_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    event_type subscription_event_type NOT NULL,
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    stripe_subscription_id VARCHAR(255),
    plan user_plan,
    amount_cents INTEGER,
    currency VARCHAR(10),
    event_data JSONB NOT NULL, -- Full webhook payload
    processed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_event_user ON subscription_event(user_id);
CREATE INDEX idx_subscription_event_type ON subscription_event(event_type);
CREATE INDEX idx_subscription_event_stripe_event ON subscription_event(stripe_event_id);
CREATE INDEX idx_subscription_event_processed ON subscription_event(processed);
CREATE INDEX idx_subscription_event_created_at ON subscription_event(created_at DESC);
