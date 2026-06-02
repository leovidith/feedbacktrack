import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FeedbackService } from '../../core/services/feedback.service';
import { UserService } from '../../core/services/user.service';
import { CategoryService } from '../../core/services/category.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { FeedbackCategory, User } from '../../core/models/models';

@Component({
  selector: 'app-submit-feedback',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Submit feedback</h1>
      <p class="page-description">Share constructive feedback with a teammate. Submit anonymously if you prefer.</p>
    </div>

    <div class="card">
      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="field-row">
          <label>Target user
            <select formControlName="targetUserId">
              <option [ngValue]="null" disabled>Select an employee…</option>
              <option *ngFor="let u of users" [ngValue]="u.userId">
                {{ u.name }} ({{ u.email }})
              </option>
            </select>
          </label>

          <label>Category
            <select formControlName="categoryId">
              <option [ngValue]="null" disabled>Select a category…</option>
              <option *ngFor="let c of categories" [ngValue]="c.categoryId">
                {{ c.categoryName }}
              </option>
            </select>
          </label>
        </div>

        <label>Comments
          <textarea formControlName="comments" rows="6" maxlength="2000"
                    placeholder="Write between 10 and 2000 characters…"></textarea>
          <span class="field-help">{{ form.controls.comments.value.length }} / 2000 characters</span>
        </label>

        <label class="checkbox-row">
          <input type="checkbox" formControlName="isAnonymous" />
          <span>Submit anonymously</span>
        </label>

        <div class="form-actions">
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
            {{ saving ? 'Submitting…' : 'Submit feedback' }}
          </button>
          <div class="alert alert-success" *ngIf="success" style="margin: 0; padding: 0.4rem 0.8rem;">Feedback submitted.</div>
          <div class="alert alert-danger" *ngIf="error" style="margin: 0; padding: 0.4rem 0.8rem;">{{ error }}</div>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .checkbox-row {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      margin: 0.5rem 0 0;
      font-weight: 500;
    }
    .checkbox-row input { width: auto; margin: 0; }
    .checkbox-row span { color: var(--text); }
  `]
})
export class SubmitFeedbackComponent {
  private fb = inject(FormBuilder);
  private feedback = inject(FeedbackService);
  private users$ = inject(UserService);
  private categories$ = inject(CategoryService);
  private auth = inject(AuthService);
  private notify = inject(NotificationService);

  users: User[] = [];
  categories: FeedbackCategory[] = [];
  saving = false;
  success = false;
  error = '';

  form = this.fb.nonNullable.group({
    targetUserId: this.fb.control<number | null>(null, Validators.required),
    categoryId: this.fb.control<number | null>(null, Validators.required),
    comments: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    isAnonymous: [false]
  });

  constructor() {
    this.users$.getAll().subscribe(list => {
      const me = this.auth.userId;
      this.users = list.filter(u => u.userId !== me && u.status !== 'Inactive');
    });
    this.categories$.getAll().subscribe(c => (this.categories = c));
  }

  submit() {
    if (this.form.invalid) return;
    this.saving = true;
    this.success = false;
    this.error = '';
    const raw = this.form.getRawValue();
    this.feedback
      .create({
        targetUserId: raw.targetUserId!,
        categoryId: raw.categoryId!,
        comments: raw.comments,
        isAnonymous: raw.isAnonymous
      })
      .subscribe({
        next: saved => {
          this.saving = false;
          this.success = true;
          this.fireNotifications(saved.targetUserId, saved.feedbackId);
          this.form.reset({ targetUserId: null, categoryId: null, comments: '', isAnonymous: false });
        },
        error: err => {
          this.saving = false;
          this.error = readApiError(err);
        }
      });
  }

  private fireNotifications(targetUserId: number, feedbackId: number) {
    const target = this.users.find(u => u.userId === targetUserId);

    this.notify.send({
      userId: targetUserId,
      message: 'You received new feedback.',
      type: 'FEEDBACK_RECEIVED',
      sourceId: feedbackId
    }).subscribe({ error: () => {} });

    if (target?.managerId) {
      const managerId = Number(target.managerId);
      if (!Number.isNaN(managerId)) {
        this.notify.send({
          userId: managerId,
          message: `${target.name} on your team received new feedback.`,
          type: 'FEEDBACK_RECEIVED',
          sourceId: feedbackId
        }).subscribe({ error: () => {} });
      }
    }
  }
}

function readApiError(err: any): string {
  if (!err) return 'Request failed';
  if (typeof err.error === 'string') return err.error;
  if (err.error?.error) return err.error.error;
  if (err.error?.fieldErrors) {
    return Object.entries(err.error.fieldErrors)
      .map(([k, v]) => `${k}: ${v}`)
      .join(', ');
  }
  return err.message || 'Request failed';
}