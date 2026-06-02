import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },

  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent)
  },

  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },

  {
    path: 'feedback/submit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/feedback/submit-feedback.component').then(m => m.SubmitFeedbackComponent)
  },
  {
    path: 'feedback/mine',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/feedback/my-feedback.component').then(m => m.MyFeedbackComponent)
  },
  {
    path: 'feedback/view/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/feedback/feedback-detail.component').then(m => m.FeedbackDetailComponent)
  },
  {
    path: 'feedback/team',
    canActivate: [authGuard, roleGuard(['MANAGER', 'ADMIN'])],
    loadComponent: () =>
      import('./features/feedback/team-feedback.component').then(m => m.TeamFeedbackComponent)
  },
  {
    path: 'feedback/all',
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    loadComponent: () =>
      import('./features/feedback/all-feedback.component').then(m => m.AllFeedbackComponent)
  },

  {
    path: 'recognition/give',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recognition/give-recognition.component').then(m => m.GiveRecognitionComponent)
  },
  {
    path: 'recognition/mine',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recognition/my-recognitions.component').then(m => m.MyRecognitionsComponent)
  },
  {
    path: 'recognition/all',
    canActivate: [authGuard, roleGuard([ 'ADMIN'])],
    loadComponent: () =>
      import('./features/recognition/my-recognitions.component').then(m => m.MyRecognitionsComponent)
  },

  {
    path: 'reviews/mine',
    canActivate: [authGuard, roleGuard(['MANAGER', 'ADMIN'])],
    loadComponent: () =>
      import('./features/reviews/my-reviews.component').then(m => m.MyReviewsComponent)
  },

  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/notifications/notifications.component').then(m => m.NotificationsComponent)
  },

  {
    path: 'profile/change-password',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/change-password.component').then(m => m.ChangePasswordComponent)
  },

  {
    path: 'admin/users',
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/users.component').then(m => m.UsersComponent)
  },
  {
    path: 'admin/departments',
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    loadComponent: () =>
      import('./features/admin/departments.component').then(m => m.DepartmentsComponent)
  },
  {
    path: 'admin/categories',
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    loadComponent: () =>
      import('./features/admin/categories.component').then(m => m.CategoriesComponent)
  },
  {
    path: 'admin/badges',
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/badges.component').then(m => m.BadgesComponent)
  },

  { path: '**', redirectTo: 'home' }
];
