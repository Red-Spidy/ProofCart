import {Component} from '@angular/core';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {CommonModule} from '@angular/common';
import {SupabaseService} from '../../services/supabase.service';
import {IconComponent} from '../icon/icon';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule, IconComponent],
  template: `
    <nav class="navbar">
      <div class="nav-inner">
        <a routerLink="/" class="logo">
          <span class="logo-mark"><app-icon name="shield-check" [size]="18"></app-icon></span>
          <span class="logo-text">ProofCart</span>
        </a>

        <div class="nav-center">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="nav-link">
            <app-icon name="home" [size]="16"></app-icon> Home
          </a>
          <ng-container *ngIf="supabase.session$ | async">
            <a routerLink="/orders" routerLinkActive="active" class="nav-link">
              <app-icon name="package" [size]="16"></app-icon> Orders
            </a>
            <a *ngIf="isSeller()" routerLink="/seller" routerLinkActive="active" class="nav-link">
              <app-icon name="store" [size]="16"></app-icon> Seller Console
            </a>
          </ng-container>
        </div>

        <div class="nav-right">
          <ng-container *ngIf="(supabase.session$ | async) === null">
            <a routerLink="/auth/login" class="nav-link">Sign In</a>
            <a routerLink="/auth/signup" class="btn btn-primary btn-sm">Sign Up</a>
          </ng-container>

          <ng-container *ngIf="supabase.session$ | async as session">
            <span class="user-email">{{ session.user.email }}</span>
            <button class="btn-icon" (click)="signOut()" aria-label="Sign out">
              <app-icon name="log-out" [size]="16"></app-icon>
            </button>
          </ng-container>

          <div class="status-pill">
            <app-icon name="lock" [size]="12"></app-icon>
            <span>Policy&#8209;gated</span>
          </div>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      position: sticky;
      top: 0;
      z-index: 100;
      background: var(--surface);
      border-bottom: 1px solid var(--border);
    }

    .nav-inner {
      max-width: 1160px;
      margin: 0 auto;
      padding: 0 1.5rem;
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1.5rem;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
      color: var(--text-primary);
      flex-shrink: 0;
    }

    .logo-mark {
      width: 30px;
      height: 30px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--radius-sm);
      background: var(--accent);
      color: #fff;
    }

    .logo-text {
      font-size: 1.0625rem;
      font-weight: 700;
      letter-spacing: -0.02em;
    }

    .nav-center {
      display: flex;
      gap: 0.25rem;
      flex: 1;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: 0.4375rem;
      padding: 0.5rem 0.75rem;
      border-radius: var(--radius-sm);
      color: var(--text-secondary);
      text-decoration: none;
      font-weight: 500;
      font-size: 0.875rem;
      transition: background-color 0.15s ease, color 0.15s ease;
    }

    .nav-link:hover {
      color: var(--text-primary);
      background: var(--surface-2);
    }

    .nav-link.active {
      color: var(--accent);
      background: var(--accent-tint);
    }

    .nav-right {
      display: flex;
      align-items: center;
      gap: 0.875rem;
      flex-shrink: 0;
    }

    .user-email {
      font-size: 0.8125rem;
      color: var(--text-muted);
    }

    .status-pill {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      padding: 0.3125rem 0.75rem;
      border-radius: var(--radius-full);
      background: var(--success-tint);
      border: 1px solid var(--success-border);
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--success);
    }

    @media (max-width: 760px) {
      .nav-center { display: none; }
      .user-email { display: none; }
    }
  `]
})
export class NavbarComponent {
  constructor(public supabase: SupabaseService, private router: Router) {}

  async signOut() {
    await this.supabase.signOut();
    this.router.navigate(['/auth/login']);
  }

  isSeller() {
    return this.supabase.currentUser?.user_metadata?.['marketplace_role'] === 'MERCHANT';
  }
}
