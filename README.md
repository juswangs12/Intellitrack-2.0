# IntelliTrack

**Capstone Submission Tracking System**

IntelliTrack helps academic coordinators manage capstone classlists, groups, requirements, and deadlines. Students submit project links and track their submission status, while advisers monitor the groups assigned to them. Administrators manage user access and system configuration.

## Core workflows

- Import classlists from Excel or Google Sheets and organize students by subject, term, section, and group code.
- Assign requirements and deadlines, then track on-time, late, overdue, and awaiting submissions.
- Review submission history and group progress according to each user's role.
- Connect Google services for supported sign-in, Calendar, spreadsheet reporting, and email workflows.
- Generate AI summaries of tracking data. IntelliTrack does not read submitted document contents for these summaries.

## Technology

| Area | Technology |
| --- | --- |
| Frontend | React 18, React Router, Recharts |
| Backend | Java 21, Spring Boot 3, Spring Data JPA, Spring Security |
| Database | MySQL for local development; PostgreSQL for production |
| Integrations | Google OAuth and Workspace APIs; Gemini summaries |

## Run locally

### Prerequisites

- Java 21
- Maven 3.8 or later
- Node.js 18 or later and npm
- A local MySQL database, unless configuring another supported database profile

### Backend

1. Create a local database and configure the backend connection. The MySQL development profile defaults to `localhost:3307/intellitrack5`; override it with `MYSQL_URL`, `MYSQL_USER`, and `MYSQL_PASSWORD` when your local database differs.
2. From the repository root, start the API:

   ```powershell
   cd Backend
   mvn spring-boot:run
   ```

   The API listens on `http://localhost:8080` by default. Optional integrations require their own credentials, such as `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `AI_API_KEY`. Keep credentials in a local environment file or environment variables; never commit them.

### Frontend

In a second terminal:

```powershell
cd Frontend
npm ci
npm start
```

The web app opens at `http://localhost:3000` and uses `http://localhost:8080` as its default API origin. Set `REACT_APP_API_BASE_URL` in `Frontend/.env.local` when the backend runs elsewhere.

## Configuration and deployment

Backend configuration is provided through Spring properties and environment variables. Local MySQL and production PostgreSQL use separate Spring profiles. Google, AI, email, storage, database, and signing credentials must be configured for the environment where each feature is used.

The current hosted setup uses Vercel for the React frontend, Render for the Spring Boot API, and PostgreSQL for application data. Set the frontend API origin and backend CORS/frontend origins to the matching deployment URLs. Store production secrets in the hosting provider's environment settings, not in source files.

See [Production release notes](docs/PRODUCTION_RELEASE.md), [Production QA plan](docs/PRODUCTION_QA_PLAN.md), and [deadline automation setup](docs/DEADLINE_AUTOMATION.md) for operational details. The [tracking handoff](docs/TRACKING_HANDOFF.md) records system behavior and implementation context.

## Project layout

```text
Backend/     Spring Boot API, persistence, integrations, and backend tests
Frontend/    React application and frontend tests
docs/        Maintained release, QA, automation, and implementation documentation
```

## Development checks

Run frontend tests from `Frontend` with `npm test`. Run backend tests from `Backend` with `mvn test`. These checks require the corresponding dependencies to be installed.
