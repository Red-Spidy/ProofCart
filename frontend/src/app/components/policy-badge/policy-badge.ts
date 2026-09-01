import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {IconComponent} from '../icon/icon';

@Component({
  selector: 'app-policy-badge',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="check-row" [class.passed]="check.passed" [class.failed]="!check.passed">
      <div class="check-icon">
        <app-icon [name]="check.passed ? 'check' : 'x'" [size]="14" [strokeWidth]="2.5"></app-icon>
      </div>
      <div class="check-info">
        <span class="check-name">{{ formatRuleName(check.rule) }}</span>
        <span class="check-msg">{{ check.message || check.reason }}</span>
      </div>
      <span class="badge" [ngClass]="check.passed ? 'badge-success' : 'badge-danger'">
        {{ check.passed ? 'Pass' : 'Fail' }}
      </span>
    </div>
  `,
  styles: [`
    .check-row {
      display: flex;
      align-items: center;
      gap: 0.875rem;
      padding: 0.75rem 0.875rem;
      border-radius: var(--radius-md);
      border: 1px solid var(--border);
      background: var(--surface);
    }

    .check-row.failed {
      border-color: var(--danger-border);
      background: var(--danger-tint);
    }

    .check-icon {
      flex-shrink: 0;
      width: 26px;
      height: 26px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
    }

    .passed .check-icon { background: var(--success-tint); color: var(--success); }
    .failed .check-icon { background: var(--surface); color: var(--danger); }

    .check-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.0625rem;
      min-width: 0;
    }

    .check-name {
      font-weight: 600;
      font-size: 0.875rem;
    }

    .check-msg {
      font-size: 0.8125rem;
      color: var(--text-secondary);
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
