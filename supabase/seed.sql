-- Mock Data Seed Script

-- We assume a test buyer and test merchant are created via Supabase Auth
-- For now, we will just insert mock IDs to keep referential integrity simple for testing if needed,
-- or we can just rely on Spring Boot tests creating them.

INSERT INTO profiles (id, role, name, city)
VALUES ('00000000-0000-0000-0000-000000000001', 'MERCHANT', 'NutriBasket Store', 'Bangalore'),
       ('00000000-0000-0000-0000-000000000002', 'BUYER', 'Test Buyer', 'Mumbai') ON CONFLICT DO NOTHING;

INSERT INTO merchants (id, owner_id, name, description)
VALUES ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'NutriBasket',
        'Healthy wellness snacks') ON CONFLICT DO NOTHING;

INSERT INTO products (merchant_id, name, description, price_paise, stock_quantity, dietary_tags, allergens,
                      delivery_days, returnable, subscription_available)
VALUES ('10000000-0000-0000-0000-000000000001', 'Vegan Trail Mix', 'Healthy vegan snack with no peanuts', 84000, 50,
        ARRAY['vegan', 'gluten-free'], ARRAY[]::text[], 0, true, false),
       ('10000000-0000-0000-0000-000000000001', 'Keto Peanut Bars', 'High protein keto bars', 120000, 20, ARRAY['keto'],
        ARRAY['peanuts'], 1, false, true),
       ('10000000-0000-0000-0000-000000000001', 'Organic Fruit Bites', 'Sweet and organic', 50000, 100, ARRAY['organic',
        'vegan'], ARRAY[]::text[], 2, true, false);
