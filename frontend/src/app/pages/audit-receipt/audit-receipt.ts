import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {CartService} from '../../services/cart';
import {IconComponent} from '../../components/icon/icon';

@Component({
  selector: 'app-audit-receipt',
  standalone: true,
  imports: [CommonModule, RouterLink, IconComponent],
  template: `
    <div class="receipt-container animate-fade-in" *ngIf="receipt">
      <div class="panel receipt-card">
        <div class="header">
          <div class="header-title">
            <div class="header-icon"><app-icon name="receipt" [size]="18"></app-icon></div>
            <h1>Audit Receipt</h1>
          </div>
          <span class="badge" [ngClass]="receipt.status === 'PAID' ? 'badge-success' : 'badge-warning'">
            {{ receipt.status }}
          </span>
        </div>

        <div class="section">
          <h3 class="eyebrow">Order Details</h3>
          <div class="grid-2">
            <div><span class="label">Order ID</span><code class="hash">{{ receipt.orderId }}</code></div>
            <div><span class="label">Amount</span><span class="value">₹{{ (receipt.amountPaise / 100).toFixed(2) }}</span></div>
            <div><span class="label">Razorpay Order</span><code class="hash">{{ receipt.razorpayOrderId }}</code></div>
            <div><span class="label">Razorpay Payment</span><code class="hash">{{ receipt.razorpayPaymentId }}</code></div>
          </div>
        </div>

        <div class="section" *ngIf="receipt.intent && receipt.intent !== 'Direct cart purchase'">
          <h3 class="eyebrow">AI Intent Contract</h3>
          <div class="intent-box">
            <p><strong>Original prompt:</strong> "{{ receipt.intent.prompt }}"</p>
            <p><strong>Confidence:</strong> {{ (receipt.intent.confidence * 100).toFixed(0) }}%</p>
            <pre>{{ receipt.intent.extractedRules | json }}</pre>
          </div>
        </div>

        <div class="section">
          <h3 class="eyebrow">Proof Cart Anti-Tamper</h3>
          <div class="tamper-box">
            <span class="label">Offer hash (SHA-256)</span>
            <code class="hash highlight">{{ receipt.proofCart.offerHash }}</code>
            <p class="mt text-muted text-sm">This hash guarantees the cart was evaluated by the policy engine and matches the final checkout amount exactly.</p>
          </div>
        </div>

        <div class="section" *ngIf="receipt.events?.length">
          <h3 class="eyebrow">Verifiable Trust Chain</h3>
          <div class="chain-box" [class.chain-valid]="chainResult?.valid ?? receipt.chain?.valid" [class.chain-invalid]="(chainResult ?? receipt.chain) && !(chainResult ?? receipt.chain).valid">
            <div class="chain-status">
              <div class="chain-status-icon">
                <app-icon [name]="(chainResult ?? receipt.chain)?.valid ? 'lock' : 'alert-triangle'" [size]="18"></app-icon>
              </div>
              <div class="chain-status-text">
                <strong>{{ (chainResult ?? receipt.chain)?.valid ? 'Chain intact' : 'Tamper detected' }}</strong>
                <p class="text-muted text-sm">{{ (chainResult ?? receipt.chain)?.message }}</p>
              </div>
              <button class="btn btn-secondary btn-sm" (click)="verify()" [disabled]="verifying">
                {{ verifying ? 'Verifying…' : 'Re-verify now' }}
              </button>
            </div>
            <ol class="chain-events">
              <li *ngFor="let e of receipt.events">
                <div class="chain-event-head">
                  <span class="badge badge-neutral">{{ e.eventType }}</span>
                  <span class="text-muted text-sm">{{ e.createdAt | date:'short' }}</span>
                </div>
                <p>{{ e.description }}</p>
                <code class="hash text-sm" *ngIf="e.hash">hash {{ e.hash.substring(0, 16) }}… ← prev {{ e.prevHash === 'GENESIS' ? 'GENESIS' : e.prevHash.substring(0, 16) + '…' }}</code>
              </li>
            </ol>
          </div>
        </div>

        <div class="actions">
          <a routerLink="/" class="btn btn-primary">Return to shop</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .receipt-container { max-width: 760px; margin: 3rem auto; padding: 0 1.5rem; }
    .receipt-card { padding: 2.5rem; }
    .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 1.5rem; margin-bottom: 2rem; }
    .header-title { display: flex; align-items: center; gap: 0.75rem; }
    .header-icon { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: var(--radius-md); background: var(--accent-tint); color: var(--accent); }
    h1 { margin: 0; font-size: 1.375rem; }
    .section { margin-bottom: 2rem; }
    .section .eyebrow { display: block; margin-bottom: 0.875rem; }
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
    .label { display: block; font-size: 0.75rem; color: var(--text-muted); margin-bottom: 0.25rem; }
    .value { font-size: 0.9375rem; font-weight: 600; }
    .hash { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.8125rem; color: var(--text-secondary); word-break: break-all; }
    .intent-box, .tamper-box, .chain-box { background: var(--surface-2); padding: 1.25rem; border-radius: var(--radius-md); border: 1px solid var(--border); }
    .intent-box p { margin-bottom: 0.5rem; font-size: 0.9375rem; }
    .intent-box pre { white-space: pre-wrap; margin-top: 0.75rem; font-size: 0.8125rem; color: var(--text-secondary); }
    .tamper-box .highlight { display: block; margin: 0.375rem 0 0.5rem; font-size: 0.9375rem; color: var(--accent); font-weight: 600; }
    .mt { margin-top: 0.5rem; }
    .actions { text-align: center; margin-top: 2.5rem; }

    .chain-box.chain-valid { border-color: var(--success-border); background: var(--success-tint); }
    .chain-box.chain-invalid { border-color: var(--danger-border); background: var(--danger-tint); }
    .chain-status { display: flex; align-items: center; gap: 0.875rem; margin-bottom: 1.25rem; }
    .chain-status-icon { width: 36px; height: 36px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--surface); }
    .chain-valid .chain-status-icon { color: var(--success); }
    .chain-invalid .chain-status-icon { color: var(--danger); }
    .chain-status-text { flex: 1; }
    .chain-events { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.875rem; }
    .chain-events li { border-left: 2px solid var(--border-strong); padding-left: 0.875rem; }
    .chain-event-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.25rem; }
    .chain-events p { margin: 0.25rem 0; font-size: 0.875rem; }
    .chain-events .hash { font-size: 0.75rem; opacity: 0.85; }

    @media (max-width: 640px) { .grid-2 { grid-template-columns: 1fr; } }
  `]
})
export class AuditReceiptComponent implements OnInit {
  receipt: any = null;
  chainResult: any = null;
  verifying = false;
  private orderId!: string;

  constructor(
    private route: ActivatedRoute,
    private cartService: CartService
  ) {
  }

  ngOnInit() {
    this.orderId = this.route.snapshot.paramMap.get('orderId')!;
    this.cartService.getAuditReceipt(this.orderId).subscribe({
      next: (res) => this.receipt = res,
      error: (err) => console.error('Failed to load receipt', err)
    });
  }

  verify() {
    this.verifying = true;
    this.cartService.verifyAuditChain(this.orderId).subscribe({
      next: (res) => {
        this.chainResult = res;
        this.verifying = false;
      },
      error: (err) => {
        console.error('Chain verification failed', err);
        this.verifying = false;
      }
    });
  }
}
