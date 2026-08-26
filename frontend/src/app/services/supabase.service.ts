import {Injectable} from '@angular/core';
import {createClient, SupabaseClient, Session, User, AuthChangeEvent} from '@supabase/supabase-js';
import {BehaviorSubject, Observable} from 'rxjs';

const SUPABASE_URL = 'https://fqrdwzzyzckinlkryand.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZxcmR3enp5emNraW5sa3J5YW5kIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc3MjY4NTQsImV4cCI6MjEwMzMwMjg1NH0.zeiMPbVUC0ql4mU472Ia9t7_J13zl2w7S8H__ab81SQ';

@Injectable({
  providedIn: 'root'
})
export class SupabaseService {
  private supabase: SupabaseClient;
  private _session$ = new BehaviorSubject<Session | null>(null);
  private _user$ = new BehaviorSubject<User | null>(null);

  readonly session$: Observable<Session | null> = this._session$.asObservable();
  readonly user$: Observable<User | null> = this._user$.asObservable();

  constructor() {
    this.supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      auth: {
        autoRefreshToken: true,
        persistSession: true,
        detectSessionInUrl: true
      }
    });

    // Restore session on init
    this.supabase.auth.getSession().then(({data}) => {
      this._session$.next(data.session);
      this._user$.next(data.session?.user ?? null);
    });

    // Listen for auth state changes (login, logout, token refresh)
    this.supabase.auth.onAuthStateChange((event: AuthChangeEvent, session: Session | null) => {
      this._session$.next(session);
      this._user$.next(session?.user ?? null);
    });
  }

  /** Sign up with email and password */
  async signUp(email: string, password: string) {
    return this.supabase.auth.signUp({email, password});
  }

  /** Sign in with email and password */
  async signIn(email: string, password: string) {
    return this.supabase.auth.signInWithPassword({email, password});
  }

  /** Sign out the current user */
  async signOut() {
    return this.supabase.auth.signOut();
  }

  /** Send password reset email */
  async forgotPassword(email: string) {
    return this.supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/auth/reset-password`
    });
  }

  /** Get the current access token (for HTTP interceptor) */
  async getAccessToken(): Promise<string | null> {
    const {data} = await this.supabase.auth.getSession();
    return data.session?.access_token ?? null;
  }

  /** Get current user synchronously */
  get currentUser(): User | null {
    return this._user$.getValue();
  }

  /** Check if user is logged in */
  get isLoggedIn(): boolean {
    return this._session$.getValue() !== null;
  }
}
