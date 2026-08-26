package com.proofcart.config;

import com.proofcart.mcp.auth.McpTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final McpTokenAuthenticationFilter mcpTokenAuthenticationFilter;
    private final BuyerAuthFilter buyerAuthFilter;

    public SecurityConfig(McpTokenAuthenticationFilter mcpTokenAuthenticationFilter, BuyerAuthFilter buyerAuthFilter) {
        this.mcpTokenAuthenticationFilter = mcpTokenAuthenticationFilter;
        this.buyerAuthFilter = buyerAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: H2 console (dev only)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Public: Catalog lookup (frontend needs this on page load)
                        .requestMatchers("/api/catalog/**").permitAll()
                        // Public: Intent parsing (entry point of the flow)
                        .requestMatchers("/api/intents/**").permitAll()
                        // Public: Razorpay webhooks (Razorpay calls this server-to-server)
                        .requestMatchers("/api/webhooks/**").permitAll()
                        // MCP: authenticated only
                        .requestMatchers("/api/mcp").authenticated()
                        // Everything else (carts, checkout, payments, audit) requires auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(buyerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mcpTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
