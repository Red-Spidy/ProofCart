# Policy Engine — Batch Evaluation

**43 scenarios · 43 correct · 0 incorrect · 100.0% accuracy**
Decisions produced: 23 ALLOWED · 17 BLOCKED · 3 REAPPROVAL_REQUIRED

Regenerate with:

```bash
cd backend && mvn -Dtest=PolicyEngineEvaluationTest test
```

Source of truth: [`PolicyEngineEvaluationTest`](../backend/src/test/java/com/proofcart/policy/PolicyEngineEvaluationTest.java).
The numbers above are printed by that run; the test fails the build if any scenario stops
producing its expected decision, so this page can't quietly drift away from reality.

## What this measures — and what it doesn't

**It measures:** every rule in the engine, across the full decision surface, including boundary
conditions (exactly-at-budget, stock exactly equal to quantity, "tomorrow" against a 1-day
delivery), rule precedence (a hard block beats a re-approval when both fire), and multi-item
carts where only one item violates.

**It does not measure:** the quality of LLM intent extraction. This harness feeds the engine
already-structured rules, deliberately — the engine is the deterministic gate on money movement,
and that's what needs to be provably correct. Extraction accuracy is a separate, softer problem,
and it's the reason a deterministic fallback parser exists behind Groq at all.

**Honest caveat:** 100% here means "the engine matches its specification on the scenarios we
defined." It is a behavioural surface measurement and a regression guard, not an independent
benchmark, and we wrote both the rules and the scenarios. Its value is that the specification is
now executable and enumerated: a reviewer can read all 43 expectations in one file and disagree
with any of them. Anything that ever fails shows up in the "exception list" at the bottom of the
run instead of being tuned away.

## Coverage by rule

| Rule | Scenarios | What's covered |
|---|---|---|
| `budget` | 4 | Under, exactly at (boundary), one paisa over (boundary), no budget stated |
| `allergens` | 5 | Absent, present, case-insensitive match, no restriction, one of several excluded |
| `dietary_tags` | 4 | Single tag present/missing, several tags all present/one missing |
| `delivery` | 6 | today/tomorrow boundaries, numeric window, no requirement |
| `returnability` | 3 | Required+returnable, required+non-returnable, not required |
| `subscription` | 3 | Subscription-only blocked, subscription-optional allowed, subscriptions permitted |
| `stock` | 4 | Sufficient, exactly equal (boundary), insufficient, zero |
| `contract_expiry` | 2 | Valid, expired |
| `merchant_ownership` | 2 | Correct merchant, cross-merchant item |
| `product_version` | 3 | Version match, version drift, product deleted |
| `offer_hash` | 2 | Hash match, hash drift |
| precedence / multi-item / baseline | 5 | Block-beats-reapproval, two simultaneous failures, multi-item carts, no constraints |

## Full results

| ID | Rule | Scenario | Expected | Actual | OK |
|---|---|---|---|---|---|
| BUD-1 | budget | Total below budget | ALLOWED | ALLOWED | yes |
| BUD-2 | budget | Total exactly equal to budget (boundary) | ALLOWED | ALLOWED | yes |
| BUD-3 | budget | Total one paisa over budget (boundary) | BLOCKED | BLOCKED | yes |
| BUD-4 | budget | No budget stated | ALLOWED | ALLOWED | yes |
| ALG-1 | allergens | Excluded allergen absent from product | ALLOWED | ALLOWED | yes |
| ALG-2 | allergens | Excluded allergen present in product | BLOCKED | BLOCKED | yes |
| ALG-3 | allergens | Allergen match is case-insensitive | BLOCKED | BLOCKED | yes |
| ALG-4 | allergens | No allergen restriction, allergenic product | ALLOWED | ALLOWED | yes |
| ALG-5 | allergens | One of several excluded allergens present | BLOCKED | BLOCKED | yes |
| TAG-1 | dietary_tags | Required tag present | ALLOWED | ALLOWED | yes |
| TAG-2 | dietary_tags | Required tag missing | BLOCKED | BLOCKED | yes |
| TAG-3 | dietary_tags | All of several required tags present | ALLOWED | ALLOWED | yes |
| TAG-4 | dietary_tags | One of several required tags missing | BLOCKED | BLOCKED | yes |
| DEL-1 | delivery | 'today' with same-day product | ALLOWED | ALLOWED | yes |
| DEL-2 | delivery | 'today' with next-day product | BLOCKED | BLOCKED | yes |
| DEL-3 | delivery | 'tomorrow' with next-day product (boundary) | ALLOWED | ALLOWED | yes |
| DEL-4 | delivery | 'tomorrow' with 2-day product | BLOCKED | BLOCKED | yes |
| DEL-5 | delivery | Numeric window (3 days) satisfied | ALLOWED | ALLOWED | yes |
| DEL-6 | delivery | No delivery requirement, slow product | ALLOWED | ALLOWED | yes |
| RET-1 | returnability | Returnable required, product returnable | ALLOWED | ALLOWED | yes |
| RET-2 | returnability | Returnable required, product non-returnable | BLOCKED | BLOCKED | yes |
| RET-3 | returnability | No return requirement, product non-returnable | ALLOWED | ALLOWED | yes |
| SUB-1 | subscription | One-time requested, subscription-only product | BLOCKED | BLOCKED | yes |
| SUB-2 | subscription | One-time requested, subscription offered but optional | ALLOWED | ALLOWED | yes |
| SUB-3 | subscription | Subscriptions allowed, subscription-only product | ALLOWED | ALLOWED | yes |
| STK-1 | stock | Stock exceeds quantity | ALLOWED | ALLOWED | yes |
| STK-2 | stock | Stock exactly equals quantity (boundary) | ALLOWED | ALLOWED | yes |
| STK-3 | stock | Stock below quantity | BLOCKED | BLOCKED | yes |
| STK-4 | stock | Zero stock | BLOCKED | BLOCKED | yes |
| EXP-1 | contract_expiry | Intent contract still valid | ALLOWED | ALLOWED | yes |
| EXP-2 | contract_expiry | Intent contract expired | BLOCKED | BLOCKED | yes |
| OWN-1 | merchant_ownership | Item belongs to the cart's merchant | ALLOWED | ALLOWED | yes |
| OWN-2 | merchant_ownership | Item belongs to a different merchant | BLOCKED | BLOCKED | yes |
| DRF-1 | product_version | Live product version matches snapshot | ALLOWED | ALLOWED | yes |
| DRF-2 | product_version | Live product version newer than snapshot | REAPPROVAL_REQUIRED | REAPPROVAL_REQUIRED | yes |
| DRF-3 | product_version | Product no longer exists in live catalog | REAPPROVAL_REQUIRED | REAPPROVAL_REQUIRED | yes |
| HSH-1 | offer_hash | Stored offer hash matches current cart | ALLOWED | ALLOWED | yes |
| HSH-2 | offer_hash | Stored offer hash differs from current cart | REAPPROVAL_REQUIRED | REAPPROVAL_REQUIRED | yes |
| MIX-1 | precedence | Hard failure alongside version drift → block wins over re-approval | BLOCKED | BLOCKED | yes |
| MIX-2 | precedence | Two simultaneous hard failures (budget + allergen) | BLOCKED | BLOCKED | yes |
| MIX-3 | multi_item | Multi-item cart, all items compliant | ALLOWED | ALLOWED | yes |
| MIX-4 | multi_item | Multi-item cart, one item violates a rule | BLOCKED | BLOCKED | yes |
| MIX-5 | baseline | No constraints at all, ordinary product | ALLOWED | ALLOWED | yes |

**Exception list: none** — every scenario produced its expected decision.
