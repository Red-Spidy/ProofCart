# ProofCart: Easy Project Explanation

## What are we building?

We are building **ProofCart**.

It is a shopping assistant that helps an AI buy things safely for a user.

The important idea is simple:

> Before an AI is allowed to pay, it must prove that the product matches exactly what the user asked for.

The AI cannot just guess, change the product or spend extra money without asking the user.

## A simple example

Imagine a user says:

> "Buy vegan snacks under Rs. 900. They should arrive today. I do not want a subscription. I only want items that can be returned."

The AI should not simply find any snack and pay for it.

ProofCart checks:

- Is the product really vegan?
- Is the final price below Rs. 900?
- Is it in stock?
- Can it arrive today?
- Can it be returned?
- Is it a one-time purchase, not a subscription?

Only if all the answers are yes can the AI create a payment checkout.

## Why do we need this?

AI shopping is becoming popular. In the future, people may tell ChatGPT or another AI:

> "Buy me groceries for dinner."

But this can be risky if the AI:

- Chooses the wrong product
- Pays a higher price than expected
- Buys something that cannot be returned
- Chooses a substitute without permission
- Continues even after stock or price changes

ProofCart makes AI shopping safer for both the customer and the merchant.

## The simple flow

```text
User asks the AI to buy something
        ↓
ProofCart understands the user's rules
        ↓
ProofCart checks the merchant's product details
        ↓
ProofCart decides: allowed, ask again, or stop
        ↓
If allowed, create a Razorpay Test Mode payment checkout
        ↓
Save a clear record of what happened
```

## The three decisions

ProofCart will make one of these decisions:

### 1. Allowed

Everything matches the user's rules.

Example: the snack is vegan, costs Rs. 840, is in stock and can arrive today.

The user can move to checkout.

### 2. Ask the user again

Something important changed, but the user may still be okay with it.

Example: the price changed from Rs. 840 to Rs. 890. It is still below the budget, but the user should know before paying.

### 3. Stop the purchase

The product breaks an important user rule.

Example: the only available product contains peanuts even though the user said no peanuts.

ProofCart stops and does not create a payment.

## Important words in easy language

### User rules

This means the things a buyer cares about: budget, product type, delivery time, refund policy and so on.

### Product proof

This means the real information from the merchant's catalog: price, stock, ingredients, delivery promise and return policy.

### Check

The app compares the user rules with the product proof.

### Record

The app saves what the user asked for, what product was chosen, why it was allowed or stopped, and the payment reference if checkout happened.

## What the AI does

The AI helps with language. It can:

- Understand what the user wants
- Turn the request into clear rules
- Search the merchant's product list
- Explain why a product is good, blocked or needs approval
- Suggest safe alternatives

## What the AI is not allowed to do

The AI must never:

- Make up product facts
- Invent a lower price
- Ignore the user's budget
- Change a product without asking
- Create a payment when the checks fail
- Create a subscription when the user said no

This is very important. The AI can help think and explain, but the app's safety checks decide whether payment is allowed.

## What happens if something changes?

This is one of the best parts of the project.

Example:

1. The user selects a cart worth Rs. 840.
2. Before payment, the merchant changes the price to Rs. 960.
3. ProofCart sees that the new price is above the user's Rs. 900 budget.
4. ProofCart stops the payment.
5. It tells the user exactly what changed.
6. The user can choose another product, change the budget or cancel.

So the AI does not make a bad purchase silently.

## What does Razorpay do in our project?

We will use **Razorpay Test Mode**.

Test Mode means we can show a real-looking Razorpay payment flow without using real money.

ProofCart will create a Razorpay test order or payment link only after all the safety checks pass.

We will never put real payment keys, card information or UPI information in the project.

## The store in our demo

For the demo, we will make a small fake wellness/snack store called **NutriBasket**.

This is a good example because customers may care about:

- Vegan or vegetarian food
- Allergies, such as peanuts
- Price limit
- Same-day delivery
- Return policy
- One-time orders versus subscriptions

This makes it easy for people watching the demo to understand why ProofCart matters.

## What will we show in the final demo?

The demo will show:

1. A user writes a shopping request.
2. ProofCart shows the user's rules clearly.
3. ProofCart finds a matching product.
4. ProofCart checks the price, stock, delivery and return policy.
5. A safe cart goes to Razorpay Test Mode checkout.
6. A changed price or wrong product is blocked safely.
7. The app shows a record of every decision.

## Why this is a good Razorpay Buildathon project

Razorpay wants projects where AI can help people pay and shop safely.

ProofCart matches that very well because it:

- Lets an AI help a buyer shop from a merchant
- Uses Razorpay Test Mode for checkout
- Does not let AI spend money without clear rules
- Shows why every payment was allowed
- Stops unsafe payments
- Saves a record for the buyer and merchant

## What we are not building

To keep the project focused, we are **not** building:

- Real-money payments
- A full e-commerce platform like Amazon
- A real bank or UPI app
- A system that changes prices or gives discounts by itself
- A system that uses private customer payment details

We are building one strong thing: **safe AI shopping with proof before payment**.

## The main message

The final message of ProofCart is:

> AI should not be trusted just because it can buy things. It should show what the user asked for, what the merchant offered, why the purchase is safe, and when the user approved it.

## Later, we will create

- The website/app
- The fake NutriBasket product catalog
- The AI shopping assistant
- The safety-check system
- Razorpay Test Mode checkout
- A decision history page
- A clean demo video and project README

For now, this document only explains the project in simple language.
