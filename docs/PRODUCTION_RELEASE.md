# Production release preparation — 24 October 2026

These settings were reviewed from the supplied screenshots and repository. No dashboard settings have been changed and no push or deployment has been performed.

## Release blockers

1. Rotate the credentials visible in the shared environment screenshot: AI API key, storage access credentials, Google OAuth client secret, JWT signing secret and database password. Update dependent services together. Rotating JWT invalidates existing sign-ins. Do not commit secrets or paste replacement values into screenshots.
2. The selected production branch is `revised` (GitHub default). Render already tracks it. Change Vercel production tracking from `main` to `revised`. Local development is on `codex/submission-tracker`; deliver the reviewed release to `revised`. Pushing the development branch alone does not update production.
3. Back up the production PostgreSQL database before the first release. The production profile currently uses Hibernate schema update; local MySQL tests do not verify the production PostgreSQL upgrade. Rehearse against a staging database copy. A code rollback does not undo schema changes.
4. `Frontend/.env` is already tracked by Git. Remove it from the index while retaining the local file (`git rm --cached Frontend/.env`) as part of the reviewed release commit. Ignoring it does not untrack it. Use Vercel environment settings for production; audit previous commits if that file ever contained secrets.

## Vercel

- Keep root directory `Frontend`.
- Repository `vercel.json` now declares `npm ci`, `npm run build`, output `build`, and serves real files before SPA route fallback (including the logo).
- Set production `REACT_APP_API_BASE_URL=https://intellitrack-backend-smaj.onrender.com` with no `/api` suffix. Confirm this hostname against the live Render service before saving.
- Redeploy after changing frontend environment variables; they are embedded during build.
- Production frontend shown: `https://intellitrack-pied.vercel.app`.
- Align the production branch with the chosen backend release branch. The screenshot's framework preset Other is supported by these explicit build settings.

## Render

- Keep the existing service; do not create a duplicate service from the blueprint. `render.yaml` is a reference for future infrastructure setup, not a change to the existing dashboard.
- Existing screenshot: root directory `Backend`, Dockerfile `Backend/Dockerfile` displayed relative to repository. Verify the effective Docker build context is `Backend`, containing `pom.xml` and `src`; the Dockerfile's COPY instructions depend on that context.
- The reference blueprint omits rootDir and explicitly uses Dockerfile `./Backend/Dockerfile` and context `./Backend`.
- Profile `prod`, health endpoint `/actuator/health/readiness`, existing PostgreSQL credentials and small connection pool.
- `APP_FRONTEND_BASE_URL=https://intellitrack-pied.vercel.app`
- `APP_CORS_ALLOWED_ORIGINS=https://intellitrack-pied.vercel.app`
- `GOOGLE_REDIRECT_URI=https://intellitrack-backend-smaj.onrender.com/login/oauth2/code/google`
- `GOOGLE_WORKSPACE_REDIRECT_URI=https://intellitrack-backend-smaj.onrender.com/api/google/callback`
- Register BOTH production redirect URIs in the same Google OAuth client. Keep localhost redirect URIs for local testing.
- `GOOGLE_TOKEN_ENCRYPTION_KEY`: base64 encoding of 32 cryptographically random bytes. Keep this stable; existing stored Google connections require the key that encrypted them. A different production database needs its own Google connections. Do not copy token records without their key.
- `GOOGLE_DEADLINE_SENDER_EMAIL=intelli.track0@gmail.com`
- Optional `GOOGLE_REPORTS_OWNER_EMAIL` only if enforcing a specific report owner.
- Start with `DEADLINE_AUTOMATION_ENABLED=false` during deployment validation.
- Set a strong `DEADLINE_AUTOMATION_TRIGGER_SECRET` (at least 32 random characters). Configure the protected external POST scheduler before enabling automation; see DEADLINE_AUTOMATION.md. Local automation stays off.

The workspace callback, encryption key, sender and automation variables were not visible in the supplied environment screenshot. Verify their presence in the actual service; the screenshot may not show the entire list. A Gmail connection against the local MySQL database does not authorize the separate production PostgreSQL database: connect the sender again in the production admin screen.

## Release sequence

1. Rotate exposed credentials, back up data, align branches and verify deployment settings.
2. Review the full working tree. Keep `.env`, credentials, build artifacts and test logs out of the release. Many changes predate this UI update.
3. Run backend tests, frontend tests and a CI-mode production build. Rehearse the PostgreSQL upgrade against staging.
4. Push the reviewed release commit to the agreed deployment branch; deploy backend first and verify health, then frontend.
5. Verify login/role access, imports, existing groups, submissions/history and the new logo on a hard refresh and direct nested URL.
6. Connect production Gmail and Calendar, verify exports, then test automation with controlled accounts. Existing overdue work can generate reminders when enabled. Check worker queue status and Google delivery before enabling for all users.
7. Keep the previous deployment available for application rollback; restore data only under a deliberate database recovery plan.

References: https://vercel.com/docs/project-configuration/vercel-json and https://render.com/docs/blueprint-spec

## Current deployment: new Supabase database

Production now uses a new Supabase database with an account already created. Preserve that account and any new QA data. **Do not run `reset-test-submissions-postgres.sql` on this database.** That script was prepared only for the discarded legacy test database; it is not an automatic migration.

Verify Render points to the new database, let the backend initialize its schema, and check startup logs. Apply `secure-public-tables-postgres.sql` after tables exist if this database is accessed only through the privileged Spring JDBC connection. Reconnect Google in the new production database after verifying Render's OAuth and encryption settings. A fresh database does not resolve missing backend environment variables.

## Legacy database repair reference (not for the new database)

The old production schema lacked `submissions.student_id`. The approved reset is limited to test submissions and their dependent records; it does not replace the database or delete accounts/enrollments.

1. Only if explicitly returning to the old disposable test database: stop the backend before running `Backend/scripts/reset-test-submissions-postgres.sql` in Supabase SQL Editor as postgres. It deletes all submissions and FK-dependent rows, then creates the required student ownership column. Skip this step for the new database.
2. Run `Backend/scripts/secure-public-tables-postgres.sql` as postgres to enable RLS and block browser Data API roles on application tables. This deployment uses privileged JDBC postgres access. If any other application uses these tables via Supabase Data API, review its access needs first. Run this protection script again after the backend creates new tables.
3. Restart/deploy the backend. Verify startup completes without schema errors, then sign out and sign in using a production account. Local MySQL passwords and Google connection records are separate from production.
4. Open Administration > System configuration. The Google setup panel now lists missing/invalid configuration by name without exposing values. Verify `GOOGLE_WORKSPACE_REDIRECT_URI=https://intellitrack-backend-smaj.onrender.com/api/google/callback` and a Base64-encoded 32-byte `GOOGLE_TOKEN_ENCRYPTION_KEY`. Keep a valid existing key stable. Save Render settings and restart before reconnecting Gmail.
5. Confirm tracking loads and Gmail connects before enabling automatic email delivery. A 401 from an expired session is different from a schema 500 or configuration 503; if fresh sign-in still fails, inspect the matching backend exception rather than disabling authorization.

## Classlist sources

Local `.xlsx` / `.xls` upload remains available. Google Sheets import requires Google Sheets API enabled in the OAuth project's Google Cloud console, the importing account allowed as an OAuth test user while the app is in Testing, and one connection from Classlist Import > Google Sheets. It requests `spreadsheets.readonly`, separately from calendar, reports and Gmail permissions. Paste the private spreadsheet URL; the connected account must have read access. Enter a worksheet name or leave it blank for the first worksheet. Imports read columns A-G with headers and at most 5,000 student rows, using the same enrollment validation as local uploads. Excel files stored in Drive must first be saved as Google Sheets, or downloaded and uploaded locally.

These scripts and local tests do not verify the live database or Google consent. Complete the production smoke checks after applying the settings and deployment.
