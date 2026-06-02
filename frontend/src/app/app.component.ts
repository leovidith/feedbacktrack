import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/services/auth.service';
import { NotificationPollerService } from './core/services/notification-poller.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="layout" [class.no-chrome]="!auth.isLoggedIn" [class.sidebar-open]="sidebarOpen">
      <aside class="sidenav" *ngIf="auth.isLoggedIn">
        <div class="brand">
          <span class="brand-mark">FT</span>
          <span class="brand-name">FeedbackTrack</span>
        </div>

        <div class="nav-section">
          <a class="nav-link" routerLink="/home" routerLinkActive="active">Dashboard</a>
        </div>

        <div class="nav-section">
          <div class="nav-section-title">Feedback</div>
          <a class="nav-link" routerLink="/feedback/submit" routerLinkActive="active" *ngIf="auth.hasRole(['MANAGER','EMPLOYEE'])">Submit feedback</a>
          <a class="nav-link" routerLink="/feedback/mine" routerLinkActive="active" *ngIf="auth.hasRole(['MANAGER','EMPLOYEE'])">My feedback</a>
          <a class="nav-link" routerLink="/feedback/team" routerLinkActive="active"
             *ngIf="auth.hasRole(['MANAGER','ADMIN'])">Team feedback</a>
        </div>

        <div class="nav-section" *ngIf="auth.hasRole(['MANAGER','EMPLOYEE'])">
          <div class="nav-section-title">Recognition</div>
          <a class="nav-link" routerLink="/recognition/give" routerLinkActive="active">Give recognition</a>
          <a class="nav-link" routerLink="/recognition/mine" routerLinkActive="active">My recognitions</a>
        </div>

        <div class="nav-section" *ngIf="auth.hasRole(['MANAGER','ADMIN'])">
          <div class="nav-section-title">Reviews</div>
          <a class="nav-link" routerLink="/reviews/mine" routerLinkActive="active">My reviews</a>
        </div>

        <div class="nav-section" *ngIf="auth.hasRole(['ADMIN'])">
          <div class="nav-section-title">Admin</div>
          <a class="nav-link" routerLink="/admin/users" routerLinkActive="active">Users</a>
          <a class="nav-link" routerLink="/admin/departments" routerLinkActive="active">Departments</a>
          <a class="nav-link" routerLink="/admin/categories" routerLinkActive="active">Categories</a>
          <a class="nav-link" routerLink="/admin/badges" routerLinkActive="active">Badges</a>
        </div>

        <div class="nav-section">
          <div class="nav-section-title">Alerts</div>
          <a class="nav-link" routerLink="/notifications" routerLinkActive="active">
            Notifications
            <span class="inline-badge" *ngIf="(poller.unreadCount$ | async) as count">{{ count }}</span>
          </a>
        </div>

        <div class="nav-section">
          <div class="nav-section-title">Account</div>
          <a class="nav-link" routerLink="/profile/change-password" routerLinkActive="active">Change password</a>
        </div>
      </aside>

      <div class="backdrop" *ngIf="auth.isLoggedIn && sidebarOpen" (click)="sidebarOpen = false"></div>

      <header class="appbar" *ngIf="auth.isLoggedIn">
        <button class="burger" type="button" (click)="sidebarOpen = !sidebarOpen" aria-label="Toggle navigation">
          <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18M3 12h18M3 18h18"/>
          </svg>
        </button>
        <div class="appbar-user">
          <a class="bell" routerLink="/notifications"
             [title]="(poller.liveConnected$ | async) ? 'Notifications · live' : 'Notifications · polling'">
            <svg class="bell-icon" xmlns="http://www.w3.org/2000/svg" width="22" height="22"
                 viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/>
              <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
            </svg>
            <span class="live-dot" [class.on]="(poller.liveConnected$ | async)"></span>
            <span class="bell-badge" *ngIf="(poller.unreadCount$ | async) as count">
              {{ count }}
            </span>
          </a>
          <div class="user-card">
            <div class="user-avatar">{{ initialsOf(auth.session?.name) }}</div>
            <div class="user-meta">
              <span class="user-name">{{ auth.session?.name }}</span>
              <span class="pill" [attr.data-role]="auth.session?.role">{{ auth.session?.role }}</span>
            </div>
          </div>
          <button class="btn btn-secondary btn-sm" (click)="auth.logout()">Logout</button>
        </div>
      </header>

      <main class="content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .bell {
      position: relative;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 38px;
      height: 38px;
      border-radius: 10px;
      text-decoration: none;
      color: var(--text);
      background: transparent;
    }
    .bell:hover { text-decoration: none; color: var(--primary); }
    .bell-icon { display: block; }
    .live-dot {
      position: absolute;
      bottom: 4px;
      right: 4px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #9CA3AF;
      border: 2px solid #fff;
    }
    .live-dot.on { background: #16A34A; }
    .bell-badge {
      position: absolute;
      top: 2px;
      right: 2px;
      min-width: 17px;
      height: 17px;
      padding: 0 4px;
      border-radius: 9999px;
      background: #DC2626;
      color: #fff;
      font-size: 0.62rem;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border: 2px solid #fff;
    }
    .user-card {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      padding: 0.25rem 0.6rem 0.25rem 0.25rem;
      border-radius: 9999px;
      background: #F3F4F6;
    }
    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--primary);
      color: #fff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.78rem;
      font-weight: 700;
      letter-spacing: 0.02em;
    }
    .inline-badge {
      background: var(--primary);
      color: #fff;
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0 6px;
      min-width: 18px;
      height: 18px;
      border-radius: 9999px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
    .nav-link.active .inline-badge { background: #fff; color: var(--primary); }

    /* Hamburger button — only visible on mobile */
    .burger {
      display: none;
      background: transparent;
      border: 0;
      padding: 0.4rem;
      border-radius: 8px;
      color: var(--text);
      cursor: pointer;
    }
    .burger:hover { background: #F3F4F6; }

    @media (max-width: 768px) {
      .burger { display: inline-flex; margin-right: auto; }
      .user-card { padding: 0; background: transparent; }
      .user-card .user-meta { display: none; }
    }
  `]
})
export class AppComponent {
  auth = inject(AuthService);
  poller = inject(NotificationPollerService);
  private router = inject(Router);

  sidebarOpen = false;

  constructor() {
    // Auto-close the drawer whenever the user navigates.
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => (this.sidebarOpen = false));
  }

  initialsOf(name: string | undefined): string {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }
}
