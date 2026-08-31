package com.proofcart.mcp.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
            var created = service.create(userId(auth), request.name(), request.expiresAt());
            return ResponseEntity.ok(Map.of("id", created.entity().getId(), "name", created.entity().getName(),
                    "expiresAt", created.entity().getExpiresAt() == null ? "" : created.entity().getExpiresAt(),
                    "token", created.rawToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public Map<String, Object> list(Authentication auth) {
        return Map.of("tokens", service.list(userId(auth)).stream().map(t -> Map.of("id", t.getId(), "name", t.getName(),
                "revoked", t.isRevoked(), "createdAt", t.getCreatedAt(), "expiresAt", t.getExpiresAt() == null ? "" : t.getExpiresAt())).toList());
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

    public record TokenRequest(String name, Instant expiresAt) {
    }
}
