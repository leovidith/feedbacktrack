import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RecognitionService } from '../../core/services/recognition.service';
import { UserService } from '../../core/services/user.service';
import { BadgeService } from '../../core/services/badge.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { Badge, User } from '../../core/models/models';

@Component({
  selector: 'app-give-recognition',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <h1>Give recognition</h1>
      <p class="page-description">Send a badge to a peer with a short message (5–500 characters).</p>
    </div>

    <div class="card">
      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="field-row">
          <label>Target user
            <select formControlName="targetUserId">
              <option [ngValue]="null" disabled>Select an employee…</option>
              <option *ngFor="let u of users" [ngValue]="u.userId">
                {{ u.name }} ({{ u.email }})
              </option>
            </select>
          </label>

          <label>Badge
            <select formControlName="badgeId">
              <option [ngValue]="null" disabled>Select a badge…</option>
              <option *ngFor="let b of badges" [ngValue]="b.badgeId">
                {{ b.badgeName }} (+{{ b.pointsValue }} pts)
              </option>
            </select>
          </label>
        </div>

        <div class="badge-preview" *ngIf="selectedBadge() as b">
          <img class="badge-icon-lg" *ngIf="b.badgeIconPath"
               [src]="b.badgeIconPath" [alt]="b.badgeName"
               (error)="onIconError($event)" />
          <span class="badge-fallback" *ngIf="!b.badgeIconPath">{{ initialOf(b.badgeName) }}</span>
          <div class="badge-meta">
            <strong>{{ b.badgeName }}</strong>
            <span class="muted">{{ b.description }}</span>
            <span class="pts-pill">+{{ b.pointsValue }} pts</span>
          </div>
        </div>

        <label>Message
          <textarea formControlName="message" rows="4" maxlength="500"
                    placeholder="Why are you recognising this teammate?"></textarea>
          <span class="field-help">{{ form.controls.message.value.length }} / 500 characters</span>
        </label>

        <div class="form-actions">
          <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
            {{ saving ? 'Sending…' : 'Send recognition' }}
          </button>
          <div class="alert alert-success" *ngIf="success" style="margin: 0; padding: 0.4rem 0.8rem;">Recognition sent.</div>
          <div class="alert alert-danger" *ngIf="error" style="margin: 0; padding: 0.4rem 0.8rem;">{{ error }}</div>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .badge-preview {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      padding: 0.65rem;
      border: 1px dashed var(--border-strong);
      border-radius: 8px;
      margin: 0.25rem 0 0.85rem;
      background: #f8fafc;
    }
    .badge-icon-lg {
      width: 56px;
      height: 56px;
      object-fit: contain;
      border-radius: 8px;
      background: var(--primary-soft);
      padding: 6px;
    }
    .badge-fallback {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 56px;
      height: 56px;
      border-radius: 8px;
      background: var(--primary-soft);
      color: var(--primary);
      font-weight: 700;
      font-size: 1.25rem;
    }
    .badge-meta {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .pts-pill {
      align-self: flex-start;
      background: var(--success-soft);
      color: var(--success);
      padding: 0.1rem 0.5rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
    }
  `]
})
export class GiveRecognitionComponent {
  private fb = inject(FormBuilder);
  private recognition = inject(RecognitionService);
  private users$ = inject(UserService);
  private badges$ = inject(BadgeService);
  private auth = inject(AuthService);
  private notify = inject(NotificationService);

  users: User[] = [];
  badges: Badge[] = [];
  saving = false;
  success = false;
  error = '';

  form = this.fb.nonNullable.group({
    targetUserId: this.fb.control<number | null>(null, Validators.required),
    badgeId: this.fb.control<number | null>(null, Validators.required),
    message: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(500)]]
  });

  constructor() {
    this.users$.getAll().subscribe(list => {
      const me = this.auth.userId;
      this.users = list.filter(u => u.userId !== me && u.status !== 'Inactive');
    });
    this.badges$.getAll().subscribe(b => (this.badges = b));
  }

  submit() {
    if (this.form.invalid) return;
    this.saving = true;
    this.success = false;
    this.error = '';
    const raw = this.form.getRawValue();
    this.recognition
      .create({
        targetUserId: raw.targetUserId!,
        badgeId: raw.badgeId!,
        message: raw.message
      })
      .subscribe({
        next: saved => {
          this.saving = false;
          this.success = true;
          const badge = this.badges.find(b => b.badgeId === saved.badgeId);
          const badgeName = badge ? badge.badgeName : 'a badge';
          this.notify.send({
            userId: saved.targetUserId,
            message: `You received recognition: ${badgeName}.`,
            type: 'RECOGNITION_RECEIVED',
            sourceId: saved.recognitionId
          }).subscribe({ error: () => {} });
          this.form.reset({ targetUserId: null, badgeId: null, message: '' });
        },
        error: err => {
          this.saving = false;
          this.error = readApiError(err);
        }
      });
  }

  selectedBadge(): Badge | null {
    const id = this.form.controls.badgeId.value;
    if (id == null) return null;
    return this.badges.find(b => b.badgeId === id) ?? null;
  }

  onIconError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }

  initialOf(name: string): string {
    return (name || '?').charAt(0).toUpperCase();
  }
}

function readApiError(err: any): string {
  if (!err) return 'Request failed';
  if (typeof err.error === 'string') return err.error;
  if (err.error?.error) return err.error.error;
  return err.message || 'Request failed';
}
