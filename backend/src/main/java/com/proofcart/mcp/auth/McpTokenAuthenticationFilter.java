package com.proofcart.mcp.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

@Component
public class McpTokenAuthenticationFilter extends OncePerRequestFilter {

    @Value("${mcp.token.pepper:}")
    private String pepper;

    /**
     * Expected SHA-256 hash of (pepper + ":" + validToken).
     * Set MCP_VALID_TOKEN_HASH env variable in production.
     */
    @Value("${mcp.valid.token.hash:}")
    private String validTokenHash;
    private final AgentTokenService agentTokens;

    public McpTokenAuthenticationFilter(AgentTokenService agentTokens) {
        this.agentTokens = agentTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String rawToken = authHeader.substring(7);

            var agentToken = agentTokens.authenticate(rawToken);
            if (agentToken.isPresent()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        agentToken.get().getBuyerId().toString(), null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }

            String tokenHash = hashToken(rawToken);

            // Only authenticate if the hash matches the configured valid token hash
            if (!validTokenHash.isBlank() && validTokenHash.equals(tokenHash)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "mcp-service", null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (validTokenHash.isBlank()) {
                // No hash configured — log warning but allow in dev mode
                System.err.println("[McpAuth] WARNING: MCP_VALID_TOKEN_HASH not set. MCP endpoint is UNSECURED.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = pepper + ":" + rawToken;
            byte[] hash = digest.digest(payload.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }
}
