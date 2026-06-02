import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Badge } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class BadgeService {
  private http = inject(HttpClient);
  private base = `${environment.gatewayBaseUrl}/api/admin/badges`;

  getAll(): Observable<Badge[]> {
    return this.http.get<Badge[]>(`${this.base}/all`);
  }

  add(badge: Partial<Badge>): Observable<Badge> {
    return this.http.post<Badge>(`${this.base}/add`, badge);
  }

  update(id: number, badge: Partial<Badge>): Observable<Badge> {
    return this.http.put<Badge>(`${this.base}/update/${id}`, badge);
  }

  delete(id: number): Observable<string> {
    return this.http.delete(`${this.base}/delete/${id}`, { responseType: 'text' });
  }
}
