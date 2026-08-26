import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterLink} from '@angular/router';
import {HttpClient} from '@angular/common/http';

interface OrderHistoryEntry {
  id: string;
  status: string;
  totalPaise: number;
  createdAt: string;
  orderId?: string;
  paymentStatus?: string;
}

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header animate-fade-in">
        <h1>Order History</h1>
        <p class="text-secondary">All your past carts and purchases</p>
      </div>

      <div class="orders-container animate-fade-in">

        <!-- Loading State -->
        <div *ngIf="loading" class="empty-state glass-card">
          <div class="empty-icon">⏳</div>
          <p>Loading your orders...</p>
        </div>

        <!-- Error State -->
        <div *ngIf="error && !loading" class="error-card glass-panel">
          <span>⚠️</span>
          <p>{{ error }}</p>
        </div>

        <!-- Empty State -->
        <div *ngIf="!loading && !error && history.length === 0" class="empty-state glass-card">
          <div class="empty-icon">🛒</div>
          <h3>No orders yet</h3>
          <p class="text-secondary">Start shopping to see your order history here.</p>
          <a routerLink="/" class="btn btn-primary">Shop Now</a>
        </div>

        <!-- Order List -->
        <div *ngIf="!loading && history.length > 0" class="order-list">
          <div *ngFor="let order of history" class="order-card glass-card animate-fade-in">
            <div class="order-header">
              <div class="order-meta">
                <span class="order-id">Cart #{{ order.id.substring(0, 8) }}...</span>
                <span class="order-date text-muted">{{ order.createdAt | date:'medium' }}</span>
              </div>
              <div class="order-badges">
                <span class="badge" [class]="getBadgeClass(order.status)">{{ order.status }}</span>
                <span *ngIf="order.paymentStatus" class="badge badge-payment" [class]="getPaymentBadgeClass(order.paymentStatus)">
                  {{ order.paymentStatus }}
                </span>
              </div>
            </div>

            <div class="order-amount">
              <span class="amount-label">Total</span>
              <span class="amount-value">₹{{ (order.totalPaise / 100) | number:'1.2-2' }}</span>
            </div>

            <div class="order-actions">
              <a *ngIf="order.orderId" [routerLink]="['/receipt', order.orderId]" class="btn btn-secondary btn-sm">
                View Receipt 🧾
              </a>
              <span *ngIf="!order.orderId" class="text-muted text-sm">No payment receipt</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page {
      max-width: 800px;
      margin: 0 auto;
      padding: 2rem 1.5rem 4rem;
    }

    .page-header {
      margin-bottom: 2rem;
    }

    .page-header h1 {
      font-size: 2rem;
      font-weight: 800;
      letter-spacing: -0.04em;
      margin-bottom: 0.5rem;
    }

    .orders-container {}

    .order-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .order-card {
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .order-meta {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .order-id {
      font-family: monospace;
      font-size: 0.875rem;
      color: var(--accent-blue-light);
      font-weight: 600;
    }

    .order-date {
      font-size: 0.8125rem;
    }

    .order-badges {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .badge {
      padding: 0.25rem 0.75rem;
      border-radius: var(--radius-full);
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .badge-allowed, .badge-approved {
      background: rgba(16, 185, 129, 0.12);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: var(--accent-emerald);
    }

    .badge-blocked {
      background: rgba(244, 63, 94, 0.12);
      border: 1px solid rgba(244, 63, 94, 0.3);
      color: var(--accent-rose);
    }

    .badge-reapproval_required {
      background: rgba(251, 191, 36, 0.12);
      border: 1px solid rgba(251, 191, 36, 0.3);
      color: #fbbf24;
    }

    .badge-payment {
      background: rgba(59, 130, 246, 0.12);
      border: 1px solid rgba(59, 130, 246, 0.3);
      color: var(--accent-blue-light);
    }

    .order-amount {
      display: flex;
      align-items: baseline;
      gap: 0.75rem;
    }

    .amount-label {
      font-size: 0.8125rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .amount-value {
      font-size: 1.5rem;
      font-weight: 800;
      letter-spacing: -0.03em;
      color: var(--text-primary);
    }

    .order-actions {
      padding-top: 0.5rem;
      border-top: 1px solid var(--border-subtle);
    }

    .empty-state {
      text-align: center;
      padding: 3rem 2rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }

    .empty-icon {
      font-size: 3rem;
    }

    .empty-state h3 {
      font-size: 1.25rem;
      font-weight: 700;
    }

    .error-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.25rem;
      border-color: rgba(244, 63, 94, 0.3);
      background: rgba(244, 63, 94, 0.06);
      border-radius: var(--radius-md);
    }

    .text-sm {
      font-size: 0.8125rem;
    }
  `]
})
export class OrdersComponent implements OnInit {
  history: OrderHistoryEntry[] = [];
  loading = true;
  error = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<{history: OrderHistoryEntry[]}>('/api/orders').subscribe({
      next: (res) => {
        this.history = res.history || [];
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load order history. Please try again.';
        this.loading = false;
        console.error('Order history error:', err);
      }
    });
  }

  getBadgeClass(status: string): string {
    const s = (status || '').toLowerCase();
    if (s === 'approved' || s === 'allowed') return 'badge-allowed';
    if (s === 'blocked') return 'badge-blocked';
    if (s === 'reapproval_required') return 'badge-reapproval_required';
    return 'badge-allowed';
  }

  getPaymentBadgeClass(status: string): string {
    return 'badge-payment';
  }
}
