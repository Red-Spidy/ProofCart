package com.proofcart.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.audit.AuditEventService;
import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.entity.WebhookEventEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import com.proofcart.domain.repo.MarketOrderRepository;
import com.proofcart.domain.repo.WebhookEventRepository;
import com.proofcart.inventory.InventoryReservationService;
import com.razorpay.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class RazorpayWebhookController {

    private final CheckoutOrderRepository orderRepo;
    private final MarketOrderRepository marketOrderRepo;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;
    private final InventoryReservationService inventory;
    private final WebhookEventRepository webhookEvents;
    private final AuditEventService audit;

    public RazorpayWebhookController(CheckoutOrderRepository orderRepo, MarketOrderRepository marketOrderRepo,
                                     ObjectMapper objectMapper, InventoryReservationService inventory,
                                     @Value("${razorpay.webhook.secret:}") String webhookSecret, WebhookEventRepository webhookEvents, AuditEventService audit) {
        this.orderRepo = orderRepo;
        this.marketOrderRepo = marketOrderRepo;
        this.objectMapper = objectMapper;
        this.inventory = inventory;
        this.webhookEvents = webhookEvents;
        this.audit = audit;
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
            String eventId = root.path("id").asText(null);
            if (eventId == null || eventId.isBlank())
                return ResponseEntity.badRequest().body("Missing webhook event id.");
            if (webhookEvents.existsById(eventId)) return ResponseEntity.ok(Map.of("status", "already_processed"));
            try {
                webhookEvents.saveAndFlush(new WebhookEventEntity(eventId, event));
            } catch (DataIntegrityViolationException duplicate) {
                return ResponseEntity.ok(Map.of("status", "already_processed"));
            }

            if ("payment.captured".equals(event)) {
                String razorpayOrderId = root.path("payload").path("payment").path("entity").path("order_id").asText();
                String razorpayPaymentId = root.path("payload").path("payment").path("entity").path("id").asText();

                List<CheckoutOrderEntity> orders = orderRepo.findByRazorpayOrderId(razorpayOrderId);
                for (CheckoutOrderEntity order : orders) {
                    // Idempotency: do not double-process
                    if (!"PAID".equals(order.getStatus())) {
                        inventory.capture(order.getId());
                        order.setRazorpayPaymentId(razorpayPaymentId);
                        order.setStatus("PAID");
                        orderRepo.save(order);
                        audit.record(order.getBuyerId(), order.getMerchantId(), order.getCartId(), order.getId(), "PAYMENT_CAPTURED", "Razorpay capture webhook processed.");
                    }
                }
                if (!orders.isEmpty()) {
                    marketOrderRepo.findByRazorpayOrderId(razorpayOrderId).ifPresent(mo -> {
                        mo.setStatus("PAID");
                        marketOrderRepo.save(mo);
                    });
                    System.out.println("[webhook] Marked order " + razorpayOrderId + " as PAID via webhook (" + orders.size() + " sub-order(s)).");
                }
            } else if ("payment.failed".equals(event)) {
                String razorpayOrderId = root.path("payload").path("payment").path("entity").path("order_id").asText();
                List<CheckoutOrderEntity> orders = orderRepo.findByRazorpayOrderId(razorpayOrderId);
                for (CheckoutOrderEntity order : orders) {
                    if (!"PAID".equals(order.getStatus())) {
                        inventory.release(order.getId(), InventoryReservationService.RELEASED);
                        order.setStatus("FAILED");
                        orderRepo.save(order);
                        audit.record(order.getBuyerId(), order.getMerchantId(), order.getCartId(), order.getId(), "PAYMENT_FAILED", "Razorpay failure webhook processed.");
                    }
                }
                if (!orders.isEmpty()) {
                    marketOrderRepo.findByRazorpayOrderId(razorpayOrderId).ifPresent(mo -> {
                        mo.setStatus("FAILED");
                        marketOrderRepo.save(mo);
                    });
                    System.out.println("[webhook] Marked order " + razorpayOrderId + " as FAILED via webhook (" + orders.size() + " sub-order(s)).");
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
