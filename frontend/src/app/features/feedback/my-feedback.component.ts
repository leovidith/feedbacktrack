import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FeedbackService } from '../../core/services/feedback.service';
import { UserService } from '../../core/services/user.service';
import { CategoryService } from '../../core/services/category.service';
import { Feedback, FeedbackCategory, User } from '../../core/models/models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-my-feedback',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <h1>My feedback</h1>
      <p class="page-description">Feedback you have given and feedback you have received. Click a row to see details.</p>
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

    <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
    <div class="card empty" *ngIf="loading">Loading…</div>

    <ng-container *ngIf="!loading && tab==='received'">
      <div class="card empty" *ngIf="!received.length">
        <h3>No feedback received yet</h3>
        <p>When colleagues submit feedback for you, it will appear here.</p>
      </div>
      <a class="card fb-row" *ngFor="let f of received"
         [routerLink]="['/feedback/view', f.feedbackId]">
        <div class="fb-head">
          <strong>{{ f.isAnonymous ? 'Anonymous' : nameOf(f.senderId) }}</strong>
          <span class="muted">{{ categoryOf(f.categoryId) }}</span>
        </div>
        <p class="fb-body">{{ truncate(f.comments) }}</p>
        <span class="fb-date">{{ f.submittedDate | date:'medium' }}</span>
      </a>
    </ng-container>

    <ng-container *ngIf="!loading && tab==='sent'">
      <div class="card empty" *ngIf="!sent.length">
        <h3>You haven't submitted feedback yet</h3>
        <p>Head over to <a routerLink="/feedback/submit">Submit feedback</a> to get started.</p>
      </div>
      <a class="card fb-row" *ngFor="let f of sent"
         [routerLink]="['/feedback/view', f.feedbackId]">
        <div class="fb-head">
          <strong>To: {{ nameOf(f.targetUserId) }}</strong>
          <span class="muted">{{ categoryOf(f.categoryId) }}</span>
          <span class="pill" data-status="Inactive" *ngIf="f.isAnonymous">Anonymous</span>
        </div>
        <p class="fb-body">{{ truncate(f.comments) }}</p>
        <span class="fb-date">{{ f.submittedDate | date:'medium' }}</span>
      </a>
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
    .fb-row {
      display: block;
      text-decoration: none;
      color: inherit;
      transition: border-color 0.12s;
    }
    .fb-row:hover { border-color: var(--primary); text-decoration: none; }
    .fb-head {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 0.4rem;
    }
    .fb-head .muted { color: var(--text-2); font-size: 0.85rem; }
    .fb-body { margin: 0 0 0.4rem; color: var(--text); }
    .fb-date { font-size: 0.8rem; color: var(--text-2); }
  `]
})
export class MyFeedbackComponent {
  private feedbacks = inject(FeedbackService);
  private users$ = inject(UserService);
  private categories$ = inject(CategoryService);

  tab: 'received' | 'sent' = 'received';
  received: Feedback[] = [];
  sent: Feedback[] = [];
  loading = true;
  error = '';

  private userNames = new Map<number, string>();
  private categoryNames = new Map<number, string>();

  constructor() {
    forkJoin({
      users: this.users$.getAll().pipe(catchError(() => of([] as User[]))),
      categories: this.categories$.getAll().pipe(catchError(() => of([] as FeedbackCategory[]))),
      received: this.feedbacks.getMyReceived().pipe(catchError(() => of([] as Feedback[]))),
      sent: this.feedbacks.getMySent().pipe(catchError(() => of([] as Feedback[])))
    }).subscribe({
      next: ({ users, categories, received, sent }) => {
        users.forEach(u => this.userNames.set(u.userId, u.name));
        categories.forEach(c => this.categoryNames.set(c.categoryId, c.categoryName));
        this.received = received;
        this.sent = sent;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = readApiError(err);
      }
    });
  }

  nameOf(id?: number | null): string {
    if (id == null) return 'Anonymous';
    return this.userNames.get(id) ?? `User #${id}`;
  }

  categoryOf(id: number): string {
    return this.categoryNames.get(id) ?? `Category #${id}`;
  }

  //preview
  truncate(text: string, max = 200): string {
    if (text.length <= max) return text;
    return text.slice(0, max).trim() + '…';
  }
}

function readApiError(err: any): string {
  if (typeof err?.error === 'string') return err.error;
  if (err?.error?.error) return err.error.error;
  return err?.message || 'Request failed';
}
