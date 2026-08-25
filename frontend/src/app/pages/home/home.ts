import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {IntentExtraction, IntentService} from '../../services/intent';
import {CartService} from '../../services/cart';
import {ProductCardComponent} from '../../components/product-card/product-card';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductCardComponent],
  template: `
    <div class="home-container">
      <!-- AI Intent Header -->
      <header class="hero glass-panel animate-fade-in">
        <h1 class="text-gradient">Shop Safely with AI</h1>
        <p>Tell us what you're looking for, and our AI will enforce strict rules to keep your cart safe.</p>

        <div class="intent-box">
          <textarea
            class="input-glass"
            [(ngModel)]="prompt"
            placeholder="e.g., I have $50, strictly no peanuts, and need it by tomorrow..."
            rows="3">
          </textarea>
          <button class="btn btn-primary" (click)="parseIntent()" [disabled]="loading">
            {{ loading ? 'Parsing...' : 'Lock Intent Rules' }}
          </button>
        </div>

        <div *ngIf="intent" class="intent-rules glass-panel mt-4">
          <h3>Locked Rules:</h3>
          <pre>{{ intent.rules | json }}</pre>
          <small class="text-muted">Source: {{ intent.source }}</small>
        </div>
      </header>

      <!-- Mock Catalog -->
      <section class="catalog mt-8 animate-fade-in" style="animation-delay: 0.1s">
        <h2>NutriBasket Catalog</h2>
        <div class="product-grid">
          <app-product-card
            *ngFor="let p of mockProducts"
            [product]="p"
            (addToCart)="onAddToCart($event)">
          </app-product-card>
        </div>
      </section>

      <!-- Sticky Cart -->
      <div class="cart-summary glass-panel animate-fade-in" *ngIf="cart.length > 0">
        <div class="cart-info">
          <h3>Cart ({{cart.length}} items)</h3>
          <p>Total: ₹{{ (cartTotal / 100).toFixed(2) }}</p>
        </div>
        <button class="btn btn-primary" (click)="checkoutCart()" [disabled]="creatingCart">
          {{ creatingCart ? 'Verifying...' : 'Evaluate & Checkout' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .hero { padding: 3rem 2rem; text-align: center; }
    .hero h1 { font-size: 3rem; margin-bottom: 1rem; }
    .intent-box {
      max-width: 600px;
      margin: 2rem auto 0;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .intent-rules {
      max-width: 600px;
      margin: 2rem auto 0;
      padding: 1rem;
      text-align: left;
      background: rgba(14, 165, 233, 0.1);
      border-color: rgba(14, 165, 233, 0.3);
    }
    .intent-rules pre {
      white-space: pre-wrap;
      font-size: 0.9rem;
      color: var(--primary-glow);
    }
    .mt-4 { margin-top: 1rem; }
    .mt-8 { margin-top: 3rem; }
    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 2rem;
      margin-top: 1.5rem;
    }
    .cart-summary {
      position: fixed;
      bottom: 2rem;
      left: 50%;
      transform: translateX(-50%);
      display: flex;
      align-items: center;
      gap: 2rem;
      padding: 1rem 2rem;
      background: rgba(15, 23, 42, 0.9);
      border-color: var(--primary-accent);
      z-index: 100;
      box-shadow: 0 10px 40px rgba(0,0,0,0.5);
    }
    .cart-info h3 { margin: 0; }
    .cart-info p { margin: 0; color: var(--primary-glow); font-weight: 600; }
  `]
})
export class HomeComponent {
  prompt = '';
  loading = false;
  intent: IntentExtraction | null = null;
  creatingCart = false;

  cart: { product: any, quantity: number }[] = [];

  // Mock catalog since we are bypassing the need for a full merchant DB fetch in Phase 3 demo
  mockProducts = [
    {
      id: '10000000-0000-0000-0000-000000000001', // We will replace with real UUID later or let backend map it
      name: 'Vegan Trail Mix',
      description: 'Healthy vegan snack with no peanuts',
      pricePaise: 84000,
      dietaryTags: ['vegan', 'gluten-free'],
      allergens: [],
      deliveryDays: 0
    },
    {
      id: '10000000-0000-0000-0000-000000000002',
      name: 'Keto Peanut Bars',
      description: 'High protein keto bars',
      pricePaise: 120000,
      dietaryTags: ['keto'],
      allergens: ['peanuts'],
      deliveryDays: 1
    },
    {
      id: '10000000-0000-0000-0000-000000000003',
      name: 'Organic Fruit Bites',
      description: 'Sweet and organic',
      pricePaise: 50000,
      dietaryTags: ['organic', 'vegan'],
      allergens: [],
      deliveryDays: 2
    }
  ];

  constructor(
    private intentService: IntentService,
    private cartService: CartService,
    private router: Router
  ) {
  }

  get cartTotal() {
    return this.cart.reduce((sum, item) => sum + (item.product.pricePaise * item.quantity), 0);
  }

  parseIntent() {
    if (!this.prompt) return;
    this.loading = true;
    this.intentService.parseIntent(this.prompt).subscribe({
      next: (res) => {
        this.intent = res;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        alert('Failed to parse intent');
      }
    });
  }

  onAddToCart(event: { product: any, quantity: number }) {
    const existing = this.cart.find(c => c.product.id === event.product.id);
    if (existing) {
      existing.quantity += event.quantity;
    } else {
      this.cart.push({...event});
    }
  }

  checkoutCart() {
    if (this.cart.length === 0) return;
    this.creatingCart = true;

    // For Phase 2 backend, we hardcoded merchantId in the mock DB.
    // Fetch live products would be better, but for demo we pass the mock merchant ID.
    const merchantId = '10000000-0000-0000-0000-000000000001';
    const items = this.cart.map(c => ({
      productId: c.product.id,
      quantity: c.quantity
    }));

    // In a real app we would have saved the intent contract in DB and passed its ID.
    // For this demo, we can just pass null if no intent is saved, or we'd need an endpoint to save intent first.
    // The backend handles intentContractId = null gracefully.

    this.cartService.createProofCart(merchantId, null, items).subscribe({
      next: (res) => {
        this.creatingCart = false;
        this.router.navigate(['/review', res.id]);
      },
      error: (err) => {
        console.error(err);
        this.creatingCart = false;
        alert('Failed to evaluate cart. Check console.');
      }
    });
  }
}
