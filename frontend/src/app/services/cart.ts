import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private apiUrl = '/api';

  constructor(private http: HttpClient) {}

  createProofCart(merchantId: string, intentContractId: string | null, items: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/proof-carts`, {
      merchantId,
      intentContractId,
      items
    });
  }

  getCart(cartId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/proof-carts/${cartId}`);
  }

  approveCart(cartId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/proof-carts/${cartId}/approve`, {});
  }

  createCheckout(cartId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/checkout/create`, {cartId});
  }

  verifyPayment(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/payments/verify`, payload);
  }

  getAuditReceipt(orderId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/audit-receipts/${orderId}`);
  }

  getCatalog(merchantId: string, searchQuery?: string): Observable<any> {
    const url = searchQuery
      ? `${this.apiUrl}/catalog/${merchantId}?q=${encodeURIComponent(searchQuery)}`
      : `${this.apiUrl}/catalog/${merchantId}`;
    return this.http.get(url);
  }
}

