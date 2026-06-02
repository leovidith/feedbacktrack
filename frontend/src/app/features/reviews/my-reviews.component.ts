import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReviewService } from '../../core/services/review.service';
import { AuthService } from '../../core/services/auth.service';
import { FeedbackReview } from '../../core/models/models';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <h1>My reviews</h1>
      <p class="page-description">Reviews you have submitted. To create or update one, head to
        <a routerLink="/feedback/team">Team feedback</a>.</p>
    </div>

    <div class="card empty" *ngIf="loading">Loading…</div>
    <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
    <div class="card empty" *ngIf="!loading && !reviews.length">
      <h3>No reviews yet</h3>
      <p>Reviews you create from <a routerLink="/feedback/team">Team feedback</a> will appear here.</p>
    </div>

    <div class="table-wrap" *ngIf="reviews.length">
      <table>
        <thead>
          <tr>
            <th>Review #</th>
            <th>Feedback #</th>
            <th>Status</th>
            <th>Notes</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let r of reviews">
            <td>{{ r.reviewId }}</td>
            <td>{{ r.feedbackId }}</td>
            <td><span class="pill" [attr.data-action]="r.actionTaken">{{ r.actionTaken }}</span></td>
            <td>{{ r.managerNotes || '—' }}</td>
            <td>{{ r.reviewDate | date:'medium' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [``]
})
export class MyReviewsComponent {
  private reviews$ = inject(ReviewService);
  private auth = inject(AuthService);

  reviews: FeedbackReview[] = [];
  loading = true;
  error = '';

  constructor() {
    const id = this.auth.userId;
    if (id === null) {
      this.loading = false;
      return;
    }
    this.reviews$.byReviewer(id).subscribe({
      next: r => { this.reviews = r; this.loading = false; },
      error: err => { this.loading = false; this.error = readApiError(err); }
    });
  }
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}
