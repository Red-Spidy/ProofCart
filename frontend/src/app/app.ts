import {Component} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {NavbarComponent} from './components/navbar/navbar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>
    <main class="container">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    main {
      display: block;
      min-height: calc(100vh - 64px);
      padding-top: 2rem;
      padding-bottom: 4rem;
    }
  `]
})
export class AppComponent {
  title = 'ProofCart';
}
