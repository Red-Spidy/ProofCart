import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {SupabaseService} from '../../../services/supabase.service';
import {MarketplaceService} from '../../../services/marketplace';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card glass-panel">
        <div class="auth-header">
          <div class="auth-logo">🛡️</div>
          <h1>Welcome Back</h1>
          <p class="text-muted">Sign in to your ProofCart account</p>
        </div>

        <form (ngSubmit)="signIn()" class="auth-form">
          <div class="field-group">
            <label for="email">Email</label>
            <input
              id="email"
              type="email"
              class="input-glass"
              [(ngModel)]="email"
              name="email"
              placeholder="you@example.com"
              required
              autocomplete="email"
            />
          </div>

          <div class="field-group">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              class="input-glass"
              [(ngModel)]="password"
              name="password"
              placeholder="••••••••"
              required
              autocomplete="current-password"
            />
          </div>

          <div class="auth-error glass-panel" *ngIf="errorMessage">
            ⚠️ {{ errorMessage }}
          </div>

          <button type="submit" class="btn btn-primary btn-full" [disabled]="loading">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div class="auth-links">
          <a routerLink="/auth/forgot-password" class="text-secondary">Forgot password?</a>
          <span class="text-muted">·</span>
          <a routerLink="/auth/signup" class="text-secondary">Create account</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: 80vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem;
    }
    .auth-card {
      width: 100%;
      max-width: 420px;
      padding: 2.5rem;
    }
    .auth-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .auth-logo {
      font-size: 3rem;
      margin-bottom: 1rem;
    }
    .auth-header h1 {
      font-size: 1.75rem;
      margin-bottom: 0.5rem;
    }
    .auth-form {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }
    .field-group {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
    }
    .field-group label {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--text-secondary);
    }
    .btn-full { width: 100%; justify-content: center; padding: 0.875rem; font-size: 1rem; }
    .auth-error {
      padding: 0.875rem 1rem;
      border-color: rgba(244, 63, 94, 0.3);
      background: rgba(244, 63, 94, 0.07);
      color: var(--accent-rose);
      font-size: 0.875rem;
    }
    .auth-links {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 0.75rem;
      margin-top: 1.5rem;
      font-size: 0.875rem;
    }
    .auth-links a { color: var(--accent-blue-light); text-decoration: none; }
    .auth-links a:hover { text-decoration: underline; }
  `]
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;
  errorMessage = '';

  constructor(private supabase: SupabaseService, private router: Router, private marketplace: MarketplaceService) {
  }

  async signIn() {
    if (!this.email || !this.password) return;
    this.loading = true;
    this.errorMessage = '';

    const {data, error} = await this.supabase.signIn(this.email, this.password);

    if (error) {
      this.errorMessage = error.message;
      this.loading = false;
    } else {
      // Keep the backend profile in sync for installations where the
      // Supabase signup trigger was not installed yet. Existing profiles are
      // intentionally never overwritten by this endpoint.
      const user = data.user;
      const role = user?.user_metadata?.['marketplace_role'] === 'MERCHANT' ? 'MERCHANT' : 'BUYER';
      const name = user?.user_metadata?.['display_name'] || user?.email || 'Shopper';
      this.marketplace.createProfile(name, role).subscribe({
        next: () => this.router.navigate(['/']),
        error: () => this.router.navigate(['/'])
      });
    }
  }
}
