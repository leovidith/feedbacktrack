import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-brand">
          <span class="auth-mark">FT</span>
          <span class="auth-name">FeedbackTrack</span>
        </div>

        <h1>Sign in</h1>
        <p class="muted">Internal employee feedback &amp; recognition portal</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <label>Email
            <input type="email" formControlName="email" autocomplete="email" placeholder="you@company.com" />
          </label>

          <label>Password
            <input type="password" formControlName="password" autocomplete="current-password" placeholder="••••••••" />
          </label>

          <button class="btn btn-primary btn-block" type="submit" [disabled]="form.invalid || loading">
            {{ loading ? 'Signing in…' : 'Sign in' }}
          </button>

          <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .auth-card {
      background: #fff;
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 2.25rem;
      width: 100%;
      max-width: 400px;
      box-shadow: 0 10px 30px -10px rgba(15, 23, 42, 0.15);
    }
    .auth-brand { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 1.5rem; }
    .auth-mark {
      display: inline-flex; align-items: center; justify-content: center;
      width: 36px; height: 36px;
      background: var(--primary); color: #fff;
      border-radius: 6px; font-weight: 700; font-size: 0.9rem;
    }
    .auth-name { font-weight: 700; font-size: 1.1rem; }
    h1 { margin-bottom: 0.25rem; }
    .muted { margin: 0 0 1.5rem; }
    .btn-block { width: 100%; padding: 0.65rem; font-size: 0.95rem; margin-top: 0.5rem; }
    .alert { margin: 1rem 0 0; }
    @media (max-width: 480px) {
      .auth-card { padding: 1.5rem; }
    }
  `]
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  loading = false;
  error = '';

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/home']),
      error: err => {
        this.loading = false;
        this.error = this.readError(err) ?? 'Login failed';
      }
    });
  }

  private readError(err: any): string | null {
    if (!err) return null;
    if (typeof err.error === 'string') return err.error;
    if (err.error?.error) return err.error.error;
    if (err.message) return err.message;
    return null;
  }

  ngOnInit(): void {
    if (this.auth.isLoggedIn) {
      this.router.navigate(['/home']);
    }
  }

}
