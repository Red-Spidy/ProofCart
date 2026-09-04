# ProofCart — 5-minute video shooting script

Word-for-word narration with on-screen directions. Every product, price, and outcome below was
verified against the live app and the actual selection code — it reproduces as written.

**Total: 5:00 · ~620 spoken words.** Segments marked ⚠️ **CUT IF OVER** are the ones to drop
first if you run long.

---

## Pre-flight — before you hit record

1. **Warm the backend.** Open `https://proofcart.onrender.com/actuator/health` and wait for
   `UP`. Render's free tier sleeps after 15 min; a cold hit costs you up to a minute on camera.
2. **Two windows.** Buyer account in your main window. Merchant account (NutriBasket owner) in a
   second window on `/seller` — you switch to it once, in Segment 3.
3. **Tabs pre-opened** (so you never load a blank page on camera):
   - `proofcart.vercel.app` — buyer, signed in
   - `/seller` — merchant, signed in, Chia Seed Pudding Mix ready to edit
   - `docs/POLICY_EVALUATION.md` on GitHub
   - README, scrolled to the Razorpay principles table
   - `proofcart.onrender.com/swagger-ui.html`
4. **Rehearse Segments 2–4 once off-camera.** AI extraction is consistent but not perfectly
   deterministic — confirm each request still lands on the product named below.
5. **After Segment 3, reset Chia Seed Pudding Mix back to ₹350.** Do it before you forget, or
   your next take breaks.

**Live catalog (verified):** Chia Seed Pudding Mix ₹350 (vegan, gluten-free, today, returnable) ·
Vegan Trail Mix ₹840 · Organic Fruit Bites ₹500 (2-day) · Matcha ₹750 (vegan, organic, 1-day,
**subscription-only**) · Keto Protein Bars ₹1200 (peanuts, **non-returnable**)

---

## 1 · The problem — 0:00–0:25

**ON SCREEN:** ProofCart landing page. Don't click anything yet.

> "AI agents are starting to shop for people. The risk isn't that the AI is dumb — it's that
> it's confident. It'll pick a product, at a price, that doesn't match what you actually asked
> for, and pay for it.
>
> ProofCart is the layer that stops that. Every purchase has to clear a deterministic policy
> engine before Razorpay ever sees an order — and the engine checks independently of whatever
> the AI chose."

---

## 2 · It works end to end — 0:25–1:30

**ON SCREEN:** Type into the request box, then let the three loading steps play — they read
"Parsing your intent with AI", "Selecting matching products", "Running policy engine checks".

**TYPE:** `I want vegan snacks under ₹900, must be returnable, one-time purchase, deliver today.`

> "Plain language in. The model's only job is turning that sentence into structured rules —
> budget, dietary tag, return policy, one-time, delivery window. That's the only thing the LLM
> does in this system, and that's deliberate."

**ON SCREEN:** Cart Review page. Point at the green **ALLOWED** card, then the "Policy checks"
list below it.

> "It picked Chia Seed Pudding Mix at ₹350. Every rule checked, every one green — and each check
> says why it passed, not just that it did."

**ON SCREEN:** Click **Approve & Pay** → Razorpay Test Mode checkout → complete payment → land on
the receipt.

> "Explicit approval, then Razorpay test mode. And this is the receipt: the original request, the
> extracted rules, the exact product snapshot, why it was allowed, when I approved it, and the
> payment result. One continuous audit record."

---

## 3 · The failure, handled gracefully — 1:30–2:25

> "Track one asks for one failure handled gracefully. Here's the one that actually matters in
> payments — a race condition."

**ON SCREEN:** Run the same request again. Approve the cart — **but do not pay.**

> "Same cart, approved, sitting there waiting to be paid."

**ON SCREEN:** Switch to the merchant window. Change Chia Seed Pudding Mix from **₹350 to ₹960**.
Save.

> "Now the merchant changes the price — ₹350 to ₹960. Above the budget the buyer stated. The
> buyer already approved this cart."

**ON SCREEN:** Switch back. Try to check out.

> "It does not silently charge ₹960. Before creating the Razorpay order, the server re-reads the
> database, recomputes the offer hash, sees the product version moved, and stops. The buyer is
> told exactly what changed. That check is not optional and it is not cached — it runs on every
> single checkout."

*(Reset the price to ₹350 after this take.)*

---

## 4 · A block the AI's own logic would have missed — 2:25–2:55

**TYPE:** `Buy something organic, one-time purchase, not a subscription, deliver by tomorrow, under ₹900.`

**ON SCREEN:** Red **BLOCKED** card. Point at the failing `subscription` check.

> "Matcha is subscription-only. I asked for a one-time purchase. Blocked — and no Razorpay order
> is ever created.
>
> Worth being honest about this one: until last night that check was a stub in our code that
> always passed. We found it, built the field properly, and shipped it. It's live in this build."

---

## 5 · Measured, not just demoed — 2:55–3:25

**ON SCREEN:** `docs/POLICY_EVALUATION.md` on GitHub. Scroll the results table.

> "A demo proves one path works. So we measured the whole decision surface: forty-three
> scenarios across every rule — boundary cases like exactly-at-budget and stock exactly equal to
> quantity, rule precedence, multi-item carts. Forty-three out of forty-three correct.
>
> And I'll be straight about what that number is: it's the engine matching a specification we
> wrote. It's a regression guard, not an independent benchmark. But the spec is now executable
> and enumerated — anyone can read all forty-three expectations and disagree with any of them.
> CI fails if one ever drifts."

---

## 6 · The same principles Razorpay published — 3:25–3:55 ⚠️ CUT IF OVER

**ON SCREEN:** README, the Razorpay principles alignment table.

> "After building this, I read Razorpay's own Agent Studio principles post. Review-first — the
> agent prepares, a human approves. No irreversible action without explicit approval. Agents
> never set prices. Data comes from the merchant's own systems, not model inference. A platform
> validation layer before execution. Full audit trail. A kill switch.
>
> We'd independently landed on all seven. Our MCP tools can search, price, and prepare a
> checkout — they can never complete a payment. That was a design decision, and it turns out
> it's your design decision too."

---

## 7 · What broke at 2am — 3:55–4:40

**ON SCREEN:** `git log` in a terminal, scrolling the Sept 2 commits. Or the README incident
section.

> "Two nights before this deadline, re-platforming under pressure, four things broke in about
> two and a half hours.
>
> The production Razorpay key was silently an empty string — checkout would have failed for every
> real user. Our host stopped being free mid-build, and the new one needed a Dockerfile that
> didn't exist. The health check hung forever, because Spring Actuator was auto-checking a Redis
> we'd intentionally made optional, and that one indicator dragged the whole app's status down
> while it was serving traffic perfectly. And cold boot took forty-seven seconds on a fraction
> of a CPU."

**ON SCREEN:** Swagger UI → `Ops → GET /api/ops/health` → **Execute**. Show the live JSON.

> "So we built the watchdog. It doesn't ask 'is the process up' — it actually exercises the
> database, Redis, and our Razorpay and Groq credentials. A rotated key shows up here as 'key
> rejected,' not a generic timeout. When something fails, Groq writes the incident summary and it
> opens a GitHub issue on its own. The exact thing that bit us at 2am now reports itself."

---

## 8 · Close — 4:40–5:00

**ON SCREEN:** Back to the ProofCart landing page.

> "ProofCart doesn't trust an AI with money because the AI sounds convincing. It trusts a policy
> engine that's boring, deterministic, and fully explainable — and it makes the agent prove its
> work, every single time, before a rupee moves."

---

## If you run long, cut in this order

1. Segment 6 (Razorpay principles) — mention it in one line instead: *"This matches the seven
   principles in Razorpay's own Agent Studio post — the table's in our README."*
2. The upsell/receipt detail in Segment 2 — go straight from ALLOWED to Approve & Pay.
3. The Swagger execution in Segment 7 — say what the watchdog does without demoing it.

**Never cut:** Segment 3 (the price-drift failure) or Segment 5 (the 43/43). Those are the two
things most submissions won't have.
