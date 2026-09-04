-- PolicyRules.checkSubscription previously had no way to distinguish "offers a subscription"
-- (subscription_available, fine to buy one-time too) from "subscription is the ONLY way to buy
-- this" (should block a buyer who explicitly asked for a one-time purchase). It silently always
-- passed. This column makes that distinction real.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS subscription_only BOOLEAN NOT NULL DEFAULT false;
