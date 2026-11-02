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

Use Classlist Import > Attach classlist > Attach cloud files or Upload from this device. Local `.xlsx` / `.xls` uploads remain available. Cloud browsing requires **Google Drive API and Google Sheets API** enabled in the OAuth project's Google Cloud console, with the importing account allowed as an OAuth test user while the app is in Testing. Reconnect imports once to grant `drive.metadata.readonly` and `spreadsheets.readonly`; existing Sheets-only consent cannot browse Drive. Calendar, reports, and Gmail connections remain separate. Select a private Google spreadsheet, then enter a worksheet name or leave it blank for the first worksheet. Imports read columns A-G with headers and at most 5,000 student rows, using the same enrollment validation as local uploads. Excel files stored in Drive must first be saved as Google Sheets, or downloaded and uploaded locally. Google Docs documents are not classlist workbooks.

## November QA changes awaiting deployment

- Deploy frontend and backend together: `/student/login` and `/staff/login` send a portal choice which the backend verifies against the saved account role. The default `/` is student sign-in. Pre-create staff Google accounts with the correct role. Unknown Google accounts cannot enter through staff sign-in.
- Approved course pairs are IT332 / Capstone 1 and IT411 / Capstone 2. New imports and subject creation reject other codes and mismatched names. IT332 is normally semester 2; IT411 is normally semester 1. School years remain explicit; no automatic promotion occurs. G01/G02 retain their leading zeros and stay separate from G1/G2. A course/term versus section-name mismatch produces an import warning rather than changing the identifier.
- Existing IT322 records are not deleted or silently reassigned. Review their enrollments and submission history separately before correcting production data.
- The outbox adds a nullable `deadline_emails.group_id` column for adviser assignment notices. Confirm schema update succeeds during backend startup; with externally managed migrations, add this column as BIGINT before starting this release. Existing email history is retained.
- Gmail authorization does not enable sending. After verifying the sender in production, set `DEADLINE_AUTOMATION_ENABLED=true` on Render, redeploy, and verify the protected scheduler. Keep it false locally when using the production database.
- Staff digests are queued once per account per Philippine calendar day, at the first worker run after 08:00. They contain current scoped counts and the previous day's submission/replacement count. Advisers receive only their permitted group scope. New adviser assignments generate a separate notice. No sign-in or analytics-refresh email is generated.
- Run the new regression cases in `docs/PRODUCTION_QA_PLAN.md` after deployment. Automated tests do not establish live Google consent, delivery, or scheduler operation.

### Calendar and analytics follow-up

Production calendar automation is now independent of email delivery. `CALENDAR_AUTOMATION_ENABLED` defaults to true under the production profile and false under local MySQL. Keep it false locally if connected to production data. Users still authorize Calendar once; the worker reconciles connected calendars every few minutes without pressing Sync. Render Free still needs the protected external scheduler calling `POST /api/automation/deadlines/run` with `X-Automation-Key` to process while asleep. No scheduler has been provisioned by this change.

Distinct deliverable IDs retain separate Google event IDs even when their timestamps match. The in-app monthly calendar shows every deadline in its day cell, labelled by subject; selecting the day shows full details. If a production requirement is absent after syncing, compare the student's assigned tracking rows and active term before assuming a Calendar event collision.

Analytics defaults to one subject and provides persistent subject and group/individual controls. Group drill-down carries the subject, term, group and requirement filters into the individual view. Google exports still reuse one workbook per subject and term; each section/filter snapshot updates independently, while Groups and Summary retain labelled scopes. Exporting mixed subject/term data is rejected.

These scripts and local tests do not verify the live database or Google consent. Complete the production smoke checks after applying the settings and deployment.

Drive browsing reference: [Google Drive files.list](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/list).
