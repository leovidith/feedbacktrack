import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoryService } from '../../core/services/category.service';
import { FeedbackCategory } from '../../core/models/models';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Feedback categories</h1>
      <p class="page-description">Topics employees can pick when submitting feedback.</p>
    </div>

    <div class="card">
      <h3>Add category</h3>
      <form [formGroup]="form" (ngSubmit)="add()" class="inline-form">
        <input formControlName="categoryName" placeholder="Category name" />
        <input formControlName="description" placeholder="Description (optional)" />
        <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">Add</button>
      </form>
      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
    </div>

    <div class="card">
      <h3>All categories <span class="count-badge">{{ categories.length }}</span></h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Actions</th></tr></thead>
          <tbody>
            <tr *ngFor="let c of categories">
              <td>{{ c.categoryId }}</td>
              <td>{{ c.categoryName }}</td>
              <td>{{ c.description || '—' }}</td>
              <td><button class="btn btn-ghost btn-sm" (click)="remove(c)">Delete</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .inline-form { display: flex; gap: 0.5rem; margin-top: 0.25rem; flex-wrap: wrap; }
    .inline-form input { flex: 1; min-width: 150px; margin: 0; }
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
export class CategoriesComponent {
  private fb = inject(FormBuilder);
  private svc = inject(CategoryService);

  categories: FeedbackCategory[] = [];
  saving = false;
  error = '';

  form = this.fb.nonNullable.group({
    categoryName: ['', Validators.required],
    description: ['']
  });

  constructor() {
    this.reload();
  }

  reload() {
    this.svc.getAll().subscribe({
      next: c => (this.categories = c),
      error: err => (this.error = readApiError(err))
    });
  }

  add() {
    if (this.form.invalid) return;
    this.saving = true;
    this.svc.add(this.form.getRawValue()).subscribe({
      next: () => {
        this.saving = false;
        this.form.reset({ categoryName: '', description: '' });
        this.reload();
      },
      error: err => {
        this.saving = false;
        this.error = readApiError(err);
      }
    });
  }

  remove(c: FeedbackCategory) {
    if (!confirm(`Delete category "${c.categoryName}"?`)) return;
    this.svc.delete(c.categoryId).subscribe({
      next: () => this.reload(),
      error: err => (this.error = readApiError(err))
    });
  }
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}
