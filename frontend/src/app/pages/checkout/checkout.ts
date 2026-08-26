import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {CartService} from '../../services/cart';

// Declare Razorpay global
declare var Razorpay: any;

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="checkout-container glass-panel animate-fade-in">
      <h1 class="text-gradient">Secure Checkout</h1>
      <p class="text-muted">Your cart has been securely locked and verified by the Policy Engine.</p>

      <div class="loader" *ngIf="loading">
        <p>Re-verifying catalog state...</p>
      </div>

      <div *ngIf="orderId" class="order-ready">
        <div class="amount">₹{{ (amountPaise / 100).toFixed(2) }}</div>
        <button class="btn btn-primary pay-btn" (click)="openRazorpay()">Pay with Razorpay</button>
      </div>
    </div>
  `,
  styles: [`
    .checkout-container {
      max-width: 600px;
      margin: 4rem auto;
      padding: 3rem;
      text-align: center;
    }
    .amount {
      font-size: 3rem;
      font-weight: 700;
      color: var(--text-main);
      margin: 2rem 0;
    }
    .pay-btn {
      width: 100%;
      font-size: 1.25rem;
      padding: 1rem;
    }
    .loader { margin: 2rem 0; color: var(--primary-glow); }
  `]
})
export class CheckoutComponent implements OnInit {
  cartId!: string;
  orderId!: string;
  razorpayOrderId!: string;
  amountPaise!: number;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cartService: CartService
  ) {
  }

  ngOnInit() {
    this.cartId = this.route.snapshot.paramMap.get('cartId')!;
    this.createOrder();
  }

  createOrder() {
    this.cartService.createCheckout(this.cartId).subscribe({
      next: (res) => {
        this.orderId = res.orderId;
        this.razorpayOrderId = res.razorpayOrderId;
        this.amountPaise = res.amountPaise;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        alert('Cart re-verification failed. Prices or stock may have changed.');
        this.router.navigate(['/']);
      }
    });
  }

  openRazorpay() {
    const options = {
      key: 'rzp_test_TU9MeiRpMwEZmq',
      amount: this.amountPaise,
      currency: 'INR',
      name: 'NutriBasket',
      description: 'Safe Shopping Purchase',
      order_id: this.razorpayOrderId,
      handler: (response: any) => {
        this.verifyPayment(response);
      },
      modal: {
        ondismiss: () => {
          console.log('Razorpay modal dismissed by user.');
        }
      },
      theme: {color: '#0ea5e9'}
    };

    if (typeof Razorpay !== 'undefined') {
      const rzp = new Razorpay(options);
      rzp.open();
    } else {
      alert('Payment gateway failed to load. Please check your internet connection and refresh the page.');
    }
  }

  verifyPayment(response: any) {
    this.cartService.verifyPayment(response).subscribe({
      next: () => {
        this.router.navigate(['/receipt', this.orderId]);
      },
      error: (err) => {
        console.error('Payment verification failed:', err);
        alert('Payment verification failed. Please contact support with your order ID: ' + this.orderId);
      }
    });
  }
}
