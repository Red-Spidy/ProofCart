package com.proofcart.payment;

import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.razorpay.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class RazorpayWebhookController {

    private final CheckoutOrderRepository orderRepo;
    private final String webhookSecret;

    public RazorpayWebhookController(CheckoutOrderRepository orderRepo, @Value("${razorpay.webhook.secret:}") String webhookSecret) {
        this.orderRepo = orderRepo;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload, @RequestHeader("x-razorpay-signature") String signature) {
        try {
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
                if (!isValid) {
                    return ResponseEntity.badRequest().body("Invalid webhook signature");
                }
            }

            // In a real implementation, you would parse the JSON payload, check if it's already processed,
            // and update the order state based on events like `payment.captured` or `payment.failed`.
            System.out.println("Received valid webhook: " + payload);

            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
