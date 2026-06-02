import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BadgeService } from '../../core/services/badge.service';
import { Badge } from '../../core/models/models';

@Component({
  selector: 'app-admin-badges',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Recognition badges</h1>
      <p class="page-description">Badges teammates can award; points feed into the recognition leaderboard.</p>
    </div>

    <div class="card">
      <h3>Add badge</h3>
      <form [formGroup]="form" (ngSubmit)="add()">
        <div class="field-row">
          <label>Badge name<input formControlName="badgeName" /></label>
          <label>Points<input formControlName="pointsValue" type="number" min="1" /></label>
        </div>
        <div class="field-row">
          <label>Description<input formControlName="description" placeholder="Optional" /></label>
          <label>Icon URL
            <input formControlName="badgeIconPath" placeholder="https://… or /assets/icons/star.png" />
          </label>
        </div>
        <div class="preview-row" *ngIf="form.controls.badgeIconPath.value">
          <span class="muted">Preview:</span>
          <img class="badge-icon-lg" [src]="form.controls.badgeIconPath.value"
               alt="" (error)="onIconError($event)" />
        </div>
        <div class="form-actions">
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
            {{ saving ? 'Saving…' : 'Add badge' }}
          </button>
          <span class="hint" *ngIf="addMessage">{{ addMessage }}</span>
        </div>
      </form>
      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
    </div>

    <div class="card">
      <h3>All badges <span class="count-badge">{{ badges.length }}</span></h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th></th><th>ID</th><th>Name</th><th>Points</th><th>Description</th><th>Actions</th></tr></thead>
          <tbody>
            <tr *ngFor="let b of badges">
              <td>
                <img class="badge-icon" *ngIf="b.badgeIconPath"
                     [src]="b.badgeIconPath" [alt]="b.badgeName"
                     (error)="onIconError($event)" />
                <span class="badge-fallback" *ngIf="!b.badgeIconPath">{{ initialOf(b.badgeName) }}</span>
              </td>
              <td>{{ b.badgeId }}</td>
              <td>{{ b.badgeName }}</td>
              <td><span class="pts-pill">+{{ b.pointsValue }}</span></td>
              <td>{{ b.description || '—' }}</td>
              <td><button class="btn btn-ghost btn-sm" (click)="remove(b)">Delete</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .inline-form { display: flex; gap: 0.5rem; margin-top: 0.25rem; flex-wrap: wrap; }
    .inline-form input { flex: 1; min-width: 130px; margin: 0; }
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
    .pts-pill {
      background: var(--success-soft);
      color: var(--success);
      padding: 0.1rem 0.5rem;
      border-radius: 9999px;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .badge-icon {
      width: 36px;
      height: 36px;
      object-fit: contain;
      border-radius: 6px;
      background: var(--primary-soft);
      padding: 4px;
    }
    .badge-icon-lg {
      width: 64px;
      height: 64px;
      object-fit: contain;
      border-radius: 8px;
      background: var(--primary-soft);
      padding: 6px;
      border: 1px solid var(--border);
    }
    .badge-fallback {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: 6px;
      background: var(--primary-soft);
      color: var(--primary);
      font-weight: 700;
      font-size: 0.9rem;
    }
    .preview-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin: 0.25rem 0 0.5rem;
    }
    .hint { color: var(--success); font-size: 0.85rem; }
  `]
})
export class BadgesComponent {
  private fb = inject(FormBuilder);
  private svc = inject(BadgeService);

  badges: Badge[] = [];
  saving = false;
  error = '';
  addMessage = '';

  form = this.fb.nonNullable.group({
    badgeName: ['', Validators.required],
    pointsValue: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    description: [''],
    badgeIconPath: ['']
  });

  constructor() {
    this.reload();
  }

  reload() {
    this.error = '';
    this.svc.getAll().subscribe({
      next: b => (this.badges = b),
      error: err => (this.error = readApiError(err))
    });
  }

  add() {
    if (this.form.invalid) return;
    this.saving = true;
    this.error = '';
    this.addMessage = '';
    const raw = this.form.getRawValue();
    this.svc
      .add({
        badgeName: raw.badgeName,
        pointsValue: raw.pointsValue!,
        description: raw.description,
        badgeIconPath: raw.badgeIconPath
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.addMessage = 'Badge added.';
          this.form.reset({ badgeName: '', pointsValue: null, description: '', badgeIconPath: '' });
          this.reload();
        },
        error: err => {
          this.saving = false;
          this.error = readApiError(err);
        }
      });
  }

  remove(b: Badge) {
    if (!confirm(`Delete badge "${b.badgeName}"?`)) return;
    this.svc.delete(b.badgeId).subscribe({
      next: () => this.reload(),
      error: err => (this.error = readApiError(err))
    });
  }

  onIconError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }

  initialOf(name: string): string {
    return (name || '?').charAt(0).toUpperCase();
  }
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}
