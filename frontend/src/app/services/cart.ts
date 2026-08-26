import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private apiUrl = '/api';

  // The buyer ID is stored in localStorage after the first use.
  // In Phase 2 this will be replaced with Supabase JWT.
  private readonly BUYER_ID_KEY = 'proofcart_buyer_id';

  constructor(private http: HttpClient) {
  }

  /** Returns (or lazily creates) a stable UUID for this browser session. */
  getBuyerId(): string {
    let id = localStorage.getItem(this.BUYER_ID_KEY);
    if (!id) {
      // Generate a v4 UUID
      id = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
      });
      localStorage.setItem(this.BUYER_ID_KEY, id);
    }
    return id;
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({'X-Buyer-Id': this.getBuyerId()});
  }

  createProofCart(merchantId: string, intentContractId: string | null, items: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/proof-carts`, {
      merchantId,
      intentContractId,
      items
    }, {headers: this.authHeaders()});
  }

  getCart(cartId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/proof-carts/${cartId}`, {headers: this.authHeaders()});
  }

  approveCart(cartId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/proof-carts/${cartId}/approve`, {}, {headers: this.authHeaders()});
  }

  createCheckout(cartId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/checkout/create`, {cartId}, {headers: this.authHeaders()});
  }

  verifyPayment(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/payments/verify`, payload, {headers: this.authHeaders()});
  }

  getAuditReceipt(orderId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/audit-receipts/${orderId}`, {headers: this.authHeaders()});
  }

  getCatalog(merchantId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/catalog/${merchantId}`);
  }
}
