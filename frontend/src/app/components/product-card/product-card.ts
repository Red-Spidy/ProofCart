import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="glass-card card">
      <div class="card-header">
        <div class="card-emoji">{{ getEmoji() }}</div>
        <div class="tag-row">
          <span class="badge badge-info" *ngFor="let tag of product.dietaryTags">{{ tag }}</span>
          <span class="badge badge-warning" *ngFor="let allergen of product.allergens">⚠ {{ allergen }}</span>
        </div>
      </div>

      <div class="card-body">
        <h3 class="card-title">{{ product.name }}</h3>
        <p class="card-desc">{{ product.description }}</p>

        <div class="card-meta">
          <span *ngIf="product.deliveryDays === 0" class="meta-item">⚡ Same Day</span>
          <span *ngIf="product.deliveryDays === 1" class="meta-item">📦 Next Day</span>
          <span *ngIf="product.deliveryDays > 1" class="meta-item">📦 {{ product.deliveryDays }} Days</span>
        </div>
      </div>

      <div class="card-footer">
        <div class="price">₹{{ (product.pricePaise / 100) }}</div>

        <div class="qty-controls">
          <button class="btn-icon" (click)="qty = qty > 1 ? qty - 1 : 1">−</button>
          <span class="qty-display">{{ qty }}</span>
          <button class="btn-icon" (click)="qty = qty + 1">+</button>
        </div>

        <button class="btn btn-primary btn-sm" (click)="onAdd()">
          Add
        </button>
      </div>
    </div>
  `,
  styles: [`
    .card {
      padding: 0;
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .card-header {
      padding: 1.25rem 1.25rem 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .card-emoji {
      font-size: 2.5rem;
      line-height: 1;
    }

    .tag-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.375rem;
      justify-content: flex-end;
    }

    .card-body {
      padding: 1rem 1.25rem;
      flex-grow: 1;
    }

    .card-title {
      font-size: 1.125rem;
      font-weight: 700;
      margin-bottom: 0.375rem;
      color: var(--text-primary);
    }

    .card-desc {
      font-size: 0.8125rem;
      color: var(--text-muted);
      line-height: 1.5;
      margin-bottom: 0.75rem;
    }

    .card-meta {
      display: flex;
      gap: 0.75rem;
    }

    .meta-item {
      font-size: 0.75rem;
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      gap: 0.25rem;
    }

    .card-footer {
      padding: 1rem 1.25rem;
      border-top: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .price {
      font-size: 1.25rem;
      font-weight: 800;
      color: var(--accent-blue-light);
      min-width: 60px;
    }

    .qty-controls {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      margin-left: auto;
    }

    .qty-display {
      width: 28px;
      text-align: center;
      font-weight: 700;
      font-size: 0.875rem;
    }
  `]
})
export class ProductCardComponent {
  @Input() product: any;
  @Output() addToCart = new EventEmitter<{ product: any, quantity: number }>();

  qty = 1;

  getEmoji(): string {
    const name = (this.product?.name || '').toLowerCase();
    if (name.includes('trail') || name.includes('mix')) return '🥜';
    if (name.includes('bar') || name.includes('protein')) return '🍫';
    if (name.includes('fruit') || name.includes('bite')) return '🍇';
    if (name.includes('seed') || name.includes('chia')) return '🌱';
    if (name.includes('tea') || name.includes('matcha')) return '🍵';
    return '🥗';
  }

  onAdd() {
    this.addToCart.emit({ product: this.product, quantity: this.qty });
    this.qty = 1;
  }
}
