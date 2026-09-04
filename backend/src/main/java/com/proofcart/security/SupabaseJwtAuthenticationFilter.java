package com.proofcart.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates Supabase-issued JWT tokens (both ES256 via JWKS and HS256 via secret).
 *
 * The token is sent by the Angular frontend as:
 *   Authorization: Bearer <supabase_access_token>
 */
@Component
public class SupabaseJwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey hmacSecretKey;
    private final String supabaseUrl;
    private final Map<String, PublicKey> ecPublicKeyCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public SupabaseJwtAuthenticationFilter(
            @Value("${supabase.jwt.secret:}") String jwtSecretBase64,
            @Value("${supabase.url:https://fqrdwzzyzckinlkryand.supabase.co}") String supabaseUrl) {

        this.supabaseUrl = supabaseUrl;

        SecretKey key = null;
        if (jwtSecretBase64 != null && !jwtSecretBase64.isBlank()) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(jwtSecretBase64);
                key = new SecretKeySpec(keyBytes, "HmacSHA256");
            } catch (Exception e) {
                System.err.println("[SupabaseJwt] Warning: Could not decode SUPABASE_JWT_SECRET as Base64: " + e.getMessage());
            }
        }
        this.hmacSecretKey = key;

        // Eagerly pre-load JWKS EC keys from Supabase
        refreshJwksKeys();
    }

    private synchronized void refreshJwksKeys() {
        if (supabaseUrl == null || supabaseUrl.isBlank()) return;
        try {
            String jwksUrl = supabaseUrl.replaceAll("/+$", "") + "/auth/v1/.well-known/jwks.json";
            String jwksJson = restTemplate.getForObject(jwksUrl, String.class);
            if (jwksJson != null) {
                JsonNode root = objectMapper.readTree(jwksJson);
                JsonNode keysNode = root.get("keys");
                if (keysNode != null && keysNode.isArray()) {
                    for (JsonNode keyNode : keysNode) {
                        String kid = keyNode.path("kid").asText(null);
                        String kty = keyNode.path("kty").asText(null);
                        String alg = keyNode.path("alg").asText(null);

                        if ("EC".equalsIgnoreCase(kty) && "ES256".equalsIgnoreCase(alg)) {
                            String x = keyNode.path("x").asText();
                            String y = keyNode.path("y").asText();
                            PublicKey pubKey = parseEcPublicKey(x, y);
                            if (pubKey != null) {
                                if (kid != null) {
                                    ecPublicKeyCache.put(kid, pubKey);
                                }
                                ecPublicKeyCache.put("default", pubKey);
                                System.out.println("[SupabaseJwt] Loaded ES256 key from Supabase JWKS (kid: " + kid + ")");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SupabaseJwt] Failed to load JWKS keys from Supabase: " + e.getMessage());
        }
    }

    private PublicKey parseEcPublicKey(String xBase64Url, String yBase64Url) {
        try {
            byte[] xBytes = Base64.getUrlDecoder().decode(xBase64Url);
            byte[] yBytes = Base64.getUrlDecoder().decode(yBase64Url);

            BigInteger xCoord = new BigInteger(1, xBytes);
            BigInteger yCoord = new BigInteger(1, yBytes);
            ECPoint ecPoint = new ECPoint(xCoord, yCoord);

            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);

            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(ecPoint, ecParameterSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(pubKeySpec);
        } catch (Exception e) {
            System.err.println("[SupabaseJwt] Error constructing EC Public Key: " + e.getMessage());
            return null;
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

        try {
            Claims claims = validateAndParseClaims(token);

            String userId = claims.getSubject();   // Supabase user UUID
            String role   = claims.get("role", String.class);  // "authenticated", "anon", etc.

            if (userId == null || userId.isBlank()) {
                sendUnauthorized(response, "Token missing subject claim.");
                return;
            }

            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "authenticated")));
            String marketplaceRole = marketplaceRole(claims);
            if (marketplaceRole != null) {
                authorities.add(new SimpleGrantedAuthority(SIGNUP_ROLE_PREFIX + marketplaceRole));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            System.err.println("[SupabaseJwt] Token validation failed: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    public static final String SIGNUP_ROLE_PREFIX = "SIGNUP_ROLE_";

    private String marketplaceRole(Claims claims) {
        try {
            Object metadata = claims.get("user_metadata");
            if (metadata instanceof java.util.Map<?, ?> map) {
                Object value = map.get("marketplace_role");
                if (value != null && !value.toString().isBlank()) {
                    return value.toString().trim().toUpperCase();
                }
            }
        } catch (Exception e) {
            System.err.println("[SupabaseJwt] Could not read marketplace_role: " + e.getMessage());
        }
        return null;
    }

    private Claims validateAndParseClaims(String token) {
        // Try parsing with cached EC Public Keys (for ES256 tokens)
        for (PublicKey pubKey : ecPublicKeyCache.values()) {
            try {
                return Jwts.parser()
                        .verifyWith(pubKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception ignored) {
            }
        }

        // Try refreshing keys if not found
        if (ecPublicKeyCache.isEmpty()) {
            refreshJwksKeys();
            for (PublicKey pubKey : ecPublicKeyCache.values()) {
                try {
                    return Jwts.parser()
                            .verifyWith(pubKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (Exception ignored) {
                }
            }
        }

        // Fallback: try HMAC secret key if configured (for HS256 tokens)
        if (hmacSecretKey != null) {
            try {
                return Jwts.parser()
                        .verifyWith(hmacSecretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception ignored) {
            }
        }

        throw new JwtException("Could not verify JWT signature with available ES256 or HS256 keys");
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "'") + "\"}");
    }
}
