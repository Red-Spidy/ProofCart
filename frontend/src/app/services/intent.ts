import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface IntentRules {
  maxTotalPaise: number | null;
  mustHaveTags: string[];
  excludedAllergens: string[];
  deliveryRequirement: string | null;
  subscriptionAllowed: boolean;
  mustBeReturnable: boolean;
  needsClarification: boolean;
  clarificationQuestion: string | null;
  confidence: number;
}

export interface IntentExtraction {
  intentId: string;
  rules: IntentRules;
  source: string;
}

@Injectable({
  providedIn: 'root'
})
export class IntentService {
  private apiUrl = '/api/intents';

  constructor(private http: HttpClient) {}

  parseIntent(prompt: string): Observable<IntentExtraction> {
    return this.http.post<IntentExtraction>(`${this.apiUrl}/parse`, {prompt});
  }
}
