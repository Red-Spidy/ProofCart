import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {SupabaseService} from '../../../services/supabase.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card glass-panel">
        <div class="auth-header">
          <div class="auth-logo">🔑</div>
          <h1>Reset Password</h1>
          <p class="text-muted">Enter your email to receive a reset link</p>
        </div>

        <!-- Success state -->
        <div class="success-card glass-panel" *ngIf="success">
          <div class="success-icon">✉️</div>
          <h3>Check your email</h3>
          <p>If an account exists for <strong>{{ email }}</strong>, we've sent instructions to reset your password.</p>
          <a routerLink="/auth/login" class="btn btn-primary btn-full" style="margin-top:1rem">Back to Sign In</a>
        </div>

        <form (ngSubmit)="resetPassword()" class="auth-form" *ngIf="!success">
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

          <div class="auth-error glass-panel" *ngIf="errorMessage">
            ⚠️ {{ errorMessage }}
          </div>

          <button type="submit" class="btn btn-primary btn-full" [disabled]="loading || !email">
            {{ loading ? 'Sending...' : 'Send Reset Link' }}
          </button>
        </form>

        <div class="auth-links" *ngIf="!success">
          <a routerLink="/auth/login" class="text-secondary">← Back to sign in</a>
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
export class ForgotPasswordComponent {
  email = '';
  loading = false;
  errorMessage = '';
  success = false;

  constructor(private supabase: SupabaseService) {}

  async resetPassword() {
    if (!this.email) return;
    this.loading = true;
    this.errorMessage = '';

    const {error} = await this.supabase.forgotPassword(this.email);

    if (error) {
      this.errorMessage = error.message;
      this.loading = false;
    } else {
      this.success = true;
      this.loading = false;
    }
  }
}
