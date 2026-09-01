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

  getUpsellSuggestions(cartId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/proof-carts/${cartId}/upsell`);
  }

  addCartItem(cartId: string, productId: string, quantity: number = 1): Observable<any> {
    return this.http.post(`${this.apiUrl}/proof-carts/${cartId}/items`, {productId, quantity});
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

  verifyAuditChain(orderId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/audit-receipts/${orderId}/verify`);
  }

  getCatalog(merchantId: string, searchQuery?: string): Observable<any> {
    const url = searchQuery
      ? `${this.apiUrl}/catalog/${merchantId}?q=${encodeURIComponent(searchQuery)}`
      : `${this.apiUrl}/catalog/${merchantId}`;
    return this.http.get(url);
  }

  recordPreference(eventType: 'SEARCH' | 'LIKE' | 'DISLIKE', productId?: string, searchTerm?: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/personalization/events`, {eventType, productId, searchTerm});
  }

  getRecommendations(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/personalization/recommendations?merchantId=${merchantId}`);
  }
}
