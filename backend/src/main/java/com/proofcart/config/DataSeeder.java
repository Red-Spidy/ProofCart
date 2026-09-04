package com.proofcart.config;

import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public DataSeeder(ProductRepository productRepository, JdbcTemplate jdbcTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER NOT NULL DEFAULT 0;");
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS subscription_only BOOLEAN NOT NULL DEFAULT false;");
            jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS inventory_reservations (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            order_id UUID NOT NULL,
                            product_id UUID NOT NULL,
                            quantity INTEGER NOT NULL,
                            status VARCHAR(20) NOT NULL,
                            expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
                        );
                    """);
        } catch (Exception e) {
            System.err.println("Schema check/migration notice: " + e.getMessage());
        }

        // Idempotent, runs every boot (not gated by productRepository.count() == 0 below) so it
        // also lands on an already-seeded live database, not just a fresh one.
        try {
            jdbcTemplate.update("UPDATE products SET subscription_only = true WHERE name = 'Matcha Green Tea Powder'");
        } catch (Exception e) {
            System.err.println("subscription_only backfill notice: " + e.getMessage());
        }

        try {
            Set<String> keys = redisTemplate.keys("orderHistory*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }

        System.out.println("Checking database product count: " + productRepository.count());
        if (productRepository.count() == 0) {
            UUID merchantId = UUID.fromString("10000000-0000-0000-0000-000000000001");

            ProductEntity p1 = new ProductEntity();
            p1.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            p1.setMerchantId(merchantId);
            p1.setName("Vegan Trail Mix");
            p1.setDescription("A premium blend of nuts, seeds & dried fruits. Peanut-free and gluten-free.");
            p1.setPricePaise(84000);
            p1.setStockQuantity(100);
            p1.setDietaryTags(List.of("vegan", "gluten-free"));
            p1.setAllergens(List.of());
            p1.setDeliveryDays(0);
            p1.setReturnable(true);
            p1.setSubscriptionAvailable(true);
            p1.setVersion(1);

            ProductEntity p2 = new ProductEntity();
            p2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
            p2.setMerchantId(merchantId);
            p2.setName("Keto Protein Bars");
            p2.setDescription("High protein, low carb bars with peanut butter and dark chocolate.");
            p2.setPricePaise(120000);
            p2.setStockQuantity(50);
            p2.setDietaryTags(List.of("keto"));
            p2.setAllergens(List.of("peanuts"));
            p2.setDeliveryDays(1);
            p2.setReturnable(false);
            p2.setSubscriptionAvailable(true);
            p2.setVersion(1);

            ProductEntity p3 = new ProductEntity();
            p3.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
            p3.setMerchantId(merchantId);
            p3.setName("Organic Fruit Bites");
            p3.setDescription("Sweet organic fruit snacks. No added sugar or artificial colors.");
            p3.setPricePaise(50000);
            p3.setStockQuantity(200);
            p3.setDietaryTags(List.of("organic", "vegan"));
            p3.setAllergens(List.of());
            p3.setDeliveryDays(2);
            p3.setReturnable(true);
            p3.setSubscriptionAvailable(false);
            p3.setVersion(1);

            ProductEntity p4 = new ProductEntity();
            p4.setId(UUID.fromString("00000000-0000-0000-0000-000000000004"));
            p4.setMerchantId(merchantId);
            p4.setName("Chia Seed Pudding Mix");
            p4.setDescription("Omega-3 rich superfood. Just add milk and refrigerate overnight.");
            p4.setPricePaise(35000);
            p4.setStockQuantity(150);
            p4.setDietaryTags(List.of("vegan", "gluten-free"));
            p4.setAllergens(List.of());
            p4.setDeliveryDays(0);
            p4.setReturnable(true);
            p4.setSubscriptionAvailable(true);
            p4.setVersion(1);

            ProductEntity p5 = new ProductEntity();
            p5.setId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
            p5.setMerchantId(merchantId);
            p5.setName("Matcha Green Tea Powder");
            p5.setDescription("Premium Japanese matcha. Rich in antioxidants and L-Theanine.");
            p5.setPricePaise(75000);
            p5.setStockQuantity(75);
            p5.setDietaryTags(List.of("vegan", "organic"));
            p5.setAllergens(List.of());
            p5.setDeliveryDays(1);
            p5.setReturnable(false);
            p5.setSubscriptionAvailable(true);
            p5.setSubscriptionOnly(true);
            p5.setVersion(1);

            productRepository.saveAll(List.of(p1, p2, p3, p4, p5));
            System.out.println("Successfully seeded products into Supabase PostgreSQL! Count: " + productRepository.count());
        }
    }
}
