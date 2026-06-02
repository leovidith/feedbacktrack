import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Change password</h1>
      <p class="page-description">Update the password for your own account. You will stay signed in.</p>
    </div>

    <div class="card">
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label>Current password
          <input type="password" formControlName="oldPassword" autocomplete="current-password" />
        </label>

        <label>New password
          <input type="password" formControlName="newPassword" autocomplete="new-password" />
          <span class="field-help">Must be at least 8 characters.</span>
        </label>

        <label>Confirm new password
          <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
          <span class="field-help" *ngIf="form.errors?.['mismatch'] && form.controls.confirmPassword.touched"
                style="color: var(--danger)">
            Passwords don't match.
          </span>
        </label>

        <div class="form-actions">
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
            {{ saving ? 'Updating…' : 'Update password' }}
          </button>
          <div class="alert alert-success" *ngIf="success" style="margin: 0; padding: 0.4rem 0.8rem;">
            Password updated.
          </div>
          <div class="alert alert-danger" *ngIf="error" style="margin: 0; padding: 0.4rem 0.8rem;">
            {{ error }}
          </div>
        </div>
      </form>
    </div>
  `
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private users$ = inject(UserService);
  private auth = inject(AuthService);

  saving = false;
  success = false;
  error = '';

  form = this.fb.nonNullable.group(
    {
      oldPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: [matchPasswords] }
  );

  submit() {
    if (this.form.invalid) return;
    const email = this.auth.session?.email;
    if (!email) {
      this.error = 'You must be signed in.';
      return;
    }

    this.saving = true;
    this.success = false;
    this.error = '';

    const raw = this.form.getRawValue();
    this.users$
      .changePassword({
        email,
        oldPassword: raw.oldPassword,
        newPassword: raw.newPassword
      })
      .subscribe({
        next: msg => {
          this.saving = false;
          if (msg && msg.toLowerCase().includes('does not match')) {
            this.error = msg;
          } else {
            this.success = true;
            this.form.reset({ oldPassword: '', newPassword: '', confirmPassword: '' });
          }
        },
        error: err => {
          this.saving = false;
          this.error = readApiError(err);
        }
      });
  }
}

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const newPw = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return newPw && confirm && newPw !== confirm ? { mismatch: true } : null;
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}
