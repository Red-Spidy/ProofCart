package com.proofcart.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.domain.entity.AuditEventEntity;
import com.proofcart.domain.repo.AuditEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records audit events and, for every event tied to a cart, chains it into a tamper-evident,
 * HMAC-signed ledger: each event's hash covers its own content plus the previous event's hash,
 * the same construction a blockchain or a Merkle-style append log uses. This turns "show the
 * audit trail" from a log line a server merely asserts into something a buyer, merchant, or
 * auditor can independently re-derive and confirm — see {@link #verifyChain(UUID)}.
 */
@Service
public class AuditEventService {
    private final AuditEventRepository repo;
    private final ObjectMapper objectMapper;
    private final String chainSecret;

    public AuditEventService(AuditEventRepository repo, ObjectMapper objectMapper,
                              @Value("${audit.chain.secret:proofcart-dev-chain-secret}") String chainSecret) {
        this.repo = repo;
        this.objectMapper = objectMapper;
        this.chainSecret = chainSecret;
    }

    public void record(UUID buyer, UUID merchant, UUID cart, UUID order, String type, String description) {
        AuditEventEntity e = new AuditEventEntity();
        e.setBuyerId(buyer);
        e.setMerchantId(merchant);
        e.setCartId(cart);
        e.setOrderId(order);
        e.setEventType(type);
        e.setDescription(description);

        if (cart != null) {
            AuditEventEntity prev = repo.findTopByCartIdOrderByChainSequenceDesc(cart).orElse(null);
            int sequence = prev == null ? 0 : prev.getChainSequence() + 1;
            String prevHash = prev == null ? "GENESIS" : prev.getHash();
            e.setChainSequence(sequence);
            e.setPrevHash(prevHash);
            e.setHash(computeHash(sequence, prevHash, e));
        }

        repo.save(e);
    }

    public List<AuditEventEntity> forOrder(UUID order) {
        return repo.findByOrderIdOrderByCreatedAtAsc(order);
    }

    public List<AuditEventEntity> forCart(UUID cart) {
        return repo.findByCartIdOrderByCreatedAtAsc(cart);
    }

    /**
     * Recomputes every event's HMAC from scratch and checks the prevHash linkage, independently
     * of whatever is stored — a stored hash proves nothing on its own, this method is what
     * actually proves the chain hasn't been altered.
     */
    public ChainVerification verifyChain(UUID cartId) {
        List<AuditEventEntity> events = repo.findByCartIdOrderByCreatedAtAsc(cartId);
        String expectedPrev = "GENESIS";
        for (AuditEventEntity e : events) {
            if (e.getChainSequence() == null) continue;
            if (!expectedPrev.equals(e.getPrevHash())) {
                return new ChainVerification(false, e.getId(),
                        "Broken link at event " + e.getId() + ": prevHash does not match the preceding event's hash.");
            }
            String recomputed = computeHash(e.getChainSequence(), e.getPrevHash(), e);
            if (!recomputed.equals(e.getHash())) {
                return new ChainVerification(false, e.getId(),
                        "Signature mismatch at event " + e.getId() + ": stored content does not match its HMAC.");
            }
            expectedPrev = e.getHash();
        }
        return new ChainVerification(true, null, events.isEmpty()
                ? "No chained events for this cart."
                : "All " + events.size() + " event(s) independently re-verified. Chain intact.");
    }

    private String computeHash(int sequence, String prevHash, AuditEventEntity e) {
        try {
            Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("sequence", sequence);
            canonical.put("prevHash", prevHash);
            canonical.put("eventType", e.getEventType());
            canonical.put("buyerId", e.getBuyerId() == null ? null : e.getBuyerId().toString());
            canonical.put("merchantId", e.getMerchantId() == null ? null : e.getMerchantId().toString());
            canonical.put("cartId", e.getCartId() == null ? null : e.getCartId().toString());
            canonical.put("orderId", e.getOrderId() == null ? null : e.getOrderId().toString());
            canonical.put("description", e.getDescription());
            canonical.put("createdAt", e.getCreatedAt().toString());
            String payload = objectMapper.writeValueAsString(canonical);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chainSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute audit chain hash", ex);
        }
    }

    public record ChainVerification(boolean valid, UUID brokenEventId, String message) {
    }
}
