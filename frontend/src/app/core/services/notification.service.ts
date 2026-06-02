import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { AppNotification, NotificationType } from '../models/models';
import { environment } from '../../../environments/environment';

export interface SendNotificationRequest {
  userId: number;
  message: string;
  type: NotificationType;
  sourceId?: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private base = environment.notificationsBaseUrl;

  list(): Observable<AppNotification[]> {
    return this.http
      .get<any[]>(this.base)
      .pipe(map(list => list.map(normalize)));
  }

  markRead(id: number): Observable<string> {
    return this.http.patch(`${this.base}/read/${id}`, {}, { responseType: 'text' });
  }

  send(req: SendNotificationRequest): Observable<string> {
    return this.http.post(this.base, req, { responseType: 'text' });
  }
}

/**
 * Lombok generates `isRead()` for the boolean `isRead` field. Jackson
 * serializes that as JSON property `read` (strips the `is` prefix).
 * Normalise both shapes into our `isRead` field.
 */
function normalize(raw: any): AppNotification {
  return {
    notificationId: raw.notificationId,
    type: raw.type,
    message: raw.message,
    sourceId: raw.sourceId,
    isRead: raw.isRead ?? raw.read ?? false,
    createdAt: raw.createdAt
  };
}
