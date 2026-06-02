import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FeedbackReview, SubmitReviewRequest } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private http = inject(HttpClient);
  private base = `${environment.gatewayBaseUrl}/api/v1/manager/reviews`;

  create(req: SubmitReviewRequest): Observable<FeedbackReview> {
    return this.http.post<FeedbackReview>(`${this.base}/create`, req);
  }

  update(id: number, req: SubmitReviewRequest): Observable<FeedbackReview> {
    return this.http.put<FeedbackReview>(`${this.base}/${id}`, req);
  }

  getById(id: number): Observable<FeedbackReview> {
    return this.http.get<FeedbackReview>(`${this.base}/${id}`);
  }

  byReviewer(reviewerId: number): Observable<FeedbackReview[]> {
    return this.http.get<FeedbackReview[]>(`${this.base}/reviewer/${reviewerId}`);
  }

  byFeedback(feedbackId: number): Observable<FeedbackReview> {
    return this.http.get<FeedbackReview>(`${this.base}/feedback/${feedbackId}`);
  }

  pending(reviewerId: number): Observable<FeedbackReview[]> {
    return this.http.get<FeedbackReview[]>(`${this.base}/reviewer/pending/${reviewerId}`);
  }

  all(): Observable<FeedbackReview[]> {
    return this.http.get<FeedbackReview[]>(`${this.base}/all`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
