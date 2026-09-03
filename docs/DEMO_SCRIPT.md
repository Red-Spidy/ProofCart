# Demo Script — 5-minute judge walkthrough

Live app: https://proofcart.vercel.app/auth/signup
Backend health: https://proofcart.onrender.com/actuator/health

**Open the live link at least 60 seconds before you start recording/presenting.** The backend is on Render's free tier and sleeps after 15 minutes idle — first request after a gap takes up to a minute to wake up. Hitting `/actuator/health` once is enough to warm it. See the README's "What broke" section if a judge asks why the first load was slow — it's a known, understood, documented tradeoff of free-tier hosting, not an unhandled bug.

Demo store: **NutriBasket**, a fictional vegan/wellness snack shop, seeded via `supabase/seed.sql`.

---

## 0:00–0:30 — The problem, in one line

"AI agents are starting to shop for people. The risk isn't that the AI is dumb — it's that it's *confident*. It'll happily buy the wrong product, at the wrong price, with an allergen the user explicitly excluded, unless something stops it. ProofCart is that something: a policy engine that has to approve every purchase before Razorpay ever sees an order request."

## 0:30–1:30 — Case 1: the happy path

1. Sign in as a buyer.
2. Type a request: *"Buy vegan snacks under ₹900, one-time purchase, must be returnable, arriving today."*
3. Show the extracted rules (budget, dietary tag, subscription = false, returnable = true, delivery deadline) — call out that this came from Groq, with a deterministic fallback parser if Groq is unavailable.
4. Show the matched product and the proof cart: price, stock, vegan tag, return policy, delivery estimate all checked against the rules — result: **ALLOWED**.
5. Approve → Razorpay Test Mode checkout opens → complete payment.
6. Land on the audit receipt: shows the original request, the extracted rules, the product snapshot, why it was allowed, the buyer's approval, and the Razorpay payment result — one continuous, append-only record.

## 1:30–2:15 — Case 2: price changed underneath the cart

1. As the merchant (seller dashboard, second browser/tab), edit the same product's price from ₹840 to ₹960 — above the buyer's ₹900 budget.
2. Go back to the buyer's already-approved cart and try to check out.
3. Show it does **not** silently charge ₹960. The server re-reads Postgres immediately before creating the Razorpay order, catches the drift via the product version/offer-hash check, and surfaces it as `REAPPROVAL_REQUIRED` (or `BLOCKED` if now over budget) — the buyer sees exactly what changed, not a mystery failure.

## 2:15–2:45 — Case 3: hard block — allergen

1. Request a snack with *"no peanuts."*
2. The only matching item in stock contains peanuts.
3. Show the policy engine returns **BLOCKED** with the specific rule that failed — no checkout is ever created, no Razorpay order exists for this cart.

## 2:45–3:15 — Case 4: subscription rejected

1. Request *"one-time purchase, not a subscription."*
2. Product is subscription-only.
3. Show **BLOCKED** on the subscription-preference rule.

## 3:15–4:00 — Architecture, fast

Show the flow diagram from the README: AI extracts rules → server re-checks live catalog → policy engine decides allowed / reapproval_required / blocked → buyer approval → Razorpay → audit trail. Emphasize the one sentence that matters for Track 01: *the AI never has a path to payment that skips the policy engine* — it can search, explain, and suggest, but the deterministic engine, not the model, gates the money action. Mention the MCP tools (`search_catalog`, `create_intent_contract`, `evaluate_proof_cart`, `create_checkout_review`, `get_audit_receipt`) as the same guarantee exposed to an external AI agent, not just the built-in UI.

## 4:00–5:00 — What broke at 2am (the honesty section)

"Two nights ago, re-platforming this under deadline pressure, four things broke in about two and a half hours, back to back:"

1. Prod Razorpay key was silently empty — checkout would have failed for every real user hitting the deployed app.
2. Railway (original host) stopped being free mid-build; Render doesn't auto-detect Java/Maven the way Railway does, so deployment needed a Dockerfile that hadn't existed before.
3. Render's health check hung forever — Spring Boot Actuator was auto-checking Redis, found nothing (Redis is intentionally optional and non-fatal everywhere else in the app), and that one health indicator was dragging the whole app's status to DOWN even though it was serving requests correctly the entire time.
4. Cold boot was 47 seconds on a fractional-CPU free tier — fixed with JIT-tier and entropy-source JVM flags plus switching Hibernate to schema-validate-only in production.

"That's the audit-trail principle applied to our own build, not just the product: know exactly what broke, why, and prove the fix — same standard we're holding the AI to."

**Optional live bit, if there's time:** open `https://proofcart.onrender.com/swagger-ui.html`, find `Ops → GET /api/ops/health`, and hit "Try it out" / "Execute" live in front of the judges. This is the AI ops watchdog we built after that night — it actually exercises the DB, Redis, Razorpay, and Groq credentials, not just "is the process up," and on a failure it asks Groq to write the same kind of plain-English diagnosis a judge just heard, then opens a GitHub Issue automatically. The exact class of incident from the story above — a credential silently going bad — gets caught and reported on its own now. (The rest of the API — checkout, MCP tools, audit receipts — is browsable from the same Swagger UI if a judge wants to poke at it directly.)

## Close

"ProofCart doesn't trust an AI with money because it's convincing — it trusts a policy engine that's boring, deterministic, and fully explainable, and it makes the AI prove its work every single time before a rupee moves."
