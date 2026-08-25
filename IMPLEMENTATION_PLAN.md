# ProofCart — Java/Angular Implementation Plan

## Architecture

| Layer | Technology |
|---|---|
| Frontend | Angular 21, Angular Material, Razorpay Checkout |
| Backend | Java 21, Spring Boot, Maven |
| Auth/database | Supabase Auth and Supabase PostgreSQL |
| Cache | Redis (Upstash hosted) via Spring Data Redis |
| AI | Groq from Spring Boot with deterministic Java fallback |
| MCP | Spring AI MCP Server over authenticated Streamable HTTP |
| Payments | Razorpay Java SDK with server-side verification |
| Optional events | Kafka for non-critical events only |

Spring Boot is the authority for product data, policy decisions, approvals, checkout, payment, and audit records. Redis is never authoritative for price, stock, allergens, approval, or payment state. Kafka is never on the synchronous payment path.

## Phase 1 — Spring Boot Foundation and MCP Agent Layer

Create the Java 21 Maven service and expose these protected MCP tools:

1. `search_catalog`
2. `create_intent_contract`
3. `evaluate_proof_cart`
4. `create_checkout_review`
5. `get_audit_receipt`

Files:

- `backend/pom.xml`
- `backend/src/main/java/com/proofcart/ProofCartApplication.java`
- `backend/src/main/java/com/proofcart/config/McpConfig.java`
- `backend/src/main/java/com/proofcart/config/SecurityConfig.java`
- `backend/src/main/java/com/proofcart/mcp/ProofCartMcpTools.java`
- `backend/src/main/java/com/proofcart/mcp/McpTokenAuthenticationFilter.java`
- `backend/src/main/java/com/proofcart/mcp/dto/`
- `backend/src/main/java/com/proofcart/intent/{IntentExtractor,GroqIntentExtractor,FallbackIntentParser}.java`
- `backend/src/main/java/com/proofcart/policy/{PolicyEngine,PolicyRules}.java`
- `backend/src/main/java/com/proofcart/domain/`
- `backend/src/main/resources/application.yml`
- `docs/MCP_SETUP.md`

Acceptance: Java 21 starts; MCP Streamable HTTP is bearer-token protected; all five tools return typed responses; Groq failures use the deterministic fallback; policy outcomes are `ALLOWED`, `REAPPROVAL_REQUIRED`, or `BLOCKED`.

## Phase 2 — Supabase Database, Redis, and Spring REST APIs

Database files:

- `supabase/migrations/001_initial_schema.sql` — profiles, merchants, products, intent contracts, proof carts/items, approvals, checkout orders, payments, MCP tokens, audit events, webhook events, indexes, constraints, and RLS
- `supabase/seed.sql` — NutriBasket products

Spring files:

- `backend/src/main/java/com/proofcart/config/{SupabaseJwtConfig,RedisConfig}.java`
- `backend/src/main/java/com/proofcart/cache/CatalogCacheService.java`
- `backend/src/main/java/com/proofcart/catalog/`
- `backend/src/main/java/com/proofcart/intent/IntentController.java`
- `backend/src/main/java/com/proofcart/cart/{ProofCartController,ProofCartService}.java`
- `backend/src/main/java/com/proofcart/checkout/CheckoutController.java`
- `backend/src/main/java/com/proofcart/payment/{RazorpayService,PaymentController,RazorpayWebhookController}.java`
- `backend/src/main/java/com/proofcart/token/McpTokenController.java`
- `backend/src/main/java/com/proofcart/audit/AuditReceiptController.java`

REST endpoints: `POST /api/intents/parse`, `POST /api/proof-carts`, `GET /api/proof-carts/{id}`, `POST /api/proof-carts/{id}/approve`, `POST /api/checkout/create`, `POST /api/payments/verify`, `POST /api/webhooks/razorpay`, `POST /api/mcp/tokens`, and `DELETE /api/mcp/tokens/{id}`.

Cache catalog searches and non-sensitive merchant metadata with short TTLs. Invalidate product keys after updates. Cache failures become misses. Immediately before checkout, re-read PostgreSQL and rerun all policy checks.

## Phase 3 — Angular Frontend

Create `frontend/` with Angular 21 and Angular Material. Add Supabase Auth, route guards, token HTTP interceptor, buyer dashboard/shop/intent review/proof-cart/approval/checkout/receipt/token pages, merchant dashboard/products/orders/audit pages, and a Razorpay service that opens Checkout only after the backend creates an approved server-side order.

Suggested structure: `frontend/src/app/core/{api,auth,models,services}`, `frontend/src/app/features/auth`, `frontend/src/app/features/buyer`, and `frontend/src/app/features/merchant`.

## Phase 4 — Integration, Testing, and Deployment

Wire controllers and MCP tools to repositories and domain services. Add environment templates, JUnit tests for policy/fallback/auth/cache/payment/webhooks, Angular tests and buyer-flow E2E tests, GitHub Actions CI, NutriBasket seed/demo verification, and deployment of Angular to Vercel and Spring Boot to Railway or Render.

Kafka is an optional later extension for `catalog.changed`, `audit.created`, and `payment.webhook.received`; it must not control approval or order creation.

## Current Status

- [ ] Phase 1 — Spring Boot foundation and MCP layer
- [ ] Phase 2 — Supabase database, Redis, and REST APIs
- [ ] Phase 3 — Angular frontend
- [ ] Phase 4 — Integration, testing, and deployment
