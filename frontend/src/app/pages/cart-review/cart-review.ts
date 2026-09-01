import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {CartService} from '../../services/cart';
import {PolicyBadgeComponent} from '../../components/policy-badge/policy-badge';
import {IconComponent} from '../../components/icon/icon';

@Component({
  selector: 'app-cart-review',
  standalone: true,
  imports: [CommonModule, PolicyBadgeComponent, IconComponent],
  template: `
    <div class="review-page animate-fade-in" *ngIf="cart">
      <div class="page-header">
        <button class="btn btn-ghost btn-sm" (click)="goBack()">
          <app-icon name="arrow-left" [size]="15"></app-icon> Back
        </button>
        <h1>Cart Review</h1>
      </div>

      <div class="cart-layout">
        <div class="cart-main">
          <h3>Items in this cart</h3>
          <div class="items-list">
            <div class="cart-item panel" *ngFor="let item of cart.items">
              <div class="item-info">
                <strong>{{ item.snapshot.name }}</strong>
                <span class="item-desc">{{ item.snapshot.description }}</span>
                <div class="tags">
                  <span class="badge badge-info" *ngFor="let tag of item.snapshot.dietaryTags">{{ tag }}</span>
                  <span class="badge badge-warning" *ngFor="let a of item.snapshot.allergens">
                    <app-icon name="alert-triangle" [size]="11"></app-icon>{{ a }}
                  </span>
                </div>
              </div>
              <div class="item-price">
                <span class="text-muted">{{ item.quantity }}×</span>
                <strong>₹{{ (item.lineTotalPaise / 100) }}</strong>
              </div>
            </div>
          </div>

          <div class="upsell-section" *ngIf="suggestions.length">
            <h3>You might also like</h3>
            <div class="upsell-list">
              <div class="upsell-card panel" *ngFor="let s of suggestions">
                <div class="upsell-info">
                  <strong>{{ s.name }}</strong>
                  <span class="upsell-reason">{{ s.reason }}</span>
                  <div class="tags" *ngIf="s.dietaryTags?.length">
                    <span class="badge badge-info" *ngFor="let tag of s.dietaryTags">{{ tag }}</span>
                  </div>
                </div>
                <div class="upsell-action">
                  <strong>₹{{ (s.pricePaise / 100) }}</strong>
                  <button class="btn btn-secondary btn-sm" (click)="addSuggestion(s)" [disabled]="addingId === s.productId">
                    {{ addingId === s.productId ? 'Adding…' : '+ Add' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="cart-sidebar">
          <div class="decision-card panel" [class.allowed]="cart.policyResult.decision === 'ALLOWED'"
                                             [class.blocked]="cart.policyResult.decision !== 'ALLOWED'">
            <div class="decision-icon">
              <app-icon [name]="cart.policyResult.decision === 'ALLOWED' ? 'check' : 'x'" [size]="20" [strokeWidth]="2.5"></app-icon>
            </div>
            <div class="decision-info">
              <h2>{{ cart.policyResult.decision }}</h2>
              <p>{{ cart.policyResult.summary || cart.policyResult.explanation }}</p>
            </div>
          </div>

          <div class="hash-bar">
            <app-icon name="lock" [size]="14"></app-icon>
            <span class="hash-label">Offer hash</span>
            <code class="hash-value">{{ cart.offerHash }}</code>
          </div>

          <div class="checks-section">
            <h3>Policy checks ({{ cart.policyResult.checks?.length || 0 }})</h3>
            <div class="checks-list">
              <app-policy-badge *ngFor="let check of cart.policyResult.checks" [check]="check"></app-policy-badge>
            </div>
          </div>
        </div>
      </div>

      <div class="action-bar panel">
        <span class="text-secondary">Ready to proceed?</span>
        <button
          class="btn btn-primary"
          *ngIf="cart.policyResult.decision === 'ALLOWED'"
          (click)="approveAndProceed()"
          [disabled]="loading">
          {{ loading ? 'Processing…' : 'Approve & Pay' }}
        </button>
        <span *ngIf="cart.policyResult.decision !== 'ALLOWED'" class="badge badge-danger">Checkout blocked</span>
      </div>
    </div>

    <div class="state-panel" *ngIf="!cart">
      <app-icon name="loader" [size]="24" class="spin"></app-icon>
      <p>Loading cart review…</p>
    </div>
  `,
  styles: [`
    .review-page { max-width: 1000px; margin: 0 auto; padding-bottom: 4rem; }

    .page-header { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.75rem; }
    .page-header h1 { font-size: 1.5rem; }

    .cart-layout { display: grid; grid-template-columns: 1fr 380px; gap: 2rem; align-items: start; }

    .cart-main h3 { font-size: 1rem; margin-bottom: 0.875rem; color: var(--text-secondary); }

    .items-list { display: flex; flex-direction: column; gap: 0.75rem; }

    .cart-item { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.125rem; }

    .item-info { display: flex; flex-direction: column; gap: 0.375rem; }
    .item-info strong { font-size: 1rem; color: var(--text-primary); }
    .item-desc { font-size: 0.8125rem; color: var(--text-muted); }
    .tags { display: flex; flex-wrap: wrap; gap: 0.375rem; margin-top: 0.125rem; }

    .item-price { text-align: right; }
    .item-price span { display: block; font-size: 0.8125rem; }
    .item-price strong { display: block; font-size: 1.0625rem; color: var(--text-primary); }

    /* ── Upsell ──────── */
    .upsell-section { margin-top: 2rem; }
    .upsell-section h3 { font-size: 1rem; margin-bottom: 0.875rem; color: var(--text-secondary); }
    .upsell-list { display: flex; flex-direction: column; gap: 0.625rem; }
    .upsell-card { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 0.875rem 1.125rem; }
    .upsell-info { display: flex; flex-direction: column; gap: 0.25rem; }
    .upsell-info strong { font-size: 0.9375rem; color: var(--text-primary); }
    .upsell-reason { font-size: 0.8125rem; color: var(--text-muted); }
    .upsell-action { display: flex; flex-direction: column; align-items: flex-end; gap: 0.5rem; flex-shrink: 0; }
    .upsell-action strong { font-size: 0.9375rem; color: var(--text-primary); }

    /* ── Sidebar ── */
    .decision-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1.25rem;
      margin-bottom: 1rem;
    }

    .decision-card.allowed { border-color: var(--success-border); background: var(--success-tint); }
    .decision-card.blocked { border-color: var(--danger-border); background: var(--danger-tint); }

    .decision-icon {
      width: 40px;
      height: 40px;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: var(--surface);
    }

    .allowed .decision-icon { color: var(--success); }
    .blocked .decision-icon { color: var(--danger); }

    .decision-info h2 { font-size: 1.125rem; margin-bottom: 0.125rem; }
    .allowed .decision-info h2 { color: var(--success); }
    .blocked .decision-info h2 { color: var(--danger); }
    .decision-info p { color: var(--text-secondary); font-size: 0.875rem; }

    .hash-bar {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.625rem 0.875rem;
      border-radius: var(--radius-md);
      background: var(--surface-2);
      border: 1px solid var(--border);
      margin-bottom: 1.5rem;
      color: var(--text-muted);
    }

    .hash-label { font-size: 0.75rem; font-weight: 600; color: var(--text-muted); flex-shrink: 0; }

    .hash-value {
      font-size: 0.75rem;
      color: var(--text-secondary);
      font-family: 'SF Mono', 'Fira Code', monospace;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .checks-section h3 { font-size: 0.9375rem; margin-bottom: 0.75rem; color: var(--text-secondary); }
    .checks-list { display: flex; flex-direction: column; gap: 0.5rem; }

    .action-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.25rem;
      margin-top: 2rem;
    }

    @media (max-width: 800px) {
      .cart-layout { grid-template-columns: 1fr; }
    }
  `]
})
export class CartReviewComponent implements OnInit {
  cartId!: string;
  cart: any = null;
  loading = false;
  loadingCart = false;
  suggestions: any[] = [];
  addingId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cartService: CartService
  ) {}

  ngOnInit() {
    this.cartId = this.route.snapshot.paramMap.get('cartId')!;
    const nav = this.router.getCurrentNavigation();
    this.cart = nav?.extras?.state?.['cart'] || history.state?.cart;

    if (!this.cart) {
      this.loadingCart = true;
      this.cartService.getCart(this.cartId).subscribe({
        next: (cartData) => {
          this.cart = cartData;
          this.loadingCart = false;
          this.loadUpsell();
        },
        error: () => {
          this.loadingCart = false;
          this.router.navigate(['/']);
        }
      });
    } else {
      this.loadUpsell();
    }
  }

  loadUpsell() {
    if (!this.cart || this.cart.policyResult?.decision !== 'ALLOWED') {
      this.suggestions = [];
      return;
    }
    this.cartService.getUpsellSuggestions(this.cartId).subscribe({
      next: (res) => (this.suggestions = res.suggestions || []),
      error: () => (this.suggestions = [])
    });
  }

  addSuggestion(suggestion: any) {
    this.addingId = suggestion.productId;
    this.cartService.addCartItem(this.cartId, suggestion.productId).subscribe({
      next: (updated) => {
        this.cart = {...this.cart, items: updated.items, policyResult: updated.policyResult, offerHash: updated.offerHash};
        this.addingId = null;
        this.loadUpsell();
      },
      error: (err) => {
        console.error('Add suggestion error:', err);
        this.addingId = null;
      }
    });
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
