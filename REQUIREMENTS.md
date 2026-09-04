# ProofCart — Development Requirements
#
# IMPORTANT: ProofCart is a Spring Boot (Java) + Angular application.
#
# This is an easy-to-read build checklist. Once coding starts, the real
# dependencies must be kept in backend/pom.xml (Maven) and frontend/package.json.

======================================================================
1. WHAT WE ARE BUILDING
======================================================================

ProofCart is a safe AI shopping application for buyers and merchants.

Before it creates a Razorpay Test Mode checkout, it must prove that the product
still matches the buyer's rules: price, stock, dietary needs, allergens,
delivery time, return policy, and subscription preference.

The first demo store is NutriBasket, a fictional wellness/snack store.

Main flow:

Buyer request
  -> AI turns the request into clear rules
  -> ProofCart checks real merchant catalog data
  -> Server makes a safety decision
  -> Buyer approves
  -> Razorpay Test Mode checkout opens
  -> ProofCart saves an audit receipt

======================================================================
2. REQUIRED FEATURES FOR VERSION 1
======================================================================

[ ] Public landing page explaining safe AI shopping.
[ ] Buyer and merchant email/password signup and login.
[ ] Buyer profile with city and optional shopping preferences.
[ ] Merchant store profile and product-management dashboard.
[ ] Product create, edit, stock update, and price update.
[ ] Product fields for dietary tags, allergens, delivery, returns, and
    subscription availability.
[ ] AI shopping request box for buyers.
[ ] Editable intent/rules review before a cart is evaluated.
[ ] Catalog search based only on merchant data stored in the database.
[ ] Deterministic policy engine with three results:
      - allowed
      - reapproval_required
      - blocked
[ ] Proof-cart page showing items, price, stock, policy checks, and explanation.
[ ] Explicit buyer approval before checkout.
[ ] Razorpay Test Mode Standard Checkout.
[ ] Server-side Razorpay payment-signature verification.
[ ] Razorpay webhook verification and duplicate-event protection.
[ ] Buyer and merchant audit-receipt/history pages.
[ ] Remote MCP endpoint with scoped and revocable buyer tokens.
[ ] Public Vercel deployment and clear GitHub documentation.

======================================================================
3. LOCAL DEVELOPMENT SOFTWARE
======================================================================

Required:

- Java 21 (Temurin or equivalent) and Maven 3.9+ for the Spring Boot backend.
- Node.js 20.19 or newer (24.x is what the frontend is built and CI-tested against).
- npm 10 or newer, included with Node.js.
- Git 2.40 or newer.
- A code editor such as VS Code or IntelliJ IDEA.
- A modern browser such as Chrome for Razorpay Test Mode checkout.

Useful command-line tools:

- Supabase CLI for database migrations and seeding.
- Docker, for building the backend image the way Render builds it (backend/Dockerfile).
- MCP Inspector for testing the MCP endpoint during development.

Not required:

- Python and pip.
- Any real card, UPI, or live Razorpay account.

======================================================================
4. REQUIRED ACCOUNTS AND SERVICES
======================================================================

- GitHub: source code, pull requests, and GitHub Actions CI.
- Supabase: PostgreSQL database, Auth, Row Level Security, and migrations.
- Groq: free-tier AI key for structured intent extraction.
- Razorpay: Test Mode account and test API keys only.
- Upstash: free Redis database for short-lived shared catalog caching.
- Vercel: public Next.js deployment connected to GitHub.

The app must still demonstrate core rule extraction when Groq is unavailable.
A deterministic parser is the fallback; it supports the demo's budget, allergy,
delivery, return, and subscription phrases.

======================================================================
5. DEPENDENCIES
======================================================================

Backend (backend/pom.xml, Spring Boot 3.3 / Java 21):

- spring-boot-starter-web            REST controllers and the MCP JSON-RPC endpoint.
- spring-boot-starter-security       Filter chain, route authorization, CORS.
- spring-boot-starter-data-jpa       Entities and repositories over Supabase Postgres.
- spring-boot-starter-data-redis     Catalog caching (optimisation only, never authoritative).
- spring-boot-starter-validation     Request payload validation.
- spring-boot-starter-actuator       /actuator/health for the platform health check.
- postgresql                         Supabase Postgres driver (runtime).
- h2                                 In-memory database for tests (runtime).
- razorpay-java                      Razorpay Orders API client.
- jjwt-api / jjwt-impl / jjwt-jackson  Supabase JWT verification.
- springdoc-openapi-starter-webmvc-ui  Swagger UI at /swagger-ui.html.
- spring-boot-starter-test           JUnit 5 test harness.
- spring-security-test               Security-aware integration tests.

Use the JDK's built-in MessageDigest for SHA-256 hashing (offer hashes, MCP token
hashes, audit chain). Do not add a separate crypto library. Groq is called over
plain RestClient rather than an SDK, so there is no extra AI dependency.

Frontend (frontend/package.json, Angular 21):

- @angular/core, common, forms, router, platform-browser   Framework.
- @supabase/supabase-js                                    Auth client and session handling.
- rxjs, zone.js, tslib                                     Angular runtime dependencies.
- typescript, @angular/cli, @angular/build                 Build toolchain.

Do not add Prisma/Drizzle, Axios, bcrypt, UUID, or an AI payment-decision
framework; none are needed. Keep the dependency surface small.

Redis caching rules:

- Cache public merchant details for up to 10 minutes and catalog search/display
  results for up to 60 seconds.
- Invalidate affected catalog keys after every merchant product update.
- Redis is an optimisation only. PostgreSQL remains the source of truth.
- Never use a Redis value to approve a cart, calculate final checkout amount, or
  verify price, stock, allergens, delivery, buyer approval, or payment status.
- If Redis is unavailable, fetch from PostgreSQL; checkout must not fail or
  become less safe because the cache is down.

======================================================================
6. DATABASE REQUIREMENTS
======================================================================

Supabase PostgreSQL must contain these tables:

- profiles: buyer/merchant identity, role, name, and city.
- merchants: store details and owner.
- products: catalog facts, price in paise, stock, policy details, and version.
- intent_contracts: original request, extracted rules, confidence, and expiry.
- proof_carts: selected immutable product snapshots and policy decision.
- checkout_orders: Razorpay order/payment IDs and verified payment status.
- audit_events: append-only explanation of every important action.
- agent_tokens: hashed MCP access tokens, expiry, and revocation state.
- webhook_events: Razorpay event IDs used for idempotency/deduplication.

Required database protection:

- Enable Supabase Row Level Security on every application table.
- Buyers can only see their own contracts, carts, payments, receipts, and MCP
  tokens.
- Merchants can only manage their own store, products, and store activity.
- Browser code must never use SUPABASE_SERVICE_ROLE_KEY.
- Include SQL migrations and NutriBasket seed data in the repository.

======================================================================
7. SAFETY AND PAYMENT REQUIREMENTS
======================================================================

[ ] Store all money as integer paise, never floating-point rupees.
[ ] Calculate cart totals only on the server.
[ ] Increase a product version when price, stock, delivery, dietary/allergen,
    return, or subscription information changes.
[ ] Save a product snapshot and offer hash with every proof cart.
[ ] Re-check the latest catalog data immediately before creating a Razorpay order.
[ ] Do not create a Razorpay order unless the cart is allowed, current, owned by
    the buyer, approved by the buyer, and not expired.
[ ] Verify Razorpay browser callback signatures on the server with HMAC-SHA256.
[ ] Verify Razorpay webhook signatures before processing their data.
[ ] Store webhook event IDs and ignore duplicate deliveries.
[ ] Keep Upstash Redis credentials server-side and invalidate catalog cache keys
    after merchant updates.
[ ] Re-read PostgreSQL directly for every policy decision and checkout creation;
    never allow cached product data to authorise payment.
[ ] Never expose Razorpay Key Secret, webhook secret, Groq key, service-role
    key, or MCP token pepper in browser code or GitHub.
[ ] MCP tools may search, prepare, and explain a checkout, but must never make
    payment happen silently. The signed-in buyer completes checkout in the UI.

Policy rules that must be tested:

- Budget/final total
- Stock availability
- Required dietary/product tags
- Excluded allergens
- Delivery deadline
- Returnability
- One-time versus subscription purchase
- Intent expiry
- Merchant ownership
- Product-version and offer-hash drift

======================================================================
8. REQUIRED ENVIRONMENT VARIABLES
======================================================================

Create backend/.env for local secrets and backend/.env.example with names only.
Configure the same values as environment variables on Render for production.

Backend (server-side only, never exposed to the browser):

SPRING_DATASOURCE_URL          Supabase Postgres JDBC URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SUPABASE_URL                   Used to fetch JWKS for JWT verification
SUPABASE_ANON_KEY
SUPABASE_SERVICE_ROLE_KEY
SUPABASE_JWT_SECRET
GROQ_API_KEY
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
MCP_TOKEN_PEPPER               Salt for hashing MCP agent tokens
MCP_VALID_TOKEN_HASH           SHA-256 of pepper:token for the static service token
REDIS_HOST / REDIS_PORT / REDIS_USERNAME / REDIS_PASSWORD / REDIS_SSL
CORS_ALLOWED_ORIGINS           Comma-separated; local and prod origins can coexist
JPA_DDL_AUTO                   'validate' in production, 'update' locally
PORT                           Injected by Render at runtime

Frontend (frontend/src/environments/environment*.ts):

supabaseUrl, supabaseAnonKey   Public by design (protected by Row Level Security)
razorpayKeyId                  Public client-side key — the Key Secret is NOT

The Razorpay Key ID and Supabase anon key are safe to ship to the browser. The
Key Secret, webhook secret, service-role key, JWT secret, Groq key, and MCP
pepper must never reach client code or the repository.

======================================================================
9. REQUIRED API ROUTES
======================================================================

POST /api/intents/parse
  Convert buyer language into a validated intent contract. Use Groq first and
  the deterministic fallback if needed.

POST /api/proof-carts
  Build a cart from database products, snapshot the offer, run policy checks,
  save the decision, and return the proof cart.

POST /api/proof-carts/:id/approve
  Record an explicit buyer approval for a currently allowed cart.

POST /api/checkout/create
  Re-check the current catalog and policy, then create a Razorpay Test Mode
  order only if all safety requirements pass.

POST /api/payments/verify
  Verify the Razorpay Checkout payment signature using the server-stored order.

POST /api/webhooks/razorpay
  Verify, deduplicate, and record Razorpay webhook events.

POST /api/mcp
  Remote Streamable HTTP MCP endpoint with bearer-token authentication.

======================================================================
10. REQUIRED MCP TOOLS
======================================================================

- search_catalog
  Returns catalog-backed products and their verified attributes.

- create_intent_contract
  Saves a typed request or returns a clarification question.

- evaluate_proof_cart
  Returns allowed, reapproval_required, or blocked with evidence.

- create_checkout_review
  Returns a browser review URL only; it never makes a payment.

- get_audit_receipt
  Returns the saved proof trail for the buyer's cart/order.

MCP requirements:

- Authenticate using a buyer-created bearer token.
- Show a new token only once, then store only its hash.
- Allow buyers to name, expire, and revoke tokens.
- Reject invalid, expired, revoked, and cross-user tokens.

======================================================================
11. REQUIRED TESTS AND DEMO CASES
======================================================================

Unit/API tests:

- Valid, incomplete, and ambiguous intent extraction.
- Groq failure/rate-limit fallback extraction.
- Each policy rule listed above.
- Correct allowed, reapproval_required, and blocked results.
- Razorpay signature verification, including rejection of invalid signatures.
- Duplicate checkout and duplicate webhook protection.
- Role-based access and Row Level Security expectations.
- MCP token expiry, revocation, and cross-user denial.
- Redis cache hit, miss, expiry, update invalidation, and PostgreSQL fallback.
- A proof that checkout policy evaluation bypasses Redis.

Browser tests:

- Buyer signup -> request -> review -> approve -> Test Mode checkout -> receipt.
- Merchant signup -> product creation/editing -> dashboard activity.
- Merchant price update -> old cart requires re-approval or is blocked.
- Buyer cannot checkout without approval.

Demo must show:

1. A successful vegan, refundable, in-stock purchase under budget.
2. A price change from Rs. 840 to Rs. 960 that blocks checkout.
3. A peanut allergen conflict that blocks the cart.
4. A subscription product rejected for a one-time purchase request.
5. An audit receipt that shows the request, proof, decision, approval, and
   payment result.

======================================================================
12. DEPLOYMENT REQUIREMENTS
======================================================================

[ ] GitHub repository with README.md, PROJECT_BRIEF.md,
    IMPLEMENTATION_PLAN.md, .env.example, migrations, seed data, and docs.
[ ] GitHub Actions workflow running lint, type-check, unit tests, and build.
[ ] Vercel deployment connected to the GitHub repository.
[ ] Supabase production project configured with production redirect URLs.
[ ] Upstash Redis environment variables configured only on the server.
[ ] Razorpay Test Mode webhook configured with the deployed endpoint.
[ ] docs/MCP_SETUP.md explaining how to connect an MCP client.
[ ] docs/DEMO_SCRIPT.md containing the five-minute judge demo flow.

======================================================================
13. OUT OF SCOPE FOR VERSION 1
======================================================================

- Real-money payments.
- Storage of raw card or UPI credentials.
- Live catalog scraping without merchant permission.
- Autonomous price changes, discounts, substitutions, or payment mandates.
- Full OAuth for MCP; scoped, revocable bearer tokens are enough for v1.
- Replacing Razorpay fraud, recovery, or reconciliation products.
