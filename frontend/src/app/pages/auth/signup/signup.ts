import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {SupabaseService} from '../../../services/supabase.service';
import {IconComponent} from '../../../components/icon/icon';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent],
  template: `
    <div class="auth-page animate-fade-in">
      <div class="auth-card panel">
        <div class="auth-header">
          <div class="auth-logo"><app-icon name="cart" [size]="20"></app-icon></div>
          <h1>Create your account</h1>
          <p class="text-muted">Start shopping with a policy-gated AI agent</p>
        </div>

        <div class="form-note success" *ngIf="success">
          <app-icon name="mail" [size]="16"></app-icon>
          <span>We sent a confirmation link to <strong>{{ email }}</strong>. Click it to verify your account, then sign in.</span>
        </div>
        <a *ngIf="success" routerLink="/auth/login" class="btn btn-primary btn-full" style="margin-top:1rem">Go to sign in</a>

        <form (ngSubmit)="signUp()" class="auth-form" *ngIf="!success">
          <div class="field-group">
            <label for="name">Your name</label>
            <input id="name" class="input" [(ngModel)]="name" name="name" placeholder="Your name or store owner name" required autocomplete="name"/>
          </div>

          <div class="field-group">
            <label for="role">I want to</label>
            <select id="role" class="input" [(ngModel)]="role" name="role">
              <option value="BUYER">Shop products</option>
              <option value="MERCHANT">Sell products</option>
            </select>
          </div>

          <div class="field-group">
            <label for="email">Email</label>
            <input id="email" type="email" class="input" [(ngModel)]="email" name="email"
                   placeholder="you@example.com" required autocomplete="email"/>
          </div>

          <div class="field-group">
            <label for="password">Password</label>
            <input id="password" type="password" class="input" [(ngModel)]="password" name="password"
                   placeholder="At least 8 characters" required minlength="8" autocomplete="new-password"/>
          </div>

          <div class="field-group">
            <label for="confirm">Confirm password</label>
            <input id="confirm" type="password" class="input" [(ngModel)]="confirmPassword" name="confirm"
                   placeholder="Re-enter your password" required autocomplete="new-password"/>
          </div>

          <div class="form-note danger" *ngIf="errorMessage">
            <app-icon name="alert-triangle" [size]="16"></app-icon>
            <span>{{ errorMessage }}</span>
          </div>

          <button type="submit" class="btn btn-primary btn-full" [disabled]="loading">
            {{ loading ? 'Creating account…' : 'Create Account' }}
          </button>
        </form>

        <div class="auth-links" *ngIf="!success">
          <span class="text-muted">Already have an account?</span>
          <a routerLink="/auth/login">Sign in</a>
        </div>
      </div>
    </div>
  `
})
export class SignupComponent {
  email = '';
  name = '';
  role: 'BUYER' | 'MERCHANT' = 'BUYER';
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

    const {error} = await this.supabase.signUp(this.email, this.password, this.name, this.role);

    if (error) {
      this.errorMessage = error.message;
      this.loading = false;
    } else {
      this.success = true;
      this.loading = false;
    }
  }
}
