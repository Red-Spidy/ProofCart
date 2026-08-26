import {Injectable} from '@angular/core';
import {AuthChangeEvent, createClient, Session, SupabaseClient, User} from '@supabase/supabase-js';
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
        detectSessionInUrl: true,
        storageKey: 'supabase.auth.token'
      }
    });

    // Listen for auth state changes FIRST — this fires synchronously with the
    // existing persisted session when the SDK initializes, before any HTTP calls
    this.supabase.auth.onAuthStateChange((event: AuthChangeEvent, session: Session | null) => {
      this._session$.next(session);
      this._user$.next(session?.user ?? null);
    });

    // Belt-and-suspenders: also do an explicit getSession to catch any edge cases
    this.supabase.auth.getSession().then(({data}) => {
      if (data.session && !this._session$.getValue()) {
        this._session$.next(data.session);
        this._user$.next(data.session.user);
      }
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
    // First try the in-memory session (populated synchronously from localStorage on init)
    const inMemorySession = this._session$.getValue();
    if (inMemorySession?.access_token) {
      return inMemorySession.access_token;
    }
    // Fallback: ask Supabase SDK directly (handles first-load race condition)
    const {data} = await this.supabase.auth.getSession();
    if (data.session) {
      // Sync the BehaviorSubject so future calls are instant
      this._session$.next(data.session);
      this._user$.next(data.session.user);
    }
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
