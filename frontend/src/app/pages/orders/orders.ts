import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterLink} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {IconComponent} from '../../components/icon/icon';

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
  imports: [CommonModule, RouterLink, IconComponent],
  template: `
    <div class="page">
      <div class="page-header animate-fade-in">
        <h1>Order History</h1>
        <p class="text-secondary">All your past carts and purchases</p>
      </div>

      <div class="animate-fade-in">
        <div *ngIf="loading" class="state-panel card">
          <div class="state-icon"><app-icon name="loader" [size]="20" class="spin"></app-icon></div>
          <p>Loading your orders…</p>
        </div>

        <div *ngIf="error && !loading" class="form-note danger">
          <app-icon name="alert-triangle" [size]="16"></app-icon>
          <span>{{ error }}</span>
        </div>

        <div *ngIf="!loading && !error && history.length === 0" class="state-panel card">
          <div class="state-icon"><app-icon name="cart" [size]="20"></app-icon></div>
          <h3>No orders yet</h3>
          <p class="text-secondary">Start shopping to see your order history here.</p>
          <a routerLink="/" class="btn btn-primary">Shop Now</a>
        </div>

        <div *ngIf="!loading && history.length > 0" class="order-list">
          <div *ngFor="let order of history" class="order-card card animate-fade-in">
            <div class="order-header">
              <div class="order-meta">
                <span class="order-id">Cart #{{ order.id.substring(0, 8) }}…</span>
                <span class="order-date text-muted">{{ order.createdAt | date:'medium' }}</span>
              </div>
              <div class="order-badges">
                <span class="badge" [class]="getBadgeClass(order.status)">{{ order.status }}</span>
                <span *ngIf="order.paymentStatus" class="badge badge-info">{{ order.paymentStatus }}</span>
              </div>
            </div>

            <div class="order-amount">
              <span class="amount-label">Total</span>
              <span class="amount-value">₹{{ (order.totalPaise / 100) | number:'1.2-2' }}</span>
            </div>

            <div class="order-actions">
              <a *ngIf="order.orderId" [routerLink]="['/receipt', order.orderId]" class="btn btn-secondary btn-sm">
                <app-icon name="receipt" [size]="14"></app-icon> View Receipt
              </a>
              <span *ngIf="!order.orderId" class="text-muted text-sm">No payment receipt</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { max-width: 760px; margin: 0 auto; padding: 0 1.5rem 4rem; }
    .page-header { margin-bottom: 1.75rem; }
    .page-header h1 { font-size: 1.625rem; margin-bottom: 0.375rem; }

    .order-list { display: flex; flex-direction: column; gap: 1rem; }
    .order-card { padding: 1.25rem 1.375rem; display: flex; flex-direction: column; gap: 1rem; }

    .order-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; flex-wrap: wrap; }
    .order-meta { display: flex; flex-direction: column; gap: 0.25rem; }
    .order-id { font-family: 'SF Mono', monospace; font-size: 0.8125rem; color: var(--text-primary); font-weight: 600; }
    .order-date { font-size: 0.8125rem; }
    .order-badges { display: flex; gap: 0.5rem; flex-wrap: wrap; }

    .badge-allowed, .badge-approved { background: var(--success-tint); border-color: var(--success-border); color: var(--success); }
    .badge-blocked { background: var(--danger-tint); border-color: var(--danger-border); color: var(--danger); }
    .badge-reapproval_required { background: var(--warning-tint); border-color: var(--warning-border); color: var(--warning); }

    .order-amount { display: flex; align-items: baseline; gap: 0.625rem; }
    .amount-label { font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .amount-value { font-size: 1.25rem; font-weight: 700; color: var(--text-primary); }

    .order-actions { padding-top: 0.75rem; border-top: 1px solid var(--border); }

    .text-sm { font-size: 0.8125rem; }
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
}
