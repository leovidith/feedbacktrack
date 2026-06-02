import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationPollerService } from '../../core/services/notification-poller.service';
import { AppNotification } from '../../core/models/models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <div class="hdr-row">
        <div>
          <h1>Notifications</h1>
          <p class="page-description">Your in-app alerts. Live over WebSocket — falls back to polling if the connection drops. Click a notification to open the related item.</p>
        </div>
        <button class="btn btn-secondary btn-sm" (click)="poller.refresh()">Refresh now</button>
      </div>
    </div>

    <div class="card empty" *ngIf="(poller.notifications$ | async)?.length === 0">
      <h3>You're all caught up</h3>
      <p>You have no notifications.</p>
    </div>

    <div class="card notif" *ngFor="let n of (poller.notifications$ | async)"
         [class.unread]="!n.isRead"
         (click)="open(n)">
      <div class="notif-meta">
        <span class="notif-type">{{ formatType(n.type) }}</span>
        <span class="notif-date">{{ n.createdAt | date:'medium' }}</span>
        <span class="pill" data-action="PENDING" *ngIf="!n.isRead">New</span>
      </div>
      <p class="notif-msg">{{ n.message }}</p>
      <div class="notif-cta">
        <span class="open-hint">{{ openHint(n) }} →</span>
        <button class="btn btn-ghost btn-sm" *ngIf="!n.isRead"
                (click)="markRead($event, n)">Mark as read</button>
      </div>
    </div>
  `,
  styles: [`
    .hdr-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
    .notif {
      cursor: pointer;
      transition: border-color 0.15s, transform 0.05s;
    }
    .notif:hover { border-color: var(--primary); }
    .notif:active { transform: scale(0.998); }
    .notif.unread { border-left: 3px solid var(--primary); }
    .notif-meta { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
    .notif-type { font-weight: 600; color: var(--text); font-size: 0.95rem; }
    .notif-date { font-size: 0.8rem; color: var(--text-2); }
    .notif-msg { white-space: pre-wrap; margin: 0 0 0.5rem; color: var(--text); }
    .notif-cta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }
    .open-hint {
      color: var(--primary);
      font-size: 0.85rem;
      font-weight: 500;
    }
  `]
})
export class NotificationsComponent {
  poller = inject(NotificationPollerService);
  private router = inject(Router);

  constructor() {
    this.poller.refresh();
  }

  open(n: AppNotification) {
    if (!n.isRead) {
      this.poller.markRead(n.notificationId);
    }
    const target = this.routeFor(n);
    if (target) {
      this.router.navigate(target);
    }
  }

  markRead(event: MouseEvent, n: AppNotification) {
    event.stopPropagation();
    this.poller.markRead(n.notificationId);
  }

  openHint(n: AppNotification): string {
    if (n.type === 'RECOGNITION_RECEIVED') return 'View recognition';
    return 'View feedback';
  }

  private routeFor(n: AppNotification): any[] | null {
    if (n.type === 'RECOGNITION_RECEIVED') {
      return ['/recognition/mine'];
    }
    if (n.sourceId) {
      return ['/feedback/view', n.sourceId];
    }
    return null;
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }
}
