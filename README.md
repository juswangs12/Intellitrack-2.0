# IntelliTrack - Capstone Deliverables Tracking System

A comprehensive web-based platform for managing capstone project submissions, tracking deliverables, and providing role-based analytics for students, advisers, coordinators, and administrators.

**Status**: Module 1 (User Management System) ✅ Complete | Module 2-3 🔄 In Development

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
- [Features - Module 1](#features---module-1-user-management-system)
- [Architecture Overview](#architecture-overview)
- [For Module 2 & 3 Developers](#for-module-2--3-developers)
- [Code Standards & Conventions](#code-standards--conventions)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites
- **Node.js** 16+ and npm
- **Java** 17+
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
│   │   │   ├── AuthController.java         # Login/Auth endpoints
│   │   │   ├── UserController.java         # User CRUD, avatars
│   │   │   └── DashboardController.java    # Role dashboards
│   │   ├── service/
│   │   │   ├── AuthService.java            # JWT, OAuth2, domain checks
│   │   │   ├── UserService.java            # Profile, password, avatars
│   │   │   ├── CustomOAuth2UserService.java # Google OAuth integration
│   │   ├── repository/
│   │   │   └── UserRepository.java         # Database queries
│   │   ├── entity/
│   │   │   └── User.java                   # User model (id, email, role, avatar, advisor_id)
│   │   ├── dto/
│   │   │   ├── UserDTO.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── UpdateProfileRequest.java
│   │   │   ├── PasswordChangeRequest.java
│   │   │   └── ErrorResponse.java
│   │   ├── security/
│   │   │   ├── OAuth2AuthenticationSuccessHandler.java
│   │   │   └── OAuth2AuthenticationFailureHandler.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java         # Spring Security + CORS + OAuth2
│   │   │   ├── CorsConfig.java             # CORS configuration
│   │   │   ├── DataInitializer.java        # Test data initialization
│   │   │   └── EnvironmentConfig.java      # Environment variables
│   │   └── IntelliTrackApplication.java
│   ├── src/main/resources/
│   │   └── application.properties           # Server config, DB, OAuth2, JWT
│   ├── pom.xml                              # Maven dependencies
│   └── Module1_UserManagementSystem.md      # Module 1 requirements
│
├── Frontend/
│   ├── src/
│   │   ├── components/
│   │   │   └── UserProfile.jsx              # Profile view/edit, password, avatar
│   │   │   └── PrivateRoute.jsx             # Role-based route protection
│   │   └── context/
│   │       └── AuthContext.jsx              # Global auth state (login, token, user)
│   │   ├── pages/
│   │   │   ├── Login.jsx                    # Email/password + OAuth2 callback
│   │   │   ├── StudentDashboard.jsx         # Student dashboard (stats, activity)
│   │   │   ├── AdviserDashboard.jsx         # Adviser dashboard (assigned students)
│   │   │   ├── CoordinatorDashboard.jsx     # Coordinator dashboard (system overview)
│   │   │   └── AdminDashboard.jsx           # Admin dashboard (user stats, management)
│   │   ├── styles/
│   │   │   ├── Dashboard.css                # Unified dashboard styling
│   │   │   ├── Login.css
│   │   │   ├── UserProfile.css
│   │   │   ├── App.css
│   │   │   └── index.css
│   │   ├── App.js                           # Routes, role-based redirects
│   │   └── index.js
│   ├── public/
│   │   └── index.html
│   └── package.json
│
└── README.md (this file)
```

---

## Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| **Frontend** | React | 18.2 |
| | React Router | 6.8 |
| | CSS (vanilla) | — |
| **Backend** | Spring Boot | 3.2.0 |
| | Java | 21 |
| | Spring Security | (with OAuth2) |
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

#### Install Dependencies
```bash
cd Backend
# Verify Java 17+ and Maven 3.6+
java -version
mvn -version
```

#### Build
```bash
mvn clean package -DskipTests
```

#### Configuration (Optional)
Edit `Backend/src/main/resources/application.properties`:
- `server.port` - Change backend port (default: 8080)
- `app.allowed-email-domains` - Restrict login domains (default: @gmail.com, @university.edu, etc.)
- `jwt.secret` - Change JWT secret in production
- `spring.datasource.url` - Switch to PostgreSQL for production

### 3. Frontend Setup

```bash
cd Frontend
npm install
```

#### Environment Variables (Optional)
Create `.env` file:
```
REACT_APP_BACKEND_URL=http://localhost:8080
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

### Pre-populated Test Users

| Email | Password | Role | Department | Notes |
|-------|----------|------|-----------|-------|
| `student@university.edu` | `password123` | Student | Computer Science | Has adviser assigned (Jane Smith) |
| `adviser@university.edu` | `password123` | Adviser | Computer Science | Has 1 assigned student |
| `coordinator@university.edu` | `password123` | Coordinator | Computer Science | System-wide permissions |
| `admin@university.edu` | `password123` | Administrator | Administration | Full system access |

### Create New Users (Admin Only)
```bash
POST /api/users
Content-Type: application/json
Authorization: Bearer {admin-token}

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@university.edu",
  "password": "securepassword123",
  "role": "student",
  "department": "Computer Science",
  "year": "3"
}
```

### Google OAuth (Students)
- Click "Sign in with Google" on login page
- Use institutional Gmail account (e.g., `@university.edu`)
- System auto-creates account on first login

---

## API Endpoints

### Authentication

```
POST /api/auth/login
- Email/password login
- Request: { email, password }
- Response: { token, refreshToken, user, role }
- Status: 200/401

POST /api/auth/refresh-token?refreshToken={token}
- Refresh access token
- Response: { token }
- Status: 200/401

POST /api/auth/logout
- Logout (frontend clears tokens)
- Status: 200
```

### User Management

```
GET /api/users
- List all users (Admin only, optional role filter)
- Query: ?role=student|adviser|coordinator|administrator
- Response: [ UserDTO, ... ]
- Status: 200/403

POST /api/users
- Create new user (Admin only)
- Request: { firstName, lastName, email, password, role, ... }
- Response: UserDTO
- Status: 201/400/403

GET /api/users/{id}
- Get user by ID (Authenticated)
- Response: UserDTO
- Status: 200/404

PUT /api/users/{id}
- Update full user object (Authenticated)
- Request: User object
- Response: UserDTO
- Status: 200/400/404

GET /api/users/{id}/profile
- Get profile details (Authenticated)
- Response: UserDTO
- Status: 200/404

PUT /api/users/{id}/profile
- Update profile (name, phone, department, year)
- Request: UpdateProfileRequest
- Response: UserDTO
- Status: 200/400/404

POST /api/users/{id}/change-password
- Change user password (Authenticated)
- Request: { currentPassword, newPassword, confirmPassword }
- Response: "Password changed successfully"
- Status: 200/400/401/404

DELETE /api/users/{id}
- Delete user account (Authenticated)
- Status: 200/404

POST /api/users/{id}/avatar
- Upload avatar image (Authenticated, multipart/form-data)
- Request: FormData { file: <image> }
- Response: UserDTO (with avatarUrl)
- Status: 200/400/404

GET /api/users/{id}/avatar
- Serve avatar image file (Public)
- Response: Image file (image/jpeg, image/png, etc.)
- Status: 200/404
```

### Dashboard

```
GET /api/dashboard/student/{id}
- Student dashboard stats and activity (Authenticated)
- Response: { totalDeliverables, completed, pending, overdue, recentActivity }
- Status: 200/404

GET /api/dashboard/adviser/{id}
- Adviser dashboard with assigned students (Authenticated)
- Response: { assignedStudents, assignedCount, activeSubmissions, reviewed, pendingReview }
- Status: 200/404

GET /api/dashboard/coordinator/{id}
- Coordinator system-wide dashboard (Authenticated)
- Response: { totalStudents, totalAdvisers, submissionsPending, recentNotifications }
- Status: 200/404

GET /api/dashboard/admin/{id}
- Admin dashboard with system stats (Authenticated)
- Response: { totalUsers, byRole: { students, advisers, coordinators, administrators }, createdAt }
- Status: 200/404
```

---

## Database Schema

### H2 (Development)
In-memory database, recreated on each server restart.

### User Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL,  -- student, adviser, coordinator, administrator
  student_id VARCHAR(50),
  department VARCHAR(255),
  user_year VARCHAR(20),
  phone VARCHAR(20),
  avatar_filename VARCHAR(255),
  advisor_id BIGINT,  -- Foreign key to User.id for student's adviser
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP
);

CREATE TABLE tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(500) UNIQUE,
  token_type VARCHAR(50),
  expiry_date TIMESTAMP,
  created_at TIMESTAMP
);
```

### Relationships
- **Student → Adviser**: `users.advisor_id` references `users.id`
- **Token → User**: One-to-many (user can have multiple active tokens)

---

## Features - Module 1: User Management System

### ✅ Implemented

1. **Secure Authentication**
   - Email/password login with BCrypt password hashing
   - Google OAuth2 integration for students
   - JWT access tokens (24-hour expiry) + refresh tokens
   - Email domain whitelist validation
   - Secure session management

2. **Role-Based Access Control (RBAC)**
   - 4 roles: Student, Adviser, Coordinator, Administrator
   - Role-based route protection (frontend)
   - Endpoint authorization (backend)
   - Automatic role detection during OAuth2 login

3. **User Profile Management**
   - View personal information (name, email, phone, department, year)
   - Edit profile fields
   - Change password securely
   - Upload/serve avatar images
   - Real-time profile updates

4. **Personalized Dashboards**
   - **Student**: Deliverables stats, recent activity, profile management
   - **Adviser**: Assigned students list, submission stats, profile management
   - **Coordinator**: System-wide metrics, notifications, profile management
   - **Admin**: User statistics by role, system settings access, user management

5. **User Management (Admin)**
   - List all users with optional role filtering
   - Create new users
   - Edit user details
   - Delete user accounts
   - Avatar upload/management

6. **Security Features**
   - CORS properly configured for frontend-backend communication
   - CSRF protection disabled (JWT-based auth)
   - Password validation and hashing
   - Token expiration and refresh mechanism
   - Secure avatar file handling

---

## Architecture Overview

### Frontend Architecture
```
App.js (Routes + Role-based redirect)
├── AuthContext (Global state)
├── PrivateRoute (Role protection)
└── Pages
    ├── Login.jsx (Email/OAuth)
    ├── StudentDashboard.jsx (Stats + Profile)
    ├── AdviserDashboard.jsx (Students + Stats)
    ├── CoordinatorDashboard.jsx (System view)
    └── AdminDashboard.jsx (User management)
```

### Backend Architecture
```
IntelliTrackApplication.java
├── SecurityConfig (Auth, CORS, OAuth2)
├── CorsConfig (CORS headers)
├── DataInitializer (Test data)
├── Controllers
│   ├── AuthController (Login, logout, refresh)
│   ├── UserController (CRUD, profile, avatar)
│   └── DashboardController (Role dashboards)
├── Services
│   ├── AuthService (JWT, OAuth, domain check)
│   ├── UserService (Profile, password, avatar)
│   └── CustomOAuth2UserService (Google OAuth)
├── Repositories
│   └── UserRepository (Database queries)
└── Entities & DTOs
    ├── User (Model)
    ├── UserDTO (Response DTO)
    └── Various request DTOs
```

---

## For Module 2 & 3 Developers

### What's Ready for You

1. **Authentication System**
   - ✅ Users can login and get JWT tokens
   - ✅ Role-based routing works
   - ✅ Profile management is functional
   - Use `useAuth()` hook in React or `Authorization: Bearer {token}` header in API calls

2. **Database**
   - ✅ User table exists with role, advisor_id, avatar_filename
   - ✅ H2 in-memory for dev (no setup needed)
   - 🔄 Switch to PostgreSQL for production (update `application.properties`)

3. **API Framework**
   - ✅ REST endpoints follow consistent pattern
   - ✅ Error handling in place
   - ✅ CORS configured
   - Extend controllers following existing patterns

4. **Frontend Routing**
   - ✅ PrivateRoute component protects pages
   - ✅ Dashboard pages structured for each role
   - ✅ AuthContext provides global auth state
   - Add new pages in `Frontend/src/pages/`

### Module 2 Requirements (Submission Tracker)

**Suggested Entities:**
```java
// Submission.java
- id (PK)
- studentId (FK → User.id)
- assignmentId (FK → Assignment)
- submissionDate
- status (pending, submitted, reviewed, approved)
- feedbackText
- score
- createdAt
- updatedAt

// Assignment.java
- id (PK)
- title
- description
- dueDate
- coordinatorId (FK → User.id)
- createdAt
```

**Suggested Endpoints:**
```
POST /api/submissions                   # Student submits assignment
GET /api/submissions?studentId={id}     # Student's submissions
GET /api/submissions?status=pending     # Adviser/Coordinator reviews
PUT /api/submissions/{id}/feedback      # Adviser provides feedback
GET /api/assignments                    # List active assignments
```

**Frontend Tasks:**
- Create `Submissions.jsx` page for students to upload/track
- Create `SubmissionsReview.jsx` for advisers to review
- Add submission list/stats to dashboards

### Module 3 Requirements (Analytics & Reporting)

**Suggested Endpoints:**
```
GET /api/analytics/completion-rate           # System-wide
GET /api/analytics/student/{id}/progress     # Student progress
GET /api/analytics/adviser/{id}/students     # Adviser's students' stats
GET /api/reports/generate?type=pdf&role=admin
```

**Frontend Tasks:**
- Create analytics charts (Chart.js, Recharts)
- Add reporting UI for coordinators/admins
- Real-time submission progress tracking

### Key Code Patterns to Follow

**Backend - Adding a new endpoint:**
```java
// 1. Entity
@Entity
public class YourEntity { ... }

// 2. Repository
@Repository
public interface YourRepository extends JpaRepository<YourEntity, Long> {
    List<YourEntity> findByUserId(Long userId);
}

// 3. Service
@Service
public class YourService {
    public List<YourEntity> getUserEntities(Long userId) {
        return repository.findByUserId(userId);
    }
}

// 4. Controller
@RestController
@RequestMapping("/api/your-entity")
public class YourController {
    @GetMapping("/{userId}")
    public ResponseEntity<List<YourDTO>> get(@PathVariable Long userId) {
        // Use service, return DTO
    }
}
```

**Frontend - Calling backend:**
```javascript
const { user, token } = useAuth();

// Make authenticated request
fetch(`http://localhost:8080/api/your-endpoint`, {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
  .then(res => res.json())
  .then(data => { /* use data */ })
  .catch(err => console.error(err));
```

---

## Code Standards & Conventions

### Backend

- **Package Structure**: `com.intellitrack.{entity,dto,service,repository,controller,config,security}`
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **Annotations**: Use `@Service`, `@Repository`, `@RestController`, `@RequestMapping`, `@GetMapping`, etc.
- **Error Handling**: Return appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 500)
- **DTOs**: Use for API responses; never expose entities directly
- **Validation**: Use `@Valid` + `@NotBlank`, `@Size`, etc.
- **CORS**: All public endpoints already have `@CrossOrigin` or use global CORS config

### Frontend

- **Structure**: Components in `components/`, pages in `pages/`, styles in `styles/`
- **Naming**: PascalCase for components, camelCase for variables/functions
- **State Management**: Use React Context for global state (auth), `useState` for local
- **Routing**: Use React Router v6 with `Routes`, `Route`, `Navigate`
- **No TypeScript**: Vanilla JSX only
- **No Tailwind**: Use vanilla CSS only
- **Authentication**: Use `useAuth()` hook to access `{ user, token, login, logout }`

### Database Naming

- **Tables**: snake_case, plural (e.g., `users`, `submissions`, `assignments`)
- **Columns**: snake_case (e.g., `first_name`, `created_at`, `advisor_id`)
- **Primary Keys**: `id` (always)
- **Foreign Keys**: `{entity}_id` (e.g., `user_id`, `advisor_id`)

---

## Troubleshooting

### Backend Issues

**Problem**: `mvn command not found`
```bash
# Ensure Maven is installed
mvn -version
# If not, install from https://maven.apache.org/download.cgi
```

**Problem**: Java version mismatch
```bash
java -version
# Should be 17 or higher; update if needed
```

**Problem**: Port 8080 already in use
```bash
# Change port in application.properties
server.port=8081
```

**Problem**: H2 console not accessible
- Visit `http://localhost:8080/h2-console`
- Driver: `org.h2.Driver`
- URL: `jdbc:h2:mem:intellitrack`
- User: `sa`, Password: (leave blank)

### Frontend Issues

**Problem**: `npm command not found`
```bash
# Install Node.js from https://nodejs.org/
node -v
npm -v
```

**Problem**: CORS errors
- Ensure backend is running on `http://localhost:8080`
- Check `SecurityConfig` CORS settings
- Clear browser cache and cookies

**Problem**: Blank page or white screen
```bash
# Check browser console for errors (F12 → Console)
# Check frontend is running on http://localhost:3000
npm start  # Restart if needed
```

**Problem**: Tokens not persisting
- Check `LocalStorage` in DevTools (F12 → Application)
- Verify `AuthContext` is wrapping App component in `index.js`

### Integration Issues

**Problem**: Login works but dashboard doesn't load
```bash
# 1. Check token is in localStorage
# 2. Verify role is being set correctly
# 3. Check PrivateRoute component
# 4. Check console errors (F12 → Console)
```

**Problem**: API returns 401/403
- Token may be expired; need to refresh
- User may not have permission for that role
- Check Authorization header is sent correctly

---

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OAuth2 Flow](https://datatracker.ietf.org/doc/html/rfc6749)
- [REST API Design Guide](https://restfulapi.net/)

---

## Contact & Support

For questions about Module 1 implementation or architecture, refer to:
- Backend Module Docs: `Backend/Module1_UserManagementSystem.md`
- Code comments throughout the codebase
- This README

---

**Last Updated**: April 23, 2026 | **Version**: 1.0