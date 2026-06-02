import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RecognitionService } from '../../core/services/recognition.service';
import { BadgeService } from '../../core/services/badge.service';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { Badge, Recognition } from '../../core/models/models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-my-recognitions',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <h1>My recognitions</h1>
      <p class="page-description">Badges given and received. You have <strong>{{ points }}</strong> total points.</p>
    </div>

    <div class="card">
      <div class="tabs">
        <button class="tab" [class.active]="tab==='received'" (click)="tab='received'">
          Received <span class="count">{{ received.length }}</span>
        </button>
        <button class="tab" [class.active]="tab==='sent'" (click)="tab='sent'">
          Sent <span class="count">{{ sent.length }}</span>
        </button>
      </div>
    </div>

    <ng-container *ngIf="tab==='received'">
      <div class="card empty" *ngIf="!received.length">
        <h3>No recognitions received yet</h3>
        <p>When teammates recognise you, their badges will show up here.</p>
      </div>
      <div class="card rec-row" *ngFor="let r of received">
        <img class="rec-badge" *ngIf="badgeIcon(r.badgeId)"
             [src]="badgeIcon(r.badgeId)" [alt]="badgeName(r.badgeId)"
             (error)="onIconError($event)" />
        <span class="rec-badge fallback" *ngIf="!badgeIcon(r.badgeId)">
          {{ initialOf(badgeName(r.badgeId)) }}
        </span>
        <div class="rec-content">
          <div class="rec-head">
            <strong>{{ badgeName(r.badgeId) }}</strong>
            <span class="pts">+{{ badgePoints(r.badgeId) }}</span>
            <span class="muted">From {{ userName(r.senderId) }}</span>
          </div>
          <p class="rec-body">{{ truncate(r.message) }}</p>
          <span class="rec-date">{{ r.recognizedDate | date:'medium' }}</span>
        </div>
      </div>
    </ng-container>

    <ng-container *ngIf="tab==='sent'">
      <div class="card empty" *ngIf="!sent.length">
        <h3>You haven't sent any recognitions yet</h3>
        <p>Pop over to <a routerLink="/recognition/give">Give recognition</a> to start.</p>
      </div>
      <div class="card rec-row" *ngFor="let r of sent">
        <img class="rec-badge" *ngIf="badgeIcon(r.badgeId)"
             [src]="badgeIcon(r.badgeId)" [alt]="badgeName(r.badgeId)"
             (error)="onIconError($event)" />
        <span class="rec-badge fallback" *ngIf="!badgeIcon(r.badgeId)">
          {{ initialOf(badgeName(r.badgeId)) }}
        </span>
        <div class="rec-content">
          <div class="rec-head">
            <strong>{{ badgeName(r.badgeId) }}</strong>
            <span class="pts">+{{ badgePoints(r.badgeId) }}</span>
            <span class="muted">To {{ userName(r.targetUserId) }}</span>
          </div>
          <p class="rec-body">{{ truncate(r.message) }}</p>
          <span class="rec-date">{{ r.recognizedDate | date:'medium' }}</span>
        </div>
      </div>
    </ng-container>
  `,
  styles: [`
    .tabs { display: flex; gap: 0.25rem; border-bottom: 1px solid var(--border); margin: -0.4rem 0 0; }
    .tab {
      background: transparent;
      border: 0;
      padding: 0.65rem 1rem;
      font-size: 0.9rem;
      font-weight: 500;
      color: var(--text-2);
      cursor: pointer;
      border-bottom: 2px solid transparent;
      margin-bottom: -1px;
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }
    .tab:hover { color: var(--text); }
    .tab.active { color: var(--primary); border-bottom-color: var(--primary); }
    .count {
      background: var(--border);
      color: var(--text-2);
      padding: 0.05rem 0.45rem;
      border-radius: 9999px;
      font-size: 0.7rem;
      font-weight: 600;
    }
    .tab.active .count { background: var(--primary-soft); color: var(--primary); }

    /* Row layout mirrors my-feedback's .fb-row, with a badge image on the left. */
    .rec-row {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
    }
    .rec-content { flex: 1; min-width: 0; }
    .rec-head {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      margin-bottom: 0.4rem;
      flex-wrap: wrap;
    }
    .rec-head strong { color: var(--text); font-size: 0.95rem; }
    .rec-head .muted {
      color: var(--text-2);
      font-size: 0.85rem;
      font-weight: 400;
    }
    .pts {
      color: var(--success);
      background: var(--success-soft);
      padding: 0.1rem 0.55rem;
      border-radius: 9999px;
      font-size: 0.72rem;
      font-weight: 600;
    }
    .rec-body { margin: 0 0 0.4rem; color: var(--text); font-weight: 400; }
    .rec-date { font-size: 0.8rem; color: var(--text-2); }

    .rec-badge {
      width: 56px;
      height: 56px;
      object-fit: contain;
      border-radius: 12px;
      background: var(--primary-soft);
      padding: 6px;
      flex-shrink: 0;
    }
    .rec-badge.fallback {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: var(--primary);
      font-weight: 700;
      font-size: 1.25rem;
      padding: 0;
    }

    @media (max-width: 480px) {
      .rec-badge { width: 44px; height: 44px; }
    }
  `]
})
export class MyRecognitionsComponent {
  private recognition = inject(RecognitionService);
  private badges$ = inject(BadgeService);
  private users$ = inject(UserService);
  private auth = inject(AuthService);

  received: Recognition[] = [];
  sent: Recognition[] = [];
  points = 0;

  private badgeMap = new Map<number, Badge>();
  private userMap = new Map<number, string>();

  tab: 'received' | 'sent' = 'received';

  constructor() {
    const id = this.auth.userId;
    if (id === null) return;

    forkJoin({
      received: this.recognition.received(id).pipe(catchError(() => of([] as Recognition[]))),
      sent: this.recognition.sent(id).pipe(catchError(() => of([] as Recognition[]))),
      points: this.recognition.pointsFor(id).pipe(catchError(() => of(0))),
      badges: this.badges$.getAll().pipe(catchError(() => of([] as Badge[]))),
      users: this.users$.getAll().pipe(catchError(() => of([])))
    }).subscribe(({ received, sent, points, badges, users }) => {
      this.received = received;
      this.sent = sent;
      this.points = points;
      badges.forEach(b => this.badgeMap.set(b.badgeId, b));
      users.forEach(u => this.userMap.set(u.userId, u.name));
    });
  }

  badgeName(id: number): string {
    return this.badgeMap.get(id)?.badgeName ?? `Badge #${id}`;
  }

  badgePoints(id: number): number {
    return this.badgeMap.get(id)?.pointsValue ?? 0;
  }

  badgeIcon(id: number): string | null {
    return this.badgeMap.get(id)?.badgeIconPath || null;
  }

  userName(id: number): string {
    return this.userMap.get(id) ?? `User #${id}`;
  }

  truncate(text: string, max = 200): string {
    if (!text) return '';
    if (text.length <= max) return text;
    return text.slice(0, max).trim() + '…';
  }

  onIconError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }

  initialOf(name: string): string {
    return (name || '?').charAt(0).toUpperCase();
  }
}
