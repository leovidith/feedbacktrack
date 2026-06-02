export type Role = 'EMPLOYEE' | 'MANAGER' | 'ADMIN';

export interface User {
  userId: number;
  name: string;
  email: string;
  role: Role;
  departmentName?: string;
  status?: string;
  managerId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginResponse {
  token: string;
  role: Role;
  name: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
  departmentId?: number;
  managerId?: number;
  status?: string;
}

export interface ChangePasswordRequest {
  email: string;
  oldPassword: string;
  newPassword: string;
}

export interface Department {
  departmentId: number;
  departmentName: string;
}

export interface FeedbackCategory {
  categoryId: number;
  categoryName: string;
  description?: string;
}

export interface Badge {
  badgeId: number;
  badgeName: string;
  pointsValue: number;
  description?: string;
  badgeIconPath?: string;
}

export interface Feedback {
  feedbackId: number;
  senderId?: number | null;
  targetUserId: number;
  categoryId: number;
  comments: string;
  isAnonymous: boolean;
  submittedDate: string;
}

export interface CreateFeedbackRequest {
  targetUserId: number;
  categoryId: number;
  comments: string;
  isAnonymous: boolean;
}

export interface Recognition {
  recognitionId: number;
  senderId: number;
  targetUserId: number;
  badgeId: number;
  message: string;
  recognizedDate: string;
}

export interface CreateRecognitionRequest {
  targetUserId: number;
  badgeId: number;
  message: string;
}

export type FeedbackAction = 'PENDING' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface FeedbackReview {
  reviewId: number;
  feedbackId: number;
  reviewerId: number;
  actionTaken: FeedbackAction;
  managerNotes?: string;
  reviewDate: string;
}

export interface SubmitReviewRequest {
  feedbackId: number;
  action: FeedbackAction;
  notes?: string;
}

export type NotificationType =
  | 'FEEDBACK_RECEIVED'
  | 'RECOGNITION_RECEIVED'
  | 'ACKNOWLEDGED'
  | 'RESOLVED';

export interface AppNotification {
  notificationId: number;
  type: NotificationType;
  message: string;
  sourceId?: number;
  isRead: boolean;
  createdAt: string;
}
