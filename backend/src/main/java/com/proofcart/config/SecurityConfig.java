package com.proofcart.config;

import com.proofcart.mcp.auth.McpTokenAuthenticationFilter;
import com.proofcart.security.SupabaseJwtAuthenticationFilter;
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

    private final SupabaseJwtAuthenticationFilter supabaseJwtFilter;
    private final McpTokenAuthenticationFilter mcpTokenAuthenticationFilter;

    public SecurityConfig(SupabaseJwtAuthenticationFilter supabaseJwtFilter,
                          McpTokenAuthenticationFilter mcpTokenAuthenticationFilter) {
        this.supabaseJwtFilter = supabaseJwtFilter;
        this.mcpTokenAuthenticationFilter = mcpTokenAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: H2 console (dev only)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Public: Catalog lookup (no login required to browse products)
                        .requestMatchers("/api/catalog/**").permitAll()
                        // Public: Intent parsing (entry point of the flow)
                        .requestMatchers("/api/intents/**").permitAll()
                        // Public: Razorpay webhooks (Razorpay calls this server-to-server)
                        .requestMatchers("/api/webhooks/**").permitAll()
                        // MCP: authenticated only
                        .requestMatchers("/api/mcp").authenticated()
                        // Everything else (carts, checkout, payments, audit) requires Supabase JWT
                        .anyRequest().authenticated()
                )
                // Run Supabase JWT filter first, then MCP token filter
                .addFilterBefore(supabaseJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mcpTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of("http://localhost:4200"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
