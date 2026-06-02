import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Subscription, interval, of } from 'rxjs';
import { catchError, filter, map, switchMap } from 'rxjs/operators';
import { NotificationService } from './notification.service';
import { NotificationSocketService } from './notification-socket.service';
import { AuthService } from './auth.service';
import { AppNotification } from '../models/models';

/**
 * Backup polling cadence used only when the WebSocket is disconnected.
 * Set generously — the socket is the primary push channel; polling here
 * exists solely as a safety net.
 */
const FALLBACK_POLL_INTERVAL_MS = 30_000;

@Injectable({ providedIn: 'root' })
export class NotificationPollerService {
  private svc = inject(NotificationService);
  private socket = inject(NotificationSocketService);
  private auth = inject(AuthService);

  private items$ = new BehaviorSubject<AppNotification[]>([]);
  readonly notifications$ = this.items$.asObservable();
  readonly unreadCount$ = this.items$.pipe(
    map(list => list.filter(n => !n.isRead).length)
  );
  readonly liveConnected$ = this.socket.connected$.asObservable();

  private pollSub?: Subscription;
  private inboundSub?: Subscription;
  private connectedSub?: Subscription;
  private lastUserId: number | null = null;

  constructor() {
    this.auth.state$.subscribe(session => {
      const userId = session?.userId ?? null;
      if (userId !== this.lastUserId) {
        this.lastUserId = userId;
        this.stop();
        this.items$.next([]);
        if (userId !== null) {
          this.start();
        }
      }
    });
  }

  private start(): void {
    // Initial backfill — always HTTP, regardless of socket state.
    this.fetchList();

    // Open the WebSocket. Inbound pushes upsert into items$ in real time.
    this.socket.connect();

    this.inboundSub = this.socket.inbound$.subscribe(n => this.upsert(n));

    // While the socket is up, no polling. When it drops, poll until it
    // comes back so the UI doesn't go stale even on transient outages.
    this.connectedSub = this.socket.connected$.subscribe(connected => {
      if (connected) {
        this.stopPolling();
      } else {
        this.startPolling();
      }
    });
  }

  private stop(): void {
    this.stopPolling();
    this.inboundSub?.unsubscribe();
    this.connectedSub?.unsubscribe();
    this.inboundSub = undefined;
    this.connectedSub = undefined;
    this.socket.disconnect();
  }

  private startPolling(): void {
    if (this.pollSub) return;
    this.pollSub = interval(FALLBACK_POLL_INTERVAL_MS)
      .pipe(
        filter(() => this.auth.isLoggedIn),
        switchMap(() =>
          this.svc.list().pipe(catchError(() => of([] as AppNotification[])))
        )
      )
      .subscribe(list => this.setList(list));
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = undefined;
  }

  refresh(): void {
    if (!this.auth.isLoggedIn) return;
    this.fetchList();
  }

  markRead(id: number): void {
    this.svc.markRead(id).subscribe({
      next: () => {
        const updated = this.items$.value.map(n =>
          n.notificationId === id ? { ...n, isRead: true } : n
        );
        this.items$.next(updated);
      },
      error: () => {}
    });
  }

  private fetchList(): void {
    this.svc
      .list()
      .pipe(catchError(() => of([] as AppNotification[])))
      .subscribe(list => this.setList(list));
  }

  private setList(list: AppNotification[]): void {
    const sorted = [...list].sort((a, b) => b.notificationId - a.notificationId);
    this.items$.next(sorted);
  }

  /** Insert a live message at the top, or replace an existing one with the same id. */
  private upsert(n: AppNotification): void {
    const existing = this.items$.value;
    const idx = existing.findIndex(x => x.notificationId === n.notificationId);
    if (idx >= 0) {
      const copy = [...existing];
      copy[idx] = n;
      this.items$.next(copy);
    } else {
      this.items$.next(
        [n, ...existing].sort((a, b) => b.notificationId - a.notificationId)
      );
    }
  }
}
