import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-policy-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="check-row" [class.passed]="check.passed" [class.failed]="!check.passed">
      <div class="check-status">
        <span class="check-icon" *ngIf="check.passed">✓</span>
        <span class="check-icon" *ngIf="!check.passed">✕</span>
      </div>
      <div class="check-info">
        <span class="check-name">{{ formatRuleName(check.rule) }}</span>
        <span class="check-msg">{{ check.message || check.reason }}</span>
      </div>
      <div class="check-badge">
        <span class="badge" [ngClass]="check.passed ? 'badge-success' : 'badge-danger'">
          {{ check.passed ? 'PASS' : 'FAIL' }}
        </span>
      </div>
    </div>
  `,
  styles: [`
    .check-row {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.875rem 1rem;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-subtle);
      background: rgba(255, 255, 255, 0.02);
      transition: all 0.2s ease;
    }

    .check-row:hover {
      background: rgba(255, 255, 255, 0.04);
    }

    .check-row.passed {
      border-left: 3px solid var(--accent-emerald);
    }

    .check-row.failed {
      border-left: 3px solid var(--accent-rose);
      background: rgba(244, 63, 94, 0.04);
    }

    .check-status {
      flex-shrink: 0;
    }

    .check-icon {
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      font-size: 0.8125rem;
      font-weight: 700;
    }

    .passed .check-icon {
      background: rgba(16, 185, 129, 0.12);
      color: var(--accent-emerald);
    }

    .failed .check-icon {
      background: rgba(244, 63, 94, 0.12);
      color: var(--accent-rose);
    }

    .check-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.125rem;
    }

    .check-name {
      font-weight: 600;
      font-size: 0.875rem;
    }

    .check-msg {
      font-size: 0.8125rem;
      color: var(--text-muted);
    }

    .check-badge {
      flex-shrink: 0;
    }
  `]
})
export class PolicyBadgeComponent {
  @Input() check: any;

  formatRuleName(name: string): string {
    if (!name) return '';
    return name.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  }
}
