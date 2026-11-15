# IntelliTrack API

The backend provides the REST API, authentication and authorization, academic data management, submission tracking, and integrations used by the React application.

## Requirements

- Java 21
- Maven 3.8 or later
- MySQL for the default local profile, or a configured PostgreSQL database

## Start locally

From this directory, run:

```powershell
mvn spring-boot:run
```

The default profile is `mysql`; the default JDBC URL is `jdbc:mysql://localhost:3307/intellitrack5`. Configure `MYSQL_URL`, `MYSQL_USER`, and `MYSQL_PASSWORD` for your local database. The API listens on port 8080 unless `PORT` or `SERVER_PORT` is set.

Set `SPRING_PROFILES_ACTIVE=prod` and configure the PostgreSQL connection variables to use the production database profile. Do not point local development at production unless specifically intended; keep local and production automation isolated.

## Optional integrations

Configure credentials through environment variables or a local `.env` file. Common variables include:

- `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` for Google sign-in and user-authorized Workspace connections.
- `GOOGLE_WORKSPACE_REDIRECT_URI` for the Workspace OAuth callback.
- `GOOGLE_TOKEN_ENCRYPTION_KEY` for encrypting stored Google tokens.
- `AI_API_KEY` for Gemini summaries.
- `JWT_SECRET` for production token signing.
- `APP_CORS_ALLOWED_ORIGINS` and `APP_FRONTEND_BASE_URL` for the frontend origin.

Never commit `.env` files, OAuth secrets, database passwords, signing keys, or service-account credentials. Production values belong in the hosting provider's secret settings.

## Verification

Run backend tests with:

```powershell
mvn test
```

See the [repository README](../README.md) for the application overview and [production documentation](../docs/PRODUCTION_RELEASE.md) for deployment configuration.
