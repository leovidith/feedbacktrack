# FeedbackTrack – Complete Project Overview

## 🏗️ Architecture Summary

FeedbackTrack is a **Java Spring Boot microservices application** built to manage internal employee feedback and recognition. It uses:

- **Spring Cloud Netflix Eureka** for service discovery
- **Spring Cloud Gateway** as the single entry-point API gateway
- **OpenFeign** for inter-service HTTP communication
- **Spring Security + JWT** for stateless authentication 
- **JPA/Hibernate + MySQL** for persistence (shared DB: `project`, separate for notification)
- **WebSocket (STOMP)** for real-time in-app notifications
- **Lombok** throughout to reduce boilerplate
- **Slf4j** for structured logging in every service

---

## 📦 Microservices Inventory

| Service | App Name | Port | DB Schema              |
|---|---|---|------------------------|
| Service Registry (Eureka) | `service-registry` | **8761** | —                      |
| API Gateway | `api-gateway` | **8088** | —                      |
| Auth & Admin Service | `feedbacktrack-auth-service` | **8086** | `project-auth`         |
| Feedback Service | `feedbacktrack-feedback-service` | **3000** | `project-feedback`     |
| Recognition Service | `feedbacktrack-recognition-service` | **3001** | `project-recognition`  |
| Manager Review Service | `feedbacktrack-manager-review-service` | **3002** | `project-manager`      |
| Notification Service | `notification-sample` | **1931** | `project-notification` |

---

## 🔗 API Gateway Routing (Port 8088)

All external traffic enters through the gateway and is load-balanced via Eureka (`lb://`):

| Path Prefix | Routes To |
|---|---|
| `/api/v1/feedback/**` | Feedback Service |
| `/api/v1/recognition/**` | Recognition Service |
| `/api/v1/manager/**` | Manager Review Service |
| `/api/admin/**` | Auth & Admin Service |
| `/notifications/**` | Notification Service |

Auto-discovery is also enabled (lower-case service IDs).

---

## 📌 Module 1 – Auth & User Management Service (Port 8086)

This is the **central authority** service that owns all user, department, category and badge data. It acts as the "admin backbone" for the entire platform.

### Entities / Tables
| Entity | Key Fields |
|---|---|
| **User** | `userId`, `name`, `email` (unique), `password` (BCrypt), `role` (EMPLOYEE/MANAGER/ADMIN), `department` (FK), `manager` (self-ref FK), `status` (Active/Inactive), `createdAt`, `updatedAt` |
| **Department** | `departmentId`, `departmentName` |
| **FeedbackCategory** | `categoryId`, `categoryName`, `description` |
| **BadgeLibrary** | `badgeId`, `badgeName`, `pointsValue`, `description`, `badgeIconPath` |

### Services
- **AuthService**: Validates email/password, blocks inactive accounts, returns the authenticated `User` object (JWT is then minted by the controller layer).
- **UserService**:
    - `registerEmployee()` – encodes password, links department (remote call to DepartmentService), links manager (local DB lookup), prevents duplicate emails.
    - `getUserById()` / `getUserByEmail()` – profile lookups.
    - `getTeamMembersByManagerId()` – used by Feedback, Recognition, and Manager Review services via Feign.
    - `updateRole()` – admin can promote/demote users.
    - `deactivateUser()` / `activateUser()` – admin manages user lifecycle; cannot deactivate self or other admins.
    - `changePassword()` – self-service with old-password verification.
    - `getAllEmployees()` / `getUsersByRole()` – bulk lookups.
- **DepartmentService** – CRUD for departments.
- **FeedbackCategoryService** – CRUD for feedback categories, queried by the Feedback Service.
- **BadgeLibraryService** – CRUD for badge types with points, queried by the Recognition Service.

### Security
- JWT secret: `FeedbackTrackProjectSecretKey2026!`
- BCrypt password encoding
- Spring Security with role-based filters

---

## 📌 Module 2 – Feedback Submission Service (Port 3000)

Allows employees to submit, view, and manage feedback entries.

### Entity – `Feedback` (table: `feedbacks`)
| Field | Detail |
|---|---|
| `feedbackId` | Auto-generated PK |
| `senderId` | References User (via Feign, not FK) |
| `targetUserId` | References User (via Feign, not FK) |
| `categoryId` | References FeedbackCategory (via Feign, not FK) |
| `comments` | 10–2000 characters, TEXT column |
| `isAnonymous` | Boolean, default `false` |
| `submittedDate` | Set to `LocalDateTime.now()` on creation |

### Key Operations (FeedbackService)
- **createNewFeedback()** – validates sender ≠ target, verifies both users and the category via Feign, saves feedback.
- **getFeedbackById()** – employees can only see feedback they sent or received; managers/admins see all.
- **deleteFeedbackById()** – only the original sender can delete.
- **getManagerFeedbacks()** – fetches all feedback received by a manager's team (via `getTeamMembersByManagerId()` Feign call).
- **toggleVisibility()** – the sender can flip the anonymous flag post-submission.
- **countByTargetUserId()** – used by the Manager Review Service's EngagementService for score calculation.

### Security
- JWT filter reads the user ID from the token's principal.
- Virtual threads enabled (`spring.threads.virtual.enabled=true`).
- Feign calls to Auth Service for user/category validation.

---

## 📌 Module 3 – Recognition & Appreciation Service (Port 3001)

Manages peer-to-peer recognition with badges and a points economy.

### Entity – `Recognition` (table: `recognitions`)
| Field | Detail |
|---|---|
| `recognitionId` | Auto-generated PK |
| `senderId` | References User (via Feign) |
| `targetUserId` | References User (via Feign) |
| `badgeId` | References BadgeLibrary in Auth Service (via Feign) |
| `message` | 5–500 characters, TEXT column |
| `recognizedDate` | `LocalDateTime.now()` on creation |

### Key Operations (RecognitionService)
- **create()** – blocks self-recognition; validates sender, target, and badge via Feign; stores IDs only (no DB foreign keys).
- **findReceived(userId)** – RBAC: self, admin, or the user's manager can view.
- **findSent(userId)** – same RBAC rules.
- **delete(recognitionId)** – only the sender or an admin can delete.
- **getTeamRecognitions(managerId)** – fetches all recognitions received by every team member under a manager.
- **viewRecognition(id)** – sender, recipient, or admin access only.
- **viewAll()** – admin-only recognition feed.
- **sumPointsByTargetUserId()** – iterates received recognitions, calls the Badge service for each badge's `pointsValue`, sums them up. This is the user's "recognition score" used for engagement calculations.

### Feign Clients
- `UserServiceClient` → Auth Service (`feedbacktrack-auth-service`)
- `AdminServiceClient` → Auth Service (for badge lookups)

---

## 📌 Module 4 – Manager Review & Engagement Service (Port 3002)

Enables managers to formally review feedback entries and computes engagement scores.

### Entity – `FeedbackReview` (table: `feedback_reviews`)
| Field | Detail |
|---|---|
| `reviewId` | Auto-generated PK |
| `feedbackId` | Unique constraint — one review per feedback |
| `reviewerId` | The manager's user ID |
| `actionTaken` | Enum: `PENDING`, `ACKNOWLEDGED`, `RESOLVED` |
| `managerNotes` | Optional, max 1000 characters |
| `reviewDate` | Immutable once set (`updatable=false`) |

### ReviewService Key Operations
- **createReview()** – enforces one-review-per-feedback, verifies reviewer is MANAGER or ADMIN, confirms reviewer manages the feedback's target user, cannot create directly as RESOLVED.
- **updateReview()** – owner or admin; RESOLVED reviews are immutable; feedbackId and reviewerId cannot change.
- **deleteReview()** – owner or admin only.
- **getReviewDetails()** – accessible by reviewer, the target employee, or admin.
- **getReviewsByReviewer()** – manager sees their own reviews; admin sees any reviewer's list.
- **getPendingReviews()** – returns reviews that are not yet RESOLVED.
- **getReviewByFeedbackId()** – accessible by reviewer, target, sender, or admin.
- **getAllReviews()** – admin-only.

### EngagementService Key Operations (within same service)
- **getTrendForReviewer(reviewerId, months)** – generates an `EngagementTrendDTO` for a manager:
    - Action breakdown (PENDING / ACKNOWLEDGED / RESOLVED counts)
    - Resolution rate percentage
    - Monthly review activity (configurable window: 1–24 months, default 6)
- **getGlobalTrend(currentUserId, months)** – admin-only global version of the above.
- **calculateIndividualScore(employeeId, requesterId)** – computes `(feedbackCount × 5) + recognitionPoints` for an employee using cross-service Feign calls to Feedback and Recognition services.
- **calculateTeamScores(managerId)** – returns a `Map<String, Double>` (employeeName → score) for the manager's full team.

### Feign Clients
- `UserClient` → Auth Service
- `FeedbackClient` → Feedback Service
- `RecognitionClient` → Recognition Service

---

## 📌 Module 5 – Notifications & In-App Alerts Service (Port 1931)

Handles in-app, real-time notifications (no external email or SMS).

### Entity – `Notification`
| Field | Detail |
|---|---|
| `notificationId` | Auto-generated PK |
| `userId` | Target recipient (validated via UserClient Feign) |
| `type` | Enum: `FEEDBACK_RECEIVED`, `RECOGNITION_RECEIVED` |
| `sourceId` | The FeedbackID or RecognitionID that triggered the alert |
| `message` | Human-readable alert text |
| `isRead` | Boolean, default `false` |
| `createdAt` | `LocalDateTime.now()` |

### NotificationService Key Operations
- **sendNotification(userId, message, type)** – validates user via Feign → saves to DB → **pushes real-time via WebSocket STOMP** to topic `/topic/notifications/{userId}`.
- **getUserNotifications(userId)** – fetches all notifications for a user (returns `NotificationResponse` DTOs).
- **markAsRead(id)** – marks a single notification as read.

### Real-Time Infrastructure
- **WebSocketConfig** registers a STOMP endpoint; clients subscribe to `/topic/notifications/{userId}`.
- `SimpMessagingTemplate` is used server-side to push messages.
- JWT validation is performed via `JwtAuthenticationFilter` before any endpoint is reached.
- **Separate database schema** (`new_project`) isolates notification data from the core `project` schema.

---

## 🔐 Security Architecture

All services except the Service Registry and API Gateway implement JWT-based security:

| Concern | Implementation |
|---|---|
| Token format | JWT (HS256), secret shared across services |
| Authentication | Login via Auth Service → JWT returned → client sends in `Authorization: Bearer` header |
| Authorization | Each service validates JWT locally via a `JwtFilter` / `JwtAuthenticationFilter` |
| Principal | JWT subject = User ID (Long), stored in SecurityContext |
| Role enforcement | Checked in service layer using the User Service (Feign) — not just from the token |
| Account status | Inactive users blocked at login |
| Password | BCrypt encoded; change requires old password verification |
| Admin safety | Admins cannot be deactivated; users cannot deactivate themselves |

---

## 🔄 Inter-Service Communication Map

```
API Gateway (8088)
    ├── → Auth Service (8086)       [User, Department, FeedbackCategory, BadgeLibrary]
    ├── → Feedback Service (3000)   [Feedback CRUD]
    ├── → Recognition Service (3001) [Recognition CRUD + Points]
    ├── → Manager Review (3002)     [Review CRUD + Engagement Scores/Trends]
    └── → Notification Service (1931) [Notifications + WebSocket]

Feedback Service ──Feign──► Auth Service (getUserById, getCategoryById, getTeamMembers)
Recognition Service ──Feign──► Auth Service (getUserById, getBadgeById, getTeamMembers)
Manager Review ──Feign──► Auth Service (getUserById, getTeamMembers)
Manager Review ──Feign──► Feedback Service (getFeedbackById, countByTargetUserId)
Manager Review ──Feign──► Recognition Service (sumPointsByTargetUserId)
Notification Service ──Feign──► Auth Service (getUserById)

All services ──► Eureka Registry (8761) [registration + discovery]
```

---

## 📊 Database Design (Actual Implementation)

**Schema: `project`** (shared by Auth, Feedback, Recognition, Manager Review)

| Table | Managed By | Key Columns |
|---|---|---|
| `user` | Auth Service | userId, name, email, password, role, departmentId (FK), managerId (self-FK), status, created_at, updated_at |
| `department` | Auth Service | departmentId, departmentName |
| `feedback_category` | Auth Service | categoryId, categoryName, description |
| `badge_library` | Auth Service | badgeId, badgeName, pointsValue, description, badgeIconPath |
| `feedbacks` | Feedback Service | feedbackId, senderId, targetUserId, categoryId, comments, isAnonymous, submittedDate |
| `recognitions` | Recognition Service | recognitionId, senderId, targetUserId, badgeId, message, recognizedDate |
| `feedback_reviews` | Manager Review | reviewId, feedbackId (unique), reviewerId, actionTaken, managerNotes, reviewDate |

**Schema: `new_project`** (Notification Service)

| Table | Key Columns |
|---|---|
| `notification` | notificationId, userId, type, sourceId, message, isRead, createdAt |

> All schemas use `spring.jpa.hibernate.ddl-auto=update` — tables are auto-created/updated on startup.

---

## 🧪 Test Coverage

The project includes JUnit unit tests with Mockito:

| Service | Test Files |
|---|---|
| Auth Service | `AuthServiceTest`, `UserServiceTest`, `BadgeLibraryServiceTest`, `FeedbackCategoryServiceTest`, `DepartmentServiceTest` |
| Feedback Service | `FeedbackServiceTest` |
| Recognition Service | `RecognitionServiceTest` |
| Manager Review | `ReviewServiceTest`, `EngagementServiceTest` |
| Notification Service | `NotificationServiceTest`, `NotificationControllerTest` |

---

## 🔑 Key Business Rules Enforced in Code

1. **No self-feedback** – Feedback Service blocks `senderId == targetUserId`.
2. **No self-recognition** – Recognition Service blocks sender recognizing themselves.
3. **One review per feedback** – Manager Review enforces a unique constraint on `feedbackId` in `feedback_reviews`.
4. **RESOLVED reviews are immutable** – Cannot update or transition from RESOLVED status.
5. **New reviews cannot start as RESOLVED** – Must go PENDING → ACKNOWLEDGED → RESOLVED.
6. **Managers can only review feedback for their own team members** – validated via Feign call to User Service.
7. **Inactive accounts are blocked at login** – AuthService checks `status` before password comparison.
8. **Admin accounts are protected** – Cannot be deactivated by anyone, even other admins.
9. **Anonymous flag is toggleable** – Sender can flip `isAnonymous` after submission.
10. **Engagement score formula**: `(feedbackCount × 5) + recognitionPoints`.

---

## 📐 Summary Diagram

```
[Frontend (Angular/React)]
           │
           ▼
  [API Gateway :8088]
      ├─ /api/admin/**     ──► [Auth & Admin Service :8086]
      │                        Users, Departments, Categories, Badges, JWT issuance
      ├─ /api/v1/feedback/**──► [Feedback Service :3000]
      │                        Submit, view, delete, anonymize feedback
      ├─ /api/v1/recognition/**►[Recognition Service :3001]
      │                        Send badges, view history, sum points
      ├─ /api/v1/manager/**──► [Manager Review Service :3002]
      │                        Review feedback, engagement trends & scores
      └─ /notifications/**──► [Notification Service :1931]
                               In-app alerts + WebSocket real-time push

  [Eureka Service Registry :8761] ← all services register here
  [MySQL :3306 / schemas: project, new_project]
```
