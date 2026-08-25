import {Component} from '@angular/core';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav class="glass-panel navbar">
      <div class="nav-content">
        <a routerLink="/" class="logo text-gradient">NutriBasket AI</a>
        <div class="nav-links">
          <a routerLink="/" class="nav-link">Shop</a>
          <span class="badge badge-success">Safe Shopping Active</span>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      position: sticky;
      top: 0;
      z-index: 50;
      border-radius: 0 0 16px 16px;
      margin-bottom: 2rem;
    }
    .nav-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 1rem 2rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .logo {
      font-size: 1.5rem;
      font-weight: 700;
      text-decoration: none;
    }
    .nav-links {
      display: flex;
      gap: 1.5rem;
      align-items: center;
    }
    .nav-link {
      color: var(--text-main);
      text-decoration: none;
      font-weight: 500;
      transition: color 0.2s;
    }
    .nav-link:hover {
      color: var(--primary-glow);
    }
  `]
})
export class NavbarComponent {
}
