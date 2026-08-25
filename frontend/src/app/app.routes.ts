import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home';
import {CartReviewComponent} from './pages/cart-review/cart-review';
import {CheckoutComponent} from './pages/checkout/checkout';
import {AuditReceiptComponent} from './pages/audit-receipt/audit-receipt';

export const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'review/:cartId', component: CartReviewComponent},
  {path: 'checkout/:cartId', component: CheckoutComponent},
  {path: 'receipt/:orderId', component: AuditReceiptComponent},
  {path: '**', redirectTo: ''}
];
