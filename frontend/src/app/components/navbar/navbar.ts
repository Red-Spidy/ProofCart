import {Component} from '@angular/core';
import {RouterLink, RouterLinkActive, Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {SupabaseService} from '../../services/supabase.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  template: `
    <nav class="navbar">
      <div class="nav-inner">
        <a routerLink="/" class="logo">
          <span class="logo-icon">🛒</span>
          <span class="logo-text">
            <span class="text-gradient">Nutri</span><span>Basket</span>
          </span>
        </a>

        <div class="nav-center">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="nav-link">
            <span>🏠</span> Home
          </a>
          <ng-container *ngIf="supabase.session$ | async">
            <a routerLink="/orders" routerLinkActive="active" class="nav-link">
              <span>📦</span> Orders
            </a>
          </ng-container>
        </div>

        <div class="nav-right">
          <!-- Shown when logged out -->
          <ng-container *ngIf="(supabase.session$ | async) === null">
            <a routerLink="/auth/login" class="nav-link">Sign In</a>
            <a routerLink="/auth/signup" class="btn btn-primary btn-sm">Sign Up</a>
          </ng-container>

          <!-- Shown when logged in -->
          <ng-container *ngIf="supabase.session$ | async as session">
            <span class="user-email text-muted">{{ session.user.email }}</span>
            <button class="nav-link" (click)="signOut()" style="background:transparent; border:none; cursor:pointer;">Sign Out</button>
          </ng-container>

          <div class="status-pill">
            <span class="status-dot"></span>
            <span>AI Protected</span>
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
      background: rgba(10, 14, 26, 0.85);
      backdrop-filter: blur(20px) saturate(1.4);
      -webkit-backdrop-filter: blur(20px) saturate(1.4);
      border-bottom: 1px solid var(--border-subtle);
    }

    .nav-inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 1.5rem;
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
      color: var(--text-primary);
    }

    .logo-icon {
      font-size: 1.5rem;
    }

    .logo-text {
      font-size: 1.25rem;
      font-weight: 800;
      letter-spacing: -0.03em;
    }

    .nav-center {
      display: flex;
      gap: 0.25rem;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      padding: 0.5rem 1rem;
      border-radius: var(--radius-sm);
      color: var(--text-secondary);
      text-decoration: none;
      font-weight: 500;
      font-size: 0.875rem;
      transition: all 0.2s ease;
    }

    .nav-link:hover {
      color: var(--text-primary);
      background: rgba(255, 255, 255, 0.05);
    }

    .nav-link.active {
      color: var(--accent-blue-light);
      background: rgba(59, 130, 246, 0.1);
    }

    .nav-right {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .user-email {
      font-size: 0.875rem;
      margin-right: 0.5rem;
    }

    .btn-sm {
      padding: 0.375rem 0.875rem;
      font-size: 0.875rem;
    }

    .status-pill {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.375rem 0.875rem;
      border-radius: var(--radius-full);
      background: rgba(16, 185, 129, 0.08);
      border: 1px solid rgba(16, 185, 129, 0.2);
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--accent-emerald);
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--accent-emerald);
      animation: pulse-dot 2s ease infinite;
    }

    @keyframes pulse-dot {
      0%, 100% { opacity: 1; box-shadow: 0 0 4px var(--accent-emerald); }
      50% { opacity: 0.5; box-shadow: 0 0 8px var(--accent-emerald); }
    }
  `]
})
export class NavbarComponent {
  constructor(public supabase: SupabaseService, private router: Router) {}

  async signOut() {
    await this.supabase.signOut();
    this.router.navigate(['/auth/login']);
  }
}
