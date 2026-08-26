-- Private, account-level signals used to personalise catalog recommendations.
CREATE TABLE personalization_events
(
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id    UUID NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    product_id  UUID REFERENCES products (id) ON DELETE CASCADE,
    event_type  VARCHAR(20) NOT NULL CHECK (event_type IN ('SEARCH', 'LIKE', 'DISLIKE')),
    search_term VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX personalization_events_buyer_created_idx
    ON personalization_events (buyer_id, created_at DESC);

ALTER TABLE personalization_events ENABLE ROW LEVEL SECURITY;
