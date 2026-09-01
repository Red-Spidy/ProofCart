import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {CartService} from '../../services/cart';
import {IconComponent} from '../../components/icon/icon';
import {PolicyBadgeComponent} from '../../components/policy-badge/policy-badge';

@Component({
  selector: 'app-audit-receipt',
  standalone: true,
  imports: [CommonModule, RouterLink, IconComponent, PolicyBadgeComponent],
  template: `
    <div class="receipt-container animate-fade-in" *ngIf="receipt">
      <div class="panel receipt-card">
        <div class="header">
          <div class="header-title">
            <div class="header-icon"><app-icon name="receipt" [size]="18"></app-icon></div>
            <div>
              <h1>Receipt</h1>
              <span class="text-muted text-sm">{{ receipt.createdAt | date:'medium' }}</span>
            </div>
          </div>
          <span class="badge" [ngClass]="receipt.status === 'PAID' ? 'badge-success' : 'badge-warning'">
            {{ receipt.status }}
          </span>
        </div>

        <!-- Items purchased -->
        <div class="section">
          <div class="items-list">
            <div class="item-row" *ngFor="let item of receipt.items">
              <div>
                <strong>{{ item.snapshot.name }}</strong>
                <span class="text-muted text-sm"> × {{ item.quantity }}</span>
              </div>
              <span class="item-price">₹{{ (item.lineTotalPaise / 100).toFixed(2) }}</span>
            </div>
          </div>
          <div class="total-row">
            <span>Total paid</span>
            <strong>₹{{ (receipt.amountPaise / 100).toFixed(2) }}</strong>
          </div>
        </div>

        <!-- Order details -->
        <div class="section">
          <h3 class="eyebrow">Order Details</h3>
          <div class="grid-2">
            <div><span class="label">Order ID</span><code class="hash">{{ receipt.orderId }}</code></div>
            <div><span class="label">Payment ID</span><code class="hash">{{ receipt.razorpayPaymentId }}</code></div>
          </div>
        </div>

        <!-- How this was understood -->
        <div class="section" *ngIf="receipt.intent && receipt.intent !== 'Direct cart purchase'">
          <h3 class="eyebrow">How This Was Understood</h3>
          <div class="intent-box">
            <p class="prompt-line">"{{ receipt.intent.prompt }}"</p>
            <div class="rule-chips">
              <span class="badge badge-info" *ngIf="receipt.intent.rules?.maxTotalPaise as budget">
                Budget ₹{{ (budget / 100).toFixed(0) }}
              </span>
              <span class="badge badge-info" *ngFor="let tag of receipt.intent.rules?.mustHaveTags">{{ tag }}</span>
              <span class="badge badge-warning" *ngFor="let a of receipt.intent.rules?.excludedAllergens">
                <app-icon name="alert-triangle" [size]="11"></app-icon>no {{ a }}
              </span>
              <span class="badge badge-neutral" *ngIf="receipt.intent.rules?.deliveryRequirement">
                Delivery: {{ receipt.intent.rules.deliveryRequirement }}
              </span>
              <span class="badge badge-neutral" *ngIf="receipt.intent.rules?.mustBeReturnable">Returnable</span>
              <span class="badge badge-neutral" *ngIf="receipt.intent.rules?.subscriptionAllowed === false">One-time purchase</span>
            </div>
          </div>
        </div>

        <!-- Policy checks — collapsed by default -->
        <div class="section" *ngIf="receipt.proofCart?.policyChecks?.length">
          <button class="disclosure" (click)="showChecks = !showChecks">
            <app-icon name="shield-check" [size]="15"></app-icon>
            <span>{{ receipt.proofCart.policyChecks.length }} policy checks passed before this was allowed to pay</span>
            <app-icon name="chevron-right" [size]="14" class="chevron" [class.open]="showChecks"></app-icon>
          </button>
          <div class="checks-list" *ngIf="showChecks">
            <app-policy-badge *ngFor="let check of receipt.proofCart.policyChecks" [check]="check"></app-policy-badge>
          </div>
        </div>

        <!-- Verification details — secondary, collapsed by default -->
        <div class="section">
          <button class="disclosure" (click)="showVerification = !showVerification">
            <app-icon name="lock" [size]="15"></app-icon>
            <span>Verification details</span>
            <app-icon name="chevron-right" [size]="14" class="chevron" [class.open]="showVerification"></app-icon>
          </button>

          <div class="verification-body" *ngIf="showVerification">
            <p class="text-muted text-sm">
              For anyone who wants to independently confirm this purchase wasn't altered after the fact.
            </p>

            <div class="tamper-box">
              <span class="label">Offer hash</span>
              <div class="hash-row">
                <code class="hash">{{ shortHash(receipt.proofCart.offerHash) }}</code>
                <button class="btn-icon" (click)="copyHash(receipt.proofCart.offerHash)" aria-label="Copy full hash">
                  <app-icon [name]="copied ? 'check' : 'layers'" [size]="14"></app-icon>
                </button>
              </div>
            </div>

            <div class="chain-box" *ngIf="receipt.events?.length"
                 [class.chain-valid]="chainResult?.valid ?? receipt.chain?.valid"
                 [class.chain-invalid]="(chainResult ?? receipt.chain) && !(chainResult ?? receipt.chain).valid">
              <div class="chain-status">
                <div class="chain-status-icon">
                  <app-icon [name]="(chainResult ?? receipt.chain)?.valid ? 'lock' : 'alert-triangle'" [size]="16"></app-icon>
                </div>
                <div class="chain-status-text">
                  <strong>{{ (chainResult ?? receipt.chain)?.valid ? 'Chain intact' : 'Tamper detected' }}</strong>
                  <p class="text-muted text-sm">{{ (chainResult ?? receipt.chain)?.message }}</p>
                </div>
                <button class="btn btn-secondary btn-sm" (click)="verify()" [disabled]="verifying">
                  {{ verifying ? 'Verifying…' : 'Re-verify' }}
                </button>
              </div>
              <ol class="chain-events">
                <li *ngFor="let e of receipt.events">
                  <div class="chain-event-head">
                    <span class="badge badge-neutral">{{ e.eventType }}</span>
                    <span class="text-muted text-sm">{{ e.createdAt | date:'short' }}</span>
                  </div>
                  <p>{{ e.description }}</p>
                </li>
              </ol>
            </div>
          </div>
        </div>

        <div class="actions">
          <a routerLink="/" class="btn btn-primary">Return to shop</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .receipt-container { max-width: 680px; margin: 3rem auto; padding: 0 1.5rem; }
    .receipt-card { padding: 2.25rem; }
    .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 1.5rem; margin-bottom: 1.5rem; }
    .header-title { display: flex; align-items: center; gap: 0.75rem; }
    .header-icon { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: var(--radius-md); background: var(--accent-tint); color: var(--accent); flex-shrink: 0; }
    h1 { margin: 0; font-size: 1.25rem; }
    .section { margin-bottom: 1.5rem; }
    .section .eyebrow { display: block; margin-bottom: 0.75rem; }

    .items-list { display: flex; flex-direction: column; gap: 0.625rem; margin-bottom: 0.875rem; }
    .item-row { display: flex; justify-content: space-between; align-items: baseline; font-size: 0.9375rem; }
    .item-price { font-weight: 600; }
    .total-row { display: flex; justify-content: space-between; padding-top: 0.875rem; border-top: 1px solid var(--border); font-size: 1.0625rem; }

    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
    .label { display: block; font-size: 0.75rem; color: var(--text-muted); margin-bottom: 0.25rem; }
    .hash { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.8125rem; color: var(--text-secondary); word-break: break-all; }

    .intent-box { background: var(--surface-2); padding: 1.125rem; border-radius: var(--radius-md); border: 1px solid var(--border); }
    .prompt-line { font-size: 0.9375rem; font-style: italic; color: var(--text-secondary); margin-bottom: 0.75rem; }
    .rule-chips { display: flex; flex-wrap: wrap; gap: 0.5rem; }

    .disclosure {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 0.625rem;
      padding: 0.75rem 0.875rem;
      border-radius: var(--radius-md);
      border: 1px solid var(--border);
      background: var(--surface);
      color: var(--text-secondary);
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      text-align: left;
    }
    .disclosure:hover { background: var(--surface-hover); }
    .disclosure span { flex: 1; }
    .disclosure .chevron { transition: transform 0.15s ease; }
    .disclosure .chevron.open { transform: rotate(90deg); }

    .checks-list, .verification-body { display: flex; flex-direction: column; gap: 0.5rem; margin-top: 0.75rem; }

    .tamper-box { background: var(--surface-2); padding: 1rem 1.125rem; border-radius: var(--radius-md); border: 1px solid var(--border); }
    .hash-row { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.25rem; }
    .hash-row .hash { flex: 1; }

    .chain-box { background: var(--surface-2); padding: 1.125rem; border-radius: var(--radius-md); border: 1px solid var(--border); }
    .chain-box.chain-valid { border-color: var(--success-border); background: var(--success-tint); }
    .chain-box.chain-invalid { border-color: var(--danger-border); background: var(--danger-tint); }
    .chain-status { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem; }
    .chain-status-icon { width: 32px; height: 32px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--surface); }
    .chain-valid .chain-status-icon { color: var(--success); }
    .chain-invalid .chain-status-icon { color: var(--danger); }
    .chain-status-text { flex: 1; }
    .chain-events { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.75rem; }
    .chain-events li { border-left: 2px solid var(--border-strong); padding-left: 0.75rem; }
    .chain-event-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.25rem; }
    .chain-events p { margin: 0; font-size: 0.8125rem; color: var(--text-secondary); }

    .actions { text-align: center; margin-top: 2rem; }

    @media (max-width: 560px) { .grid-2 { grid-template-columns: 1fr; } }
  `]
})
export class AuditReceiptComponent implements OnInit {
  receipt: any = null;
  chainResult: any = null;
  verifying = false;
  showChecks = false;
  showVerification = false;
  copied = false;
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

  shortHash(hash: string): string {
    if (!hash) return '';
    return hash.length > 20 ? `${hash.substring(0, 10)}…${hash.substring(hash.length - 6)}` : hash;
  }

  copyHash(hash: string) {
    navigator.clipboard?.writeText(hash).then(() => {
      this.copied = true;
      setTimeout(() => (this.copied = false), 1500);
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
