package com.proofcart.account;

import com.proofcart.domain.entity.Profile;
import com.proofcart.domain.repo.ProfileRepository;
import com.proofcart.security.SupabaseJwtAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccountService {
    private final ProfileRepository profiles;

    public AccountService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    public Profile get(UUID userId) {
        Profile existing = profiles.findById(userId).orElse(null);
        if (existing == null) {
            return createProfile(userId, signupRoleFromToken());
        }
        if ("BUYER".equals(existing.getRole()) && "MERCHANT".equals(signupRoleFromToken())) {
            existing.setRole("MERCHANT");
            return profiles.save(existing);
        }
        return existing;
    }

    private String signupRoleFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(SupabaseJwtAuthenticationFilter.SIGNUP_ROLE_PREFIX))
                .map(a -> a.substring(SupabaseJwtAuthenticationFilter.SIGNUP_ROLE_PREFIX.length()))
                .findFirst()
                .orElse(null);
    }

    public Profile createInitialProfile(UUID userId, String name, String requestedRole) {
        Profile existing = profiles.findById(userId).orElse(null);
        if (existing != null) {
            if ("BUYER".equals(existing.getRole()) && "MERCHANT".equalsIgnoreCase(requestedRole)) {
                existing.setRole("MERCHANT");
                if (name != null && !name.isBlank()) existing.setName(normalizeName(name));
                return profiles.save(existing);
            }
            return existing;
        }
        return profiles.findById(userId).orElseGet(() -> {
            Profile profile = new Profile();
            profile.setId(userId);
            profile.setName(normalizeName(name));
            profile.setRole(normalizeRole(requestedRole));
            return profiles.save(profile);
        });
    }

    public void requireMerchant(UUID userId) {
        if (!"MERCHANT".equals(get(userId).getRole())) {
            throw new ForbiddenRoleException("A seller account is required for this action.");
        }
    }

    private Profile createProfile(UUID userId, String requestedRole) {
        Profile profile = new Profile();
        profile.setId(userId);
        profile.setName("Shopper");
        profile.setRole(normalizeRole(requestedRole));
        return profiles.save(profile);
    }

    private String normalizeRole(String role) {
        return "MERCHANT".equalsIgnoreCase(role == null ? "" : role.trim()) ? "MERCHANT" : "BUYER";
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.isBlank() ? "Shopper" : trimmed.substring(0, Math.min(trimmed.length(), 255));
    }

    public static class ForbiddenRoleException extends RuntimeException {
        public ForbiddenRoleException(String message) {
            super(message);
        }
    }
}
