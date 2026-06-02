import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DepartmentService } from '../../core/services/department.service';
import { Department } from '../../core/models/models';

@Component({
  selector: 'app-admin-departments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Departments</h1>
      <p class="page-description">Org structure used when onboarding new users.</p>
    </div>

    <div class="card">
      <h3>Add department</h3>
      <form [formGroup]="form" (ngSubmit)="add()" class="inline-form">
        <input formControlName="departmentName" placeholder="New department name" />
        <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">Add</button>
      </form>
      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
    </div>

    <div class="card">
      <h3>All departments <span class="count-badge">{{ departments.length }}</span></h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>Name</th></tr></thead>
          <tbody>
            <tr *ngFor="let d of departments">
              <td>{{ d.departmentId }}</td>
              <td>{{ d.departmentName }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .inline-form { display: flex; gap: 0.5rem; margin-top: 0.25rem; }
    .inline-form input { flex: 1; margin: 0; }
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
  `]
})
export class DepartmentsComponent {
  private fb = inject(FormBuilder);
  private svc = inject(DepartmentService);

  departments: Department[] = [];
  saving = false;
  error = '';

  form = this.fb.nonNullable.group({
    departmentName: ['', Validators.required]
  });

  constructor() {
    this.reload();
  }

  reload() {
    this.svc.getAll().subscribe({
      next: d => (this.departments = d),
      error: err => (this.error = readApiError(err))
    });
  }

  add() {
    if (this.form.invalid) return;
    this.saving = true;
    this.svc.add(this.form.getRawValue()).subscribe({
      next: () => {
        this.saving = false;
        this.form.reset({ departmentName: '' });
        this.reload();
      },
      error: err => {
        this.saving = false;
        this.error = readApiError(err);
      }
    });
  }
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}
