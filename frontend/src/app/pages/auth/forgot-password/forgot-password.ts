import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {SupabaseService} from '../../../services/supabase.service';
import {IconComponent} from '../../../components/icon/icon';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent],
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card panel">
        <div class="auth-header">
          <div class="auth-logo"><app-icon name="key" [size]="20"></app-icon></div>
          <h1>Reset your password</h1>
          <p class="text-muted">Enter your email to receive a reset link</p>
        </div>

        <div class="form-note success" *ngIf="success">
          <app-icon name="mail" [size]="16"></app-icon>
          <span>If an account exists for <strong>{{ email }}</strong>, we've sent instructions to reset your password.</span>
        </div>
        <a *ngIf="success" routerLink="/auth/login" class="btn btn-primary btn-full" style="margin-top:1rem">Back to sign in</a>

        <form (ngSubmit)="resetPassword()" class="auth-form" *ngIf="!success">
          <div class="field-group">
            <label for="email">Email</label>
            <input id="email" type="email" class="input" [(ngModel)]="email" name="email"
                   placeholder="you@example.com" required autocomplete="email"/>
          </div>

          <div class="form-note danger" *ngIf="errorMessage">
            <app-icon name="alert-triangle" [size]="16"></app-icon>
            <span>{{ errorMessage }}</span>
          </div>

          <button type="submit" class="btn btn-primary btn-full" [disabled]="loading || !email">
            {{ loading ? 'Sending…' : 'Send Reset Link' }}
          </button>
        </form>

        <div class="auth-links" *ngIf="!success">
          <a routerLink="/auth/login"><app-icon name="arrow-left" [size]="13"></app-icon> Back to sign in</a>
        </div>
      </div>
    </div>
  `
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
