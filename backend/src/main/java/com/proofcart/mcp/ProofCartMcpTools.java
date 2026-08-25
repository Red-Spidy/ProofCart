package com.proofcart.mcp;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Phase 1 Mock MCP Server Endpoint.
 * In a real implementation or Phase 2, this would parse JSON-RPC over HTTP,
 * dispatch to individual tools, and support SSE if streamable HTTP is required.
 * For Phase 1, we map tool names to standard JSON responses to prove the layer exists.
 */
@RestController
@RequestMapping("/api/mcp")
public class ProofCartMcpTools {

    @PostMapping
    public ResponseEntity<Object> handleMcpRequest(@RequestBody Map<String, Object> request, Authentication authentication) {
        String buyerId = (String) authentication.getPrincipal();

        String method = (String) request.get("method");
        if ("tools/call".equals(method)) {
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            String toolName = (String) params.get("name");

            return ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", request.get("id"),
                    "result", Map.of(
                            "content", List.of(
                                    Map.of("type", "text", "text", "Simulated tool call for " + toolName + " by " + buyerId)
                            )
                    )
            ));
        }

        return ResponseEntity.badRequest().build();
    }
}
