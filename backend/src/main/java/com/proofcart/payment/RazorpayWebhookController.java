package com.proofcart.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.entity.CheckoutOrderEntity;
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
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public RazorpayWebhookController(CheckoutOrderRepository orderRepo,
                                     ObjectMapper objectMapper,
                                     @Value("${razorpay.webhook.secret:}") String webhookSecret) {
        this.orderRepo = orderRepo;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload,
                                           @RequestHeader(value = "x-razorpay-signature", required = false) String signature) {
        try {
            // Reject if webhook secret is not configured
            if (webhookSecret == null || webhookSecret.isBlank() ||
                    "your_razorpay_webhook_secret_here".equals(webhookSecret)) {
                return ResponseEntity.status(503).body("Webhook secret not configured.");
            }

            if (signature == null || signature.isBlank()) {
                return ResponseEntity.badRequest().body("Missing x-razorpay-signature header.");
            }

            // Verify HMAC signature
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                return ResponseEntity.badRequest().body("Invalid webhook signature.");
            }

            // Parse the event
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();

            if ("payment.captured".equals(event)) {
                String razorpayOrderId = root.path("payload").path("payment").path("entity").path("order_id").asText();
                String razorpayPaymentId = root.path("payload").path("payment").path("entity").path("id").asText();

                CheckoutOrderEntity order = orderRepo.findByRazorpayOrderId(razorpayOrderId);
                if (order != null) {
                    // Idempotency: do not double-process
                    if (!"PAID".equals(order.getStatus())) {
                        order.setRazorpayPaymentId(razorpayPaymentId);
                        order.setStatus("PAID");
                        orderRepo.save(order);
                        System.out.println("[webhook] Marked order " + razorpayOrderId + " as PAID via webhook.");
                    }
                }
            } else if ("payment.failed".equals(event)) {
                String razorpayOrderId = root.path("payload").path("payment").path("entity").path("order_id").asText();
                CheckoutOrderEntity order = orderRepo.findByRazorpayOrderId(razorpayOrderId);
                if (order != null && !"PAID".equals(order.getStatus())) {
                    order.setStatus("FAILED");
                    orderRepo.save(order);
                    System.out.println("[webhook] Marked order " + razorpayOrderId + " as FAILED via webhook.");
                }
            } else {
                System.out.println("[webhook] Unhandled event type: " + event);
            }

            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (Exception e) {
            System.err.println("[webhook] Error processing webhook: " + e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
