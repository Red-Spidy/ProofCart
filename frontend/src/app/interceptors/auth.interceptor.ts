import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {SupabaseService} from '../services/supabase.service';
import {from, switchMap} from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const supabase = inject(SupabaseService);

  // Convert the promise to an observable
  return from(supabase.getAccessToken()).pipe(
    switchMap(token => {
      // If we have a token, clone the request and add the Authorization header
      if (token) {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        return next(authReq);
      }
      
      // If no token, just pass the original request (e.g. for public routes)
      return next(req);
    })
  );
};
