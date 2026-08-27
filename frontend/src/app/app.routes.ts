import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home';
import {CartReviewComponent} from './pages/cart-review/cart-review';
import {CheckoutComponent} from './pages/checkout/checkout';
import {AuditReceiptComponent} from './pages/audit-receipt/audit-receipt';
import {LoginComponent} from './pages/auth/login/login';
import {SignupComponent} from './pages/auth/signup/signup';
import {ForgotPasswordComponent} from './pages/auth/forgot-password/forgot-password';
import {OrdersComponent} from './pages/orders/orders';
import {SellerDashboardComponent} from './pages/seller-dashboard/seller-dashboard';
import {authGuard, guestGuard} from './guards/auth.guard';

export const routes: Routes = [
  // Auth routes (only accessible if NOT logged in)
  {path: 'auth/login', component: LoginComponent, canActivate: [guestGuard]},
  {path: 'auth/signup', component: SignupComponent, canActivate: [guestGuard]},
  {path: 'auth/forgot-password', component: ForgotPasswordComponent, canActivate: [guestGuard]},
  
  // Protected app routes
  {path: '', component: HomeComponent, canActivate: [authGuard]},
  {path: 'orders', component: OrdersComponent, canActivate: [authGuard]},
  {path: 'seller', component: SellerDashboardComponent, canActivate: [authGuard]},
  {path: 'review/:cartId', component: CartReviewComponent, canActivate: [authGuard]},
  {path: 'checkout/:cartId', component: CheckoutComponent, canActivate: [authGuard]},
  {path: 'receipt/:orderId', component: AuditReceiptComponent, canActivate: [authGuard]},
  
  {path: '**', redirectTo: ''}
];
