import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {CartService} from '../../services/cart';
import {PolicyBadgeComponent} from '../../components/policy-badge/policy-badge';

@Component({
  selector: 'app-cart-review',
  standalone: true,
  imports: [CommonModule, PolicyBadgeComponent],
  template: `
    <div class="review-container animate-fade-in" *ngIf="cart">
      <h1 class="text-gradient">Cart Policy Review</h1>

      <div class="decision-banner glass-panel" [ngClass]="cart.policyResult.decision.toLowerCase()">
        <h2>Decision: {{ cart.policyResult.decision }}</h2>
        <p *ngIf="cart.policyResult.decision === 'ALLOWED'">All safety checks passed. You may proceed to checkout.</p>
        <p *ngIf="cart.policyResult.decision === 'BLOCKED'">Cart violates strict rules. Checkout disabled.</p>
      </div>

      <div class="checks-grid mt-4">
        <app-policy-badge *ngFor="let check of cart.policyResult.checks" [check]="check"></app-policy-badge>
      </div>

      <div class="actions glass-panel mt-4">
        <button class="btn btn-glass" (click)="goBack()">Back to Shop</button>
        <button
          class="btn btn-primary"
          *ngIf="cart.policyResult.decision === 'ALLOWED'"
          (click)="approveAndProceed()"
          [disabled]="loading">
          {{ loading ? 'Processing...' : 'Approve & Pay' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .review-container { max-width: 800px; margin: 0 auto; }
    .decision-banner {
      padding: 1.5rem;
      text-align: center;
      margin-top: 2rem;
      border-width: 2px;
    }
    .allowed { border-color: var(--success-glow); background: var(--success-bg); color: var(--success-glow); }
    .blocked { border-color: var(--danger-glow); background: var(--danger-bg); color: var(--danger-glow); }
    .checks-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 1rem;
    }
    .mt-4 { margin-top: 2rem; }
    .actions {
      display: flex;
      justify-content: space-between;
      padding: 1.5rem;
    }
  `]
})
export class CartReviewComponent implements OnInit {
  cartId!: string;
  cart: any = null;
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cartService: CartService
  ) {
  }

  ngOnInit() {
    this.cartId = this.route.snapshot.paramMap.get('cartId')!;
    // In a real app we'd fetch the cart by ID. Since we didn't write a GET /proof-carts/:id
    // endpoint in Phase 2, we will just use the state passed from Router or mock it.
    // For now, let's assume we can fetch it, or we rely on the backend.
    // Wait, the backend returns the full `policyResult` on POST. We can pass it via router state.
    this.cart = history.state.cart || JSON.parse(localStorage.getItem('tempCart') || '{}');
  }

  goBack() {
    this.router.navigate(['/']);
  }

  approveAndProceed() {
    this.loading = true;
    this.cartService.approveCart(this.cartId).subscribe({
      next: () => {
        this.router.navigate(['/checkout', this.cartId]);
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        alert('Failed to approve cart');
      }
    });
  }
}
