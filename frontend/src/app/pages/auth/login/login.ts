import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {SupabaseService} from '../../../services/supabase.service';
import {MarketplaceService} from '../../../services/marketplace';
import {IconComponent} from '../../../components/icon/icon';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent],
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card panel">
        <div class="auth-header">
          <div class="auth-logo"><app-icon name="shield-check" [size]="22"></app-icon></div>
          <h1>Welcome back</h1>
          <p class="text-muted">Sign in to your ProofCart account</p>
        </div>

        <form (ngSubmit)="signIn()" class="auth-form">
          <div class="field-group">
            <label for="email">Email</label>
            <input id="email" type="email" class="input" [(ngModel)]="email" name="email"
                   placeholder="you@example.com" required autocomplete="email"/>
          </div>

          <div class="field-group">
            <label for="password">Password</label>
            <input id="password" type="password" class="input" [(ngModel)]="password" name="password"
                   placeholder="••••••••" required autocomplete="current-password"/>
          </div>

          <div class="form-note danger" *ngIf="errorMessage">
            <app-icon name="alert-triangle" [size]="16"></app-icon>
            <span>{{ errorMessage }}</span>
          </div>

          <button type="submit" class="btn btn-primary btn-full" [disabled]="loading">
            {{ loading ? 'Signing in…' : 'Sign In' }}
          </button>
        </form>

        <div class="auth-links">
          <a routerLink="/auth/forgot-password">Forgot password?</a>
          <span class="text-muted">·</span>
          <a routerLink="/auth/signup">Create account</a>
        </div>
      </div>
    </div>
  `
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
      this.marketplace.createProfile(name, role).subscribe({error: () => {}});

      this.loading = false;
      this.router.navigate(['/']);
    }
  }
}
