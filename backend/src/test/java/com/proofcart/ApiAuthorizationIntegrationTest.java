package com.proofcart;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "supabase.url=",
        "supabase.jwt.secret=dGVzdC1zaWduaW5nLWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class ApiAuthorizationIntegrationTest {

    private static final String JWT_SECRET = "dGVzdC1zaWduaW5nLWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void merchantCanCreateAProductAfterCreatingAStore() throws Exception {
        String token = tokenFor(UUID.fromString("00000000-0000-0000-0000-000000000111"));

        mockMvc.perform(post("/api/account/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Merchant\",\"role\":\"MERCHANT\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/merchant/store")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Store\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/merchant/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Product\",\"pricePaise\":100,\"stockQuantity\":1," +
                                "\"dietaryTags\":[],\"allergens\":[],\"deliveryDays\":0," +
                                "\"returnable\":true,\"subscriptionAvailable\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void agentTokenCanCallMcpAndCanBeRevoked() throws Exception {
        String jwt = tokenFor(UUID.fromString("00000000-0000-0000-0000-000000000112"));
        MvcResult created = mockMvc.perform(post("/api/mcp/tokens")
                        .header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo agent\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").exists()).andReturn();
        String body = created.getResponse().getContentAsString();
        String raw = com.jayway.jsonpath.JsonPath.read(body, "$.token");
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        mockMvc.perform(post("/api/mcp").header("Authorization", "Bearer " + raw)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"search_catalog\",\"arguments\":{\"merchantId\":\"10000000-0000-0000-0000-000000000001\"}}}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/mcp/tokens/" + id).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/mcp").header("Authorization", "Bearer " + raw)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"search_catalog\"}}"))
                .andExpect(status().isForbidden());
    }

    private String tokenFor(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        return Jwts.builder().subject(userId.toString()).claim("role", "authenticated").signWith(key).compact();
    }
}
