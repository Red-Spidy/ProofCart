# Connecting Claude to the ProofCart MCP Server (Java)

ProofCart uses the Model Context Protocol (MCP) to allow AI assistants (like Claude) to securely access the catalog,
understand shopping intents, evaluate carts, and create checkouts.

## Local Setup for Claude Desktop

1. **Get an MCP Token**
    - In Phase 1, the backend accepts any token that is 5 characters or longer for the mock buyer `buyer-123`. For
      example: `test-token`

2. **Configure Claude Desktop**
    - Open your Claude Desktop configuration file:
        - **Mac**: `~/Library/Application Support/Claude/claude_desktop_config.json`
        - **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

3. **Add the ProofCart Server**
   Since we are using a standard HTTP REST endpoint for the MCP, you can connect it via `curl` as the command, or by
   using a generic HTTP MCP adapter. If you just want to test it locally:

   ```json
   {
     "mcpServers": {
       "proofcart": {
         "command": "curl",
         "args": [
           "-s",
           "-X", "POST",
           "http://localhost:8080/api/mcp",
           "-H", "Content-Type: application/json",
           "-H", "Authorization: Bearer test-token",
           "-d", "@-"
         ]
       }
     }
   }
   ```
   *Note: Using `curl` with `@-` reads JSON-RPC from stdin and passes it to our Java endpoint. This creates a bridge
   between Claude's stdio protocol and our Spring Boot Streamable HTTP server.*

4. **Restart Claude Desktop**
    - Quit Claude completely and open it again.
    - Click the 🔌 (plug) icon in Claude to see the tools: `search_catalog`, `create_intent_contract`,
      `evaluate_proof_cart`, `create_checkout_review`, and `get_audit_receipt`.

## Available Tools

- `search_catalog(query, dietary_tags, allergens)`
- `create_intent_contract(prompt)`
- `evaluate_proof_cart(product_id, quantity, intent_contract_id)`
- `create_checkout_review(proof_cart_id)`
- `get_audit_receipt(order_id)`
