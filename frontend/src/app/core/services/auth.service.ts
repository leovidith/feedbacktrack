import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { LoginResponse, Role } from '../models/models';
import { environment } from '../../../environments/environment';

export interface AuthSession {
  token: string;
  userId: number;
  email: string;
  name: string;
  role: Role;
}

const STORAGE_KEY = 'feedbacktrack.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private session$ = new BehaviorSubject<AuthSession | null>(this.loadFromStorage());

  readonly state$ = this.session$.asObservable();

  get session(): AuthSession | null {
    return this.session$.value;
  }

  get isLoggedIn(): boolean {
    return this.session !== null;
  }

  get role(): Role | null {
    return this.session?.role ?? null;
  }

  get userId(): number | null {
    return this.session?.userId ?? null;
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.gatewayBaseUrl}/api/admin/auth/login`, { email, password })
      .pipe(
        tap(res => {
          const userId = this.extractUserIdFromJwt(res.token);
          const session: AuthSession = {
            token: res.token,
            userId,
            email,
            name: res.name,
            role: res.role
          };
          localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
          this.session$.next(session);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session$.next(null);
    this.router.navigate(['/login']);
  }

  hasRole(roles: Role[]): boolean {
    const r = this.role;
    return r !== null && roles.includes(r);
  }

  private loadFromStorage(): AuthSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthSession) : null;
    } catch {
      return null;
    }
  }

  private extractUserIdFromJwt(token: string): number {
    const parts = token.split('.');
    if (parts.length < 2) {
      throw new Error('Malformed JWT');
    }
    const payload = JSON.parse(this.b64UrlDecode(parts[1]));
    return Number(payload.sub);
  }

  private b64UrlDecode(s: string): string {
    const pad = '='.repeat((4 - (s.length % 4)) % 4);
    const b64 = (s + pad).replace(/-/g, '+').replace(/_/g, '/');
    return atob(b64);
  }
}
