import {Component, OnInit} from '@angular/core';
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
    <div class="page">
      <!-- Hero -->
      <section class="hero animate-fade-in">
        <div class="hero-badge">🤖 AI-Powered Safe Shopping</div>
        <h1 class="hero-title">
          Tell us what you need.<br/>
          <span class="text-gradient">AI picks it for you.</span>
        </h1>
        <p class="hero-subtitle">
          Describe your budget, allergies, and dietary needs. Our AI will automatically select
          the best matching products and enforce every rule before checkout.
        </p>

        <div class="intent-box">
          <textarea
            class="input-glass"
            [(ngModel)]="prompt"
            placeholder="e.g. I want vegan snacks under ₹900, no peanuts, deliver today..."
            rows="3"
            [disabled]="loading">
          </textarea>
          <button class="btn btn-primary intent-btn" (click)="goShopping()" [disabled]="loading || !prompt.trim()">
            <span *ngIf="!loading">🛒 Build My Cart</span>
            <span *ngIf="loading">{{ loadingStep }}</span>
          </button>
        </div>
      </section>

      <!-- Loading State -->
      <section class="loading-section animate-fade-in" *ngIf="loading">
        <div class="step-list">
          <div class="step" [class.done]="stepsDone >= 1" [class.active]="stepsDone === 0">
            <span class="step-icon">{{ stepsDone >= 1 ? '✓' : '⏳' }}</span>
            <span>Parsing your intent with AI...</span>
          </div>
          <div class="step" [class.done]="stepsDone >= 2" [class.active]="stepsDone === 1">
            <span class="step-icon">{{ stepsDone >= 2 ? '✓' : (stepsDone === 1 ? '⏳' : '○') }}</span>
            <span>Selecting matching products...</span>
          </div>
          <div class="step" [class.done]="stepsDone >= 3" [class.active]="stepsDone === 2">
            <span class="step-icon">{{ stepsDone >= 3 ? '✓' : (stepsDone === 2 ? '⏳' : '○') }}</span>
            <span>Running policy engine checks...</span>
          </div>
        </div>
      </section>

      <!-- Error State -->
      <section class="error-section animate-fade-in" *ngIf="errorMessage">
        <div class="error-card glass-panel">
          <span>⚠️</span>
          <div>
            <strong>Something went wrong</strong>
            <p>{{ errorMessage }}</p>
          </div>
          <button class="btn btn-secondary btn-sm" (click)="errorMessage = ''">Dismiss</button>
        </div>
      </section>

      <!-- How It Works (Hidden while loading) -->
      <section class="how-it-works animate-fade-in animate-fade-in-delay-2" *ngIf="!loading && !intent">
        <h2>How It Works</h2>
        <div class="steps-grid">
          <div class="hiw-card glass-card">
            <div class="hiw-icon">💬</div>
            <h3>1. Describe</h3>
            <p>Tell the AI your budget, dietary needs, and allergen restrictions in plain language.</p>
          </div>
          <div class="hiw-card glass-card">
            <div class="hiw-icon">🤖</div>
            <h3>2. AI Selects</h3>
            <p>Our engine automatically picks the best products that match ALL your constraints.</p>
          </div>
          <div class="hiw-card glass-card">
            <div class="hiw-icon">🛡️</div>
            <h3>3. Review & Pay</h3>
            <p>See every policy check (allergens, budget, diet) before approving checkout.</p>
          </div>
        </div>
      </section>

      <!-- Catalog (Always visible below) -->
      <section class="catalog-section animate-fade-in animate-fade-in-delay-3" *ngIf="!loading">
        <div class="catalog-header">
          <h2>🛍️ NutriBasket Catalog</h2>
          <span class="text-secondary">{{ catalog.length }} available products</span>
        </div>
        <div class="product-grid">
          <app-product-card
            *ngFor="let p of catalog; let i = index"
            [product]="p"
            [class]="'animate-fade-in'"
            [style.animation-delay.ms]="i * 80">
          </app-product-card>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .page { padding-bottom: 4rem; }

    /* ── Hero ──── */
    .hero {
      text-align: center;
      padding: 3rem 0 2rem;
    }

    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.375rem 1rem;
      border-radius: var(--radius-full);
      background: rgba(59, 130, 246, 0.08);
      border: 1px solid rgba(59, 130, 246, 0.15);
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--accent-blue-light);
      margin-bottom: 1.5rem;
    }

    .hero-title {
      font-size: 2.75rem;
      font-weight: 800;
      letter-spacing: -0.04em;
      line-height: 1.15;
      margin-bottom: 1rem;
    }

    .hero-subtitle {
      color: var(--text-secondary);
      font-size: 1.0625rem;
      max-width: 540px;
      margin: 0 auto 2.5rem;
      line-height: 1.6;
    }

    .intent-box {
      max-width: 600px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .intent-btn {
      align-self: flex-end;
      padding: 0.75rem 2rem;
      font-size: 1rem;
    }

    /* ── Loading ── */
    .loading-section {
      max-width: 480px;
      margin: 2rem auto 0;
    }

    .step-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .step {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-subtle);
      color: var(--text-muted);
      font-size: 0.9375rem;
      transition: all 0.3s ease;
    }

    .step.active {
      border-color: var(--accent-blue);
      background: rgba(59, 130, 246, 0.06);
      color: var(--text-primary);
    }

    .step.done {
      border-color: rgba(16, 185, 129, 0.3);
      color: var(--accent-emerald);
    }

    .step-icon {
      font-size: 1rem;
      width: 24px;
      text-align: center;
    }

    /* ── Error ──── */
    .error-section {
      max-width: 600px;
      margin: 2rem auto 0;
    }

    .error-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.25rem;
      border-color: rgba(244, 63, 94, 0.3);
      background: rgba(244, 63, 94, 0.06);
    }

    .error-card p {
      color: var(--text-secondary);
      font-size: 0.875rem;
      margin: 0;
    }

    /* ── Rules Preview ── */
    .rules-preview {
      max-width: 600px;
      margin: 2rem auto 0;
    }

    .rules-card {
      padding: 1.25rem;
    }

    .rules-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .rules-header h3 { font-size: 1rem; }

    .rules-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .rule-chip {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      padding: 0.375rem 0.875rem;
      border-radius: var(--radius-full);
      background: rgba(59, 130, 246, 0.08);
      border: 1px solid rgba(59, 130, 246, 0.15);
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--accent-blue-light);
    }

    .rule-chip.danger {
      background: rgba(244, 63, 94, 0.08);
      border-color: rgba(244, 63, 94, 0.2);
      color: var(--accent-rose);
    }

    /* ── Catalog ── */
    .catalog-section {
      margin-top: 4rem;
      max-width: 1000px;
      margin-left: auto;
      margin-right: auto;
    }

    .catalog-header {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      margin-bottom: 2rem;
    }

    .catalog-header h2 {
      font-size: 1.75rem;
    }

    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1.5rem;
    }

    /* ── How It Works ── */
    .how-it-works {
      margin-top: 4rem;
    }

    .how-it-works h2 {
      text-align: center;
      margin-bottom: 2rem;
      font-size: 1.5rem;
    }

    .steps-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.25rem;
    }

    .hiw-card {
      padding: 1.5rem;
      text-align: center;
    }

    .hiw-icon {
      font-size: 2.5rem;
      margin-bottom: 1rem;
    }

    .hiw-card h3 {
      font-size: 1rem;
      margin-bottom: 0.5rem;
    }

    .hiw-card p {
      font-size: 0.8125rem;
      color: var(--text-muted);
      line-height: 1.5;
    }

    @media (max-width: 640px) {
      .hero-title { font-size: 2rem; }
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
  }


  goShopping() {
    if (!this.prompt.trim()) return;
    this.loading = true;
    this.errorMessage = '';
    this.stepsDone = 0;
    this.loadingStep = '⏳ Step 1: Parsing intent...';

    // Step 1: Parse intent (saves to DB, returns intentId)
    this.intentService.parseIntent(this.prompt).subscribe({
      next: (intentResult) => {
        this.intent = intentResult;
        this.stepsDone = 1;
        this.loadingStep = '⏳ Step 2: Selecting products...';

        // Step 2: AI auto-selects products based on rules
        const selectedItems = this.autoSelectProducts(intentResult.rules);

        if (selectedItems.length === 0) {
          this.loading = false;
          this.errorMessage = 'No products matched your constraints. Try adjusting your budget or dietary requirements.';
          return;
        }

        this.stepsDone = 2;
        this.loadingStep = '⏳ Step 3: Running policy checks...';

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
