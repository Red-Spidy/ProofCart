// Test-mode key — meant to be public client-side (Razorpay key_id is not a secret).
// Matches environment.ts so the deployed app stays in test mode, as the track requires.
export const environment = {production: true, razorpayKeyId: 'rzp_test_TU9MeiRpMwEZmq'};
