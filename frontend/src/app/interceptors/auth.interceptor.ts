import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {SupabaseService} from '../services/supabase.service';
import {from, switchMap} from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const supabase = inject(SupabaseService);

  // Always do a live getSession() call — Supabase returns it from its internal
  // cache instantly if the token is in localStorage. This is the only reliable
  // way to get a token that is guaranteed to exist on first page load, because
  // the BehaviorSubject may not be populated yet when Angular components fire
  // their first HTTP request in ngOnInit.
  return from(supabase.getAccessToken()).pipe(
    switchMap(token => {
      if (token) {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        return next(authReq);
      }
      // No token — pass through for public routes (catalog, intents, webhooks)
      return next(req);
    })
  );
};
