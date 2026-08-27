package com.proofcart.payment;

import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.inventory.InventoryReservationService;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final CheckoutOrderRepository orderRepo;
    private final String keySecret;
    private final InventoryReservationService inventory;

    public PaymentController(CheckoutOrderRepository orderRepo, InventoryReservationService inventory,
                             @Value("${razorpay.key.secret:}") String keySecret) {
        this.orderRepo = orderRepo;
        this.inventory = inventory;
        this.keySecret = keySecret;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        try {
            String razorpayOrderId = request.get("razorpay_order_id");
            String razorpayPaymentId = request.get("razorpay_payment_id");
            String razorpaySignature = request.get("razorpay_signature");

            CheckoutOrderEntity order = orderRepo.findByRazorpayOrderId(razorpayOrderId);
            if (order == null) {
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
                inventory.capture(order.getId());
                order.setRazorpayPaymentId(razorpayPaymentId);
                order.setStatus("PAID");
                orderRepo.save(order);
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                inventory.release(order.getId(), InventoryReservationService.RELEASED);
                order.setStatus("FAILED_VERIFICATION");
                orderRepo.save(order);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid payment signature. Payment rejected."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
