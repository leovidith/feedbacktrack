import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateFeedbackRequest, Feedback } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private http = inject(HttpClient);
  private base = `${environment.gatewayBaseUrl}/api/v1/feedback`;

  create(req: CreateFeedbackRequest): Observable<Feedback> {
    return this.http.post<Feedback>(this.base, req);
  }

  getById(id: number): Observable<Feedback> {
    return this.http.get<Feedback>(`${this.base}/${id}`);
  }

  getMySent(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.base}/sent`);
  }

  getMyReceived(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.base}/received`);
  }

  getManagerFeedbacks(managerId: number): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.base}/manager/${managerId}`);
  }

  toggleVisibility(id: number): Observable<Feedback> {
    return this.http.patch<Feedback>(`${this.base}/${id}/visibility`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
  getAll(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.base}/all`);
  }
}
