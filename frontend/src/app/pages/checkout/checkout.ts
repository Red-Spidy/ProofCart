import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {CartService} from '../../services/cart';
import {environment} from '../../../environments/environment';
import {IconComponent} from '../../components/icon/icon';

// Declare Razorpay global
declare var Razorpay: any;

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="checkout-container panel animate-fade-in">
      <div class="lock-badge"><app-icon name="lock" [size]="18"></app-icon></div>
      <h1>Secure Checkout</h1>
      <p class="text-muted">This cart has been re-verified against the live catalog and policy engine.</p>

      <div class="loader" *ngIf="loading">
        <app-icon name="loader" [size]="18" class="spin"></app-icon>
        <span>Re-verifying catalog state…</span>
      </div>

      <div *ngIf="orderId" class="order-ready">
        <div class="amount">₹{{ (amountPaise / 100).toFixed(2) }}</div>
        <button class="btn btn-primary pay-btn" (click)="openRazorpay()">Pay with Razorpay</button>
        <p class="test-note">Razorpay test mode — no real payment will be collected.</p>
      </div>
    </div>
  `,
  styles: [`
    .checkout-container {
      max-width: 480px;
      margin: 4rem auto;
      padding: 2.5rem;
      text-align: center;
    }
    .lock-badge {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: var(--accent-tint);
      color: var(--accent);
      margin: 0 auto 1.25rem;
    }
    h1 { font-size: 1.375rem; margin-bottom: 0.375rem; }
    .amount {
      font-size: 2.5rem;
      font-weight: 700;
      letter-spacing: -0.02em;
      color: var(--text-primary);
      margin: 1.75rem 0 1.25rem;
    }
    .pay-btn { width: 100%; font-size: 1rem; padding: 0.875rem; }
    .test-note { margin-top: 0.875rem; font-size: 0.75rem; color: var(--text-muted); }
    .loader {
      margin: 1.75rem 0 0;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      color: var(--text-secondary);
      font-size: 0.875rem;
    }
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
      key: environment.razorpayKeyId,
      amount: this.amountPaise,
      currency: 'INR',
      name: 'ProofCart',
      description: 'Policy-verified purchase',
      order_id: this.razorpayOrderId,
      handler: (response: any) => {
        this.verifyPayment(response);
      },
      modal: {
        ondismiss: () => {
          console.log('Razorpay modal dismissed by user.');
        }
      },
      theme: {color: '#4338ca'}
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
