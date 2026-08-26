import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {SupabaseService} from '../services/supabase.service';
import {from, map, take} from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const supabase = inject(SupabaseService);
  const router = inject(Router);

  return from(supabase.getAccessToken()).pipe(
    take(1),
    map(token => {
      if (token) {
        return true;
      }
      router.navigate(['/auth/login']);
      return false;
    })
  );
};

/** Guard for already-authenticated users (redirect away from login/signup pages) */
export const guestGuard: CanActivateFn = () => {
  const supabase = inject(SupabaseService);
  const router = inject(Router);

  return from(supabase.getAccessToken()).pipe(
    take(1),
    map(token => {
      if (!token) {
        return true;
      }
      router.navigate(['/']);
      return false;
    })
  );
};
