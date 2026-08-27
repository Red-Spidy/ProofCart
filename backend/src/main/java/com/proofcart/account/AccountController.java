package com.proofcart.account;

import com.proofcart.domain.entity.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        Profile profile = accounts.get(userId(authentication));
        return response(profile);
    }

    /**
     * Used once after authenticated signup; existing roles can never be escalated through this endpoint.
     */
    @PostMapping("/profile")
    public Map<String, Object> createProfile(@RequestBody ProfileRequest request, Authentication authentication) {
        Profile profile = accounts.createInitialProfile(userId(authentication), request.name(), request.role());
        return response(profile);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }

    private Map<String, Object> response(Profile p) {
        return Map.of("id", p.getId().toString(), "name", p.getName(), "role", p.getRole(), "city", p.getCity() == null ? "" : p.getCity());
    }

    public record ProfileRequest(String name, String role) {
    }
}
