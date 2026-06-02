import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { RecognitionService } from '../../core/services/recognition.service';
import { UserService } from '../../core/services/user.service';
import { PendingReviewsService } from '../../core/services/pending-reviews.service';
import { NotificationPollerService } from '../../core/services/notification-poller.service';
import { User } from '../../core/models/models';
import { catchError, of } from 'rxjs';

interface Tile {
  title: string;
  description: string;
  link: string;
  visible: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header welcome">
      <h1>Welcome back, {{ auth.session?.name }}</h1>
      <p class="welcome-sub">
        Signed in as <span class="pill" [attr.data-role]="auth.session?.role">{{ auth.session?.role }}</span>
        <ng-container *ngIf="managerName">
          · Reports to <strong>{{ managerName }}</strong>
        </ng-container>
      </p>
    </div>

    <div class="stat-grid">
      <div class="stat-card" *ngIf="points !== null">
        <div class="stat-icon stat-icon-info">★</div>
        <div>
          <div class="stat-value">{{ points }}</div>
          <div class="stat-label">Recognition points</div>
        </div>
      </div>

      <div class="stat-card highlight" *ngIf="auth.hasRole(['MANAGER'])">
        <div class="stat-icon">▤</div>
        <div>
          <div class="stat-value">{{ pendingReviews.pendingCount$ | async }}</div>
          <div class="stat-label">Pending reviews</div>
        </div>
      </div>

      <div class="stat-card" *ngIf="auth.hasRole(['MANAGER'])">
        <div class="stat-icon stat-icon-success">👥</div>
        <div>
          <div class="stat-value">{{ team.length }}</div>
          <div class="stat-label">Direct reports</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon stat-icon-danger">🔔</div>
        <div>
          <div class="stat-value">{{ (poller.unreadCount$ | async) ?? 0 }}</div>
          <div class="stat-label">Unread alerts</div>
        </div>
      </div>
    </div>

    <ng-container *ngIf="auth.hasRole(['MANAGER'])">
      <h2 class="section-title">Your team</h2>
      <div class="card empty" *ngIf="loadingTeam">Loading your team…</div>
      <div class="card empty" *ngIf="!loadingTeam && !team.length">
        <h3>No direct reports yet</h3>
        <p>An admin can assign team members to you from the Users page.</p>
      </div>
      <div class="card team-card" *ngIf="team.length">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Department</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let m of team">
              <td>{{ m.name }}</td>
              <td>{{ m.email }}</td>
              <td><span class="pill" [attr.data-role]="m.role">{{ m.role }}</span></td>
              <td>{{ m.departmentName || '—' }}</td>
              <td><span class="pill" [attr.data-status]="m.status">{{ m.status || '—' }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </ng-container>

    <h2 class="section-title">Quick actions</h2>

    <div class="tile-grid">
      <ng-container *ngFor="let tile of tiles">
        <a *ngIf="tile.visible" [routerLink]="tile.link" class="tile">
          <div class="tile-body">
            <h3>{{ tile.title }}</h3>
            <p>{{ tile.description }}</p>
          </div>
          <span class="tile-arrow">→</span>
        </a>
      </ng-container>
    </div>
  `,
  styles: [`
    .welcome h1 { font-size: 1.5rem; margin-bottom: 0.35rem; }
    .welcome-sub {
      margin: 0;
      color: var(--text-2);
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }
    .stat-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    .section-title {
      font-size: 0.78rem;
      color: var(--text-2);
      text-transform: uppercase;
      letter-spacing: 0.06em;
      font-weight: 700;
      margin: 1.5rem 0 0.85rem;
    }
    .team-card { padding: 0; overflow: hidden; }
    .team-card table { min-width: 540px; }
    .tile-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 0.85rem;
    }
    .tile {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      background: var(--surface);
      border: 1px solid #EEF0F4;
      border-radius: 12px;
      padding: 1.15rem 1.25rem;
      text-decoration: none;
      color: inherit;
      box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
      transition: border-color 0.12s, transform 0.12s;
    }
    .tile:hover {
      border-color: var(--primary);
      text-decoration: none;
    }
    .tile h3 { margin: 0 0 0.3rem; font-size: 0.95rem; }
    .tile p { margin: 0; color: var(--text-2); font-size: 0.85rem; line-height: 1.45; font-weight: 400; }
    .tile-arrow {
      color: var(--muted);
      font-size: 1.25rem;
      transition: color 0.12s;
    }
    .tile:hover .tile-arrow { color: var(--primary); }
  `]
})
export class HomeComponent {
  auth = inject(AuthService);
  pendingReviews = inject(PendingReviewsService);
  poller = inject(NotificationPollerService);

  private recognition = inject(RecognitionService);
  private users$ = inject(UserService);

  points: number | null = null;
  managerName: string | null = null;
  team: User[] = [];
  loadingTeam = false;

  tiles: Tile[] = [];

  constructor() {
    const id = this.auth.userId;

    if (id !== null) {
      this.recognition
        .pointsFor(id)
        .pipe(catchError(() => of(null)))
        .subscribe(p => (this.points = p));

      // Resolve current user's manager name (shown for anyone with a managerId).
      this.users$
        .getById(id)
        .pipe(catchError(() => of(null)))
        .subscribe(me => {
          if (me?.managerId) {
            const mgrId = Number(me.managerId);
            if (!Number.isNaN(mgrId)) {
              this.users$
                .getById(mgrId)
                .pipe(catchError(() => of(null)))
                .subscribe(mgr => (this.managerName = mgr?.name ?? null));
            }
          }
        });

      // Manager-only: team list + pending review count.
      if (this.auth.hasRole(['MANAGER'])) {
        this.loadingTeam = true;
        this.users$
          .getTeam(id)
          .pipe(catchError(() => of([] as User[])))
          .subscribe(t => {
            this.team = t;
            this.loadingTeam = false;
          });

        this.pendingReviews.refresh();
      }
    }

    this.tiles = [
      { title: 'Submit feedback', description: 'Share constructive feedback with a teammate.', link: '/feedback/submit', visible: true },
      { title: 'My feedback', description: 'Track feedback you have sent and received.', link: '/feedback/mine', visible: true },
      { title: 'Give recognition', description: 'Award a badge to someone who deserves it.', link: '/recognition/give', visible: true },
      { title: 'My recognitions', description: "See badges you've received and given.", link: '/recognition/mine', visible: true },
      { title: 'Notifications', description: 'Check your in-app alerts.', link: '/notifications', visible: true },
      { title: 'Team feedback', description: 'Review feedback received by your team.', link: '/feedback/team', visible: this.auth.hasRole(['MANAGER', 'ADMIN']) },
      { title: 'My reviews', description: 'Manage the reviews you have submitted.', link: '/reviews/mine', visible: this.auth.hasRole(['MANAGER', 'ADMIN']) },
      { title: 'Manage users', description: 'Onboard employees and manage account status.', link: '/admin/users', visible: this.auth.hasRole(['ADMIN']) },
      { title: 'Recognition library', description: 'Configure badges and feedback categories.', link: '/admin/badges', visible: this.auth.hasRole(['ADMIN']) },
      { title: 'Change password', description: 'Update your own account password.', link: '/profile/change-password', visible: true }
    ];
  }
}
