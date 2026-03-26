# IntelliTrack Backend Setup Guide

## Environment Configuration

This project uses environment variables for secure configuration management. The `application.properties` file contains all configuration with environment variable fallbacks, while the `.env` file contains only sensitive secrets.

### Quick Start

1. **Copy the environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Configure your secrets in `.env`:**
   ```bash
   # Required
   GOOGLE_CLIENT_ID=your-actual-google-client-id
   GOOGLE_CLIENT_SECRET=your-actual-google-client-secret
   JWT_SECRET=your-very-secure-random-jwt-secret

   # Optional (leave empty for defaults)
   SPRING_DATASOURCE_USERNAME=your-db-username
   SPRING_DATASOURCE_PASSWORD=your-db-password
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## Configuration Structure

### application.properties
- Contains **all configuration** with environment variable references
- Provides **sensible defaults** for development
- Uses `${VARIABLE_NAME:default_value}` syntax

### .env File
- Contains **only sensitive secrets and keys**
- Should **never be committed** to version control
- Loaded automatically by `EnvironmentConfig.java`

## Environment Variables Reference

### 🔐 Required Secrets
- `GOOGLE_CLIENT_ID` - Google OAuth2 Client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth2 Client Secret
- `JWT_SECRET` - JWT signing secret (256-bit minimum)

### 🔧 Optional Configuration
- `SERVER_PORT` - Server port (default: 8080)
- `SPRING_DATASOURCE_*` - Database configuration
- `SPRING_JPA_*` - JPA/Hibernate settings
- `LOGGING_LEVEL_*` - Logging configuration
- `APP_CORS_*` - CORS settings

### 📧 External Services (Future Use)
- `SPRING_MAIL_*` - Email configuration
- `AWS_*` - AWS S3 configuration

## Getting Google OAuth Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create/select a project → APIs & Services → Credentials
3. Click "+ CREATE CREDENTIALS" → "OAuth Client ID"
4. Configure OAuth consent screen if prompted
5. Application type: **Web application**
6. Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
7. Copy Client ID and Client Secret to your `.env` file

## Security Best Practices

- ✅ **Never commit `.env` files** (already in `.gitignore`)
- ✅ **Use strong random secrets** in production
- ✅ **Rotate credentials** regularly
- ✅ **Different credentials** per environment
- ✅ **Environment-specific `.env` files** (`.env.production`, `.env.staging`)

## Development vs Production

### Development (Default)
- H2 in-memory database
- Debug logging enabled
- H2 console accessible
- All defaults optimized for development

### Production
Set these environment variables:
```bash
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:postgresql://your-prod-db:5432/db
SPRING_H2_CONSOLE_ENABLED=false
LOGGING_LEVEL_ROOT=WARN
```

## Troubleshooting

### "invalid_client" Error
- Verify `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are correct
- Check OAuth credentials are for the right Google Cloud project
- Ensure redirect URI matches exactly: `http://localhost:8080/login/oauth2/code/google`

### Database Connection Issues
- Check `SPRING_DATASOURCE_*` variables
- Verify database server is running
- Ensure correct JDBC URL format

### Port Conflicts
- Change `SERVER_PORT` if 8080 is in use
- Check for other applications using the port

### Environment Variables Not Loading
- Ensure `.env` file exists in the backend root directory
- Check that `EnvironmentConfig.java` is loading correctly
- Verify variable names match exactly (case-sensitive)

## File Structure
```
Backend/
├── .env                    # Secrets (gitignored)
├── .env.example           # Template (committed)
├── .gitignore            # Excludes .env files
├── application.properties # Configuration with env vars
├── README.md             # This file
└── src/main/java/com/intellitrack/config/
    └── EnvironmentConfig.java  # Loads .env files
```

#### Development Settings
- `SPRING_H2_CONSOLE_ENABLED`: Enable H2 console (default: true)
- `SPRING_JPA_SHOW_SQL`: Show SQL queries (default: true)

### Getting Google OAuth Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create/select a project
3. Enable Google+ API
4. Create OAuth 2.0 Client ID
5. Configure OAuth consent screen
6. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`

### Security Best Practices

- ✅ Never commit `.env` files to version control
- ✅ Use strong, random secrets in production
- ✅ Rotate credentials regularly
- ✅ Use different credentials for different environments
- ✅ Store production secrets securely (e.g., AWS Secrets Manager, Azure Key Vault)

### Troubleshooting

**"invalid_client" error:**
- Check that GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are correct
- Verify the OAuth credentials are for the right Google Cloud project
- Ensure the redirect URI matches exactly

**Database connection issues:**
- Verify SPRING_DATASOURCE_URL format
- Check database credentials
- Ensure database server is running

**Port conflicts:**
- Change SERVER_PORT if 8080 is in use
- Check for other applications using the port