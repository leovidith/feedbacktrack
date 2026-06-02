import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateRecognitionRequest, Recognition } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RecognitionService {
  private http = inject(HttpClient);
  private base = `${environment.gatewayBaseUrl}/api/v1/recognition`;

  create(req: CreateRecognitionRequest): Observable<Recognition> {
    return this.http.post<Recognition>(this.base, req);
  }

  received(userId: number): Observable<Recognition[]> {
    return this.http.get<Recognition[]>(`${this.base}/received/${userId}`);
  }

  sent(userId: number): Observable<Recognition[]> {
    return this.http.get<Recognition[]>(`${this.base}/sent/${userId}`);
  }

  team(managerId: number): Observable<Recognition[]> {
    return this.http.get<Recognition[]>(`${this.base}/manager/${managerId}`);
  }

  feed(): Observable<Recognition[]> {
    return this.http.get<Recognition[]>(`${this.base}/feed`);
  }

  pointsFor(userId: number): Observable<number> {
    return this.http.get<number>(`${this.base}/points/target/${userId}`);
  }
}
