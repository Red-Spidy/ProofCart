import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-policy-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="policy-item glass-panel" [ngClass]="check.passed ? 'passed' : 'failed'">
      <div class="header">
        <div class="icon">
          <span *ngIf="check.passed">✓</span>
          <span *ngIf="!check.passed">✕</span>
        </div>
        <span class="rule-name">{{ check.rule | titlecase }}</span>
      </div>
      <div class="reason">{{ check.reason }}</div>
    </div>
  `,
  styles: [`
    .policy-item {
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      border-left: 4px solid;
    }
    .passed {
      border-left-color: var(--success-glow);
    }
    .failed {
      border-left-color: var(--danger-glow);
      background: rgba(248, 113, 113, 0.05);
    }
    .header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 600;
    }
    .passed .icon { color: var(--success-glow); }
    .failed .icon { color: var(--danger-glow); }
    .reason {
      color: var(--text-muted);
      font-size: 0.9rem;
    }
  `]
})
export class PolicyBadgeComponent {
  @Input() check: any;
}
