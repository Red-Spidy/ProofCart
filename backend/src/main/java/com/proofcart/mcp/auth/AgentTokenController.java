package com.proofcart.mcp.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/mcp/tokens")
public class AgentTokenController {
    private final AgentTokenService service;

    public AgentTokenController(AgentTokenService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TokenRequest request, Authentication auth) {
        try {
            var created = service.create(userId(auth), request.name(), request.expiresAt(),
                    request.maxPerTransactionPaise(), request.maxDailyPaise(), request.allowedMerchantIds());
            return ResponseEntity.ok(tokenResponse(created.entity(), created.rawToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public Map<String, Object> list(Authentication auth) {
        return Map.of("tokens", service.list(userId(auth)).stream().map(t -> tokenResponse(t, null)).toList());
    }

    private Map<String, Object> tokenResponse(com.proofcart.domain.entity.AgentTokenEntity t, String rawToken) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("revoked", t.isRevoked());
        m.put("createdAt", t.getCreatedAt());
        m.put("expiresAt", t.getExpiresAt() == null ? "" : t.getExpiresAt());
        m.put("maxPerTransactionPaise", t.getMaxPerTransactionPaise());
        m.put("maxDailyPaise", t.getMaxDailyPaise());
        m.put("allowedMerchantIds", t.getAllowedMerchantIds() == null ? List.of() : t.getAllowedMerchantIds());
        if (rawToken != null) m.put("token", rawToken);
        return m;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revoke(@PathVariable UUID id, Authentication auth) {
        try {
            service.revoke(id, userId(auth));
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private UUID userId(Authentication a) {
        return UUID.fromString(a.getName());
    }

    public record TokenRequest(String name, Instant expiresAt, Integer maxPerTransactionPaise,
                                Integer maxDailyPaise, List<String> allowedMerchantIds) {
    }
}
