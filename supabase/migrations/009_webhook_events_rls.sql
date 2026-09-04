-- webhook_events was the one application table without an explicit ENABLE ROW LEVEL SECURITY
-- in the migrations. It is already ON in the live database, so this is a no-op there — but a
-- database built from these migrations alone would have come up with the table unprotected,
-- readable and writable by anyone holding the public anon key.
--
-- That matters more here than for most tables: this table is the idempotency ledger for
-- Razorpay webhooks. An attacker who could insert an event id ahead of time would cause the
-- real webhook carrying that id to be treated as an already-processed duplicate and dropped,
-- suppressing a genuine payment notification.
--
-- No policies are added deliberately: only the backend touches this table, and it connects
-- with credentials that bypass RLS. Enabled-with-no-policy is deny-all for the anon key.

ALTER TABLE webhook_events ENABLE ROW LEVEL SECURITY;
