# ProofCart Implementation Plan

## 1. Project Goal

ProofCart is a safe shopping assistant for AI-led commerce.

It solves a real problem: an AI agent may misunderstand a buyer, choose the wrong product, ignore a price change, or create a payment without enough approval.

ProofCart will connect the buyer's request to the merchant's actual product data and allow checkout only when the purchase follows the buyer's rules.

The main proof chain is:

```text
Buyer request
  -> Intent rules
  -> Verified merchant product
  -> Safety decision
  -> Buyer approval
  -> Razorpay Test Mode checkout
  -> Audit receipt
```

The first demo merchant will be NutriBasket, a fictional wellness and snack store.

## 2. Main Users

### Buyer

- Creates an account and profile.
- Describes what they want in normal language.
- Sets rules such as budget, allergy restrictions, delivery time, returnability, and subscription preference.
- Reviews the selected cart.
- Approves or rejects checkout.
- Views the final audit receipt.

### Merchant

- Creates an account and store profile.
- Adds and edits products.
- Controls prices, stock, ingredients, delivery information, returns, and subscriptions.
- Sees buyer requests, carts, payment status, and audit history.

## 3. Technology Stack

### Frontend and backend

- Next.js App Router
- React
- TypeScript
- Tailwind CSS
- shadcn/ui
- Lucide icons

Next.js will provide both the web interface and server-side API routes, keeping the first version simple to deploy and maintain.

### Authentication and database

- Supabase Auth for email/password registration and login
- Supabase PostgreSQL for application data
- Supabase Row Level Security for buyer and merchant data protection
- `@supabase/ssr` for secure Next.js sessions
- Zod for shared input and output validation

### Caching

- Upstash Redis free tier as the shared cache, using `@upstash/redis`
- Cache-aside pattern for catalog browsing and non-sensitive merchant data
- Short cache time-to-live values and explicit invalidation after a merchant product update
- Database remains the source of truth; Redis is never used to approve a cart or payment
- Cache failures fall back to Supabase PostgreSQL instead of blocking a buyer

### Artificial intelligence

- Groq free tier as the primary AI provider
- `openai/gpt-oss-20b` for structured intent extraction
- Zod validation after every AI response
- Deterministic parser fallback when the AI provider is unavailable or rate-limited

The AI will understand language and explain decisions. It will never directly decide whether money can move.

### Payments

- Razorpay Node SDK
- Razorpay Test Mode Orders API
- Razorpay Standard Checkout
- Razorpay payment signature verification using HMAC-SHA256
- Razorpay Test Mode webhooks
- Node.js `crypto` for secure signature comparison

### Agent interface

- Official Model Context Protocol TypeScript SDK
- Remote MCP endpoint at `/api/mcp`
- Streamable HTTP transport
- User-generated scoped ProofCart token for MCP access
- MCP setup documentation for Claude or other compatible clients

### Deployment and development

- GitHub for source control
- Vercel for the public Next.js deployment
- Supabase hosted database and authentication
- GitHub Actions for continuous integration
- Vitest for unit and API tests
- Playwright for end-to-end browser tests

## 4. Database Design

### `profiles`

Stores the authenticated user's basic information.

- `id`
- `full_name`
- `email`
- `role`: `buyer` or `merchant`
- `city`
- `created_at`

### `merchants`

Stores each merchant's store information.

- `id`
- `owner_id`
- `store_name`
- `store_slug`
- `description`
- `created_at`

### `products`

Stores the merchant's products.

- `id`
- `merchant_id`
- `name`
- `description`
- `price_paise`
- `stock_quantity`
- `dietary_tags`
- `allergens`
- `delivery_days`
- `returnable`
- `subscription_available`
- `version`
- `updated_at`

The product version increases whenever important data changes. This lets ProofCart detect that a cart is based on old information.

### `intent_contracts`

Stores what the buyer asked for after AI extraction.

- `id`
- `buyer_id`
- `merchant_id`
- `original_prompt`
- `rules_json`
- `confidence`
- `status`
- `expires_at`
- `approved_at`
- `created_at`

### `proof_carts`

Stores the selected product snapshot and safety decision.

- `id`
- `intent_id`
- `buyer_id`
- `merchant_id`
- `items_snapshot_json`
- `total_paise`
- `offer_hash`
- `decision`: `allowed`, `reapproval_required`, or `blocked`
- `reasons_json`
- `approved_at`
- `created_at`

### `checkout_orders`

Connects approved ProofCart carts to Razorpay Test Mode orders.

- `id`
- `proof_cart_id`
- `razorpay_order_id`
- `razorpay_payment_id`
- `amount_paise`
- `status`
- `signature_verified`
- `created_at`

### `audit_events`

Stores the full explanation of important actions.

- `id`
- `actor_id`
- `merchant_id`
- `entity_type`
- `entity_id`
- `event_type`
- `message`
- `metadata_json`
- `event_hash`
- `created_at`

### `agent_tokens`

Stores hashed access tokens for remote MCP clients.

- `id`
- `buyer_id`
- `token_hash`
- `label`
- `expires_at`
- `revoked_at`
- `created_at`

### `webhook_events`

Prevents duplicate Razorpay webhook processing.

- `id`
- `razorpay_event_id`
- `event_type`
- `payload_json`
- `processed_at`

## 5. Buyer Flow

1. Buyer signs up and creates a profile.
2. Buyer chooses NutriBasket or another available merchant.
3. Buyer types a request such as:

   > Buy vegan snacks under Rs. 900, deliver today, no peanuts, no subscription.

4. Groq converts the request into structured rules.
5. The UI shows the extracted rules for buyer confirmation.
6. ProofCart checks Redis for a recent catalog-search result, then reads the verified merchant catalog from PostgreSQL if there is no safe cache hit.
7. The policy engine checks the cart.
8. The buyer sees the product, price, stock, delivery, allergen, return, and subscription information.
9. The buyer approves the cart.
10. ProofCart checks the product data one final time.
11. ProofCart creates a Razorpay Test Mode Order.
12. Razorpay Checkout opens in the browser.
13. The server verifies the payment signature.
14. The webhook updates the payment status.
15. ProofCart creates the audit receipt.

## 6. Merchant Flow

1. Merchant creates an account.
2. Merchant creates a store profile.
3. Merchant adds NutriBasket products.
4. Merchant edits product price, stock, ingredients, delivery time, return policy, and subscription availability. The server updates the product version and invalidates the related Redis catalog cache.
5. Merchant sees incoming AI shopping requests.
6. Merchant sees which carts were allowed, blocked, or sent for re-approval.
7. Merchant can open the audit history for every cart and payment.

## 7. AI Intent Extraction

The endpoint will be:

```text
POST /api/intents/parse
```

Input:

```json
{
  "merchantSlug": "nutribasket",
  "prompt": "Buy vegan snacks under Rs. 900, deliver today, no peanuts"
}
```

Output:

```json
{
  "maxTotalPaise": 90000,
  "mustHaveTags": ["vegan"],
  "excludedAllergens": ["peanuts"],
  "deliveryRequirement": "today",
  "subscriptionAllowed": false,
  "needsClarification": false,
  "confidence": 0.94
}
```

Rules:

- AI output must match the Zod schema.
- Missing important information produces a clarification question.
- The AI cannot invent price, stock, delivery, return, or ingredient information.
- If Groq fails, the fallback parser supports common budget, allergy, delivery, return, and subscription patterns.

## 8. Policy Engine

The policy engine is deterministic and runs on the server.

It checks:

- Final total including delivery and fees.
- Product stock.
- Required product tags.
- Excluded allergens.
- Delivery deadline.
- Returnability.
- Subscription permission.
- Merchant ownership.
- Contract expiration.
- Product version and offer hash.

### Redis cache rules

Redis makes repeated browsing faster, but it must never affect a money decision.

Safe cache entries:

- Public merchant/store details: up to 10 minutes.
- Catalog search results and product-list cards: up to 60 seconds.
- Non-sensitive product display data: up to 60 seconds.

Never read from Redis as the source of truth for:

- A proof-cart snapshot.
- Final price, stock, allergen, delivery, return, or subscription check.
- Policy decision.
- Buyer approval.
- Razorpay order amount or payment status.

On every product create, edit, stock change, price change, or policy change, the server invalidates that merchant's relevant catalog cache keys. Before a buyer approves a cart and again before a Razorpay order is created, the policy engine reads the current PostgreSQL records directly.

If Redis is unavailable, catalog browsing continues by querying PostgreSQL. Checkout and payment must continue to use PostgreSQL and must not depend on Redis.

### Decision rules

#### Allowed

All required rules pass and the offer has not changed.

#### Re-approval required

The offer changed, but the new offer may still fit the buyer's rules.

Example: price changed from Rs. 840 to Rs. 890 while the budget is Rs. 900.

#### Blocked

A hard requirement fails.

Example: the product contains peanuts while the buyer explicitly rejected peanuts.

## 9. Razorpay Payment Flow

### Create order

```text
POST /api/checkout/create
```

The server will only create an order when:

- The user is authenticated.
- The buyer owns the proof cart.
- The proof cart is approved.
- The contract has not expired.
- The product data is unchanged.
- The latest policy decision is `allowed`.
- The amount is calculated by the server.

### Verify payment

```text
POST /api/payments/verify
```

The server verifies the Razorpay payment signature using:

```text
HMAC_SHA256(order_id + "|" + payment_id, key_secret)
```

The server uses the order ID stored in its own database, not an untrusted browser value.

### Webhook

```text
POST /api/webhooks/razorpay
```

The webhook handler will:

- Verify the webhook signature.
- Store the Razorpay event ID.
- Ignore duplicate events.
- Process `payment.captured`, `payment.failed`, and `order.paid`.
- Update the order and audit records.
- Return quickly and keep heavy work outside the webhook request.

## 10. MCP Design

MCP will let a compatible AI assistant use ProofCart's safe functions.

Endpoint:

```text
POST /api/mcp
```

Tools:

### `search_catalog`

Searches only the token owner's allowed merchant catalog.

### `create_intent_contract`

Converts a buyer request into a saved intent contract.

### `evaluate_proof_cart`

Checks selected products and returns the full safety decision.

### `create_checkout_review`

Creates a browser review URL only if the cart is approved and allowed.

It must never silently complete payment.

### `get_audit_receipt`

Returns the proof of what was requested, selected, approved, and paid.

MCP access tokens will be:

- Generated from the buyer's profile page.
- Displayed only once.
- Stored as hashes.
- Revocable.
- Scoped to the buyer account.

The repository will include `docs/MCP_SETUP.md` with the remote endpoint and configuration example.

## 11. Application Pages

### Public pages

- Landing page
- Product/store preview
- Login
- Signup

### Buyer pages

- Buyer dashboard
- Profile and preferences
- AI shopping assistant
- Intent review
- Proof cart review
- Razorpay checkout status
- Audit receipt
- MCP token management

### Merchant pages

- Merchant dashboard
- Store profile
- Product list
- Add product
- Edit product
- Shopping requests
- Orders and payments
- Audit history

## 12. Security Rules

- Never expose Razorpay Key Secret in frontend code.
- Keep all AI provider keys server-side.
- Store prices as integer paise.
- Use Supabase Row Level Security for tenant separation.
- Verify all browser payment callbacks on the server.
- Verify all Razorpay webhooks.
- Use constant-time signature comparison.
- Never trust product data sent from the browser.
- Recalculate totals on the server before checkout.
- Re-check product version before creating a payment.
- Keep Upstash Redis URL and token server-side; do not cache buyer payment, approval, or personal data.
- Invalidate affected Redis catalog keys whenever a merchant changes a product.
- Fall back to PostgreSQL when Redis is unavailable; do not fail or relax a payment check because the cache is down.
- Never allow an AI tool to bypass buyer approval.
- Revoke compromised MCP tokens.
- Keep real credentials out of GitHub.

## 13. Build Schedule

### First 1–2 days: working product

1. Scaffold Next.js and install dependencies.
2. Connect Supabase Auth and database.
3. Add buyer and merchant roles.
4. Add product management.
5. Add NutriBasket seed data.
6. Connect Upstash Redis and add cache-aside helpers, short TTLs, product-update invalidation, and PostgreSQL fallback.
7. Build intent extraction and fallback parser.
8. Build policy engine.
9. Build proof-cart review.
10. Add Razorpay Test Mode checkout.
11. Add audit receipt.
12. Add basic remote MCP tools.
13. Deploy to Vercel.

### Remaining time: quality and testing

- Run all policy tests.
- Run Razorpay success and failure scenarios.
- Test price, stock, allergen, return, and subscription changes.
- Test duplicate webhooks.
- Test role-based access.
- Test MCP token revocation.
- Improve the UI and mobile experience.
- Prepare the five-minute demo.
- Complete README and architecture documentation.

## 14. Test Plan

### Unit tests

- Intent extraction schema validation.
- Fallback parser behavior.
- Budget checks.
- Allergen checks.
- Product tag checks.
- Stock checks.
- Delivery checks.
- Return and subscription checks.
- Expired intent checks.
- Changed product version checks.
- Redis cache hit, cache miss, expiry, and PostgreSQL fallback behavior.
- Redis invalidation after price, stock, ingredient, delivery, return, or subscription changes.
- Confirmation that the policy engine and checkout creation bypass Redis.

### Payment tests

- Valid Razorpay signature.
- Invalid Razorpay signature.
- Duplicate checkout request.
- Duplicate webhook event.
- Failed payment.
- Captured payment.
- Out-of-order events.

### End-to-end tests

1. Buyer signup and login.
2. Merchant signup and product creation.
3. Buyer submits a valid request.
4. Product is selected and approved.
5. Razorpay Test Mode checkout opens.
6. Payment status appears in the receipt.
7. Merchant changes price.
8. Old cart requires re-approval or becomes blocked.
9. Buyer cannot access another buyer's receipt.
10. MCP search and cart evaluation work with a valid token.

## 15. Required Demo Scenarios

### Successful purchase

The buyer asks for vegan snacks under Rs. 900. ProofCart finds a compliant product and opens Razorpay Test Mode Checkout.

### Price change

The merchant increases the price from Rs. 840 to Rs. 960. ProofCart refuses to create checkout and explains the change.

### Allergen violation

The product contains peanuts. ProofCart blocks the cart because the buyer rejected peanuts.

### Subscription violation

The buyer wants a one-time purchase. ProofCart rejects a subscription product.

### Audit proof

The receipt shows the original request, extracted rules, product snapshot, policy checks, approval, order ID, and payment status.

## 16. GitHub Deliverables

- `README.md` with setup and demo instructions.
- `PROJECT_BRIEF.md` with the product explanation.
- `IMPLEMENTATION_PLAN.md` with this technical plan.
- `.env.example` with variable names only.
- Database migrations and seed data.
- `docs/MCP_SETUP.md` with MCP instructions.
- `docs/DEMO_SCRIPT.md` with the five-minute presentation flow.
- Architecture diagram.
- Automated tests.
- GitHub Actions CI workflow.
- Deployed Vercel URL.

## 17. Environment Variables

```text
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
```

Only variables beginning with `NEXT_PUBLIC_` may be used in browser code. The Razorpay Key ID is safe to expose; all other listed secrets remain server-side.

## 18. Definition of Done

The project is ready for submission when:

- A new buyer can register and shop.
- A new merchant can register and manage products.
- AI can convert a request into clear rules.
- The fallback parser works without the AI provider.
- Unsafe or changed carts are blocked.
- Approved carts open Razorpay Test Mode Checkout.
- Payment signatures and webhooks are verified.
- Audit receipts explain all important actions.
- Remote MCP tools work with a scoped token.
- Redis speeds catalog browsing, invalidates after merchant changes, and has no authority over policy or payment decisions.
- The application is deployed publicly.
- The GitHub repository is understandable to a reviewer.
- The demo shows one successful flow and one safe failure.

## 19. Important Scope Decisions

- Use Vercel and Supabase for public deployment, authentication, and database.
- Use Groq free tier with a deterministic fallback.
- Use Upstash Redis free tier for short-lived catalog caching, with PostgreSQL as the source of truth.
- Support buyer and merchant roles.
- Let merchants manage products through a dashboard.
- Use Razorpay Test Mode only.
- Use remote MCP with scoped API tokens instead of full OAuth.
- Keep all payment actions behind buyer approval and server-side policy checks.
