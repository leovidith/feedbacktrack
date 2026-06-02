import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FeedbackCategory } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);
  private base = `${environment.gatewayBaseUrl}/api/admin/categories`;

  getAll(): Observable<FeedbackCategory[]> {
    return this.http.get<FeedbackCategory[]>(`${this.base}/all`);
  }

  add(category: Partial<FeedbackCategory>): Observable<FeedbackCategory> {
    return this.http.post<FeedbackCategory>(`${this.base}/add`, category);
  }

  update(id: number, category: Partial<FeedbackCategory>): Observable<FeedbackCategory> {
    return this.http.put<FeedbackCategory>(`${this.base}/${id}`, category);
  }

  delete(id: number): Observable<string> {
    return this.http.delete(`${this.base}/${id}`, { responseType: 'text' });
  }
}
