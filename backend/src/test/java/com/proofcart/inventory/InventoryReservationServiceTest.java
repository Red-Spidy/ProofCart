package com.proofcart.inventory;

import com.proofcart.domain.CartItem;
import com.proofcart.domain.ProductSnapshot;
import com.proofcart.domain.entity.ProductEntity;
import com.proofcart.domain.repo.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"supabase.url=", "spring.task.scheduling.enabled=false"})
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class InventoryReservationServiceTest {
    @Autowired
    private InventoryReservationService inventory;
    @Autowired
    private ProductRepository products;

    @Test
    void reservationHoldsStockThenReleaseMakesItAvailableAgain() {
        ProductEntity product = products.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")).orElseThrow();
        int originalReserved = product.getReservedQuantity();
        CartItem item = new CartItem(product.getId().toString(), 1, product.getPricePaise(), product.getPricePaise(),
                new ProductSnapshot(product.getId().toString(), product.getMerchantId().toString(), product.getName(),
                        product.getDescription(), product.getPricePaise(), product.getStockQuantity(), product.getDietaryTags(),
                        product.getAllergens(), product.getDeliveryDays(), product.getReturnable(), product.getSubscriptionAvailable(),
                        product.getVersion(), null, null));
        UUID orderId = UUID.randomUUID();

        inventory.reserve(orderId, List.of(item));
        assertEquals(originalReserved + 1, products.findById(product.getId()).orElseThrow().getReservedQuantity());

        inventory.release(orderId, InventoryReservationService.RELEASED);
        assertEquals(originalReserved, products.findById(product.getId()).orElseThrow().getReservedQuantity());
    }
}
