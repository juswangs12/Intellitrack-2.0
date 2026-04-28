# IntelliTrack - Capstone Deliverables Tracking System

A comprehensive web-based platform for managing capstone project submissions, tracking deliverables, and providing role-based analytics for students, advisers, coordinators, and administrators.

| Module | Feature | Status |
|--------|---------|--------|
| 1 | User Management System | ✅ Complete |
| 2.1 | Deadline Monitoring & Smart Reminders | ✅ Complete |
| 2.2 | Submission Status Monitoring | ✅ Complete |
| 3.1 | Real-Time Submission Insights | ✅ Complete |
| 3.2 | AI Submission Summary | ✅ Complete |
| 3.3 | Submission Tracking Analytics Dashboard | ✅ Complete |

---

## Table of Contents

- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [Test Accounts](#test-accounts)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Features — Module 1](#features--module-1-user-management-system)
- [Features — Modules 2 & 3](#features--modules-2--3)
- [Architecture Overview](#architecture-overview)
- [Security Architecture](#security-architecture)
- [Code Standards & Conventions](#code-standards--conventions)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites
- **Node.js** 18+ and npm
- **Java** 21+
- **Maven** 3.6+
- **Git** (optional)

### 60-Second Setup
```bash
# Terminal 1 - Backend
cd Backend
mvn clean package -DskipTests
mvn spring-boot:run

# Terminal 2 - Frontend
cd Frontend
npm install
npm start
```

Then open `http://localhost:3000` and login with:
- **Email**: `student@university.edu`
- **Password**: `password123`

---

## Project Structure

```
IntelliTrack-2.0/
├── Backend/
│   ├── src/main/java/com/intellitrack/
│   │   ├── controller/
│   │   │   ├── AuthController.java                 # Login / OAuth2 / refresh
│   │   │   ├── UserController.java                 # User CRUD, avatars
│   │   │   ├── DashboardController.java            # Role-specific dashboards
│   │   │   ├── SubmissionStatusController.java     # Module 2.2 — status monitoring
│   │   │   ├── AnalyticsController.java            # Module 3.1 / 3.3 — insights & tracking
│   │   │   ├── SummaryController.java              # Module 3.2 — AI submission summary
│   │   │   └── DeadlineMonitoringController.java   # Module 2.1 — deadlines, calendar, reminders
│   │   ├── service/
│   │   │   ├── AuthService.java                    # JWT, OAuth2, domain validation
│   │   │   ├── UserService.java                    # Profile, password, avatars
│   │   │   ├── CustomOAuth2UserService.java        # Google OAuth integration
│   │   │   ├── StatusEvaluationService.java        # Computes PENDING/SUBMITTED/LATE/UPDATED
│   │   │   ├── MetricsCalculationService.java      # Builds TrackingSnapshotDto
│   │   │   ├── AnalyticsFormatterService.java      # Formats snapshot → dashboard DTO
│   │   │   ├── SubmissionMetricsEngine.java        # Computes insight percentages & trends
│   │   │   ├── TrendVisualizationService.java      # Formats insight → InsightHubDto
│   │   │   ├── SubmissionDataService.java          # Fetches raw summary data by group
│   │   │   ├── AISummaryEngine.java                # Rule-based summary text generation
│   │   │   ├── SummaryFormatterService.java        # Composes final SubmissionSummaryDto
│   │   │   ├── AISubmissionRiskEngine.java         # Rule-based risk scoring per deadline
│   │   │   ├── SmartReminderService.java           # Builds ReminderDto from risk + deadline
│   │   │   ├── DeadlineMonitoringService.java      # Active deadlines, calendar, reminders
│   │   │   └── AutomatedAlertDispatcher.java       # @Scheduled hourly reminder dispatch
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── ProjectGroupRepository.java
│   │   │   ├── DeliverableRepository.java
│   │   │   ├── DeadlineRepository.java
│   │   │   ├── SubmissionRepository.java
│   │   │   ├── ReminderLogRepository.java
│   │   │   └── RiskAssessmentLogRepository.java
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── ProjectGroup.java
│   │   │   ├── Deliverable.java
│   │   │   ├── Deadline.java
│   │   │   ├── Submission.java
│   │   │   ├── SubmissionStatus.java               # Enum: PENDING, SUBMITTED, LATE, UPDATED
│   │   │   ├── ReminderLog.java
│   │   │   └── RiskAssessmentLog.java
│   │   ├── dto/
│   │   │   ├── UserDTO.java                        # Includes groupId, groupCode, groupTitle
│   │   │   ├── LoginRequest.java / LoginResponse.java
│   │   │   ├── UpdateProfileRequest.java / PasswordChangeRequest.java / ErrorResponse.java
│   │   │   ├── DeliverableStatusDto.java
│   │   │   ├── GroupStatusSummaryDto.java
│   │   │   ├── MetricCardDto.java / ChartPointDto.java / ActivityFeedItemDto.java
│   │   │   ├── GroupProgressDto.java / TrackingSnapshotDto.java
│   │   │   ├── SubmissionTrackingDashboardDto.java
│   │   │   ├── RealTimeInsightDto.java / InsightHubDto.java
│   │   │   ├── RiskAssessmentDto.java / DeadlineCardDto.java
│   │   │   ├── CalendarDeadlineDto.java / ReminderDto.java
│   │   │   ├── DeliverableSummaryRowDto.java / SummaryInsightDto.java
│   │   │   ├── RawSubmissionSummaryData.java
│   │   │   └── SubmissionSummaryDto.java
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java        # Validates Bearer tokens on every request
│   │   │   ├── OAuth2AuthenticationSuccessHandler.java
│   │   │   └── OAuth2AuthenticationFailureHandler.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java                 # Spring Security + JWT filter + headers
│   │   │   ├── CorsConfig.java
│   │   │   ├── DataInitializer.java                # Seeds all test data on startup
│   │   │   └── EnvironmentConfig.java
│   │   └── IntelliTrackApplication.java            # @EnableScheduling
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── Frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── PrivateRoute.jsx
│   │   │   ├── UserProfile.jsx
│   │   │   ├── MetricCard.jsx                      # Reusable stat card
│   │   │   ├── ActivityFeed.jsx                    # Timestamped activity list
│   │   │   ├── ProgressList.jsx                    # Per-group progress bars
│   │   │   ├── StatusMonitoringPanel.jsx           # Module 2.2 — deliverable status cards
│   │   │   ├── SubmissionTrackingDashboard.jsx     # Module 3.3 — analytics dashboard
│   │   │   ├── InsightHubDashboard.jsx             # Module 3.1 — real-time insights
│   │   │   ├── InstitutionalOversightDashboard.jsx # Module 3.2 — AI summary by group
│   │   │   ├── SubmissionTrackerDashboard.jsx      # Module 2.1 — deadline cards + calendar
│   │   │   └── NotificationCenter.jsx             # Module 2.1 — smart reminder notifications
│   │   ├── context/
│   │   │   └── AuthContext.jsx
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   ├── StudentDashboard.jsx                # Tabs: dashboard, deliverables, analytics, profile
│   │   │   ├── AdviserDashboard.jsx                # Tabs: dashboard, students, submissions, insights, profile
│   │   │   ├── CoordinatorDashboard.jsx            # Tabs: dashboard, analytics, submissions, reports, profile
│   │   │   └── AdminDashboard.jsx
│   │   ├── services/
│   │   │   └── ApiService.js                       # Centralised HTTP client with JWT + token refresh
│   │   ├── styles/
│   │   │   ├── Dashboard.css / Login.css / UserProfile.css / App.css / index.css / StudentDashboard.css
│   │   ├── App.js
│   │   └── index.js
│   ├── public/
│   │   └── index.html
│   └── package.json
│
├── README.md
└── TESTING_GUIDE.md                                # QA testing guide
```

---

## Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| **Frontend** | React | 18.2 |
| | React Router | 6.8 |
| | Recharts | ^2.15.4 |
| | date-fns | ^4.1.0 |
| | CSS (vanilla) | — |
| **Backend** | Spring Boot | 3.2.0 |
| | Java | 21 |
| | Spring Security | (with OAuth2 + JWT filter) |
| | Spring Scheduling | (@EnableScheduling) |
| **Auth** | JWT (JJWT) | 0.11.5 |
| | Google OAuth2 | — |
| **Database** | H2 (dev) | In-memory |
| | PostgreSQL (prod) | — |
| **Build** | Maven | 3.6+ |
| | npm | — |

---

## Setup Instructions

### 1. Clone Repository
```bash
git clone <repo-url>
cd Intellitrack-2.0
```

### 2. Backend Setup

```bash
cd Backend
# Verify Java 21+ and Maven 3.6+
java -version
mvn -version
```

```bash
mvn clean package -DskipTests
```

Set the following environment variables (or use the defaults for local development):

| Variable | Default (dev) | Required in prod |
|----------|--------------|-----------------|
| `JWT_SECRET` | `default-jwt-secret-change-in-production` | **Yes** — use 32+ random chars |
| `GOOGLE_CLIENT_ID` | `your-google-client-id` | Yes for OAuth2 |
| `GOOGLE_CLIENT_SECRET` | `your-google-client-secret` | Yes for OAuth2 |
| `SPRING_DATASOURCE_URL` | H2 in-memory | Yes — set PostgreSQL URL |
| `SPRING_H2_CONSOLE_ENABLED` | `true` | Set `false` in prod |

### 3. Frontend Setup

```bash
cd Frontend
npm install
```

---

## Running the Application

### Option 1: Development Mode (Recommended)

**Terminal 1 - Backend:**
```bash
cd Backend
mvn spring-boot:run
```
Backend runs on `http://localhost:8080`

**Terminal 2 - Frontend:**
```bash
cd Frontend
npm start
```
Frontend runs on `http://localhost:3000` (opens automatically)

### Option 2: Production Build

**Backend:**
```bash
cd Backend
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

**Frontend:**
```bash
cd Frontend
npm run build
# Serve build folder with your preferred web server
npx serve -s build -l 3000
```

### Option 3: Docker (If Dockerfile exists)
```bash
docker-compose up
```

---

## Test Accounts

The `DataInitializer` seeds all test data automatically on every backend startup (H2 is recreated on each restart). The seeded group has **id = 1** and the student user has **id = 2**.

### Pre-populated Test Users

| Email | Password | Role | Group / Notes |
|-------|----------|------|---------------|
| `adviser@university.edu` | `password123` | Adviser | Adviser of group CS-2026-A (id=1) |
| `student@university.edu` | `password123` | Student | Member of group CS-2026-A (groupId=1) |
| `coordinator@university.edu` | `password123` | Coordinator | System-wide access |
| `admin@university.edu` | `password123` | Administrator | Full system access |

### Seeded Deliverables, Deadlines & Submissions

| Deliverable | Stage | Due (relative to startup) | Submission Status |
|-------------|-------|--------------------------|-------------------|
| Proposal Document | Proposal | +5 days | SUBMITTED |
| Midterm Report | Midterm | +12 days | PENDING |
| Final Defense | Final | +25 days | PENDING |

---

## API Endpoints

All endpoints except `/api/auth/**`, `/login/**`, `/oauth2/**`, and `/h2-console/**` require `Authorization: Bearer <token>`.

### Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | Public | Email/password login → `{ token, refreshToken, user }` |
| POST | `/api/auth/refresh-token?refreshToken={token}` | Public | Refresh JWT → `{ token }` |
| POST | `/api/auth/logout` | Public | Frontend clears tokens |

### User Management

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all users (`?role=student\|adviser\|coordinator\|administrator`) |
| POST | `/api/users` | Create new user |
| GET | `/api/users/{id}` | Get user by ID |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |
| GET | `/api/users/{id}/profile` | Get profile |
| PUT | `/api/users/{id}/profile` | Update profile |
| POST | `/api/users/{id}/change-password` | Change password |
| POST | `/api/users/{id}/avatar` | Upload avatar (multipart) |
| GET | `/api/users/{id}/avatar` | Serve avatar image |

### Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard/student/{id}` | Student stats + deliverable counts |
| GET | `/api/dashboard/adviser/{id}` | Adviser stats + assigned student count |
| GET | `/api/dashboard/coordinator/{id}` | System-wide metrics |
| GET | `/api/dashboard/admin/{id}` | User counts by role |

### Module 2.2 — Status Monitoring

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/status-monitoring/groups/{groupId}` | Deliverable status list for a group → `List<DeliverableStatusDto>` |
| GET | `/api/status-monitoring/classes?adviserId={id}` | All groups' submission summary for an adviser → `List<GroupStatusSummaryDto>` |

### Module 3.3 — Submission Tracking Analytics

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/analytics/tracking?adviserId={id}` | Full dashboard DTO (metric cards, pie chart, activity feed, group progress) → `SubmissionTrackingDashboardDto` |
| GET | `/api/analytics/insights?stage={stage}&adviserId={id}` | Trend charts and status breakdown → `InsightHubDto` |

### Module 3.2 — AI Submission Summary

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/submission-summary?groupId={id}` | Rule-based AI narrative summary for a group → `SubmissionSummaryDto` |

### Module 2.1 — Deadline Monitoring & Smart Reminders

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/deadlines/active?groupId={id}` | Active deadline cards with risk scores → `List<DeadlineCardDto>` |
| GET | `/api/deadlines/calendar?year={y}&month={m}` | All deadlines in a calendar month → `List<CalendarDeadlineDto>` |
| GET | `/api/deadlines/reminders?userId={id}` | Smart reminders for a user's group → `List<ReminderDto>` |

---

## Database Schema

H2 in-memory database is recreated on every backend restart (`ddl-auto=create-drop`). For production, point `SPRING_DATASOURCE_URL` to a PostgreSQL instance.

### Core Tables

```sql
-- User accounts (all roles)
users (id, first_name, last_name, email, password, role, student_id,
       department, user_year, phone, avatar_filename, group_id FK→project_groups, created_at, updated_at)

-- Capstone project groups
project_groups (id, code UNIQUE, title, adviser_id FK→users, created_at)

-- Deliverable types (Proposal, Midterm, Final)
deliverables (id, name UNIQUE, stage, active)

-- Due dates per deliverable
deadlines (id, deliverable_id FK→deliverables, due_at, academic_term)

-- Submission records per group per deliverable
submissions (id, group_id FK→project_groups, deliverable_id FK→deliverables,
             status ENUM(PENDING,SUBMITTED,LATE,UPDATED),
             submitted_at, version_number, revision_count, file_url, notes)

-- Audit log of dispatched reminders
reminder_logs (id, group_id FK, deliverable_id FK, message, channel, sent_at)

-- Audit log of risk assessments
risk_assessment_logs (id, group_id FK, deliverable_id FK, risk_score, risk_level, assessed_at)
```

### Entity Relationships

- `users.group_id` → `project_groups.id` (student's group)
- `project_groups.adviser_id` → `users.id` (adviser of the group)
- `submissions.group_id` → `project_groups.id`
- `submissions.deliverable_id` → `deliverables.id`
- `deadlines.deliverable_id` → `deliverables.id`

---

## Features — Module 1: User Management System

### ✅ Implemented

1. **Secure Authentication**
   - Email/password login with BCrypt password hashing
   - Google OAuth2 integration for students
   - JWT access tokens (24-hour expiry) + refresh tokens
   - Email domain whitelist validation

2. **Role-Based Access Control (RBAC)**
   - 4 roles: Student, Adviser, Coordinator, Administrator
   - Role-based frontend route protection via `PrivateRoute`
   - JWT-validated endpoint authorization via `JwtAuthenticationFilter`

3. **User Profile Management**
   - View/edit name, email, phone, department, year
   - Secure password change
   - Avatar upload and serving

4. **Personalized Dashboards**
   - **Student**: Deliverable stats, recent activity, notification center, status panel
   - **Adviser**: Insight hub, student list, analytics, submission tracking
   - **Coordinator**: System-wide analytics, submission reports
   - **Admin**: User statistics by role, user management

5. **User Management (Admin)**
   - List, create, edit, delete users
   - Avatar management

---

## Features — Modules 2 & 3

### Module 2.1 — Deadline Monitoring & Smart Reminders

- **Active Deadline Cards** — per-group cards showing deliverable name, stage, due date, hours remaining, countdown label, risk score/level, and revision count
- **Deadline Calendar** — returns all deadlines in a given year/month for a calendar view
- **Smart Reminders** — per-user reminders derived from risk assessment (score 0–100, levels: LOW / MEDIUM / HIGH / CRITICAL), driven by hours remaining, current submission status, revision history, and prior late count
- **Automated Dispatcher** — `@Scheduled` job runs every hour, builds the dispatch queue, and persists reminder logs to `reminder_logs`

### Module 2.2 — Submission Status Monitoring

- **Group Status View** — lists every deliverable for a group with status (PENDING / SUBMITTED / LATE / UPDATED), deadline, hours remaining, submittedAt timestamp, and revision count
- **Class Status View** — adviser-scoped aggregated view showing submitted / pending / late counts across all groups

### Module 3.1 — Real-Time Submission Insights

- **Insight Hub** — metric cards (on-time %, late %, pending %), filterable by `stage` (Proposal / Midterm / Final) and `adviserId`
- **Trend Series** — chart-ready data points representing submission counts by period
- **Status Breakdown** — pie/bar-ready data for current distribution across all statuses

### Module 3.2 — AI Submission Summary

- **Rule-Based Narrative Engine** — generates a human-readable headline + detail sentence per group based on submitted, late, pending, and revision counts (no external AI API required)
- **Deliverable Table** — per-deliverable breakdown with status, submittedAt, and revisionCount
- **Activity Timeline** — chronological list of submission events for the group

### Module 3.3 — Submission Tracking Analytics Dashboard

- **Metric Cards** — total deliverables, submitted, pending, late with semantic tone (positive / warning / danger)
- **Status Distribution Chart** — pie chart data by status
- **Group Progress** — per-group completion rate (0–100 %) for bar chart and progress list
- **Activity Feed** — most recent submission events across all groups

---

## Architecture Overview

### Frontend Architecture

```
App.js (Routes + Role-based redirect)
├── AuthContext (Global JWT + user state)
├── PrivateRoute (Role guard)
├── ApiService (Centralized HTTP client with auto token refresh)
└── Pages
    ├── Login.jsx
    ├── StudentDashboard.jsx        → NotificationCenter, StatusMonitoringPanel,
    │                                  SubmissionTrackerDashboard
    ├── AdviserDashboard.jsx        → InsightHubDashboard, SubmissionTrackingDashboard,
    │                                  InstitutionalOversightDashboard
    ├── CoordinatorDashboard.jsx    → SubmissionTrackingDashboard, InsightHubDashboard,
    │                                  InstitutionalOversightDashboard
    └── AdminDashboard.jsx
```

### Backend Architecture

```
IntelliTrackApplication.java  (@EnableScheduling)
├── SecurityConfig              JWT filter chain, stateless sessions, security headers
├── JwtAuthenticationFilter     Validates Bearer token on every request
├── CorsConfig                  CORS for http://localhost:3000
├── DataInitializer             Seeds adviser, student, group, deliverables, deadlines, submissions
├── Controllers
│   ├── AuthController
│   ├── UserController          (@Transactional)
│   ├── DashboardController     (@Transactional readOnly)
│   ├── SubmissionStatusController
│   ├── AnalyticsController
│   ├── SummaryController
│   └── DeadlineMonitoringController
├── Services (all @Transactional readOnly unless noted)
│   ├── AuthService             (@Transactional — prevents LazyInitializationException)
│   ├── UserService             (@Transactional)
│   ├── StatusEvaluationService
│   ├── MetricsCalculationService / AnalyticsFormatterService
│   ├── SubmissionMetricsEngine / TrendVisualizationService
│   ├── SubmissionDataService / AISummaryEngine / SummaryFormatterService
│   ├── AISubmissionRiskEngine / SmartReminderService
│   ├── DeadlineMonitoringService
│   └── AutomatedAlertDispatcher  (@Scheduled cron "0 0 * * * *")
└── Repositories (JpaRepository for all 7 entities)
```

---

## Security Architecture

| Control | Implementation |
|---------|---------------|
| Token validation | `JwtAuthenticationFilter` — runs before every request, validates HMAC-SHA signature, expiry, and claims |
| Stateless sessions | `SessionCreationPolicy.STATELESS` — no `JSESSIONID`, no server-side session state |
| Protected endpoints | All `/api/users/**`, `/api/dashboard/**`, `/api/analytics/**`, `/api/deadlines/**`, `/api/status-monitoring/**`, `/api/submission-summary/**` require a valid JWT |
| Public endpoints | `/api/auth/**`, `/login/**`, `/oauth2/**`, `/h2-console/**` |
| Password storage | BCrypt with default strength factor |
| Clickjacking | `X-Frame-Options: SAMEORIGIN` (H2 console works; external embedding blocked) |
| XSS | `X-XSS-Protection: 1; mode=block` response header |
| MIME sniffing | `X-Content-Type-Options: nosniff` response header |
| CORS | Restricted to `http://localhost:3000`; credentials allowed |
| SQL injection | JPA/Hibernate parameterised queries only; no raw SQL |
| Secret management | All secrets via environment variables; no hard-coded values in committed code |

> **Production checklist**: set `JWT_SECRET` to a 32+ character random string, disable the H2 console (`SPRING_H2_CONSOLE_ENABLED=false`), and switch to PostgreSQL.

---

## Code Standards & Conventions

### Backend

- **Package Structure**: `com.intellitrack.{entity,dto,service,repository,controller,config,security}`
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **Transactions**: Services are `@Transactional(readOnly=true)` by default; write operations override with `@Transactional`. Controllers that construct DTOs from lazy-loaded entities must also be `@Transactional`
- **Error Handling**: Return appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 500)
- **DTOs**: Java records; never expose JPA entities directly in responses
- **Validation**: Use `@Valid` + `@NotBlank`, `@Size`, etc. at controller boundaries

### Frontend

- **Structure**: Components in `components/`, pages in `pages/`, styles in `styles/`
- **Naming**: PascalCase for components, camelCase for variables/functions
- **State Management**: React Context for global auth state, `useState` for local state
- **HTTP**: Use `apiService` (singleton in `ApiService.js`) — handles `Authorization` header and automatic token refresh on 401
- **Routing**: React Router v6 — `Routes`, `Route`, `Navigate`
- **No TypeScript** — vanilla JSX only
- **No Tailwind** — vanilla CSS only

### Database Naming

- **Tables**: snake_case, plural (`users`, `submissions`, `project_groups`)
- **Columns**: snake_case (`first_name`, `created_at`, `group_id`)
- **Primary Keys**: `id`
- **Foreign Keys**: `{entity}_id`

---

## Troubleshooting

### Backend Issues

**Problem**: `mvn command not found`
```bash
mvn -version   # If not found: https://maven.apache.org/download.cgi
```

**Problem**: Java version mismatch
```bash
java -version  # Must be 21+
```

**Problem**: Port 8080 already in use
```bash
# In application.properties or via env var:
SERVER_PORT=8081
```

**Problem**: H2 console not accessible
- Visit `http://localhost:8080/h2-console`
- Driver: `org.h2.Driver` | URL: `jdbc:h2:mem:intellitrack` | User: `sa` | Password: *(blank)*

**Problem**: API returns 401 on module endpoints (status-monitoring, analytics, etc.)
- All module endpoints require a valid JWT. Ensure the frontend sends `Authorization: Bearer <token>`.
- Tokens expire after 24 hours. The frontend retries with a refreshed token automatically on 401.

### Frontend Issues

**Problem**: CORS errors in browser console
- Ensure backend is running on `http://localhost:8080`
- Verify `app.cors.allowed-origins` in `application.properties` includes `http://localhost:3000`

**Problem**: Blank page or white screen
```bash
# Check browser console (F12 → Console) for errors
npm start  # Restart dev server
```

**Problem**: Charts not rendering
- Confirm `recharts` is installed: `npm list recharts`
- Run `npm install` if missing

**Problem**: `date-fns` import errors
- Run `npm install` — `date-fns ^4.1.0` is in `package.json`

---

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [Recharts Documentation](https://recharts.org)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

---

**Last Updated**: April 28, 2026 | **Version**: 2.0