import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface MarketplaceProfile {
  id: string;
  name: string;
  role: 'BUYER' | 'MERCHANT';
  city: string;
}

export interface SellerProduct {
  id: string;
  name: string;
  description: string;
  pricePaise: number;
  stockQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  dietaryTags: string[];
  allergens: string[];
  deliveryDays: number;
  returnable: boolean;
  subscriptionAvailable: boolean;
}

@Injectable({providedIn: 'root'})
export class MarketplaceService {
  constructor(private http: HttpClient) {
  }

  me(): Observable<MarketplaceProfile> {
    return this.http.get<MarketplaceProfile>('/api/account/me');
  }

  sellerDashboard(): Observable<any> {
    return this.http.get('/api/merchant/dashboard');
  }

  createStore(name: string, description: string): Observable<any> {
    return this.http.post('/api/merchant/store', {name, description});
  }

  createProduct(product: Omit<SellerProduct, 'id' | 'reservedQuantity' | 'availableQuantity'>): Observable<any> {
    return this.http.post('/api/merchant/products', product);
  }

  updateProduct(id: string, product: Omit<SellerProduct, 'id' | 'reservedQuantity' | 'availableQuantity'>): Observable<any> {
    return this.http.put(`/api/merchant/products/${id}`, product);
  }
}
