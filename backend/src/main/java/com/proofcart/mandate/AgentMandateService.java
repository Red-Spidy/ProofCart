package com.proofcart.mandate;

import com.proofcart.domain.entity.AgentTokenEntity;
import com.proofcart.domain.entity.CheckoutOrderEntity;
import com.proofcart.domain.repo.CheckoutOrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces the spending mandate a buyer attached to an agent token — a per-transaction ceiling,
 * a rolling daily cap, and an optional merchant allow-list — the same way a UPI Autopay or card
 * network mandate bounds a delegated payment authority. This is checked server-side at the
 * moment a real Razorpay order would be created, so no MCP tool call or API request an agent
 * makes can spend more than the buyer explicitly delegated, regardless of what the agent asks for.
 */
@Service
public class AgentMandateService {
    private static final Set<String> NON_COMMITTED_STATUSES = Set.of(
            "FAILED_VERIFICATION", "FAILED", "STOCK_UNAVAILABLE", "EXPIRED");

    private final CheckoutOrderRepository orders;

    public AgentMandateService(CheckoutOrderRepository orders) {
        this.orders = orders;
    }

    public void enforce(AgentTokenEntity token, UUID merchantId, int amountPaise) {
        if (!merchantAllowed(token, merchantId)) {
            throw new MandateViolationException("This agent is not authorized to transact with this merchant.");
        }
        if (token.getMaxPerTransactionPaise() != null && amountPaise > token.getMaxPerTransactionPaise()) {
            throw new MandateViolationException("Amount " + rupees(amountPaise)
                    + " exceeds this agent's per-transaction limit of " + rupees(token.getMaxPerTransactionPaise()) + ".");
        }
        if (token.getMaxDailyPaise() != null) {
            int spentToday = spentToday(token.getId());
            if (spentToday + amountPaise > token.getMaxDailyPaise()) {
                throw new MandateViolationException("This purchase would exceed the agent's daily mandate of "
                        + rupees(token.getMaxDailyPaise()) + " (already committed " + rupees(spentToday) + " today).");
            }
        }
    }

    /** Non-throwing preview of mandate status so an agent can check before it tries. */
    public Map<String, Object> describe(AgentTokenEntity token, UUID merchantId, int amountPaise) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("merchantAllowed", merchantAllowed(token, merchantId));
        m.put("maxPerTransactionPaise", token.getMaxPerTransactionPaise());
        m.put("withinPerTransactionLimit",
                token.getMaxPerTransactionPaise() == null || amountPaise <= token.getMaxPerTransactionPaise());
        if (token.getMaxDailyPaise() != null) {
            int spentToday = spentToday(token.getId());
            m.put("maxDailyPaise", token.getMaxDailyPaise());
            m.put("spentTodayPaise", spentToday);
            m.put("remainingTodayPaise", Math.max(0, token.getMaxDailyPaise() - spentToday));
            m.put("withinDailyMandate", spentToday + amountPaise <= token.getMaxDailyPaise());
        }
        return m;
    }

    private boolean merchantAllowed(AgentTokenEntity token, UUID merchantId) {
        List<String> allowed = token.getAllowedMerchantIds();
        return allowed == null || allowed.isEmpty() || allowed.contains(merchantId.toString());
    }

    private int spentToday(UUID tokenId) {
        Instant startOfDay = Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        return orders.findByAgentTokenIdAndCreatedAtAfter(tokenId, startOfDay).stream()
                .filter(o -> !NON_COMMITTED_STATUSES.contains(o.getStatus()))
                .mapToInt(CheckoutOrderEntity::getAmountPaise)
                .sum();
    }

    private String rupees(int paise) {
        return "₹" + String.format("%.2f", paise / 100.0);
    }

    public static class MandateViolationException extends RuntimeException {
        public MandateViolationException(String message) {
            super(message);
        }
    }
}
