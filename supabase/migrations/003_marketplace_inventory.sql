-- Marketplace accounts, seller-owned catalogues, and safe checkout stock holds.

-- Every new Supabase identity gets a marketplace profile. The role comes from the
-- signup metadata and is constrained again here, so only BUYER or MERCHANT persists.
CREATE
OR REPLACE FUNCTION public.create_profile_for_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
BEGIN
INSERT INTO public.profiles (id, role, name)
VALUES (NEW.id,
        CASE
            WHEN upper(coalesce(NEW.raw_user_meta_data ->> 'marketplace_role', 'BUYER')) = 'MERCHANT'
                THEN 'MERCHANT'
            ELSE 'BUYER' END,
        coalesce(nullif(trim(NEW.raw_user_meta_data ->> 'display_name'), ''), 'Shopper')) ON CONFLICT (id) DO NOTHING;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS create_profile_on_auth_user ON auth.users;
CREATE TRIGGER create_profile_on_auth_user
    AFTER INSERT
    ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.create_profile_for_new_user();

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT products_reserved_quantity_check CHECK (reserved_quantity >= 0),
    ADD CONSTRAINT products_stock_covers_reservations_check CHECK (stock_quantity >= reserved_quantity);

CREATE TABLE IF NOT EXISTS inventory_reservations
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    uuid_generate_v4
(
),
    order_id UUID NOT NULL REFERENCES checkout_orders
(
    id
) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products
(
    id
),
    quantity INTEGER NOT NULL CHECK
(
    quantity >
    0
),
    status VARCHAR
(
    20
) NOT NULL CHECK
(
    status
    IN
(
    'RESERVED',
    'CAPTURED',
    'RELEASED',
    'EXPIRED'
)),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP
  WITH TIME ZONE NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS inventory_reservations_order_status_idx
    ON inventory_reservations (order_id, status);
CREATE INDEX IF NOT EXISTS inventory_reservations_expiry_idx
    ON inventory_reservations (status, expires_at);

ALTER TABLE inventory_reservations ENABLE ROW LEVEL SECURITY;
