INSERT INTO profiles (id, role, name, city)
VALUES ('00000000-0000-0000-0000-000000000001', 'MERCHANT', 'NutriBasket Store', 'Bangalore')
ON CONFLICT DO NOTHING;

INSERT INTO merchants (id, owner_id, name, description)
VALUES ('10000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'NutriBasket',
        'Healthy wellness snacks')
ON CONFLICT DO NOTHING;

INSERT INTO products (id, merchant_id, name, description, price_paise, stock_quantity,
                      dietary_tags, allergens, delivery_days, returnable,
                      subscription_available, subscription_only, version)
VALUES
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
     'Vegan Trail Mix',
     'A premium blend of nuts, seeds & dried fruits. Peanut-free and gluten-free.',
     84000, 100, ARRAY['vegan', 'gluten-free'], ARRAY[]::text[], 0, true, true, false, 1),

    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
     'Keto Protein Bars',
     'High protein, low carb bars with peanut butter and dark chocolate.',
     120000, 50, ARRAY['keto'], ARRAY['peanuts'], 1, false, true, false, 1),

    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
     'Organic Fruit Bites',
     'Sweet organic fruit snacks. No added sugar or artificial colors.',
     50000, 200, ARRAY['organic', 'vegan'], ARRAY[]::text[], 2, true, false, false, 1),

    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001',
     'Chia Seed Pudding Mix',
     'Omega-3 rich superfood. Just add milk and refrigerate overnight.',
     35000, 150, ARRAY['vegan', 'gluten-free'], ARRAY[]::text[], 0, true, true, false, 1),

    ('00000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001',
     'Matcha Green Tea Powder',
     'Premium Japanese matcha. Rich in antioxidants and L-Theanine.',
     75000, 75, ARRAY['vegan', 'organic'], ARRAY[]::text[], 1, false, true, true, 1)
ON CONFLICT DO NOTHING;
