import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Location } from '@angular/common';
import { FeedbackService } from '../../core/services/feedback.service';
import { UserService } from '../../core/services/user.service';
import { CategoryService } from '../../core/services/category.service';
import { ReviewService } from '../../core/services/review.service';
import { Feedback, FeedbackCategory, FeedbackReview, User } from '../../core/models/models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-feedback-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <button class="btn btn-ghost btn-sm back" (click)="back()">← Back</button>
      <h1>Feedback #{{ id }}</h1>
      <p class="page-description">Details of this feedback and any manager review.</p>
    </div>

    <div class="card empty" *ngIf="loading">Loading…</div>
    <div class="alert alert-danger" *ngIf="error">{{ error }}</div>

    <div class="card" *ngIf="feedback">
      <div class="kv">
        <div><label>To</label><div>{{ nameOf(feedback.targetUserId) }}</div></div>
        <div><label>From</label><div>{{ feedback.isAnonymous ? 'Anonymous' : nameOf(feedback.senderId) }}</div></div>
        <div><label>Category</label><div>{{ categoryOf(feedback.categoryId) }}</div></div>
        <div><label>Date</label><div>{{ feedback.submittedDate | date:'medium' }}</div></div>
      </div>
      <h3 class="section">Comments</h3>
      <p class="quote">{{ feedback.comments }}</p>
    </div>

    <div class="card" *ngIf="feedback && review">
      <h3>Manager review</h3>
      <div class="kv">
        <div><label>Status</label><div><span class="pill" [attr.data-action]="review.actionTaken">{{ review.actionTaken }}</span></div></div>
        <div><label>Reviewed by</label><div>{{ nameOf(review.reviewerId) }}</div></div>
        <div><label>Reviewed on</label><div>{{ review.reviewDate | date:'medium' }}</div></div>
      </div>
      <h3 class="section" *ngIf="review.managerNotes">Notes</h3>
      <p class="quote" *ngIf="review.managerNotes">{{ review.managerNotes }}</p>
    </div>

    <div class="card empty" *ngIf="feedback && !review && !loadingReview">
      <p>No manager review yet.</p>
    </div>
  `,
  styles: [`
    .back { margin-bottom: 0.5rem; }
    .kv {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 0.75rem 1.5rem;
      margin: 0.25rem 0 0.5rem;
    }
    .kv label {
      text-transform: uppercase;
      font-size: 0.7rem;
      letter-spacing: 0.06em;
      color: var(--muted);
      margin: 0 0 0.15rem;
    }
    .kv > div > div { font-weight: 500; color: var(--text); font-size: 0.95rem; }
    .section { margin-top: 1rem; }
    .quote {
      margin: 0;
      padding: 0.75rem 1rem;
      background: #f8fafc;
      border-left: 3px solid var(--primary);
      border-radius: 6px;
      white-space: pre-wrap;
    }
  `]
})
export class FeedbackDetailComponent implements OnInit {
  @Input() id!: string;

  private feedbacks = inject(FeedbackService);
  private users$ = inject(UserService);
  private categories$ = inject(CategoryService);
  private reviews = inject(ReviewService);
  private location = inject(Location);

  feedback: Feedback | null = null;
  review: FeedbackReview | null = null;
  loading = true;
  loadingReview = true;
  error = '';

  private userNames = new Map<number, string>();
  private categoryNames = new Map<number, string>();

  ngOnInit(): void {
    const feedbackId = Number(this.id);
    if (!feedbackId) {
      this.loading = false;
      this.error = 'Invalid feedback id.';
      return;
    }

    forkJoin({
      users: this.users$.getAll().pipe(catchError(() => of([] as User[]))),
      categories: this.categories$.getAll().pipe(catchError(() => of([] as FeedbackCategory[])))
    }).subscribe(({ users, categories }) => {
      users.forEach(u => this.userNames.set(u.userId, u.name));
      categories.forEach(c => this.categoryNames.set(c.categoryId, c.categoryName));
    });

    this.feedbacks.getById(feedbackId).subscribe({
      next: f => { this.feedback = f; this.loading = false; },
      error: err => { this.loading = false; this.error = readApiError(err); }
    });

    this.reviews.byFeedback(feedbackId).pipe(catchError(() => of(null))).subscribe(r => {
      this.review = r;
      this.loadingReview = false;
    });
  }

  back() { this.location.back(); }

  nameOf(id?: number | null): string {
    if (id == null) return 'Anonymous';
    return this.userNames.get(id) ?? `User #${id}`;
  }

  categoryOf(id: number): string {
    return this.categoryNames.get(id) ?? `Category #${id}`;
  }
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}