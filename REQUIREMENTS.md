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

- Node.js 20.9 or newer (use the current LTS release).
- npm 10 or newer, included with Node.js.
- Git 2.40 or newer.
- A code editor such as VS Code.
- A modern browser such as Chrome for Razorpay Test Mode checkout.

Useful command-line tools:

- Supabase CLI for database migrations, seeding, and type generation.
- Vercel CLI for optional local deployment checks.
- MCP Inspector for testing the remote MCP endpoint during development.

Not required:

- Python and pip.
- Docker for the first deployed version.
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
5. NODE.JS PACKAGES TO INSTALL IN package.json
======================================================================

Production dependencies:

- next                     Web app and server API routes.
- react                    User-interface library.
- react-dom                Browser rendering for React.
- @supabase/supabase-js    Supabase database and Auth client.
- @supabase/ssr            Secure Supabase sessions in Next.js.
- @upstash/redis           Server-side Upstash Redis cache client.
- zod                      Shared validation for forms, APIs, AI, and MCP.
- groq-sdk                 Groq API client for intent extraction.
- razorpay                 Razorpay server-side Orders API client.
- @modelcontextprotocol/sdk Remote MCP server and Streamable HTTP support.
- react-hook-form          Form state for login, product, and intent forms.
- @hookform/resolvers      Connects react-hook-form to Zod.
- lucide-react             Icons.
- clsx                     Conditional CSS class names.
- class-variance-authority Reusable component variants.
- tailwind-merge           Safely merges Tailwind CSS classes.
- sonner                   Toast messages for success and error feedback.

Development dependencies:

- typescript               Type safety.
- @types/node              Type definitions for Node.js.
- @types/react             Type definitions for React.
- @types/react-dom         Type definitions for React DOM.
- tailwindcss              Styling system.
- @tailwindcss/postcss     Tailwind PostCSS integration for Next.js.
- postcss                  CSS processing required by Tailwind.
- shadcn                   Component generator; components are copied into the
                            project instead of imported as one large library.
- eslint                   Code-quality checks.
- eslint-config-next       Next.js ESLint rules.
- prettier                 Consistent formatting.
- prettier-plugin-tailwindcss Tailwind class-order formatting.
- vitest                   Fast unit and API tests.
- jsdom                    Browser-like environment for UI tests.
- @testing-library/react   React component testing utilities.
- @testing-library/jest-dom Readable DOM assertions.
- @testing-library/user-event Realistic user interaction tests.
- @playwright/test         End-to-end browser tests.
- @modelcontextprotocol/inspector Optional MCP testing tool.

Use Node's built-in crypto module for HMAC and token hashing. Do not add a
separate crypto package. Do not add Prisma/Drizzle, Axios, bcrypt, UUID, JWT,
or an AI payment-decision framework for v1; they are not needed.

Install only the shadcn/Radix UI component packages actually used by the UI.
This keeps the first version small and avoids unused dependencies.

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

Create .env.local for local secrets and .env.example with names only.
Configure the same values in Vercel for production.

NEXT_PUBLIC_SUPABASE_URL
NEXT_PUBLIC_SUPABASE_ANON_KEY
SUPABASE_SERVICE_ROLE_KEY
GROQ_API_KEY
NEXT_PUBLIC_RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
MCP_TOKEN_PEPPER
UPSTASH_REDIS_REST_URL
UPSTASH_REDIS_REST_TOKEN
NEXT_PUBLIC_APP_URL

Only variables beginning with NEXT_PUBLIC_ may be sent to the browser. The
Razorpay Key ID is safe to expose; its Key Secret is not.

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
