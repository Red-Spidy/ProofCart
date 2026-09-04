# Demo Video Script — 5 minutes

Every request/product/number below was hand-verified against the live app and the actual
client-side selection code (`autoSelectProducts` in `home.ts`), not guessed. It'll reproduce
exactly as written — but AI extraction has some run-to-run variance, so **rehearse each case
once before the take you keep** (see Pre-flight, step 5).

---

## Pre-flight (do this before hitting record)

1. **Open the live link 60+ seconds before recording.** Render's free tier sleeps after 15 min
   idle; cold start can take up to a minute. Hit `https://proofcart.onrender.com/actuator/health`
   once to warm it, or just load the app and wait for the home page.
2. **Two accounts, two windows.** A buyer account in your main window; a merchant account
   (owner of the NutriBasket store) in a second window/incognito profile, logged into
   `/seller`. You'll swap to the merchant window for Case 2.
3. **Know the reset step.** Case 2 involves editing a real product's price on the live merchant
   dashboard. After recording that segment, change **Chia Seed Pudding Mix** back to **₹350**
   before you forget — otherwise Case 1 breaks for the next person who tries it (including you,
   re-recording a take).
4. **Live catalog reference** (confirmed via `GET /api/catalog/10000000-0000-0000-0000-000000000001`
   moments before writing this — reconfirm if it's been a while):

   | Product | Price | Tags | Allergens | Delivery | Returnable | Sub. only |
   |---|---|---|---|---|---|---|
   | Vegan Trail Mix | ₹840 | vegan, gluten-free | — | today | yes | no |
   | Keto Protein Bars | ₹1200 | keto | peanuts | 1 day | **no** | no |
   | Organic Fruit Bites | ₹500 | organic, vegan | — | 2 days | yes | no |
   | Chia Seed Pudding Mix | ₹350 | vegan, gluten-free | — | today | yes | no |
   | Matcha Green Tea Powder | ₹750 | vegan, organic | — | 1 day | **no** | **yes** |

5. **Rehearse all four requests once, off-camera, right before recording.** The AI extraction
   (Groq, temperature 0.1) is consistent but not perfectly deterministic — confirm each one
   still lands on the product named below before you record the take you'll submit. If one
   drifts, the fallback deterministic parser and the exact trigger phrases below (lifted
   straight from the extractor's own prompt) are your levers to fix it.

---

## 0:00–0:25 — The problem, in one line

*(talking head or voiceover over the landing page)*

"AI agents are starting to shop for people. The risk isn't that the AI is dumb — it's that it's
*confident*. It'll pick a product, at a price, that doesn't actually match what you asked for,
unless something double-checks it. ProofCart is that something: every purchase has to clear a
policy engine before Razorpay ever sees an order — and critically, the engine checks *independently*
of whatever the AI selected, not just alongside it."

## 0:25–1:25 — Case 1: the happy path

**Type:** *"I want vegan snacks under ₹900, must be returnable, one-time purchase, deliver today."*

1. Show the extracted rules panel: budget ₹900, tag `vegan`, `mustBeReturnable: true`,
   `subscriptionAllowed: false`, delivery `today`.
2. The engine auto-selects **Chia Seed Pudding Mix (₹350)** — it's the cheapest item that's
   vegan and delivers today, so it's what the greedy selector picks; Vegan Trail Mix (₹840)
   qualifies too but doesn't fit alongside it under the ₹900 cap, so it's correctly left out.
3. Show the proof cart: every check green — budget, dietary tag, delivery, returnability,
   subscription. Result: **ALLOWED.**
4. Approve → Razorpay Test Mode checkout → complete payment.
5. Land on the audit receipt: original request, extracted rules, product snapshot, why it was
   allowed, the approval, the payment result — one continuous record.

## 1:25–2:10 — Case 2: price changed underneath an approved cart

1. Run the **exact same request** again to build a second, fresh cart (also selects Chia Seed
   Pudding Mix, ₹350) and **approve it** — don't check out yet.
2. Switch to the merchant window (`/seller`), open Chia Seed Pudding Mix, change its price from
   **₹350 to ₹960** — above the ₹900 budget the buyer stated — and save.
3. Switch back to the buyer window and try to complete checkout on the already-approved cart.
4. It does **not** silently charge ₹960. The server re-reads Postgres immediately before
   creating the Razorpay order, catches the price/version drift, and the cart moves to
   `REAPPROVAL_REQUIRED` or `BLOCKED` — the buyer sees exactly what changed, not a silent charge
   or a generic error.
5. **Reset the price back to ₹350** in the merchant window right after this take.

## 2:10–2:50 — Case 3: a check the AI's own selection logic doesn't even run

"Here's something worth being honest about: the product-matching step is a convenience layer,
not a safety layer. It filters by budget, allergens, tags, and delivery — but not by return
policy. That's a real gap in the AI's own logic. The server-side policy engine is what actually
closes it."

**Type:** *"Buy a keto protein bar, one-time purchase, must be returnable, under ₹1300."*

1. Rules extracted: tag `keto`, `mustBeReturnable: true`, budget ₹1300.
2. The selector's own filters don't check returnability, so it happily picks **Keto Protein
   Bars (₹1200)** — the only keto-tagged item, well within budget.
3. Show the proof cart: budget ✅, tags ✅, delivery ✅ — **returnability ❌**. Result:
   **BLOCKED**, specifically on the returnability rule, with the product named in the reason.
4. No Razorpay order is ever created for this cart.

## 2:50–3:30 — Case 4: subscription-only, fixed the night before this recording

"This one has a story. Until last night, this exact check was a stub in the code — it always
passed, no matter what. We caught it, fixed it properly, and it's live as of this build."

**Type:** *"Buy something organic, one-time purchase, not a subscription, deliver by tomorrow,
under ₹900."*

1. Rules: tag `organic`, `subscriptionAllowed: false`, delivery `tomorrow`, budget ₹900.
2. Organic Fruit Bites also matches the tag but delivers in 2 days, past the "tomorrow" window —
   so the selector picks **Matcha Green Tea Powder (₹750)** alone.
3. Show the proof cart: budget ✅, tags ✅, delivery ✅ — **subscription ❌** (Matcha is
   subscription-only, buyer asked for one-time). Result: **BLOCKED.**

## 3:30–4:00 — Architecture, fast

Flow diagram from the README: AI extracts rules → server re-checks the live catalog → policy
engine decides allowed / reapproval_required / blocked → buyer approval → Razorpay → audit
trail. The one sentence that matters for Track 01: *the AI never has a path to payment that
skips the policy engine* — Cases 3 and 4 just proved that literally, not just in theory, since
in both the AI's own selection step let the risky product through and the server still caught
it. Mention the MCP tools (`search_catalog`, `create_intent_contract`, `evaluate_proof_cart`,
`create_checkout_review`, `get_audit_receipt`) as the same guarantee exposed to an external AI
agent, not just this UI.

## 4:00–4:50 — What broke at 2am (the honesty section)

"Two nights before this deadline, re-platforming under pressure, four things broke back to back
in about two and a half hours:"

1. Prod Razorpay key was silently empty — checkout would have failed for every real user.
2. The original host stopped being free mid-build; the new one needed a Dockerfile that hadn't
   existed before.
3. The platform's health check hung forever — Actuator was auto-checking Redis, found nothing
   (Redis is intentionally optional everywhere else in the app), and that one indicator dragged
   the whole health status down even though the app was serving requests correctly the entire
   time.
4. Cold boot was 47 seconds on a fractional-CPU free tier — fixed with JVM startup flags and a
   schema-validate-only setting in production.

"Same standard, turned on ourselves: know exactly what broke, why, and prove the fix — that's
the audit trail principle, applied to our own build and not just the product."

**Optional live bit, if there's time:** open `https://proofcart.onrender.com/swagger-ui.html`,
find `Ops → GET /api/ops/health`, hit **Execute** live. This is the AI ops watchdog built after
that night — it actually exercises the DB, Redis, Razorpay, and Groq credentials, not just "is
the process up," and on a failure asks Groq to write the same kind of plain-English diagnosis
just narrated, then opens a GitHub Issue automatically. The exact class of incident from the
story above — a credential silently going bad — gets caught and reported on its own now.

## 4:50–5:00 — Close

"ProofCart doesn't trust an AI with money because it's convincing — it trusts a policy engine
that's boring, deterministic, and fully explainable, and it makes the AI prove its work every
single time before a rupee moves."
