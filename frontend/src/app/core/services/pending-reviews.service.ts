import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, from, of } from 'rxjs';
import { catchError, map, mergeMap, switchMap, toArray } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { FeedbackService } from './feedback.service';
import { ReviewService } from './review.service';

/**
 * Tracks how many feedback items in the manager's team still need a
 * review action (ACKNOWLEDGED or RESOLVED). The count refreshes on
 * demand and decrements locally when the manager marks one done in
 * the team-feedback view.
 */
@Injectable({ providedIn: 'root' })
export class PendingReviewsService {
  private auth = inject(AuthService);
  private feedbacks = inject(FeedbackService);
  private reviews = inject(ReviewService);

  private count$ = new BehaviorSubject<number>(0);
  readonly pendingCount$ = this.count$.asObservable();

  refresh(): void {
    const userId = this.auth.userId;
    if (userId === null || !this.auth.hasRole(['MANAGER', 'ADMIN'])) {
      this.count$.next(0);
      return;
    }

    this.feedbacks
      .getManagerFeedbacks(userId)
      .pipe(
        switchMap(fbList => {
          if (!fbList.length) return of(0);
          return from(fbList).pipe(
            mergeMap(
              f =>
                this.reviews.byFeedback(f.feedbackId).pipe(
                  map(r => r.actionTaken === 'ACKNOWLEDGED' || r.actionTaken === 'RESOLVED'),
                  catchError(() => of(false))
                ),
              4
            ),
            toArray(),
            map(results => results.filter(done => !done).length)
          );
        }),
        catchError(() => of(0))
      )
      .subscribe(count => this.count$.next(count));
  }

  /** Called when the manager transitions a review from pending → ACK/RESOLVED. */
  markReviewed(): void {
    this.count$.next(Math.max(0, this.count$.value - 1));
  }

  /** Manual reset on logout. */
  reset(): void {
    this.count$.next(0);
  }
}
