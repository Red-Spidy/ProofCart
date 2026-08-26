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

Product information has two sources:

- **Open Food Facts** supplies externally sourced product metadata such as barcode, name, brand, image, ingredients, allergens, dietary labels, and nutrition data.
- **Supabase merchant offers** remain the checkout authority for a merchant's price, stock, delivery estimate, return policy, subscription terms, and whether an external product is currently sellable.

The application must never create a Razorpay order from an external catalog API response alone. It may only use a current, active merchant offer stored in PostgreSQL.

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

## Phase 2 — Supabase Authentication and Authorisation

Replace the temporary `X-Buyer-Id` identity with real Supabase authentication before treating the application as multi-user.

### Supabase configuration

- Create a Supabase project and enable email/password authentication.
- Configure the Angular application URL and approved redirect URLs in Supabase Auth.
- Create buyer and merchant profiles at signup; a profile role is assigned server-side, never trusted from the browser.
- Add Row Level Security policies so buyers can access only their own intents, proof carts, orders, MCP tokens, and audit receipts; merchants can access only their stores/products/orders.

### Backend files

- `backend/src/main/java/com/proofcart/config/SupabaseJwtConfig.java` — JWT issuer, audience, and JWKS configuration
- `backend/src/main/java/com/proofcart/security/SupabaseJwtAuthenticationConverter.java` — maps the authenticated Supabase `sub` claim to the user UUID and role
- `backend/src/main/java/com/proofcart/security/CurrentUser.java` — shared authenticated-user accessor
- `backend/src/main/java/com/proofcart/security/OwnershipGuard.java` — verifies buyer/merchant ownership before every sensitive read or write
- Update `SecurityConfig.java` to require a valid Supabase bearer token for every buyer/merchant endpoint; only signed Razorpay webhooks and explicitly public catalog-search routes remain unauthenticated.
- Remove `BuyerAuthFilter.java`, hard-coded buyer UUIDs, and the browser-generated `X-Buyer-Id` header.

### Angular files

- Add `@supabase/supabase-js`.
- `frontend/src/app/core/auth/supabase.service.ts`
- `frontend/src/app/core/auth/auth.guard.ts`
- `frontend/src/app/core/auth/role.guard.ts`
- `frontend/src/app/interceptors/auth.interceptor.ts` — attaches only the Supabase access token.
- `frontend/src/app/pages/auth/login/`
- `frontend/src/app/pages/auth/signup/`
- `frontend/src/app/pages/auth/forgot-password/`

### Acceptance criteria

- Unauthenticated cart, checkout, token, payment-status, and audit endpoints return HTTP 401.
- A buyer cannot read, approve, checkout, pay for, or view the audit receipt of another buyer's cart/order.
- Merchant-only routes reject buyer users with HTTP 403.
- Expired, altered, wrong-issuer, and wrong-audience JWTs are rejected.
- No request path relies on a client-supplied user ID.

## Phase 3 — Real Product Metadata, Order History, and Redis

Integrate the Open Food Facts read API from Spring Boot to serve as the direct catalog. We will skip building merchant capabilities for now. Users will also be able to view their order history, utilizing Redis for fast responses.

### Data model changes

- `external_products` — Cache of Open Food Facts metadata (barcode, name, brand, image URL, ingredients text, normalized allergens, dietary labels, nutrition JSON, last-fetched timestamp).
- Use `external_products` directly as the catalog source, appending a mock price and stock for the prototype if needed, or define a simplified `products` table that acts as a projection of external products + local pricing.
- Ensure the `orders` (or `intent_contracts` / `audit_receipts`) tables are correctly linked to the buyer's Supabase UUID for history retrieval.

### Spring Boot files

- `backend/src/main/java/com/proofcart/catalog/openfoodfacts/OpenFoodFactsClient.java`
- `backend/src/main/java/com/proofcart/catalog/openfoodfacts/OpenFoodFactsProductMapper.java`
- `backend/src/main/java/com/proofcart/catalog/CatalogService.java` — Orchestrates fetching from Open Food Facts and caching in Redis/DB.
- `backend/src/main/java/com/proofcart/catalog/CatalogController.java` — Updated to serve the new catalog.
- `backend/src/main/java/com/proofcart/order/OrderHistoryController.java` — New controller for fetching user order history.

### API behaviour

- `GET /api/catalog` — Fetches catalog (popular/seeded items) from Open Food Facts, cached in Redis.
- `GET /api/catalog/search?q=` — Searches Open Food Facts, cached in Redis.
- `GET /api/orders` — Returns the authenticated user's past orders/receipts.

### Redis policy

- Cache Open Food Facts product lookups and searches for 24 hours.
- Cache user order history for short durations (e.g., 5 minutes) or invalidate on new order creation.
- Use a fixed application `User-Agent` on every Open Food Facts request.

### Checkout safety rules

- At cart creation, verify the product still exists and fetch its latest safety metadata (allergens, dietary tags).
- Since we are not doing merchant offers, assume a static price for external products or pull from a simplified local pricing table.

### Acceptance criteria

- Catalog displays real products from Open Food Facts.
- Users can view a history of their past orders.
- Redis is actively used to cache catalog responses and order history, improving response times.

## Phase 4 — Supabase Database, Payment, MCP, and REST Completion

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

## Phase 5 — Angular Frontend

Create `frontend/` with Angular 21 and Angular Material. Add Supabase Auth, route guards, token HTTP interceptor, buyer dashboard/shop/intent review/proof-cart/approval/checkout/receipt/token pages, merchant dashboard/products/offers/orders/audit pages, and a Razorpay service that opens Checkout only after the backend creates an approved server-side order.

Suggested structure: `frontend/src/app/core/{api,auth,models,services}`, `frontend/src/app/features/auth`, `frontend/src/app/features/buyer`, and `frontend/src/app/features/merchant`.

## Phase 6 — Integration, Testing, and Deployment

Wire controllers and MCP tools to repositories and domain services. Add environment templates, JUnit tests for policy/fallback/auth/cache/payment/webhooks, Angular tests and buyer-flow E2E tests, GitHub Actions CI, NutriBasket seed/demo verification, and deployment of Angular to Vercel and Spring Boot to Railway or Render.

End-to-end tests must cover signup/login/logout, JWT expiry, buyer-to-buyer data isolation, merchant ownership, Open Food Facts timeout/rate-limit/cache behaviour, allergen-metadata absence, offer-price drift, stock race conditions, Razorpay signature rejection, webhook idempotency, and audit-receipt ownership.

Kafka is an optional later extension for `catalog.changed`, `audit.created`, and `payment.webhook.received`; it must not control approval or order creation.

## Current Status

- [x] Phase 1 — Spring Boot foundation and MCP layer (existing)
- [x] Phase 2 — Supabase authentication and authorisation
- [x] Phase 3 — Real product metadata (Open Food Facts), order history, and Redis caching
- [ ] Phase 4 — Supabase database, payments, MCP, and REST completion
- [x] Phase 5 — Angular frontend (buyer-side complete)
- [ ] Phase 6 — Integration, testing, and deployment
