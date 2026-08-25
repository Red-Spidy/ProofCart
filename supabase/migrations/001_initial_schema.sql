-- ProofCart Initial Schema

-- Enable UUID extension
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- Profiles (extends Supabase Auth users)
CREATE TABLE profiles
(
    id         UUID PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    role       VARCHAR(20)  NOT NULL CHECK (role IN ('BUYER', 'MERCHANT')),
    name       VARCHAR(255) NOT NULL,
    city       VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Merchants
CREATE TABLE merchants
(
    id          UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    owner_id    UUID         NOT NULL REFERENCES profiles (id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Products
CREATE TABLE products
(
    id                     UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    merchant_id            UUID         NOT NULL REFERENCES merchants (id),
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    price_paise            INTEGER      NOT NULL,
    stock_quantity         INTEGER      NOT NULL    DEFAULT 0,
    dietary_tags           TEXT[] DEFAULT '{}',
    allergens              TEXT[] DEFAULT '{}',
    delivery_days          INTEGER      NOT NULL    DEFAULT 0,
    returnable             BOOLEAN      NOT NULL    DEFAULT false,
    subscription_available BOOLEAN      NOT NULL    DEFAULT false,
    version                INTEGER      NOT NULL    DEFAULT 1,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Intent Contracts
CREATE TABLE intent_contracts
(
    id              UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    buyer_id        UUID                     NOT NULL REFERENCES profiles (id),
    raw_prompt      TEXT                     NOT NULL,
    extracted_rules JSONB                    NOT NULL,
    confidence      NUMERIC(3, 2)            NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Proof Carts
CREATE TABLE proof_carts
(
    id                 UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    buyer_id           UUID        NOT NULL REFERENCES profiles (id),
    merchant_id        UUID        NOT NULL REFERENCES merchants (id),
    intent_contract_id UUID REFERENCES intent_contracts (id),
    total_paise        INTEGER     NOT NULL,
    offer_hash         VARCHAR(64) NOT NULL,
    snapshot_data      JSONB       NOT NULL,
    policy_decision    VARCHAR(30) NOT NULL CHECK (policy_decision IN ('ALLOWED', 'REAPPROVAL_REQUIRED', 'BLOCKED')),
    policy_checks      JSONB       NOT NULL,
    approved           BOOLEAN     NOT NULL     DEFAULT false,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Checkout Orders
CREATE TABLE checkout_orders
(
    id                  UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    buyer_id            UUID         NOT NULL REFERENCES profiles (id),
    cart_id             UUID         NOT NULL REFERENCES proof_carts (id),
    merchant_id         UUID         NOT NULL REFERENCES merchants (id),
    razorpay_order_id   VARCHAR(255) NOT NULL,
    razorpay_payment_id VARCHAR(255),
    amount_paise        INTEGER      NOT NULL,
    status              VARCHAR(50)  NOT NULL    DEFAULT 'CREATED',
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit Events
CREATE TABLE audit_events
(
    id          UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    buyer_id    UUID REFERENCES profiles (id),
    merchant_id UUID REFERENCES merchants (id),
    cart_id     UUID REFERENCES proof_carts (id),
    order_id    UUID REFERENCES checkout_orders (id),
    event_type  VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    metadata    JSONB                    DEFAULT '{}',
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Webhook Events (Idempotency)
CREATE TABLE webhook_events
(
    id           VARCHAR(255) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Agent Tokens (MCP)
CREATE TABLE agent_tokens
(
    id         UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    buyer_id   UUID         NOT NULL REFERENCES profiles (id),
    name       VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    revoked    BOOLEAN      NOT NULL    DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE intent_contracts ENABLE ROW LEVEL SECURITY;
ALTER TABLE proof_carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE checkout_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_tokens ENABLE ROW LEVEL SECURITY;

-- For Phase 1/2 Spring Boot Backend testing, we will use a Service Role key which bypasses RLS.
-- RLS policies will be strictly applied in the Angular Frontend phase if using Supabase client directly,
-- but since Spring Boot is the authority, it will manage data access rules internally.
