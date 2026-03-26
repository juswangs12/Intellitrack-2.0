# IntelliTrack

A web-based capstone deliverables submission tracking and analytics system.

## Project Structure

```
IntelliTrack-2.0/
├── Frontend/          # React application
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   │   └── UserProfile.jsx
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   └── StudentDashboard.jsx
│   │   ├── styles/    # All CSS files consolidated here
│   │   │   ├── index.css
│   │   │   ├── App.css
│   │   │   ├── Login.css
│   │   │   ├── StudentDashboard.css
│   │   │   └── UserProfile.css
│   │   ├── services/
│   │   ├── App.js
│   │   └── index.js
│   └── package.json
├── Backend/           # Spring Boot application
│   ├── src/main/java/com/intellitrack/
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   └── UserController.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   └── UserService.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   ├── entity/
│   │   │   └── User.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   └── LoginResponse.java
│   │   ├── config/
│   │   │   └── DataInitializer.java
│   │   └── IntelliTrackApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
└── README.md
```

## Tech Stack

- **Frontend**: React (JSX + CSS), React Router
- **Backend**: Spring Boot Java
- **Database**: Supabase PostgreSQL
- **API**: RESTful

## Setup Instructions

### Prerequisites
- Node.js 16+ and npm
- Java 17+
- Maven 3.6+

### Frontend Setup
1. Navigate to `Frontend/` directory
2. Run `npm install`
3. Run `npm start` to start the development server

### Backend Setup
1. Navigate to `Backend/` directory
2. Ensure Java 17+ and Maven are installed
3. Run `mvn spring-boot:run` to start the server

**Note**: The backend now uses H2 in-memory database for development (no external database setup required).

## Test Accounts

The application includes test accounts for development:

| Email | Password | Role |
|-------|----------|------|
| student@university.edu | password123 | Student |
| adviser@university.edu | password123 | Adviser |
| coordinator@university.edu | password123 | Coordinator |
| admin@university.edu | password123 | Administrator |

## Features Implemented

### User Management System (Module 1)
- ✅ Secure login with role-based authentication
- ✅ Role-based dashboard routing
- ✅ User profile management with edit functionality
- ✅ Student-specific academic information fields
- ✅ Responsive dashboard with sidebar navigation
- ✅ Basic analytics overview cards
- ✅ Recent activity feed

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout

### User Management
- `GET /api/users/{id}` - Get user profile
- `PUT /api/users/{id}` - Update user profile

## Testing the Application

1. **Start the Backend**:
   ```
   cd Backend
   mvn spring-boot:run
   ```

2. **Start the Frontend**:
   ```
   cd Frontend
   npm start
   ```

3. **Test Login**:
   - Open `http://localhost:3000`
   - Login with any test account (e.g., student@university.edu / password123)
   - Should redirect to student dashboard

4. **Test Profile Management**:
   - Click "Profile" in sidebar
   - Click "Edit Profile" to modify information
   - Save changes and verify updates

## Roles

- Student
- Adviser
- Coordinator
- Administrator

## Core Modules

1. ✅ User Management System (Login, Profile, Dashboard)
2. 🔄 Submission Tracker (Next)
3. 🔄 Analytics & Reporting Dashboard

## Development Rules

- Follow existing file structure and naming conventions
- Use React JSX + CSS only (no Tailwind, no TypeScript)
- Use Spring Boot layered architecture (Controller/Service/Repository/Entity)
- Connect backend to Supabase PostgreSQL
- Implement role-based access control
- Keep code simple, modular, and readable