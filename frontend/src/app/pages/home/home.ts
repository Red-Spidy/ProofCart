import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {IntentExtraction, IntentService} from '../../services/intent';
import {CartService} from '../../services/cart';
import {ProductCardComponent} from '../../components/product-card/product-card';
import {IconComponent} from '../../components/icon/icon';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductCardComponent, IconComponent],
  template: `
    <div class="page">
      <!-- Hero -->
      <section class="hero animate-fade-in">
        <div class="hero-badge">
          <app-icon name="sparkles" [size]="13"></app-icon> AI-powered, policy-gated shopping
        </div>
        <h1 class="hero-title">Tell us what you need.<br/>The policy engine decides what's safe to buy.</h1>
        <p class="hero-subtitle">
          Describe your budget, allergies, and dietary needs. The agent selects matching products —
          every rule is checked before a payment can ever be created.
        </p>

        <div class="intent-box">
          <textarea
            class="input"
            [(ngModel)]="prompt"
            placeholder="e.g. I want vegan snacks under ₹900, no peanuts, deliver today…"
            rows="3"
            [disabled]="loading">
          </textarea>
          <button class="btn btn-primary intent-btn" (click)="goShopping()" [disabled]="loading || !prompt.trim()">
            <ng-container *ngIf="!loading">
              <app-icon name="cart" [size]="16"></app-icon> Build My Cart
            </ng-container>
            <span *ngIf="loading">{{ loadingStep }}</span>
          </button>
        </div>
      </section>

      <!-- Loading State -->
      <section class="loading-section animate-fade-in" *ngIf="loading">
        <div class="step-list">
          <div class="step" [class.done]="stepsDone >= 1" [class.active]="stepsDone === 0">
            <app-icon [name]="stepsDone >= 1 ? 'check' : 'loader'" [size]="15" [class.spin]="stepsDone < 1"></app-icon>
            <span>Parsing your intent with AI</span>
          </div>
          <div class="step" [class.done]="stepsDone >= 2" [class.active]="stepsDone === 1">
            <app-icon [name]="stepsDone >= 2 ? 'check' : 'loader'" [size]="15" [class.spin]="stepsDone === 1"></app-icon>
            <span>Selecting matching products</span>
          </div>
          <div class="step" [class.done]="stepsDone >= 3" [class.active]="stepsDone === 2">
            <app-icon [name]="stepsDone >= 3 ? 'check' : 'loader'" [size]="15" [class.spin]="stepsDone === 2"></app-icon>
            <span>Running policy engine checks</span>
          </div>
        </div>
      </section>

      <!-- Error State -->
      <section class="error-section animate-fade-in" *ngIf="errorMessage">
        <div class="form-note danger error-card">
          <app-icon name="alert-triangle" [size]="18"></app-icon>
          <div class="error-body">
            <strong>Something went wrong</strong>
            <p>{{ errorMessage }}</p>
          </div>
          <button class="btn btn-secondary btn-sm" (click)="errorMessage = ''">Dismiss</button>
        </div>
      </section>

      <!-- How It Works -->
      <section class="how-it-works animate-fade-in" *ngIf="!loading && !intent">
        <h2>How it works</h2>
        <div class="steps-grid">
          <div class="hiw-card card">
            <div class="hiw-icon"><app-icon name="user" [size]="18"></app-icon></div>
            <h3>1. Describe</h3>
            <p>Tell the agent your budget, dietary needs, and allergen restrictions in plain language.</p>
          </div>
          <div class="hiw-card card">
            <div class="hiw-icon"><app-icon name="sparkles" [size]="18"></app-icon></div>
            <h3>2. Agent selects</h3>
            <p>The engine automatically picks products that match every constraint you set.</p>
          </div>
          <div class="hiw-card card">
            <div class="hiw-icon"><app-icon name="shield-check" [size]="18"></app-icon></div>
            <h3>3. Review & pay</h3>
            <p>See every policy check — allergens, budget, delivery — before approving checkout.</p>
          </div>
        </div>
      </section>

      <!-- Catalog -->
      <section class="catalog-section animate-fade-in" *ngIf="!loading">
        <div class="recommendations" *ngIf="recommendations.length && !catalogSearch">
          <h2>Picked for you</h2>
          <p class="text-secondary text-sm">Learns from your searches and feedback. Only you influence these suggestions.</p>
          <div class="product-grid">
            <div *ngFor="let p of recommendations" class="recommendation-card">
              <span class="recommendation-reason">{{ p.reason }}</span>
              <app-product-card [product]="p" (feedback)="recordFeedback($event)"></app-product-card>
            </div>
          </div>
        </div>

        <div class="catalog-header">
          <h2>Catalog</h2>
          <div class="catalog-search">
            <input
              type="text"
              class="input catalog-search-input"
              placeholder="Search products (e.g. oats, almond milk)…"
              [(ngModel)]="catalogSearch"
              (keyup.enter)="searchCatalog()"
            />
            <button class="btn btn-secondary btn-sm" (click)="searchCatalog()" [disabled]="catalogSearching">
              <app-icon name="search" [size]="14"></app-icon> Search
            </button>
            <button *ngIf="catalogSearch" class="btn btn-ghost btn-sm" (click)="clearSearch()">
              <app-icon name="x" [size]="14"></app-icon> Clear
            </button>
          </div>
        </div>
        <p *ngIf="catalogSearch && !catalogSearching" class="text-secondary text-sm search-note">
          Showing results for "{{ catalogSearch }}" from Open Food Facts
        </p>
        <div class="product-grid">
          <app-product-card
            *ngFor="let p of catalog; let i = index"
            [product]="p"
            class="animate-fade-in"
            [style.animation-delay.ms]="i * 60"
            (feedback)="recordFeedback($event)">
          </app-product-card>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .page { padding-bottom: 4rem; }
    .recommendations { margin: 1.5rem 0 3rem; }
    .recommendations h2 { margin-bottom: 0.35rem; }
    .recommendations .product-grid { row-gap: 2.25rem; }
    .recommendation-card { display: flex; flex-direction: column; gap: 0.5rem; }
    .recommendation-card app-product-card { display: block; height: 100%; }
    .recommendation-reason {
      display: block;
      margin: 0;
      color: var(--accent);
      font-size: 0.6875rem;
      font-weight: 600;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      line-height: 1.3;
    }

    /* ── Hero ──── */
    .hero { text-align: center; padding: 2.5rem 0 2rem; }

    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.4375rem;
      padding: 0.375rem 0.875rem;
      border-radius: var(--radius-full);
      background: var(--accent-tint);
      border: 1px solid var(--accent-border);
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--accent);
      margin-bottom: 1.5rem;
    }

    .hero-title {
      font-size: 2.25rem;
      font-weight: 700;
      letter-spacing: -0.03em;
      line-height: 1.2;
      margin-bottom: 1rem;
    }

    .hero-subtitle {
      color: var(--text-secondary);
      font-size: 1.0625rem;
      max-width: 560px;
      margin: 0 auto 2.25rem;
      line-height: 1.6;
    }

    .intent-box { max-width: 600px; margin: 0 auto; display: flex; flex-direction: column; gap: 0.75rem; }
    .intent-btn { align-self: flex-end; padding: 0.75rem 1.75rem; font-size: 0.9375rem; }

    /* ── Loading ── */
    .loading-section { max-width: 460px; margin: 2rem auto 0; }
    .step-list { display: flex; flex-direction: column; gap: 0.625rem; }

    .step {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      background: var(--surface);
      border: 1px solid var(--border);
      color: var(--text-muted);
      font-size: 0.9375rem;
    }

    .step.active { border-color: var(--accent-border); background: var(--accent-tint); color: var(--text-primary); }
    .step.done { border-color: var(--success-border); background: var(--success-tint); color: var(--success); }

    /* ── Error ──── */
    .error-section { max-width: 600px; margin: 2rem auto 0; }
    .error-card { align-items: center; }
    .error-body { flex: 1; }
    .error-body p { margin-top: 0.125rem; }

    /* ── Catalog ── */
    .catalog-section { margin-top: 3.5rem; max-width: 1000px; margin-left: auto; margin-right: auto; }

    .catalog-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 0.75rem;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .catalog-header h2 { font-size: 1.5rem; }
    .search-note { margin-bottom: 1.25rem; }

    .catalog-search { display: flex; gap: 0.5rem; align-items: center; }
    .catalog-search-input { padding: 0.5rem 0.875rem; font-size: 0.875rem; min-width: 260px; }

    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1.25rem;
      margin-top: 1.5rem;
    }

    /* ── How It Works ── */
    .how-it-works { margin-top: 3.5rem; }
    .how-it-works h2 { text-align: center; margin-bottom: 1.75rem; font-size: 1.375rem; }
    .steps-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.25rem; }
    .hiw-card { padding: 1.5rem; text-align: center; }

    .hiw-icon {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--radius-md);
      background: var(--accent-tint);
      color: var(--accent);
      margin: 0 auto 1rem;
    }

    .hiw-card h3 { font-size: 0.9375rem; margin-bottom: 0.5rem; }
    .hiw-card p { font-size: 0.8125rem; color: var(--text-muted); line-height: 1.5; }

    @media (max-width: 640px) {
      .hero-title { font-size: 1.75rem; }
      .steps-grid { grid-template-columns: 1fr; }
    }
  `]
})
export class HomeComponent implements OnInit {
  prompt = '';
  loading = false;
  loadingStep = '';
  stepsDone = 0;
  intent: IntentExtraction | null = null;
  errorMessage = '';
  catalogLoading = true;
  catalogSearch = '';
  catalogSearching = false;
  recommendations: any[] = [];

  // Full catalog — loaded from the API. The AI will filter this based on parsed rules.
  catalog: any[] = [];

  private readonly MERCHANT_ID = '10000000-0000-0000-0000-000000000001';

  constructor(
    private intentService: IntentService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit() {
    this.cartService.getCatalog(this.MERCHANT_ID).subscribe({
      next: (res) => {
        this.catalog = res.products || [];
        this.catalogLoading = false;
      },
      error: (err) => {
        console.error('Failed to load catalog:', err);
        this.catalogLoading = false;
      }
    });
    this.cartService.getRecommendations(this.MERCHANT_ID).subscribe({
      next: (res) => this.recommendations = res.products || [],
      error: () => this.recommendations = []
    });
  }

  searchCatalog() {
    if (!this.catalogSearch.trim()) {
      this.ngOnInit();
      return;
    }
    this.catalogSearching = true;
    this.cartService.getCatalog(this.MERCHANT_ID, this.catalogSearch).subscribe({
      next: (res) => {
        this.catalog = res.products || [];
        this.catalogSearching = false;
        this.cartService.recordPreference('SEARCH', undefined, this.catalogSearch.trim()).subscribe();
      },
      error: (err) => {
        console.error('Catalog search error:', err);
        this.catalogSearching = false;
      }
    });
  }

  clearSearch() {
    this.catalogSearch = '';
    this.ngOnInit();
  }

  recordFeedback(event: { product: any, liked: boolean }) {
    this.cartService.recordPreference(event.liked ? 'LIKE' : 'DISLIKE', event.product.id).subscribe({
      next: () => this.refreshRecommendations(),
      error: (err) => console.error('Could not save feedback:', err)
    });
  }

  private refreshRecommendations() {
    this.cartService.getRecommendations(this.MERCHANT_ID).subscribe({
      next: (res) => this.recommendations = res.products || []
    });
  }


  goShopping() {
    if (!this.prompt.trim()) return;
    this.loading = true;
    this.errorMessage = '';
    this.stepsDone = 0;
    this.loadingStep = 'Step 1: Parsing intent…';

    // Step 1: Parse intent (saves to DB, returns intentId)
    this.intentService.parseIntent(this.prompt).subscribe({
      next: (intentResult) => {
        this.intent = intentResult;
        this.stepsDone = 1;
        this.loadingStep = 'Step 2: Selecting products…';

        // Step 2: AI auto-selects products based on rules
        const selectedItems = this.autoSelectProducts(intentResult.rules);

        if (selectedItems.length === 0) {
          this.loading = false;
          this.errorMessage = 'No products matched your constraints. Try adjusting your budget or dietary requirements.';
          return;
        }

        this.stepsDone = 2;
        this.loadingStep = 'Step 3: Running policy checks…';

        // Step 3: Create proof cart with the real intentId
        this.cartService.createProofCart(this.MERCHANT_ID, intentResult.intentId, selectedItems).subscribe({
          next: (cartResult) => {
            this.stepsDone = 3;
            this.loading = false;

            // Ensure items array is attached for the review page
            const itemsWithDetails = selectedItems.map(item => {
              const product = this.catalog.find(p => p.id === item.productId)!;
              return {
                productId: item.productId,
                quantity: item.quantity,
                lineTotalPaise: product ? product.pricePaise * item.quantity : 0,
                snapshot: product || {}
              };
            });

            const enrichedCart = {
              ...cartResult,
              items: (cartResult.items && cartResult.items.length > 0) ? cartResult.items : itemsWithDetails
            };

            // Navigate to review page with cart data
            this.router.navigate(['/review', cartResult.id], {
              state: { cart: enrichedCart, intent: intentResult }
            });
          },
          error: (err) => {
            console.error('Cart creation error:', err);
            this.loading = false;
            this.errorMessage = 'Failed to create cart. Please try again.';
          }
        });
      },
      error: (err) => {
        console.error('Intent parse error:', err);
        this.loading = false;
        this.errorMessage = 'Failed to parse your request. Please try again.';
      }
    });
  }

  /**
   * AI-based product selection:
   * 1. Exclude products containing any excluded allergens
   * 2. Filter by required dietary tags (if any)
   * 3. Filter by delivery requirement
   * 4. Fit within budget by greedily picking products
   */
  private autoSelectProducts(rules: any): { productId: string, quantity: number }[] {
    let candidates = [...this.catalog];

    // Filter out allergens
    if (rules.excludedAllergens && rules.excludedAllergens.length > 0) {
      candidates = candidates.filter(p =>
        !p.allergens.some((a: string) =>
          rules.excludedAllergens.some((excluded: string) =>
            a.toLowerCase().includes(excluded.toLowerCase())
          )
        )
      );
    }

    // Filter by dietary tags (product must contain ALL required tags)
    if (rules.mustHaveTags && rules.mustHaveTags.length > 0) {
      candidates = candidates.filter(p =>
        rules.mustHaveTags.every((tag: string) =>
          p.dietaryTags.some((dt: string) => dt.toLowerCase().includes(tag.toLowerCase()))
        )
      );
    }

    // Filter by delivery
    if (rules.deliveryRequirement != null) {
      // deliveryRequirement can be a string ("today", "tomorrow") or a number
      const req = String(rules.deliveryRequirement).toLowerCase();
      if (req.includes('today') || req.includes('same day') || req === '0') {
        candidates = candidates.filter((p: any) => p.deliveryDays === 0);
      } else if (req.includes('tomorrow') || req.includes('next day') || req === '1') {
        candidates = candidates.filter((p: any) => p.deliveryDays <= 1);
      }
    }

    // Select products within budget (greedy: pick as many as possible)
    const budget = rules.maxTotalPaise || Infinity;
    const selected: { productId: string, quantity: number }[] = [];
    let spent = 0;

    // Sort by price ascending so we can fit more items
    candidates.sort((a, b) => a.pricePaise - b.pricePaise);

    for (const product of candidates) {
      if (spent + product.pricePaise <= budget) {
        selected.push({ productId: product.id, quantity: 1 });
        spent += product.pricePaise;
      }
    }

    return selected;
  }
}
