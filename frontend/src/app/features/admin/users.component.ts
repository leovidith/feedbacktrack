import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { DepartmentService } from '../../core/services/department.service';
import { Department, Role, User } from '../../core/models/models';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Manage users</h1>
      <p class="page-description">Onboard new employees and manage account status. The role chosen at creation is final — it cannot be changed later.</p>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">Add user</h2>
          <p class="card-subtitle">A welcome notification will not be sent — share credentials privately.</p>
        </div>
      </div>

      <form [formGroup]="form" (ngSubmit)="add()">
        <div class="field-row">
          <label>Name<input formControlName="name" /></label>
          <label>Email<input formControlName="email" type="email" /></label>
          <label>Password<input formControlName="password" type="password" placeholder="Min. 8 characters" /></label>
        </div>
        <div class="field-row">
          <label>Role
            <select formControlName="role">
              <option value="EMPLOYEE">Employee</option>
              <option value="MANAGER">Manager</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>
          <label>Department
            <select formControlName="departmentId">
              <option [ngValue]="null">(none)</option>
              <option *ngFor="let d of departments" [ngValue]="d.departmentId">{{ d.departmentName }}</option>
            </select>
          </label>
          <label>Manager
            <select formControlName="managerId">
              <option [ngValue]="null">(none)</option>
              <option *ngFor="let m of managers" [ngValue]="m.userId">{{ m.name }}</option>
            </select>
          </label>
        </div>
        <div class="form-actions">
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
            {{ saving ? 'Saving…' : 'Add user' }}
          </button>
          <span class="hint" *ngIf="addMessage">{{ addMessage }}</span>
        </div>
      </form>
    </div>

    <div class="card">
      <div class="card-header">
        <h2 class="card-title">All users <span class="count-badge">{{ users.length }}</span></h2>
      </div>
      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Department</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let u of users">
              <td>{{ u.userId }}</td>
              <td>{{ u.name }}</td>
              <td>{{ u.email }}</td>
              <td><span class="pill" [attr.data-role]="u.role">{{ u.role }}</span></td>
              <td>{{ u.departmentName || '—' }}</td>
              <td><span class="pill" [attr.data-status]="u.status">{{ u.status || '—' }}</span></td>
              <td class="actions-cell">
                <button class="btn btn-danger btn-sm" *ngIf="u.status === 'Active'"
                        [disabled]="busyId === u.userId"
                        (click)="deactivate(u)">
                  {{ busyId === u.userId ? '…' : 'Deactivate' }}
                </button>
                <button class="btn btn-secondary btn-sm" *ngIf="u.status !== 'Active'"
                        [disabled]="busyId === u.userId"
                        (click)="activate(u)">
                  {{ busyId === u.userId ? '…' : 'Activate' }}
                </button>
                <div class="row-err" *ngIf="rowError[u.userId]">{{ rowError[u.userId] }}</div>
                <div class="row-ok" *ngIf="rowOk[u.userId]">{{ rowOk[u.userId] }}</div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .hint { color: var(--success); font-size: 0.85rem; }
    .count-badge {
      display: inline-block;
      background: var(--primary-soft);
      color: var(--primary);
      font-size: 0.75rem;
      padding: 0.15rem 0.55rem;
      border-radius: 9999px;
      margin-left: 0.5rem;
      font-weight: 600;
    }
    .inline-select { padding: 0.3rem 0.5rem; font-size: 0.825rem; margin: 0; }
    .actions-cell { white-space: nowrap; }
    .row-err {
      display: block;
      color: var(--danger);
      font-size: 0.78rem;
      margin-top: 0.3rem;
      white-space: normal;
      max-width: 240px;
    }
    .row-ok {
      display: block;
      color: var(--success);
      font-size: 0.78rem;
      margin-top: 0.3rem;
    }
  `]
})
export class UsersComponent {
  private fb = inject(FormBuilder);
  private users$ = inject(UserService);
  private departments$ = inject(DepartmentService);

  users: User[] = [];
  managers: User[] = [];
  departments: Department[] = [];
  saving = false;
  addMessage = '';
  error = '';

  busyId: number | null = null;
  rowError: Record<number, string> = {};
  rowOk: Record<number, string> = {};

  form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: this.fb.nonNullable.control<Role>('EMPLOYEE'),
    departmentId: this.fb.control<number | null>(null),
    managerId: this.fb.control<number | null>(null)
  });

  constructor() {
    this.reload();
    this.departments$.getAll().subscribe(d => (this.departments = d));
  }

  private reload() {
    this.users$.getAll().subscribe({
      next: list => {
        this.users = list;
        this.managers = list.filter(u => u.role === 'MANAGER' || u.role === 'ADMIN');
      },
      error: err => (this.error = readApiError(err))
    });
  }

  add() {
    if (this.form.invalid) return;
    this.saving = true;
    this.addMessage = '';
    const raw = this.form.getRawValue();
    this.users$
      .add({
        name: raw.name,
        email: raw.email,
        password: raw.password,
        role: raw.role,
        departmentId: raw.departmentId ?? undefined,
        managerId: raw.managerId ?? undefined
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.addMessage = 'User added.';
          this.form.reset({
            name: '', email: '', password: '',
            role: 'EMPLOYEE', departmentId: null, managerId: null
          });
          this.reload();
        },
        error: err => {
          this.saving = false;
          this.addMessage = readApiError(err);
        }
      });
  }

  activate(u: User) {
    if (!confirm(`Activate ${u.name} (${u.email})?`)) return;
    this.runStatusChange(u, 'activate');
  }

  deactivate(u: User) {
    if (!confirm(`Deactivate ${u.name} (${u.email})? They will no longer be able to log in.`)) return;
    this.runStatusChange(u, 'deactivate');
  }

  private runStatusChange(u: User, mode: 'activate' | 'deactivate') {
    this.busyId = u.userId;
    this.rowError[u.userId] = '';
    this.rowOk[u.userId] = '';
    const obs = mode === 'activate' ? this.users$.activate(u.email) : this.users$.deactivate(u.email);
    obs.subscribe({
      next: () => {
        this.busyId = null;
        this.rowOk[u.userId] = mode === 'activate' ? 'Activated.' : 'Deactivated.';
        this.reload();
      },
      error: err => {
        this.busyId = null;
        this.rowError[u.userId] = readApiError(err);
      }
    });
  }
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}
