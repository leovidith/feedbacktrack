import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { FeedbackService } from '../../core/services/feedback.service';
import { UserService } from '../../core/services/user.service';
import { CategoryService } from '../../core/services/category.service';
import { ReviewService } from '../../core/services/review.service';
import { NotificationService } from '../../core/services/notification.service';
import { PendingReviewsService } from '../../core/services/pending-reviews.service';
import { Feedback, FeedbackAction, NotificationType } from '../../core/models/models';

interface ReviewState {
  action: FeedbackAction;
  notes: string;
  saving: boolean;
  message?: string;
}

@Component({
  selector: 'app-all-feedback',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <h1>All feedback</h1>
      <p class="page-description">Every feedback submission across the organisation. As an admin you can review any item.</p>
    </div>

    <div class="toolbar" *ngIf="!loading && feedbacks.length">
      <label class="filter-label">
        Filter by status
        <select [(ngModel)]="filterAction" (ngModelChange)="applyFilter()">
          <option value="">All</option>
          <option value="PENDING">Pending</option>
          <option value="ACKNOWLEDGED">Acknowledged</option>
          <option value="RESOLVED">Resolved</option>
        </select>
      </label>
      <span class="count">{{ filtered.length }} of {{ feedbacks.length }} items</span>
    </div>

    <div class="card empty" *ngIf="loading">Loading…</div>
    <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
    <div class="card empty" *ngIf="!loading && !feedbacks.length">
      <h3>No feedback yet</h3>
      <p>Feedback submitted across the organisation will appear here.</p>
    </div>
    <div class="card empty" *ngIf="!loading && feedbacks.length && !filtered.length">
      <p>No feedback matches the selected filter.</p>
    </div>

    <div class="card feedback-item" *ngFor="let f of filtered">
      <div class="feedback-header">
        <div>
          <div class="meta-row">
            <strong>To:</strong> {{ nameOf(f.targetUserId) }}
            <span class="dot">·</span>
            <strong>From:</strong> {{ f.isAnonymous ? 'Anonymous' : nameOf(f.senderId) }}
          </div>
          <div class="meta-row sub">
            {{ categoryOf(f.categoryId) }}
            <span class="dot">·</span>
            {{ f.submittedDate | date:'medium' }}
          </div>
        </div>
        <span class="status-badge" [ngClass]="statusClass(f.feedbackId)">
          {{ currentAction(f.feedbackId) }}
        </span>
      </div>

      <p class="comment">{{ f.comments }}</p>

      <div class="review-block">
        <h4>Review</h4>
        <div class="field-row">
          <label>Action
            <select [(ngModel)]="state(f.feedbackId).action">
              <option value="PENDING">Pending</option>
              <option value="ACKNOWLEDGED">Acknowledged</option>
              <option value="RESOLVED">Resolved</option>
            </select>
          </label>
          <label>Notes
            <textarea rows="2" maxlength="1000" [(ngModel)]="state(f.feedbackId).notes"
                      placeholder="Optional notes (up to 1000 chars)"></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="btn btn-primary btn-sm"
                  (click)="saveReview(f)"
                  [disabled]="state(f.feedbackId).saving">
            {{ state(f.feedbackId).saving ? 'Saving…' : (existingReviewIds[f.feedbackId] ? 'Update review' : 'Create review') }}
          </button>
          <span class="hint" *ngIf="state(f.feedbackId).message">
            {{ state(f.feedbackId).message }}
          </span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1rem;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .filter-label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
      color: var(--text-2);
    }
    .count { font-size: 0.825rem; color: var(--muted); }

    .feedback-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 0.75rem;
    }
    .meta-row { color: var(--text); font-size: 0.9rem; }
    .meta-row.sub { color: var(--text-2); font-size: 0.825rem; margin-top: 0.2rem; }
    .dot { color: var(--muted); margin: 0 0.4rem; }

    .status-badge {
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      padding: 0.2rem 0.55rem;
      border-radius: 999px;
      white-space: nowrap;
    }
    .badge-pending      { background: #fef9c3; color: #854d0e; }
    .badge-acknowledged { background: #dbeafe; color: #1e40af; }
    .badge-resolved     { background: #dcfce7; color: #166534; }

    .comment {
      margin: 0 0 1rem;
      padding: 0.75rem 1rem;
      background: #f8fafc;
      border-left: 3px solid var(--primary);
      border-radius: 6px;
      white-space: pre-wrap;
    }
    .review-block { padding-top: 0.75rem; border-top: 1px solid var(--border); }
    .review-block h4 {
      color: var(--text-2);
      text-transform: uppercase;
      font-size: 0.75rem;
      letter-spacing: 0.05em;
    }
    .hint { color: var(--success); font-size: 0.85rem; }
  `]
})
export class AllFeedbackComponent {
  private feedback$ = inject(FeedbackService);
  private users$ = inject(UserService);
  private categories$ = inject(CategoryService);
  private reviews = inject(ReviewService);
  private notify = inject(NotificationService);
  private pendingReviews = inject(PendingReviewsService);

  feedbacks: Feedback[] = [];
  filtered: Feedback[] = [];
  loading = true;
  error = '';
  filterAction = '';

  private userNames = new Map<number, string>();
  private categoryNames = new Map<number, string>();

  reviewStates: Record<number, ReviewState> = {};
  existingReviewIds: Record<number, number> = {};
  previousActions: Record<number, FeedbackAction> = {};

  constructor() {
    this.users$.getAll().subscribe(us =>
      us.forEach(u => this.userNames.set(u.userId, u.name))
    );
    this.categories$.getAll().subscribe(cs =>
      cs.forEach(c => this.categoryNames.set(c.categoryId, c.categoryName))
    );

    this.feedback$.getAll().subscribe({
      next: list => {
        // Newest first
        this.feedbacks = list.sort(
          (a, b) => new Date(b.submittedDate).getTime() - new Date(a.submittedDate).getTime()
        );
        this.filtered = [...this.feedbacks];
        list.forEach(f => this.loadExistingReview(f.feedbackId));
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = readApiError(err);
      }
    });
  }

  // ── State helpers ──────────────────────────────────────────────────────────

  state(feedbackId: number): ReviewState {
    return (this.reviewStates[feedbackId] ??= { action: 'PENDING', notes: '', saving: false });
  }

  currentAction(feedbackId: number): FeedbackAction {
    return this.reviewStates[feedbackId]?.action ?? 'PENDING';
  }

  statusClass(feedbackId: number): string {
    const action = this.currentAction(feedbackId);
    return `status-badge badge-${action.toLowerCase()}`;
  }

  nameOf(id?: number | null): string {
    if (id == null) return 'Unknown';
    return this.userNames.get(id) ?? `User #${id}`;
  }

  categoryOf(id: number): string {
    return this.categoryNames.get(id) ?? `Category #${id}`;
  }

  applyFilter(): void {
    this.filtered = this.filterAction
      ? this.feedbacks.filter(
          f => this.currentAction(f.feedbackId) === this.filterAction
        )
      : [...this.feedbacks];
  }

  // ── Data loading ───────────────────────────────────────────────────────────

  private loadExistingReview(feedbackId: number): void {
    this.reviews
      .byFeedback(feedbackId)
      .pipe(catchError(() => of(null)))
      .subscribe(r => {
        if (r) {
          this.existingReviewIds[feedbackId] = r.reviewId;
          this.previousActions[feedbackId] = r.actionTaken;
          this.reviewStates[feedbackId] = {
            action: r.actionTaken,
            notes: r.managerNotes ?? '',
            saving: false
          };
        }
      });
  }

  // ── Save ───────────────────────────────────────────────────────────────────

  saveReview(f: Feedback): void {
    const s = this.state(f.feedbackId);
    s.saving = true;
    s.message = '';
    const existingId = this.existingReviewIds[f.feedbackId];
    const previous = this.previousActions[f.feedbackId];

    const payload = { feedbackId: f.feedbackId, action: s.action, notes: s.notes };
    const obs = existingId
      ? this.reviews.update(existingId, payload)
      : this.reviews.create(payload);

    obs.subscribe({
      next: r => {
        s.saving = false;
        s.message = existingId ? 'Updated.' : 'Created.';
        this.existingReviewIds[f.feedbackId] = r.reviewId;
        this.notifyParticipants(f, r.actionTaken);
        this.updatePendingCount(previous, r.actionTaken);
        this.previousActions[f.feedbackId] = r.actionTaken;
        // Re-apply filter so the badge and list stay in sync
        this.applyFilter();
      },
      error: err => {
        s.saving = false;
        s.message = readApiError(err);
      }
    });
  }

  // ── Side effects ───────────────────────────────────────────────────────────

  private updatePendingCount(
    previous: FeedbackAction | undefined,
    current: FeedbackAction
  ): void {
    const wasDone = previous === 'ACKNOWLEDGED' || previous === 'RESOLVED';
    const isDone  = current  === 'ACKNOWLEDGED' || current  === 'RESOLVED';
    if (!wasDone && isDone) {
      this.pendingReviews.markReviewed();
    }
  }

  private notifyParticipants(f: Feedback, action: FeedbackAction): void {
    if (action !== 'ACKNOWLEDGED' && action !== 'RESOLVED') return;

    const type: NotificationType = action;
    const verb = action === 'RESOLVED' ? 'resolved' : 'acknowledged';

    this.notify.send({
      userId: f.targetUserId,
      message: `Feedback about you was ${verb}.`,
      type,
      sourceId: f.feedbackId
    }).subscribe({ error: () => {} });

    if (f.senderId && f.senderId !== f.targetUserId) {
      this.notify.send({
        userId: f.senderId,
        message: `Your submitted feedback has been ${verb}.`,
        type,
        sourceId: f.feedbackId
      }).subscribe({ error: () => {} });
    }
  }
}

function readApiError(err: any): string {
  if (!err) return 'Request failed';
  if (typeof err.error === 'string') return err.error;
  if (err.error?.error) return err.error.error;
  return err.message || 'Request failed';
}