import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {IconComponent} from '../icon/icon';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="card card-interactive product">
      <div class="product-top">
        <div class="tag-row">
          <span class="badge badge-info" *ngFor="let tag of product.dietaryTags">{{ tag }}</span>
          <span class="badge badge-warning" *ngFor="let allergen of product.allergens">
            <app-icon name="alert-triangle" [size]="11"></app-icon>{{ allergen }}
          </span>
        </div>
      </div>

      <div class="product-body">
        <h3 class="product-title">{{ product.name }}</h3>
        <p class="product-desc">{{ product.description }}</p>

        <div class="product-meta" *ngIf="product.deliveryDays != null">
          <span class="meta-item" *ngIf="product.deliveryDays === 0">
            <app-icon name="zap" [size]="13"></app-icon> Same day
          </span>
          <span class="meta-item" *ngIf="product.deliveryDays === 1">
            <app-icon name="truck" [size]="13"></app-icon> Next day
          </span>
          <span class="meta-item" *ngIf="product.deliveryDays > 1">
            <app-icon name="truck" [size]="13"></app-icon> {{ product.deliveryDays }} days
          </span>
        </div>
      </div>

      <div class="product-footer">
        <div class="price">₹{{ (product.pricePaise / 100) }}</div>

        <div class="qty-controls">
          <button class="btn-icon" (click)="qty = qty > 1 ? qty - 1 : 1" aria-label="Decrease quantity">
            <app-icon name="minus" [size]="14"></app-icon>
          </button>
          <span class="qty-display">{{ qty }}</span>
          <button class="btn-icon" (click)="qty = qty + 1" aria-label="Increase quantity">
            <app-icon name="plus" [size]="14"></app-icon>
          </button>
        </div>

        <button class="btn btn-primary btn-sm" (click)="onAdd()">Add</button>
      </div>

      <div class="feedback-row">
        <span>Useful?</span>
        <button class="feedback-btn" (click)="feedback.emit({product: product, liked: true})" aria-label="Like product">
          <app-icon name="thumbs-up" [size]="14"></app-icon>
        </button>
        <button class="feedback-btn" (click)="feedback.emit({product: product, liked: false})" aria-label="Dislike product">
          <app-icon name="thumbs-down" [size]="14"></app-icon>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .product {
      padding: 0;
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .product-top {
      padding: 1.125rem 1.125rem 0;
      display: flex;
      justify-content: flex-end;
    }

    .tag-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.375rem;
      justify-content: flex-end;
    }

    .product-body {
      padding: 0.75rem 1.125rem 1rem;
      flex-grow: 1;
    }

    .product-title {
      font-size: 1.0625rem;
      font-weight: 600;
      margin-bottom: 0.3125rem;
      color: var(--text-primary);
    }

    .product-desc {
      font-size: 0.8125rem;
      color: var(--text-muted);
      line-height: 1.5;
      margin-bottom: 0.75rem;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .product-meta {
      display: flex;
      gap: 0.75rem;
    }

    .meta-item {
      font-size: 0.75rem;
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      gap: 0.3125rem;
    }

    .product-footer {
      padding: 0.875rem 1.125rem;
      border-top: 1px solid var(--border);
      display: flex;
      align-items: center;
      gap: 0.625rem;
    }

    .price {
      font-size: 1.125rem;
      font-weight: 700;
      color: var(--text-primary);
      min-width: 56px;
    }

    .qty-controls {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      margin-left: auto;
    }

    .qty-display {
      width: 20px;
      text-align: center;
      font-weight: 600;
      font-size: 0.875rem;
    }

    .feedback-row {
      padding: 0 1.125rem 0.875rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: var(--text-muted);
      font-size: 0.75rem;
    }

    .feedback-btn {
      border: 1px solid var(--border);
      background: var(--surface);
      border-radius: var(--radius-sm);
      cursor: pointer;
      color: var(--text-muted);
      padding: 0.25rem 0.375rem;
      display: flex;
      align-items: center;
      transition: color 0.15s ease, border-color 0.15s ease;
    }

    .feedback-btn:hover {
      color: var(--accent);
      border-color: var(--accent-border);
    }
  `]
})
export class ProductCardComponent {
  @Input() product: any;
  @Output() addToCart = new EventEmitter<{ product: any, quantity: number }>();
  @Output() feedback = new EventEmitter<{ product: any, liked: boolean }>();

  qty = 1;

  onAdd() {
    this.addToCart.emit({ product: this.product, quantity: this.qty });
    this.qty = 1;
  }
}
