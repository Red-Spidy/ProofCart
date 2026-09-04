# Connecting an AI agent to the ProofCart MCP server

ProofCart exposes its catalog, intent, policy, and audit surface over the Model Context Protocol
so an external AI agent (Claude Desktop, or anything else that speaks MCP) can shop on a buyer's
behalf — under the same policy engine and spending mandates that govern the web UI.

**Endpoint:** `POST /api/mcp` (JSON-RPC over HTTP)
Local: `http://localhost:8080/api/mcp` · Deployed: `https://proofcart.onrender.com/api/mcp`

## The rule that matters

MCP tools can **search, prepare, and explain** a checkout. They cannot complete a payment.
`create_checkout_review` returns a browser review URL — the signed-in buyer finishes checkout
themselves. An agent can never move money without a human seeing the proof cart first.

## Authentication

Every request needs `Authorization: Bearer <token>`. Two kinds of token are accepted:

### 1. Buyer-issued agent tokens (the real path)

A signed-in buyer mints a scoped token with spending mandates attached:

```bash
curl -X POST https://proofcart.onrender.com/api/mcp/tokens \
  -H "Authorization: Bearer <supabase-jwt>" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Claude Desktop",
        "expiresAt": "2026-12-31T00:00:00Z",
        "maxPerTransactionPaise": 100000,
        "maxDailyPaise": 300000,
        "allowedMerchantIds": ["10000000-0000-0000-0000-000000000001"]
      }'
```

The response contains `token` — **the only time the raw value is ever returned.** Only its
SHA-256 hash is stored. The mandate fields are enforced server-side at checkout by
`AgentMandateService`, independently of anything the agent claims.

| Endpoint | Purpose |
|---|---|
| `POST /api/mcp/tokens` | Create a token (returns the raw secret once) |
| `GET /api/mcp/tokens` | List your tokens (hashes only, never the secret) |
| `DELETE /api/mcp/tokens/{id}` | Revoke immediately — the kill switch |

Requests carrying an agent token are attributed to that token: checkout orders record which
delegated agent authorized them, and mandate limits apply to that agent specifically.

### 2. A static service token

For a single non-delegated integration, set `MCP_VALID_TOKEN_HASH` to the SHA-256 hex digest of
`"<MCP_TOKEN_PEPPER>:<your-token>"`. Generate it with:

```bash
echo -n "your-pepper:your-token" | shasum -a 256
```

> **Dev-mode warning:** if `MCP_VALID_TOKEN_HASH` is unset, the server logs
> `[McpAuth] WARNING: MCP_VALID_TOKEN_HASH not set. MCP endpoint is UNSECURED.` and does not
> enforce the static-token path. Always set it in any deployed environment.

## Available tools

| Tool | What it does |
|---|---|
| `search_catalog` | Catalog-backed products with their verified attributes |
| `create_intent_contract` | Turns a buyer request into a typed, stored intent contract |
| `evaluate_proof_cart` | Runs the policy engine — returns `ALLOWED`, `REAPPROVAL_REQUIRED`, or `BLOCKED` with per-rule evidence |
| `create_checkout_review` | Returns a browser review URL. **Never completes a payment.** |
| `suggest_upsell` | Policy-checked add-on suggestions — an accepted upsell re-enters the same engine, it is never force-added |
| `get_audit_receipt` | The saved proof trail for a cart/order the buyer owns |

## Claude Desktop configuration

The server speaks JSON-RPC over plain HTTP, so bridge stdio to it. Edit:

- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "proofcart": {
      "command": "curl",
      "args": [
        "-s", "-X", "POST",
        "https://proofcart.onrender.com/api/mcp",
        "-H", "Content-Type: application/json",
        "-H", "Authorization: Bearer YOUR_AGENT_TOKEN",
        "-d", "@-"
      ]
    }
  }
}
```

Restart Claude Desktop fully, then check the tools list for the six tools above.

## Quick check without an MCP client

```bash
curl -X POST https://proofcart.onrender.com/api/mcp \
  -H "Authorization: Bearer YOUR_AGENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"method":"tools/call","params":{"name":"search_catalog","arguments":{"query":"vegan"}}}'
```

Rejected tokens (invalid, expired, revoked, or belonging to another buyer) get no
authentication — the request falls through to Spring Security and is refused.
