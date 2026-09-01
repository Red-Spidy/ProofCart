-- Verifiable, tamper-evident audit trust chain.

-- Every audit event belonging to a cart is linked to the previous event for that cart and
-- HMAC-signed at write time, so the trail can be independently re-verified later (see
-- AuditEventService.verifyChain) instead of only ever being displayed as a log line.
ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS chain_sequence INTEGER,
    ADD COLUMN IF NOT EXISTS prev_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS audit_events_cart_chain_idx
    ON audit_events (cart_id, chain_sequence);
