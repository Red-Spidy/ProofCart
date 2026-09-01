import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {MarketplaceService, SellerProduct} from '../../services/marketplace';
import {IconComponent} from '../../components/icon/icon';

type ProductForm = Omit<SellerProduct, 'id' | 'reservedQuantity' | 'availableQuantity'>;

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <main class="page">
      <header class="page-header">
        <p class="eyebrow">Seller Console</p>
        <h1>Manage your inventory</h1>
        <p class="text-secondary">Stock is reserved at checkout, then reduced only once payment is verified.</p>
      </header>

      <section *ngIf="loading" class="card state">Loading your store…</section>
      <section *ngIf="error" class="form-note danger"><app-icon name="alert-triangle" [size]="16"></app-icon><span>{{ error }}</span></section>

      <section *ngIf="!loading && onboarding" class="card form-card">
        <h2>Create your store</h2>
        <p class="text-secondary">This is the store buyers will see when they purchase your products.</p>
        <form (ngSubmit)="saveStore()" class="form-grid">
          <label>Store name<input class="input" [(ngModel)]="storeName" name="storeName" required/></label>
          <label class="full">Description<textarea class="input" [(ngModel)]="storeDescription" name="storeDescription" rows="3"></textarea></label>
          <button class="btn btn-primary" [disabled]="saving">{{ saving ? 'Creating…' : 'Create store' }}</button>
        </form>
      </section>

      <ng-container *ngIf="!loading && !onboarding">
        <section class="card store-card">
          <div>
            <p class="eyebrow">Your Store</p>
            <h2>{{ merchant?.name }}</h2>
            <p class="text-secondary">{{ merchant?.description || 'No store description yet.' }}</p>
          </div>
          <div class="stock-summary">
            <strong>{{ availableUnits }}</strong>
            <span>units available</span>
          </div>
        </section>

        <section class="layout">
          <form (ngSubmit)="saveProduct()" class="card form-card">
            <h2>{{ editingId ? 'Edit product' : 'Add a product' }}</h2>
            <div class="form-grid">
              <label class="full">Name<input class="input" [(ngModel)]="product.name" name="name" required/></label>
              <label>Price (₹)<input class="input" type="number" min="0.01" step="0.01" [(ngModel)]="priceRupees" name="price" required/></label>
              <label>Stock<input class="input" type="number" min="0" step="1" [(ngModel)]="product.stockQuantity" name="stock" required/></label>
              <label class="full">Description<textarea class="input" [(ngModel)]="product.description" name="description" rows="3"></textarea></label>
              <label>Delivery days<input class="input" type="number" min="0" [(ngModel)]="product.deliveryDays" name="deliveryDays"/></label>
              <label>Dietary tags<input class="input" [(ngModel)]="tagsText" name="tags" placeholder="vegan, organic"/></label>
              <label>Allergens<input class="input" [(ngModel)]="allergensText" name="allergens" placeholder="nuts, dairy"/></label>
              <label class="toggle"><input type="checkbox" [(ngModel)]="product.returnable" name="returnable"/> Returnable</label>
              <label class="toggle"><input type="checkbox" [(ngModel)]="product.subscriptionAvailable" name="subscription"/> Subscription available</label>
              <div class="actions full">
                <button class="btn btn-primary" [disabled]="saving">{{ saving ? 'Saving…' : (editingId ? 'Save changes' : 'Add product') }}</button>
                <button *ngIf="editingId" type="button" class="btn btn-secondary" (click)="resetProduct()">Cancel</button>
              </div>
            </div>
          </form>

          <section class="products">
            <h2>Products <span>{{ products.length }}</span></h2>
            <div *ngIf="products.length === 0" class="card state">Add your first product to start selling.</div>
            <article *ngFor="let p of products" class="card product">
              <div>
                <h3>{{ p.name }}</h3>
                <p class="text-secondary">₹{{ p.pricePaise / 100 }} · {{ p.availableQuantity }} available
                  <span *ngIf="p.reservedQuantity">· {{ p.reservedQuantity }} reserved</span>
                </p>
              </div>
              <button class="btn btn-secondary btn-sm" (click)="edit(p)">Edit</button>
            </article>
          </section>
        </section>
      </ng-container>
    </main>`,
  styles: [`
    .page { max-width: 1160px; margin: 0 auto; padding: 2.5rem 1.5rem 4rem; }
    .page-header { margin-bottom: 2rem; }
    .page-header h1 { font-size: 1.75rem; margin: 0.25rem 0 0.5rem; }
    .layout { display: grid; grid-template-columns: minmax(320px, 0.9fr) 1.1fr; gap: 1.25rem; margin-top: 1.25rem; }
    .form-card, .store-card { padding: 1.5rem; }
    .form-card h2, .products h2 { margin-bottom: 1rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem; }
    .form-grid label { display: flex; flex-direction: column; gap: 0.4rem; font-size: 0.8125rem; color: var(--text-secondary); font-weight: 500; }
    .full { grid-column: 1 / -1; }
    .toggle { flex-direction: row !important; align-items: center; }
    .actions { display: flex; gap: 0.75rem; }
    .store-card { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; }
    .stock-summary { text-align: right; display: flex; flex-direction: column; }
    .stock-summary strong { color: var(--success); font-size: 1.75rem; font-weight: 700; }
    .stock-summary span { font-size: 0.75rem; color: var(--text-muted); }
    .products { display: flex; flex-direction: column; gap: 0.75rem; }
    .products h2 span { font-size: 0.8rem; color: var(--text-muted); font-weight: 500; }
    .product { padding: 1rem 1.25rem; display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    .product h3 { margin: 0 0 0.3rem; font-size: 0.9375rem; }
    .state { padding: 2rem; text-align: center; color: var(--text-muted); }
    @media (max-width: 800px) { .layout { grid-template-columns: 1fr; } .form-grid { grid-template-columns: 1fr; } .full { grid-column: auto; } }
  `]
})
export class SellerDashboardComponent implements OnInit {
  loading = true;
  saving = false;
  onboarding = false;
  error = '';
  merchant: any;
  products: SellerProduct[] = [];
  editingId = '';
  storeName = '';
  storeDescription = '';
  tagsText = '';
  allergensText = '';
  priceRupees = 1;
  product: ProductForm = this.emptyProduct();

  constructor(private marketplace: MarketplaceService) {
  }

  get availableUnits() {
    return this.products.reduce((sum, p) => sum + p.availableQuantity, 0);
  }

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.marketplace.sellerDashboard().subscribe({
      next: (data: any) => {
        this.onboarding = data.onboardingRequired;
        this.merchant = data.merchant;
        this.products = data.products || [];
        this.loading = false;
      }, error: err => {
        this.error = err.error?.error || 'Seller access is required to view this page.';
        this.loading = false;
      }
    });
  }

  saveStore() {
    this.saving = true;
    this.marketplace.createStore(this.storeName, this.storeDescription).subscribe({
      next: () => {
        this.saving = false;
        this.load();
      }, error: err => {
        this.error = err.error?.error || 'Could not create store.';
        this.saving = false;
      }
    });
  }

  saveProduct() {
    const payload = {
      ...this.product,
      pricePaise: Math.round(Number(this.priceRupees) * 100),
      dietaryTags: this.csv(this.tagsText),
      allergens: this.csv(this.allergensText)
    };
    this.saving = true;
    const request = this.editingId ? this.marketplace.updateProduct(this.editingId, payload) : this.marketplace.createProduct(payload);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.resetProduct();
        this.load();
      }, error: err => {
        this.error = err.error?.error || 'Could not save product.';
        this.saving = false;
      }
    });
  }

  edit(p: SellerProduct) {
    this.editingId = p.id;
    this.product = {...p};
    this.priceRupees = p.pricePaise / 100;
    this.tagsText = p.dietaryTags.join(', ');
    this.allergensText = p.allergens.join(', ');
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  resetProduct() {
    this.editingId = '';
    this.product = this.emptyProduct();
    this.priceRupees = 1;
    this.tagsText = '';
    this.allergensText = '';
  }

  private csv(value: string) {
    return value.split(',').map(v => v.trim()).filter(Boolean);
  }

  private emptyProduct(): ProductForm {
    return {
      name: '',
      description: '',
      pricePaise: 100,
      stockQuantity: 0,
      dietaryTags: [],
      allergens: [],
      deliveryDays: 0,
      returnable: false,
      subscriptionAvailable: false
    };
  }
}
