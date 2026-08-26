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
    <div class="review-page animate-fade-in" *ngIf="cart">
      <!-- Header -->
      <div class="page-header">
        <button class="btn btn-secondary btn-sm" (click)="goBack()">← Back</button>
        <h1>Cart <span class="text-gradient">Review</span></h1>
      </div>

      <div class="cart-layout">
        <!-- Left Column: Items & AI Rules -->
        <div class="cart-main">
          <h3>🛒 Items Added by AI</h3>
          <div class="items-list">
            <div class="cart-item glass-panel" *ngFor="let item of cart.items">
              <div class="item-info">
                <strong>{{ item.snapshot.name }}</strong>
                <span class="item-desc">{{ item.snapshot.description }}</span>
                <div class="tags">
                  <span class="badge badge-info" *ngFor="let tag of item.snapshot.dietaryTags">{{ tag }}</span>
                  <span class="badge badge-warning" *ngFor="let a of item.snapshot.allergens">⚠ {{ a }}</span>
                </div>
              </div>
              <div class="item-price">
                <span>{{ item.quantity }}x</span>
                <strong>₹{{ (item.lineTotalPaise / 100) }}</strong>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Column: Policy & Decision -->
        <div class="cart-sidebar">
          <!-- Decision Banner -->
          <div class="decision-card glass-panel" [class.allowed]="cart.policyResult.decision === 'ALLOWED'"
                                                  [class.blocked]="cart.policyResult.decision !== 'ALLOWED'">
            <div class="decision-icon">
              {{ cart.policyResult.decision === 'ALLOWED' ? '✅' : '🚫' }}
            </div>
            <div class="decision-info">
              <h2>{{ cart.policyResult.decision }}</h2>
              <p>{{ cart.policyResult.summary || cart.policyResult.explanation }}</p>
            </div>
          </div>

          <!-- Offer Hash -->
          <div class="hash-bar">
            <span class="hash-label">🔐 Offer Hash</span>
            <code class="hash-value">{{ cart.offerHash }}</code>
          </div>

          <!-- Policy Checks -->
          <div class="checks-section">
            <h3>Policy Checks ({{ cart.policyResult.checks?.length || 0 }})</h3>
            <div class="checks-list">
              <app-policy-badge *ngFor="let check of cart.policyResult.checks" [check]="check"></app-policy-badge>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Bar -->
      <div class="action-bar glass-panel">
        <div class="action-info">
          <span class="text-secondary">Ready to proceed?</span>
        </div>
        <button
          class="btn btn-primary"
          *ngIf="cart.policyResult.decision === 'ALLOWED'"
          (click)="approveAndProceed()"
          [disabled]="loading">
          {{ loading ? 'Processing...' : '✓ Approve & Pay' }}
        </button>
        <span *ngIf="cart.policyResult.decision !== 'ALLOWED'" class="badge badge-danger">
          Checkout Blocked
        </span>
      </div>
    </div>

    <!-- Loading State -->
    <div class="loading-state" *ngIf="!cart">
      <p class="text-muted">Loading cart review...</p>
    </div>
  `,
  styles: [`
    .review-page {
      max-width: 1000px;
      margin: 0 auto;
      padding-bottom: 4rem;
    }

    .page-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .page-header h1 {
      font-size: 1.75rem;
    }

    .cart-layout {
      display: grid;
      grid-template-columns: 1fr 400px;
      gap: 2rem;
    }

    /* ── Left Column (Items) ── */
    .cart-main h3 {
      font-size: 1.25rem;
      margin-bottom: 1rem;
      color: var(--text-secondary);
    }

    .items-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .cart-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.25rem;
    }

    .item-info {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
    }

    .item-info strong {
      font-size: 1.125rem;
      color: var(--text-primary);
    }

    .item-desc {
      font-size: 0.875rem;
      color: var(--text-muted);
    }

    .tags {
      display: flex;
      gap: 0.5rem;
      margin-top: 0.25rem;
    }

    .item-price {
      text-align: right;
    }

    .item-price span {
      display: block;
      font-size: 0.875rem;
      color: var(--text-muted);
    }

    .item-price strong {
      display: block;
      font-size: 1.25rem;
      color: var(--accent-blue-light);
    }

    /* ── Right Column (Sidebar) ── */
    .decision-card {
      display: flex;
      align-items: center;
      gap: 1.25rem;
      padding: 1.5rem;
      margin-bottom: 1rem;
      border-width: 1px;
    }

    .decision-card.allowed {
      border-color: rgba(16, 185, 129, 0.3);
      background: rgba(16, 185, 129, 0.06);
    }

    .decision-card.blocked {
      border-color: rgba(244, 63, 94, 0.3);
      background: rgba(244, 63, 94, 0.06);
    }

    .decision-icon {
      font-size: 2.5rem;
      flex-shrink: 0;
    }

    .decision-info h2 {
      font-size: 1.5rem;
      margin-bottom: 0.25rem;
    }

    .allowed .decision-info h2 { color: var(--accent-emerald); }
    .blocked .decision-info h2 { color: var(--accent-rose); }

    .decision-info p {
      color: var(--text-secondary);
      font-size: 0.9375rem;
    }

    /* ── Hash ────────── */
    .hash-bar {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-subtle);
      margin-bottom: 2rem;
    }

    .hash-label {
      font-size: 0.75rem;
      font-weight: 700;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      flex-shrink: 0;
    }

    .hash-value {
      font-size: 0.75rem;
      color: var(--accent-blue-light);
      font-family: 'SF Mono', 'Fira Code', monospace;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    /* ── Checks ──────── */
    .checks-section {
      margin-bottom: 2rem;
    }

    .checks-section h3 {
      font-size: 1rem;
      margin-bottom: 0.75rem;
      color: var(--text-secondary);
    }

    .checks-list {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    /* ── Action Bar ──── */
    .action-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.25rem;
      margin-top: 2rem;
    }

    .loading-state {
      text-align: center;
      padding: 4rem 2rem;
    }
    
    @media (max-width: 800px) {
      .cart-layout {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CartReviewComponent implements OnInit {
  cartId!: string;
  cart: any = null;
  loading = false;
  loadingCart = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cartService: CartService
  ) {}

  ngOnInit() {
    this.cartId = this.route.snapshot.paramMap.get('cartId')!;
    // Get cart data from router navigation state
    const nav = this.router.getCurrentNavigation();
    this.cart = nav?.extras?.state?.['cart'] || history.state?.cart;

    if (!this.cart) {
      // State lost (page refresh) — reload from backend instead of redirecting away
      this.loadingCart = true;
      this.cartService.getCart(this.cartId).subscribe({
        next: (cartData) => {
          this.cart = cartData;
          this.loadingCart = false;
        },
        error: () => {
          this.loadingCart = false;
          this.router.navigate(['/']);
        }
      });
    }
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
        console.error('Approve error:', err);
        this.loading = false;
      }
    });
  }
}
