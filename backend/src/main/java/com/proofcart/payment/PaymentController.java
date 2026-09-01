package com.proofcart.payment;

import com.proofcart.audit.AuditEventService;
import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.domain.repo.MarketOrderRepository;
import com.proofcart.inventory.InventoryReservationService;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final CheckoutOrderRepository orderRepo;
    private final MarketOrderRepository marketOrderRepo;
    private final String keySecret;
    private final InventoryReservationService inventory;
    private final AuditEventService audit;

    public PaymentController(CheckoutOrderRepository orderRepo, MarketOrderRepository marketOrderRepo, InventoryReservationService inventory,
                             @Value("${razorpay.key.secret:}") String keySecret, AuditEventService audit) {
        this.orderRepo = orderRepo;
        this.marketOrderRepo = marketOrderRepo;
        this.inventory = inventory;
        this.keySecret = keySecret;
        this.audit = audit;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        try {
            String razorpayOrderId = request.get("razorpay_order_id");
            String razorpayPaymentId = request.get("razorpay_payment_id");
            String razorpaySignature = request.get("razorpay_signature");

            // One Razorpay order collects one merchant's sub-order in the common case, or
            // several sub-orders at once for a multi-merchant market order sharing one payment.
            List<CheckoutOrderEntity> orders = orderRepo.findByRazorpayOrderId(razorpayOrderId);
            if (orders.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order not found"));
            }

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            if (keySecret == null || keySecret.isBlank()) {
                return ResponseEntity.status(503).body(
                        Map.of("error", "Payment verification is not configured. Set RAZORPAY_KEY_SECRET in environment."));
            }

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                for (CheckoutOrderEntity order : orders) {
                    inventory.capture(order.getId());
                    order.setRazorpayPaymentId(razorpayPaymentId);
                    order.setStatus("PAID");
                    orderRepo.save(order);
                    audit.record(order.getBuyerId(), order.getMerchantId(), order.getCartId(), order.getId(), "PAYMENT_VERIFIED", "Razorpay payment signature verified.");
                }
                marketOrderRepo.findByRazorpayOrderId(razorpayOrderId).ifPresent(mo -> {
                    mo.setStatus("PAID");
                    marketOrderRepo.save(mo);
                });
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                for (CheckoutOrderEntity order : orders) {
                    inventory.release(order.getId(), InventoryReservationService.RELEASED);
                    order.setStatus("FAILED_VERIFICATION");
                    orderRepo.save(order);
                    audit.record(order.getBuyerId(), order.getMerchantId(), order.getCartId(), order.getId(), "PAYMENT_REJECTED", "Razorpay payment signature rejected.");
                }
                marketOrderRepo.findByRazorpayOrderId(razorpayOrderId).ifPresent(mo -> {
                    mo.setStatus("FAILED_VERIFICATION");
                    marketOrderRepo.save(mo);
                });
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid payment signature. Payment rejected."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
