package com.proofcart.mcp.auth;

import com.proofcart.domain.entity.AgentTokenEntity;
import com.proofcart.domain.repo.AgentTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class AgentTokenService {
    private final AgentTokenRepository tokens;
    private final String pepper;

    public AgentTokenService(AgentTokenRepository tokens, @Value("${mcp.token.pepper:default-pepper}") String pepper) {
        this.tokens = tokens;
        this.pepper = pepper;
    }

    public TokenCreated create(UUID buyerId, String name, Instant expiresAt, Integer maxPerTransactionPaise,
                                Integer maxDailyPaise, List<String> allowedMerchantIds) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Token name is required.");
        if (expiresAt != null && !expiresAt.isAfter(Instant.now()))
            throw new IllegalArgumentException("Token expiry must be in the future.");
        if (maxPerTransactionPaise != null && maxPerTransactionPaise < 1)
            throw new IllegalArgumentException("maxPerTransactionPaise must be positive.");
        if (maxDailyPaise != null && maxDailyPaise < 1)
            throw new IllegalArgumentException("maxDailyPaise must be positive.");
        if (maxPerTransactionPaise != null && maxDailyPaise != null && maxPerTransactionPaise > maxDailyPaise)
            throw new IllegalArgumentException("The per-transaction limit cannot exceed the daily mandate.");
        String raw = "pc_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        AgentTokenEntity entity = new AgentTokenEntity();
        entity.setBuyerId(buyerId);
        entity.setName(name.trim().substring(0, Math.min(255, name.trim().length())));
        entity.setTokenHash(hash(raw));
        entity.setExpiresAt(expiresAt);
        entity.setMaxPerTransactionPaise(maxPerTransactionPaise);
        entity.setMaxDailyPaise(maxDailyPaise);
        entity.setAllowedMerchantIds(allowedMerchantIds == null ? List.of() : allowedMerchantIds);
        AgentTokenEntity saved = tokens.save(entity);
        return new TokenCreated(saved, raw);
    }

    public Optional<AgentTokenEntity> authenticate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        Optional<AgentTokenEntity> result = tokens.findByTokenHashAndRevokedFalse(hash(raw));
        if (result.isEmpty() || (result.get().getExpiresAt() != null && !result.get().getExpiresAt().isAfter(Instant.now())))
            return Optional.empty();
        return result;
    }

    public void revoke(UUID id, UUID buyerId) {
        AgentTokenEntity token = tokens.findById(id).orElseThrow(() -> new NoSuchElementException("Token not found."));
        if (!token.getBuyerId().equals(buyerId)) throw new SecurityException("Access denied.");
        token.setRevoked(true);
        tokens.save(token);
    }

    public List<AgentTokenEntity> list(UUID buyerId) {
        return tokens.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((pepper + ":" + raw).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record TokenCreated(AgentTokenEntity entity, String rawToken) {
    }
}
