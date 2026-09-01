-- Cross-merchant market orders.

-- One Razorpay payment collected once and settled across several merchants' proof carts that
-- share a single intent contract (e.g. "birthday dinner: cake + flowers, combined under one
-- budget"). settlement_json is the per-merchant breakdown of that one collection.
CREATE TABLE IF NOT EXISTS market_orders
(
    id                 UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    buyer_id           UUID         NOT NULL REFERENCES profiles (id),
    intent_contract_id UUID         NOT NULL REFERENCES intent_contracts (id),
    razorpay_order_id  VARCHAR(255) NOT NULL UNIQUE,
    total_paise        INTEGER      NOT NULL,
    status             VARCHAR(50)  NOT NULL DEFAULT 'CREATED',
    settlement_json    JSONB        NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS market_orders_buyer_idx ON market_orders (buyer_id, created_at DESC);

ALTER TABLE market_orders ENABLE ROW LEVEL SECURITY;
