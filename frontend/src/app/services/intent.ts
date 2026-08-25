import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface IntentExtraction {
  rules: any;
  source: string;
}

@Injectable({
  providedIn: 'root'
})
export class IntentService {
  private apiUrl = '/api/intents';

  constructor(private http: HttpClient) {
  }

  parseIntent(prompt: string): Observable<IntentExtraction> {
    return this.http.post<IntentExtraction>(`${this.apiUrl}/parse`, {prompt});
  }
}
