package com.proofcart.account;

import com.proofcart.domain.entity.Profile;
import com.proofcart.domain.repo.ProfileRepository;
import com.proofcart.security.SupabaseJwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceRoleTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final AccountService accounts = new AccountService(profiles);
    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void signedInWithSignupRole(String role) {
        List<SimpleGrantedAuthority> authorities = role == null
                ? List.of(new SimpleGrantedAuthority("ROLE_authenticated"))
                : List.of(new SimpleGrantedAuthority("ROLE_authenticated"),
                          new SimpleGrantedAuthority(SupabaseJwtAuthenticationFilter.SIGNUP_ROLE_PREFIX + role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities));
    }

    private Profile profileWithRole(String role) {
        Profile p = new Profile();
        p.setId(userId);
        p.setName("Shopper");
        p.setRole(role);
        return p;
    }

    @Test
    void createsMerchantProfileWhenTokenSaysMerchant() {
        signedInWithSignupRole("MERCHANT");
        when(profiles.findById(userId)).thenReturn(Optional.empty());
        when(profiles.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("MERCHANT", accounts.get(userId).getRole());
    }

    @Test
    void createsBuyerProfileWhenTokenCarriesNoRole() {
        signedInWithSignupRole(null);
        when(profiles.findById(userId)).thenReturn(Optional.empty());
        when(profiles.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("BUYER", accounts.get(userId).getRole());
    }

    @Test
    void upgradesAnExistingBuyerProfileWhenTokenSaysMerchant() {
        signedInWithSignupRole("MERCHANT");
        when(profiles.findById(userId)).thenReturn(Optional.of(profileWithRole("BUYER")));
        when(profiles.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("MERCHANT", accounts.get(userId).getRole());
    }

    @Test
    void neverDowngradesAnExistingMerchantProfile() {
        signedInWithSignupRole("BUYER");
        when(profiles.findById(userId)).thenReturn(Optional.of(profileWithRole("MERCHANT")));

        assertEquals("MERCHANT", accounts.get(userId).getRole());
        verify(profiles, never()).save(any(Profile.class));
    }

    @Test
    void requireMerchantPassesForATokenBackedMerchant() {
        signedInWithSignupRole("MERCHANT");
        when(profiles.findById(userId)).thenReturn(Optional.of(profileWithRole("MERCHANT")));

        accounts.requireMerchant(userId);
    }
}
