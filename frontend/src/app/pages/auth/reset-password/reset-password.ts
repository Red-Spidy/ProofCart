import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {SupabaseService} from '../../../services/supabase.service';
import {IconComponent} from '../../../components/icon/icon';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent],
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card panel">
        <div class="auth-header">
          <div class="auth-logo"><app-icon name="key" [size]="20"></app-icon></div>
          <h1>Set a new password</h1>
          <p class="text-muted">Choose a new password for your account</p>
        </div>

        <div class="form-note success" *ngIf="success">
          <app-icon name="check" [size]="16"></app-icon>
          <span>Your password has been updated.</span>
        </div>
        <a *ngIf="success" routerLink="/" class="btn btn-primary btn-full" style="margin-top:1rem">Continue to ProofCart</a>

        <form (ngSubmit)="submit()" class="auth-form" *ngIf="!success">
          <div class="field-group">
            <label for="password">New password</label>
            <input id="password" type="password" class="input" [(ngModel)]="password" name="password"
                   placeholder="At least 8 characters" required minlength="8" autocomplete="new-password"/>
          </div>

          <div class="field-group">
            <label for="confirm">Confirm new password</label>
            <input id="confirm" type="password" class="input" [(ngModel)]="confirmPassword" name="confirm"
                   placeholder="Re-enter your new password" required autocomplete="new-password"/>
          </div>

          <div class="form-note danger" *ngIf="errorMessage">
            <app-icon name="alert-triangle" [size]="16"></app-icon>
            <span>{{ errorMessage }}</span>
          </div>

          <button type="submit" class="btn btn-primary btn-full" [disabled]="loading">
            {{ loading ? 'Updating…' : 'Update Password' }}
          </button>
        </form>

        <div class="auth-links" *ngIf="!success">
          <a routerLink="/auth/forgot-password">Request a new link</a>
          <span class="text-muted">·</span>
          <a routerLink="/auth/login">Back to sign in</a>
        </div>
      </div>
    </div>
  `
})
export class ResetPasswordComponent {
  password = '';
  confirmPassword = '';
  loading = false;
  errorMessage = '';
  success = false;

  constructor(private supabase: SupabaseService, private router: Router) {
  }

  async submit() {
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

    const {error} = await this.supabase.updatePassword(this.password);

    if (error) {
      this.errorMessage = 'This reset link is invalid or has expired. Request a new one below.';
      this.loading = false;
    } else {
      this.success = true;
      this.loading = false;
    }
  }
}
