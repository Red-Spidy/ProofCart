package com.proofcart.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofcart.config.HttpClientTimeouts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemHealthService {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient razorpayClient;
    private final RestClient groqClient;

    public SystemHealthService(
            DataSource dataSource,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${razorpay.key.id:}") String razorpayKeyId,
            @Value("${razorpay.key.secret:}") String razorpayKeySecret,
            @Value("${groq.api.key:}") String groqApiKey) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        this.razorpayClient = (razorpayKeyId == null || razorpayKeyId.isBlank()) ? null :
                RestClient.builder()
                        .baseUrl("https://api.razorpay.com/v1")
                        .requestFactory(HttpClientTimeouts.bounded(5000, 10000))
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                                .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes()))
                        .build();

        this.groqClient = (groqApiKey == null || groqApiKey.isBlank()) ? null :
                RestClient.builder()
                        .baseUrl("https://api.groq.com/openai/v1")
                        .requestFactory(HttpClientTimeouts.bounded(5000, 10000))
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                        .build();
    }

    public SystemHealthReport check() {
        List<HealthCheckResult> checks = List.of(
                checkDatabase(),
                checkRedis(),
                checkRazorpay(),
                checkGroq()
        );

        List<HealthCheckResult> down = checks.stream().filter(c -> "DOWN".equals(c.status())).toList();
        boolean anyDegraded = checks.stream().anyMatch(c -> "DEGRADED".equals(c.status()));

        String overall = !down.isEmpty() ? "unhealthy" : anyDegraded ? "degraded" : "healthy";
        String diagnosis = down.isEmpty() ? null : diagnose(down);

        return new SystemHealthReport(overall, Instant.now(), checks, diagnosis);
    }

    private HealthCheckResult checkDatabase() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SELECT 1");
            return HealthCheckResult.up("database", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return HealthCheckResult.down("database", System.currentTimeMillis() - start, e.getMessage());
        }
    }

    private HealthCheckResult checkRedis() {
        long start = System.currentTimeMillis();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            return HealthCheckResult.degraded("redis", 0, "not configured");
        }
        try (RedisConnection conn = factory.getConnection()) {
            conn.ping();
            return HealthCheckResult.up("redis", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return HealthCheckResult.degraded("redis", System.currentTimeMillis() - start, e.getMessage());
        }
    }

    private HealthCheckResult checkRazorpay() {
        long start = System.currentTimeMillis();
        if (razorpayClient == null) {
            return HealthCheckResult.down("razorpay", 0, "RAZORPAY_KEY_ID not configured");
        }
        try {
            razorpayClient.get().uri("/orders?count=1").retrieve().toBodilessEntity();
            return HealthCheckResult.up("razorpay", System.currentTimeMillis() - start);
        } catch (RestClientResponseException e) {
            return HealthCheckResult.down("razorpay", System.currentTimeMillis() - start,
                    classifyHttpFailure(e.getStatusCode().value(), "Razorpay"));
        } catch (Exception e) {
            return HealthCheckResult.down("razorpay", System.currentTimeMillis() - start, "unreachable: " + e.getMessage());
        }
    }

    private HealthCheckResult checkGroq() {
        long start = System.currentTimeMillis();
        if (groqClient == null) {
            return HealthCheckResult.down("groq", 0, "GROQ_API_KEY not configured");
        }
        try {
            groqClient.get().uri("/models").retrieve().toBodilessEntity();
            return HealthCheckResult.up("groq", System.currentTimeMillis() - start);
        } catch (RestClientResponseException e) {
            return HealthCheckResult.down("groq", System.currentTimeMillis() - start,
                    classifyHttpFailure(e.getStatusCode().value(), "Groq"));
        } catch (Exception e) {
            return HealthCheckResult.down("groq", System.currentTimeMillis() - start, "unreachable: " + e.getMessage());
        }
    }

    static String classifyHttpFailure(int statusCode, String service) {
        if (statusCode == 401 || statusCode == 403) {
            return service + " key rejected (HTTP " + statusCode + ") — likely rotated or revoked";
        }
        return service + " returned HTTP " + statusCode;
    }

    private String diagnose(List<HealthCheckResult> down) {
        if (groqClient != null && down.stream().noneMatch(c -> "groq".equals(c.name()))) {
            try {
                String summary = down.stream()
                        .map(c -> c.name() + ": " + c.detail())
                        .collect(Collectors.joining("; "));

                var requestBody = Map.of(
                        "model", "llama-3.3-70b-versatile",
                        "messages", List.of(
                                Map.of("role", "system", "content",
                                        "You are ProofCart's on-call ops engineer. Given a list of failing " +
                                                "system checks, write a 2-3 sentence plain-English incident summary: " +
                                                "what's failing, the likely cause, and the first thing to check. " +
                                                "No markdown, no preamble."),
                                Map.of("role", "user", "content", summary)
                        ),
                        "temperature", 0.2,
                        "max_tokens", 200
                );

                String responseStr = groqClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                var root = objectMapper.readTree(responseStr);
                String content = root.path("choices").path(0).path("message").path("content").asText();
                if (!content.isBlank()) {
                    return content.trim();
                }
            } catch (Exception e) {
                System.err.println("[opsHealth] Groq diagnosis failed, using fallback template: " + e.getMessage());
            }
        }
        return fallbackDiagnosis(down);
    }

    static String fallbackDiagnosis(List<HealthCheckResult> down) {
        String names = down.stream().map(HealthCheckResult::name).collect(Collectors.joining(", "));
        String details = down.stream()
                .map(c -> "- " + c.name() + ": " + c.detail())
                .collect(Collectors.joining("\n"));
        return "The following systems are failing: " + names + ". " + details +
                "\nCheck credentials and connectivity for the affected service(s) first — " +
                "the most common cause here is a rotated or expired API key.";
    }
}
