import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {CartService} from '../../services/cart';

@Component({
  selector: 'app-audit-receipt',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="receipt-container animate-fade-in" *ngIf="receipt">
      <div class="glass-panel receipt-card">
        <div class="header">
          <h1 class="text-gradient">Audit Receipt</h1>
          <span class="badge" [ngClass]="receipt.status === 'PAID' ? 'badge-success' : 'badge-warning'">
            {{ receipt.status }}
          </span>
        </div>

        <div class="section">
          <h3>Order Details</h3>
          <div class="grid-2">
            <div><strong>Order ID:</strong><br/> <span class="hash">{{ receipt.orderId }}</span></div>
            <div><strong>Amount:</strong><br/> ₹{{ (receipt.amountPaise / 100).toFixed(2) }}</div>
            <div><strong>Razorpay Order:</strong><br/> <span class="hash">{{ receipt.razorpayOrderId }}</span></div>
            <div><strong>Razorpay Payment:</strong><br/> <span class="hash">{{ receipt.razorpayPaymentId }}</span></div>
          </div>
        </div>

        <div class="section" *ngIf="receipt.intent && receipt.intent !== 'Direct cart purchase'">
          <h3>AI Intent Contract</h3>
          <div class="intent-box">
            <p><strong>Original Prompt:</strong> "{{ receipt.intent.prompt }}"</p>
            <p><strong>Confidence:</strong> {{ receipt.intent.confidence * 100 }}%</p>
            <pre>{{ receipt.intent.extractedRules | json }}</pre>
          </div>
        </div>

        <div class="section">
          <h3>Proof Cart Anti-Tamper</h3>
          <div class="tamper-box">
            <p><strong>Offer Hash (SHA-256):</strong></p>
            <code class="hash highlight">{{ receipt.proofCart.offerHash }}</code>
            <p class="mt-2 text-muted">This hash guarantees the cart was evaluated by the engine and matches the final checkout amount exactly.</p>
          </div>
        </div>

        <div class="actions">
          <a routerLink="/" class="btn btn-primary">Return to Shop</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .receipt-container { max-width: 800px; margin: 4rem auto; }
    .receipt-card { padding: 3rem; }
    .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-glass); padding-bottom: 1.5rem; margin-bottom: 2rem; }
    h1 { margin: 0; }
    .section { margin-bottom: 2.5rem; }
    .section h3 { margin-bottom: 1rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; font-size: 0.9rem; }
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
    .hash { font-family: monospace; color: var(--primary-glow); word-break: break-all; }
    .intent-box { background: rgba(15, 23, 42, 0.5); padding: 1.5rem; border-radius: 12px; border: 1px solid var(--border-glass); }
    .intent-box pre { white-space: pre-wrap; margin-top: 1rem; color: var(--success-glow); font-size: 0.9rem; }
    .tamper-box { background: rgba(139, 92, 246, 0.1); padding: 1.5rem; border-radius: 12px; border: 1px solid rgba(139, 92, 246, 0.3); }
    .highlight { font-size: 1.1rem; color: #a78bfa; font-weight: bold; }
    .actions { text-align: center; margin-top: 3rem; }
  `]
})
export class AuditReceiptComponent implements OnInit {
  receipt: any = null;

  constructor(
    private route: ActivatedRoute,
    private cartService: CartService
  ) {
  }

  ngOnInit() {
    const orderId = this.route.snapshot.paramMap.get('orderId')!;
    this.cartService.getAuditReceipt(orderId).subscribe({
      next: (res) => this.receipt = res,
      error: (err) => console.error('Failed to load receipt', err)
    });
  }
}
