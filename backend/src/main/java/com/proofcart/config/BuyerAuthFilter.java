package com.proofcart.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Reads the X-Buyer-Id header from all secured requests.
 * This is a lightweight session token for Phase 1 development.
 * Phase 2 will replace this with Supabase JWT validation.
 */
@Component
public class BuyerAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String buyerIdHeader = request.getHeader("X-Buyer-Id");

        if (buyerIdHeader != null && !buyerIdHeader.isBlank()) {
            try {
                // Validate it is a valid UUID format
                UUID buyerId = UUID.fromString(buyerIdHeader.trim());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(buyerId.toString(), null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                // Invalid UUID format — let Spring Security reject at the authorizeHttpRequests level
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
