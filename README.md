# IntelliTrack

**Capstone Submission Tracking System**

IntelliTrack is an academic management system for organizing capstone classlists, groups, requirements, deadlines, and submissions. It provides role-specific workspaces for coordinators, advisers, students, and administrators.

| Service | URL |
| --- | --- |
| Web application | [intellitrack-pied.vercel.app](https://intellitrack-pied.vercel.app) |
| API | [intellitrack-backend-smaj.onrender.com](https://intellitrack-backend-smaj.onrender.com) |

## Features

- **Academic setup:** organize students by subject, academic year, semester, section, and group code.
- **Classlist imports:** import Excel workbooks or Google Sheets using shared validation.
- **Submission tracking:** assign requirements and deadlines, accept project links, and retain submission history.
- **Role-based workspaces:** coordinators oversee academic progress, advisers monitor assigned groups, students track their work, and administrators manage accounts and system configuration.
- **Calendar and reporting:** view applicable deadlines and use supported Google Calendar and Sheets integrations.
- **AI summaries:** summarize submission tracking data without reading submitted document contents.

Submission deadlines and displayed times use Asia/Manila.

## Technology

| Area | Technology |
| --- | --- |
| Frontend | React 18, React Router, Recharts |
| Backend | Java 21, Spring Boot 3.2, Spring Data JPA, Spring Security |
| Database | MySQL for local development; PostgreSQL for production |
| Google integrations | OAuth 2.0, Google Calendar, Google Drive, Google Sheets |
| AI summaries | Google Gemini |

## Run locally

### Requirements

- Java 21
- Maven 3.8 or later
- Node.js 18 or later and npm
- MySQL 8 or a configured PostgreSQL database

### 1. Get the source

```bash
git clone https://github.com/juswangs12/Intellitrack-2.0.git
cd Intellitrack-2.0
```

### 2. Configure and start the backend

The local default profile is `mysql`, with a database at `localhost:3307/intellitrack5`. Create that database in MySQL, or set `MYSQL_URL`, `MYSQL_USER`, and `MYSQL_PASSWORD` in `Backend/.env` to match your local setup. For example:

```dotenv
MYSQL_URL=jdbc:mysql://localhost:3307/intellitrack5?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
MYSQL_USER=root
MYSQL_PASSWORD=your-local-password
```

From the repository root, start the API:

```bash
cd Backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080` by default. Google sign-in, Workspace features, and AI summaries need their respective provider credentials. Configure optional values such as `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `AI_API_KEY` locally; never commit real credentials.

### 3. Configure and start the frontend

In a second terminal:

```bash
cd Frontend
npm ci
npm start
```

The app opens at `http://localhost:3000` and uses `http://localhost:8080` as its default API origin. To use another API origin, set `REACT_APP_API_BASE_URL` in `Frontend/.env.local`.

## Roles

| Role | Main responsibilities |
| --- | --- |
| Coordinator | Import classlists; manage academic setup, sections, groups, advisers, requirements, deadlines, analytics, and reports. |
| Adviser | Review assigned groups, student submissions, and progress. |
| Student | View assigned requirements and deadlines, submit project links, and review submission history. |
| Administrator | Manage users and inspect system configuration. |

## Production configuration

The hosted application uses Vercel for the frontend, Render for the API, and PostgreSQL for application data. Configure the frontend API origin and backend CORS and frontend origins to match the deployments. Set production database, JWT, Google, AI, email, and storage secrets through the hosting provider's environment settings.

Do not connect local development to the production database or enable local automation against production data. No sample login credentials are provided; user accounts are managed through the application.

## Project structure

```text
Backend/     Spring Boot API, persistence, integrations, and backend tests
Frontend/    React application, role workspaces, and frontend tests
docs/        Production release, QA, automation, and implementation notes
```

## Development checks

Run frontend tests from `Frontend` with `npm test`. Run backend tests from `Backend` with `mvn test`.

## Documentation

- [Production release guide](docs/PRODUCTION_RELEASE.md)
- [Production QA plan](docs/PRODUCTION_QA_PLAN.md)
- [Deadline automation](docs/DEADLINE_AUTOMATION.md)
- [Implementation handoff](docs/TRACKING_HANDOFF.md)
- [Backend setup](Backend/README.md)
