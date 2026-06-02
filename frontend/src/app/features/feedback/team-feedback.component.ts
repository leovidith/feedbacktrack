import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FeedbackService } from '../../core/services/feedback.service';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { CategoryService } from '../../core/services/category.service';
import { ReviewService } from '../../core/services/review.service';
import { NotificationService } from '../../core/services/notification.service';
import { PendingReviewsService } from '../../core/services/pending-reviews.service';
import { Feedback, FeedbackAction, NotificationType } from '../../core/models/models';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';

interface ReviewState {
  action: FeedbackAction;
  notes: string;
  saving: boolean;
  message?: string;
}

@Component({
  selector: 'app-team-feedback',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <h1>Team feedback</h1>
      <p class="page-description">Feedback received by members of your team. Create or update a review for each item.</p>
    </div>

    <div class="card empty" *ngIf="loading">Loading…</div>
    <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
    <div class="card empty" *ngIf="!loading && !feedbacks.length">
      <h3>No team feedback yet</h3>
      <p>When teammates submit feedback for your reports, it will appear here.</p>
    </div>

    <div class="card feedback-item" *ngFor="let f of feedbacks">
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
          <button class="btn btn-primary btn-sm" (click)="saveReview(f)" [disabled]="state(f.feedbackId).saving">
            {{ state(f.feedbackId).saving ? 'Saving…' : (existingReviewIds[f.feedbackId] ? 'Update review' : 'Create review') }}
          </button>
          <span class="hint" *ngIf="state(f.feedbackId).message">{{ state(f.feedbackId).message }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .feedback-header { margin-bottom: 0.75rem; }
    .meta-row { color: var(--text); font-size: 0.9rem; }
    .meta-row.sub { color: var(--text-2); font-size: 0.825rem; margin-top: 0.2rem; }
    .dot { color: var(--muted); margin: 0 0.4rem; }
    .comment {
      margin: 0 0 1rem;
      padding: 0.75rem 1rem;
      background: #f8fafc;
      border-left: 3px solid var(--primary);
      border-radius: 6px;
      white-space: pre-wrap;
    }
    .review-block { padding-top: 0.75rem; border-top: 1px solid var(--border); }
    .review-block h4 { color: var(--text-2); text-transform: uppercase; font-size: 0.75rem; letter-spacing: 0.05em; }
    .hint { color: var(--success); font-size: 0.85rem; }
  `]
})
export class TeamFeedbackComponent {
  private feedback$ = inject(FeedbackService);
  private auth = inject(AuthService);
  private users$ = inject(UserService);
  private categories$ = inject(CategoryService);
  private reviews = inject(ReviewService);
  private notify = inject(NotificationService);
  private pendingReviews = inject(PendingReviewsService);

  feedbacks: Feedback[] = [];
  loading = true;
  error = '';

  private userNames = new Map<number, string>();
  private categoryNames = new Map<number, string>();

  reviewStates: Record<number, ReviewState> = {};
  existingReviewIds: Record<number, number> = {};
  /** Tracks the previously-saved review action so we know when a feedback transitions from pending → done. */
  previousActions: Record<number, FeedbackAction> = {};

  constructor() {
    this.users$.getAll().subscribe(us => us.forEach(u => this.userNames.set(u.userId, u.name)));
    this.categories$.getAll().subscribe(cs =>
      cs.forEach(c => this.categoryNames.set(c.categoryId, c.categoryName))
    );

    const id = this.auth.userId;
    if (id === null) {
      this.loading = false;
      this.error = 'Not signed in';
      return;
    }

    this.feedback$.getManagerFeedbacks(id).subscribe({
      next: list => {
        this.feedbacks = list;
        list.forEach(f => this.loadExistingReview(f.feedbackId));
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = readApiError(err);
      }
    });
  }

  state(feedbackId: number): ReviewState {
    return (this.reviewStates[feedbackId] ??= { action: 'PENDING', notes: '', saving: false });
  }

  nameOf(id?: number | null): string {
    if (id == null) return 'Unknown';
    return this.userNames.get(id) ?? `User #${id}`;
  }

  categoryOf(id: number): string {
    return this.categoryNames.get(id) ?? `Category #${id}`;
  }

  private loadExistingReview(feedbackId: number) {
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

  saveReview(f: Feedback) {
    const s = this.state(f.feedbackId);
    s.saving = true;
    s.message = '';
    const existingId = this.existingReviewIds[f.feedbackId];
    const previous = this.previousActions[f.feedbackId];

    const payload = {
      feedbackId: f.feedbackId,
      action: s.action,
      notes: s.notes
    };

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
      },
      error: err => {
        s.saving = false;
        s.message = readApiError(err);
      }
    });
  }

  /**
   * Decrement the pending-reviews counter whenever this feedback
   * transitions from "no review or PENDING" into ACKNOWLEDGED/RESOLVED.
   */
  private updatePendingCount(previous: FeedbackAction | undefined, current: FeedbackAction) {
    const wasDone = previous === 'ACKNOWLEDGED' || previous === 'RESOLVED';
    const isDone = current === 'ACKNOWLEDGED' || current === 'RESOLVED';
    if (!wasDone && isDone) {
      this.pendingReviews.markReviewed();
    }
  }

  /**
   * Notify both the feedback sender and the receiver when a manager
   * reviews the feedback. Only sent for ACKNOWLEDGED or RESOLVED.
   * PENDING is just "in progress" and doesn't warrant a notification.
   */
  private notifyParticipants(f: Feedback, action: FeedbackAction) {
    if (action !== 'ACKNOWLEDGED' && action !== 'RESOLVED') return;

    const type: NotificationType = action;
    const verb = action === 'RESOLVED' ? 'resolved' : 'acknowledged';

    this.notify.send({
      userId: f.targetUserId,
      message: `Feedback about you was ${verb} by your manager.`,
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
