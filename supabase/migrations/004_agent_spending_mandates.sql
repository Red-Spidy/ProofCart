-- Agent spending mandates.

-- Spending mandate on an agent token: bounds on what a delegated AI agent may commit without a
-- fresh buyer-issued token, mirroring NPCI UAP / UPI Autopay-style delegated payment mandates.
ALTER TABLE agent_tokens
    ADD COLUMN IF NOT EXISTS max_per_transaction_paise INTEGER,
    ADD COLUMN IF NOT EXISTS max_daily_paise INTEGER,
    ADD COLUMN IF NOT EXISTS allowed_merchant_ids TEXT[] DEFAULT '{}',
    ADD CONSTRAINT agent_tokens_max_per_transaction_check CHECK (max_per_transaction_paise IS NULL OR max_per_transaction_paise > 0),
    ADD CONSTRAINT agent_tokens_max_daily_check CHECK (max_daily_paise IS NULL OR max_daily_paise > 0);

-- Which agent token (if any) authorized a checkout order, so the daily mandate can be computed
-- as a rolling sum of that token's own committed orders.
ALTER TABLE checkout_orders
    ADD COLUMN IF NOT EXISTS agent_token_id UUID REFERENCES agent_tokens (id);
