import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {SupabaseService} from '../../../services/supabase.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card glass-panel">
        <div class="auth-header">
          <div class="auth-logo">🛒</div>
          <h1>Create Account</h1>
          <p class="text-muted">Start shopping smarter with AI</p>
        </div>

        <!-- Success state -->
        <div class="success-card glass-panel" *ngIf="success">
          <div class="success-icon">✉️</div>
          <h3>Check your email!</h3>
          <p>We sent a confirmation link to <strong>{{ email }}</strong>. Click it to verify your account, then sign in.</p>
          <a routerLink="/auth/login" class="btn btn-primary btn-full" style="margin-top:1rem">Go to Sign In</a>
        </div>

        <form (ngSubmit)="signUp()" class="auth-form" *ngIf="!success">
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
              placeholder="At least 8 characters"
              required
              minlength="8"
              autocomplete="new-password"
            />
          </div>

          <div class="field-group">
            <label for="confirm">Confirm Password</label>
            <input
              id="confirm"
              type="password"
              class="input-glass"
              [(ngModel)]="confirmPassword"
              name="confirm"
              placeholder="Re-enter your password"
              required
              autocomplete="new-password"
            />
          </div>

          <div class="auth-error glass-panel" *ngIf="errorMessage">
            ⚠️ {{ errorMessage }}
          </div>

          <button type="submit" class="btn btn-primary btn-full" [disabled]="loading">
            {{ loading ? 'Creating account...' : 'Create Account' }}
          </button>
        </form>

        <div class="auth-links" *ngIf="!success">
          <span class="text-muted">Already have an account?</span>
          <a routerLink="/auth/login" class="text-secondary">Sign in</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page { min-height: 80vh; display: flex; align-items: center; justify-content: center; padding: 2rem 1rem; }
    .auth-card { width: 100%; max-width: 420px; padding: 2.5rem; }
    .auth-header { text-align: center; margin-bottom: 2rem; }
    .auth-logo { font-size: 3rem; margin-bottom: 1rem; }
    .auth-header h1 { font-size: 1.75rem; margin-bottom: 0.5rem; }
    .auth-form { display: flex; flex-direction: column; gap: 1.25rem; }
    .field-group { display: flex; flex-direction: column; gap: 0.375rem; }
    .field-group label { font-size: 0.875rem; font-weight: 600; color: var(--text-secondary); }
    .btn-full { width: 100%; justify-content: center; padding: 0.875rem; font-size: 1rem; }
    .auth-error { padding: 0.875rem 1rem; border-color: rgba(244,63,94,0.3); background: rgba(244,63,94,0.07); color: var(--accent-rose); font-size: 0.875rem; }
    .auth-links { display: flex; justify-content: center; align-items: center; gap: 0.5rem; margin-top: 1.5rem; font-size: 0.875rem; }
    .auth-links a { color: var(--accent-blue-light); text-decoration: none; }
    .auth-links a:hover { text-decoration: underline; }
    .success-card { padding: 2rem; text-align: center; border-color: rgba(16,185,129,0.3); background: rgba(16,185,129,0.06); }
    .success-icon { font-size: 3rem; margin-bottom: 1rem; }
    .success-card h3 { margin-bottom: 0.5rem; }
    .success-card p { color: var(--text-secondary); font-size: 0.9375rem; line-height: 1.6; }
  `]
})
export class SignupComponent {
  email = '';
  password = '';
  confirmPassword = '';
  loading = false;
  errorMessage = '';
  success = false;

  constructor(private supabase: SupabaseService, private router: Router) {}

  async signUp() {
    this.errorMessage = '';
    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }
    if (this.password.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters.';
      return;
    }
    this.loading = true;

    const {error} = await this.supabase.signUp(this.email, this.password);

    if (error) {
      this.errorMessage = error.message;
      this.loading = false;
    } else {
      this.success = true;
      this.loading = false;
    }
  }
}
