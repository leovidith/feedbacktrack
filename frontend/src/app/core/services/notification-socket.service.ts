import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { AppNotification } from '../models/models';

const SOCKET_URL = 'http://localhost:1931/ws';

/**
 * Live notification feed over STOMP/SockJS. Connects to the notification
 * service's `/ws` endpoint and subscribes to the per-user topic that
 * NotificationService.sendNotification publishes to:
 *   /topic/api/v1/notifications/{userId}
 *
 * `connected$` reflects the live socket state — the poller uses this to
 * fall back to HTTP polling whenever the socket is down.
 */
@Injectable({ providedIn: 'root' })
export class NotificationSocketService {
  private auth = inject(AuthService);

  private client: Client | null = null;

  readonly connected$ = new BehaviorSubject<boolean>(false);
  readonly inbound$ = new Subject<AppNotification>();

  connect(): void {
    if (this.client?.active) return;
    const userId = this.auth.userId;
    if (userId === null) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(SOCKET_URL) as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {} // silence STOMP debug spam
    });

    this.client.onConnect = () => {
      this.connected$.next(true);
      this.client!.subscribe(
        `/topic/api/v1/notifications/${userId}`,
        (message: IMessage) => this.handleMessage(message)
      );
    };

    this.client.onStompError = frame => {
      console.warn('STOMP error', frame.headers['message']);
      this.connected$.next(false);
    };

    this.client.onWebSocketClose = () => this.connected$.next(false);
    this.client.onWebSocketError = () => this.connected$.next(false);

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.connected$.next(false);
  }

  private handleMessage(message: IMessage): void {
    try {
      const raw = JSON.parse(message.body);
      this.inbound$.next({
        notificationId: raw.notificationId,
        type: raw.type,
        message: raw.message,
        sourceId: raw.sourceId,
        // backend may emit `isRead` (now annotated) or `read` (legacy Lombok+Jackson)
        isRead: raw.isRead ?? raw.read ?? false,
        createdAt: raw.createdAt
      });
    } catch (e) {
      console.error('Failed to parse incoming notification', e);
    }
  }
}
