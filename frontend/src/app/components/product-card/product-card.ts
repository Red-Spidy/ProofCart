import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="glass-card product-card">
      <div class="card-content">
        <h3>{{ product.name }}</h3>
        <p class="desc">{{ product.description }}</p>

        <div class="tags">
          <span class="badge badge-success" *ngFor="let tag of product.dietaryTags">{{ tag }}</span>
          <span class="badge badge-warning" *ngFor="let allergen of product.allergens">Allergen: {{ allergen }}</span>
        </div>

        <div class="price">₹{{ (product.pricePaise / 100).toFixed(2) }}</div>

        <div class="actions">
          <button class="btn btn-glass" (click)="qty = qty > 1 ? qty - 1 : 1">-</button>
          <span>{{ qty }}</span>
          <button class="btn btn-glass" (click)="qty = qty + 1">+</button>
          <button class="btn btn-primary" (click)="onAdd()">Add to Cart</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .product-card {
      padding: 1.5rem;
      height: 100%;
      display: flex;
      flex-direction: column;
    }
    .card-content {
      display: flex;
      flex-direction: column;
      flex-grow: 1;
      gap: 1rem;
    }
    h3 { margin: 0; color: var(--text-main); }
    .desc { color: var(--text-muted); font-size: 0.9rem; flex-grow: 1; }
    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }
    .price {
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--primary-glow);
    }
    .actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: auto;
    }
    .btn-primary { flex-grow: 1; }
    .btn-glass { padding: 0.5rem 1rem; }
  `]
})
export class ProductCardComponent {
  @Input() product: any;
  @Output() addToCart = new EventEmitter<{ product: any, quantity: number }>();

  qty: number = 1;

  onAdd() {
    this.addToCart.emit({product: this.product, quantity: this.qty});
    this.qty = 1;
  }
}
