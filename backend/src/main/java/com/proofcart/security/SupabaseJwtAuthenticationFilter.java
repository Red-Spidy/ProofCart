package com.proofcart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * Validates Supabase-issued JWT tokens (HS256 / HMAC-SHA-256).
 *
 * The token is sent by the Angular frontend as:
 *   Authorization: Bearer <supabase_access_token>
 *
 * On success:  SecurityContext principal = Supabase user UUID (sub claim)
 *              Authorities                = ["ROLE_" + role]  e.g. ROLE_authenticated
 * On failure:  Returns 401 JSON and stops the filter chain.
 *
 * Replaces the temporary BuyerAuthFilter (X-Buyer-Id header).
 */
@Component
public class SupabaseJwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey signingKey;
    private final String expectedIssuer;
    private final String expectedAudience;

    public SupabaseJwtAuthenticationFilter(
            @Value("${supabase.jwt.secret:}") String jwtSecretBase64,
            @Value("${supabase.jwt.issuer:}") String issuer,
            @Value("${supabase.jwt.audience:authenticated}") String audience) {

        this.expectedIssuer = issuer;
        this.expectedAudience = audience;

        if (jwtSecretBase64 != null && !jwtSecretBase64.isBlank()) {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecretBase64);
            this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        } else {
            this.signingKey = null;
            System.err.println("[SupabaseJwt] WARNING: SUPABASE_JWT_SECRET not set. All JWT auth will fail.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No Authorization header — let Spring Security decide (public routes pass through)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // JWT secret not configured — reject immediately
        if (signingKey == null) {
            sendUnauthorized(response, "Authentication not configured on server.");
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(expectedIssuer)
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();   // Supabase user UUID
            String role   = claims.get("role", String.class);  // "authenticated", "anon", etc.

            if (userId == null || userId.isBlank()) {
                sendUnauthorized(response, "Token missing subject claim.");
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "authenticated")))
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            // Token is present but invalid (expired, bad signature, wrong issuer, etc.)
            // Clear context and continue the chain — Spring Security will enforce
            // authorization rules. Public routes will still be accessible;
            // protected routes will receive a 401/403 from Spring Security itself.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "'") + "\"}");
    }
}
