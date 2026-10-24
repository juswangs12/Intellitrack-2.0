# IntelliTrack submission tracker implementation

This branch changes the active student, adviser and coordinator workflow to individual Google Docs submissions. It has not been deployed and has not changed Supabase data or connected anyone's Google account.

## Architecture and rules

- React routes use `TrackingDashboard`, `SubmissionLink`, and `AcademicSetup`. The older grading/comment/review pages remain in source for reference but are not used by these routes. Comment and old upload/AI endpoints are denied.
- `TrackingService` is the shared source for dashboard rows, counts, AI inputs and Sheets exports. Rows represent an enrolled student × active requirement, including students who never submitted. Unlinked imported students remain visible to staff.
- Section, group and requirement share a subject and academic term (`2026-2027 / 1`). Cross-section groups are allowed within that scope. Carry-forward creates separate enrollment/group records and preserves the old team's records.
- Every student submits a public `docs.google.com/document/d/...` link. An anonymous Google export request verifies public access and discards the response body. AI never receives document contents. A Google outage or denied access leaves the submission unsaved; users receive a retry/sharing message.
- `submittedAt` is first submission; `lastModifiedAt` is latest replacement. Every accepted replacement adds a version. First submission determines compliance; a late replacement is displayed separately. Exactly-at-deadline submission is on time. Times are interpreted as Asia/Manila, with an explicit deadline time.
- Adviser reads are limited to their assigned groups. Students see their own rows/history. Coordinator manages requirements, enrollment and groups; admin retains account/system controls. Account role edits now use an administrator-only endpoint and preserve unrelated profile fields. Self-demotion/deletion and removal of accounts/groups/members with academic history are rejected.
- `GoogleWorkspaceService` uses personal-account OAuth consent, PKCE, expiring one-use state bound to the initiating IntelliTrack user, and encrypted refresh tokens. Google sign-in uses the existing login integration; Calendar and Reports require separate consent.
- Calendar sync creates an app calendar, reconciles all assigned nonlegacy terms, updates deterministic event IDs, and removes stale app events. Reminders: one day and one hour before deadline. Students press **Sync my deadlines** again after deadline changes. This is explicit one-way sync, not a background bidirectional integration.
- Reports use `drive.file` permission to create an `IntelliTrack Reports` folder and a dated spreadsheet with Students, Groups and Summary tabs in the consenting coordinator's Drive. Values use RAW input; student strings cannot become formulas. Each export is a snapshot of the selected filters. Group IDs keep different teams with the same title separate. Filters are preserved in the dashboard URL through Google consent and refresh.
- AI uses Gemini with only scoped tracking metadata, a short factual prompt, bounded caching and per-user cooldown. Computed summaries are explicitly labelled when no key is configured or the API fails. No grading, document evaluation or comment generation.

## Before deploying

1. Back up the existing Supabase database. Restore a separate test database and review/run `migrations/001_submission_tracking_postgresql.sql` there first. It refuses duplicate submission/membership data rather than deleting it. This PostgreSQL script has not been executed against Supabase.
2. Check existing timestamps: the old application used server-local times in places. Do not blindly shift them. Record which deployment timezone created the samples before any conversion. New submission/deadline workflows use Manila consistently.
3. Stop writes during the production migration. Run the same reviewed SQL only after test acceptance. Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` for the migrated deployment; the old default `update` is not a substitute for removing the obsolete uniqueness constraints.
4. In **Requirements & deadlines**, assign legacy sample sections to their intended term, then edit legacy requirements to the matching subject/term/deadline. Connected cross-section groups are assigned together. Create new offerings for later terms; do not overwrite old ones.
5. Verify imported emails match the personal Gmail accounts used for sign-in. Name-only automatic account linking was removed. Coordinators can explicitly link unmatched enrollment records after checking identity.

## Google Cloud setup

Use the existing Google Cloud project. Enable Google Drive API, Google Sheets API and Google Calendar API. Configure an External OAuth audience and add the demo Gmail accounts as test users if the app is in Testing.

Use a **Web application OAuth client**. Authorized JavaScript origin: `https://intellitrack-pied.vercel.app`. Register both exact redirect URIs:

```
https://intellitrack-backend-smaj.onrender.com/login/oauth2/code/google
https://intellitrack-backend-smaj.onrender.com/api/google/callback
```

Add localhost equivalents only for local development. Login requests openid/email/profile. Calendar requests `calendar.app.created`; Reports requests `drive.file` (accepted by Sheets creation and values methods). Do not replace this with a service-account key: the new workflow writes to the authorized person's Drive.

Google's external Testing-mode refresh tokens can expire after seven days when additional scopes are requested. Reconnect before the presentation if needed. Publishing/verification requirements depend on the actual consent-screen scope configuration; check Google Cloud's status rather than assuming verification is complete.

References: [Google web-server OAuth](https://developers.google.com/identity/protocols/oauth2/web-server), [Calendar event insertion](https://developers.google.com/workspace/calendar/api/v3/reference/events/insert), [Sheets creation](https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets/create), [Drive sharing](https://developers.google.com/workspace/drive/api/guides/manage-sharing).

## Render environment

Keep secrets in Render environment settings, not Git or Vercel's public environment.

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | Existing Supabase JDBC URL with SSL; use the connection option supported by Render |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Supabase connection credentials |
| `JWT_SECRET` | Existing strong signing secret; keep stable across restarts |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Web OAuth client |
| `GOOGLE_REDIRECT_URI` | Login redirect URI above |
| `GOOGLE_WORKSPACE_REDIRECT_URI` | Workspace callback URI above |
| `GOOGLE_TOKEN_ENCRYPTION_KEY` | Base64 encoding of 32 cryptographically random bytes; keep stable and back it up securely |
| `GOOGLE_REPORTS_OWNER_EMAIL` | Optional designated report owner's Gmail; blank permits each coordinator to connect their own account |
| `APP_FRONTEND_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS` | `https://intellitrack-pied.vercel.app` |
| `APP_ALLOWED_EMAIL_DOMAINS` | Empty for personal Gmail support |
| `AI_API_KEY` | Gemini developer API key; optional until AI is enabled |
| `AI_API_URL`, `AI_MODEL` | Existing supported Gemini endpoint/model, if overriding defaults |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` after migration |

The server now honors Render's `PORT`. Use the existing Backend Dockerfile (Java 21). Set Render's health check path to `/actuator/health/readiness`; it checks database connectivity without requiring Google credentials or exposing connection details. Local `.env` loading no longer prints values or overrides deployment settings. Do not change encryption keys casually: existing Google connections need the original key to decrypt their refresh tokens. No API key or payment details need to be pasted into chat.

Vercel: project root `Frontend`, build `npm run build`, output `build`, and `REACT_APP_API_BASE_URL=https://intellitrack-backend-smaj.onrender.com` (no `/api` suffix or trailing slash). Redeploy after changing build-time variables. SPA rewrites include the new `/google-connect` return route.

## Acceptance checks on the deployed test environment

1. Sign in as two different students with personal Gmail. Enroll them in different sections of the same subject/term and assign one group/adviser. An unrelated adviser must see neither student's rows/history.
2. Assign a shared deadline. Before submission, each member appears independently. Submit a publicly shared Docs link; private and non-Docs links must fail with a useful message.
3. Submit on time, then replace after the deadline: first status stays on time; latest replacement is late; history has both links/timestamps. Another member submitting for the first time after the deadline is late.
4. Extend a deadline with a reason. Confirm all sections update and the audit records the old/new deadline and reason. Resync Calendar twice and verify no duplicate events. Archive a requirement and sync; only its app event is removed.
5. Export a filtered scope. Grant Reports consent, return to the dashboard and press Export again. Verify the automatically created folder, dated spreadsheet, member details and totals against the displayed scope. The report must be in the expected Gmail account.
6. Generate student, assigned-adviser and filtered-coordinator summaries. Confirm no cross-role data or document-quality claims. With AI disabled/quota-limited, the UI must identify its computed fallback.
7. Carry the team to Capstone 2; confirm membership is retained while submissions and requirements remain separate from Capstone 1.
8. Reload nested frontend URLs and test backend wake-up before presenting. A frontend login page or Render wake-up page alone does not prove DB/OAuth/API health.

## Known operational limits

Local verification after cleanup on 2026-09-30: **26 backend checks passed**; the legacy live Sheets test was deliberately skipped. **Four frontend regression checks passed.** The production frontend build completed successfully (Browserslist data freshness notice only). Backend tests use isolated H2 databases and mocked Google responses; they do not verify PostgreSQL migration syntax or actual Google permissions. No production database or Google files were changed.

Re-run with `mvn -o test` in Backend and `npm test -- --watchAll=false --runInBand` plus `npm run build` in Frontend. On this workstation the global npm shim is broken; the equivalent `node node_modules/react-scripts/bin/react-scripts.js test --watchAll=false --runInBand` and `... build` were used successfully.

- Live OAuth, Docs public-access verification, Calendar and Sheets calls require the real configuration and remain unverified until the acceptance checks above. ChatGPT Plus does not supply backend API credits.
- The tracking query currently reads the capstone dataset and filters in the service. Suitable for demo-scale data; database-scoped queries and server pagination are a next optimization for larger deployments.
- Google operations are serialized within one backend process. Use one Render instance for this version; multi-instance resource creation needs database locking/idempotency beyond the event IDs.
- Deadline changes are recorded in the same transaction as the requirement update, and the coordinator can inspect them through the requirement's History button. The old group-level automatic alert scheduler is disabled by default because it assumed one group submission; Calendar reminders and dashboard statuses are the active reminder workflow.
- A partially failed Sheets export can leave an incomplete snapshot in Drive. The UI reports the failure; retry creates a new dated snapshot. A deleted app calendar or deleted/trashed report folder is recreated on the next operation. Revoked Google consent requires **Connect / reconnect Google**.
- Older source modules remain for later removal. Legacy dashboard/student workspace/adviser metrics/deliverable/deadline/export/submission endpoints are now blocked; active screens use `/tracking`, `/academic` and `/google`. Do not use old metrics or grading outputs as the new submission tracker acceptance criteria.

## Cleanup review before workflow testing

The review traced the active React routes into their API calls and checked the corresponding permissions, persistence rules, Google requests, configuration, and error handling. It corrected these additional mismatches:

- Admin account role changes were silently ignored and partial updates cleared unrelated fields; the new account endpoint fixes both.
- The system settings page wrote unused database flags. It now reports effective runtime configuration without presenting nonfunctional controls, and the admin home uses account counts and audit records instead of legacy metrics.
- Group removal previously cascaded through submissions and versions. Groups with submission/carry-forward history and members with submissions now retain their records; grouped students cannot be directly unenrolled.
- Classlist imports no longer associate accounts using student numbers that users can edit. Matching uses email; same-number duplicates across sections within a subject/term are rejected. Imported group titles remain readable.
- Login tokens return in a URL fragment, and refresh tokens use a JSON body instead of query parameters. Refresh also updates the frontend's account role. Deploy frontend and backend together because the refresh request contract changed.
- Unknown/retired API routes are denied. Missing records, malformed requests and data conflicts get useful HTTP responses; unhandled server errors do not expose database details.
- AI cache entries compare the complete scope rather than only its hash. Google exports use actual Drive parent IDs when moving new reports into their folder.

This is a completed code cleanup and automated regression pass, not a claim that every runtime condition has been exercised. Next, run the role-by-role workflow checks above with representative data, then verify live Google services and the PostgreSQL migration in an isolated deployment. Browser visual/responsive checks and real provider behavior remain part of that workflow phase.

## Review fixes — 2026-10-01

- Carry-forward rejects ambiguous target enrollments, conflicting linked accounts, and matches based only on a student number. An existing record for the same account remains reusable even when its enrollment details have changed. Conflicts require coordinator reconciliation before carrying the team.
- Manual enrollment checks account ID, case-insensitive email, and nonblank student number across the subject/term, including unlinked imports. It directs the coordinator to link the existing record instead of creating a duplicate; separate terms remain allowed.
- Submission history now calculates status using the current deadline, matching dashboard compliance after extensions. Saved version statuses remain intact and are displayed as “originally recorded” when they differ.
- Regression verification: 31 backend tests passed, with the live Google Sheets test skipped; all five frontend tests passed. Five new backend cases and one frontend case cover these fixes. No schema changes or production actions were needed for this follow-up.

## Local MySQL setup — 2026-10-15

- MySQL initialization now runs after Hibernate has created/updated tables. Migration failures stop startup instead of being silently ignored.
- The MySQL script establishes subject/term requirement uniqueness before removing the obsolete subject-only or section-only constraints. It detects indexes by their definitions and retains a supporting section foreign-key index. No application rows are deleted.
- Verified on the local MySQL service in an isolated temporary database: two consecutive migration runs succeeded, existing rows and foreign keys survived, different terms accepted the same requirement name, and same-term duplicates were rejected. The temporary database was removed; the existing application database was not migrated during this check.
- A fresh 32-byte Google token encryption key was generated in the Git-ignored `Backend/.env`. Its value was not printed. Preserve this key across restarts; replacing it later requires reconnecting Google accounts.
- The repeatable local check is `Backend/scripts/LocalMigrationCheck.java`. From Backend, run it using Java 21 with the cached MySQL connector JAR on the classpath. It refuses nonlocal database hosts and requires permission to create/remove its own temporary database.
- Start the backend from Backend so it loads that `.env`. On startup, the corrected migration will run against the configured local database. Verify `/actuator/health/readiness` before proceeding with role-by-role tests. Google Cloud localhost redirect registration and live Google consent still need verification.

## Coordinator setup flow — 2026-10-15

- Empty coordinator analytics now links to an ordered setup guide. Empty scopes disable AI/export actions, and a fresh dashboard defaults to All terms instead of Legacy.
- `/coordinator/setup` has Subjects & sections, Requirements & deadlines, and Carry team forward tabs. The coordinator can now create a subject directly via the role-protected `/api/academic/subjects` endpoint. Subject codes are normalized and duplicates rejected.
- Academic setup, classlist import, and group forms share school-year and semester selectors. Coordinator pages share content width, form sizing, and responsive layout rules. Group rows show their subject/term and flag incomplete setup.
- Empty classlists link directly to import/manual setup. Import results link to enrolled students. Existing legacy academic URLs remain available.
- Legacy sample group/deadline creation is disabled by default (`app.seed-legacy-samples=false`). Existing sample records were not deleted. Starter local accounts remain available.
- Verification: 32 backend tests passed, one live Google test skipped; seven frontend tests passed across the existing suite and new setup test. Browser automation was unavailable, so visual review at desktop/mobile sizes remains a manual acceptance step.
- Restart the backend for the new subject endpoint, refresh the frontend, then open Academic setup → Subjects & sections. Create CAP1 / Capstone 1, select the year/semester, and add the first section before enrollment testing.
- This pass covers initial setup and layout consistency. Spreadsheet template downloads/import preview and stricter admin/coordinator feature separation remain follow-up work.

## Google students awaiting enrollment — 2026-10-15

- Students & sections includes a searchable Awaiting enrollment table inside the application (not a separate Google spreadsheet). It lists student accounts with a Google identity and no linked enrollment in any term. Seeded password-only students and staff are excluded.
- The coordinator can choose a section for a new enrollment or explicitly link an existing unlinked imported record after verifying identity. Both actions reuse the existing ownership/duplicate guards. Refreshing after enrollment removes resolved accounts from the list; imports that link accounts have the same effect.
- `/api/student-enrollments/awaiting` is coordinator/admin-only and returns limited identity fields, never passwords, tokens, or Google subject IDs. Existing accounts signing in through the OIDC success handler now also retain their Google identity marker.
- No schema migration is required. Restart the backend and refresh Students & sections. For live acceptance, sign in with a student Gmail absent from the classlist, refresh the awaiting list, then enroll/link that student and verify disappearance.
- Verification: 33 backend tests passed, one live Google test skipped, nine frontend tests passed, and the production frontend build succeeded. Live Google sign-in and browser visual verification remain pending.

## Coordinator dashboard and calendar - 2026-10-15

- Coordinator Home now has its own overview: enrollment/group/requirement counts, upcoming deadlines, overdue follow-ups, and enrollment/setup attention items. Submission analytics retains its separate route.
- Deadline calendar shows active requirements directly from the academic catalog, including requirements with no enrollments yet. It supports month navigation, day selection, a month agenda, and subject/term filters in Asia/Manila time.
- Analytics links retain the selected scope, including All terms. Old legacy-term bookmarks select unassigned records correctly.
- This is an in-app deadline calendar. Coordinator Google Calendar OAuth/sync is not implemented in this pass. Other role pages and backend code were not changed for this feature.
- Verification: all 13 frontend tests passed across six suites; production build compiled successfully. Browserslist reports outdated compatibility data. Browser visual/responsive review remains manual; live Google calls were not tested.
- Next local check: refresh the coordinator page, open Dashboard, then Deadline calendar. Verify a requirement appears on its assigned date and View submissions opens the matching analytics scope. This frontend-only pass does not require a backend restart.
## Exported explanation and report formatting - 2026-10-21

- Report export now calls TrackingSummaryService with the same authorized, filtered rows used for totals and spreadsheet data. A matching cached Gemini summary is reused; otherwise generation is attempted with the existing timeout/cooldown and computed fallback.
- Summary includes explanation text, its actual source (Gemini AI or Computed summary), the service notice, and a metadata-only/no-document-content note. Totals have readable labels.
- New reports use wider wrapped columns, styled frozen headers, and filters on Students/Groups. Summary has a wide explanation column. Values remain RAW, including AI text and student names.
- Verification: 35 backend tests passed; one live Google test skipped. Tests cover Gemini/fallback export payloads, formatting requests, and filtering before summarization. No live report was created during automated checks.
- Restart the backend and export again; the existing Google connection can be reused. Previous reports are snapshots and are not modified. Inspect the new Summary tab for explanation/source and verify the live formatting.
## Coordinator Google Calendar - 2026-10-21

- Deadline calendar now offers Connect / reconnect Google Calendar and Sync all deadlines. OAuth returns to the initiating page; the coordinator then presses Sync. Reports and Calendar permissions remain separate.
- Coordinator-only POST /api/google/calendar/coordinator/sync reconciles every active, nonarchived requirement with a deadline, across all terms and subjects, including requirements without enrollments. Page filters never narrow this reconciliation.
- Reuses the dedicated IntelliTrack Deadlines calendar, stable user/requirement event IDs, Asia/Manila deadlines, one-day/one-hour reminders, and deletion of app-tagged stale events. Edits are one-way from IntelliTrack on manual sync; Google changes do not edit academic requirements.
- Student sync behavior remains intact; advisers and students cannot call coordinator sync. No schema, credential, or Google Cloud redirect changes were made.
- Verification: 38 backend tests passed, one live test skipped; 15 frontend tests passed. Tests cover catalog selection without enrollment, role protection, repeated sync, changed dates, archive cleanup, filtered-view sync and UI errors. Live Google Calendar consent and event creation still need user acceptance testing.
- Restart backend, refresh frontend, open coordinator Deadline calendar, connect a real Gmail account and grant Calendar permission, then Sync all deadlines. Google Calendar API must be enabled in the same Cloud project. Existing localhost Workspace callback is reused.
## Calendar timestamp correction - 2026-10-21

- After enabling Calendar API, live sync returned HTTP 400. Code inspection found OffsetDateTime.toString() omitted seconds for minute-aligned deadlines (23:59+08:00), violating Calendar's RFC 3339 dateTime format.
- Event start/end now use ISO_OFFSET_DATE_TIME, retaining zero seconds and the Manila offset. Regression assertions cover insert/update payloads and end-time rollover to the next day for student and coordinator paths.
- All seven GoogleWorkspaceService tests passed. This fixes a confirmed payload defect; the cause of the live 400 still requires confirmation by retry after backend restart. No credentials or Cloud configuration changed.
## Reusable subject/semester report workbooks - 2026-10-21

- Exports now reuse a Drive spreadsheet identified by a private appProperties key scoped to the IntelliTrack user, subject ID, and academic term (SY plus semester). A readable filename includes subject and term. Legacy timestamp-named files lack this key and remain untouched.
- A section export writes its section tab (G1, G2, etc.); an unfiltered section selection writes All Students. Shared Groups and Summary retain labelled snapshots for each report scope, including AI source, explanation, filters and update time. Additional group/adviser/requirement/status/search filters produce a stable filtered-tab suffix so a partial view does not overwrite a full section.
- Re-exporting a scope atomically replaces that tab's values and its shared summary/group blocks, clearing trailing stale rows while preserving other scope snapshots. Text is written as explicit stringValue cells to prevent formula interpretation. Different subjects or terms create separate workbooks; mixed-subject/term exports are rejected with a selection prompt.
- Drive creates the tagged workbook in the reports folder before Sheets population, so an interrupted population can reuse that file on retry. A trashed/deleted workbook is recreated on the next export. Operations are serialized in one backend process; concurrent multi-instance deployments still require distributed coordination. No database migration is needed.
- Automated verification: 39 backend tests passed, one live Google test skipped; eight Google service tests re-passed after preserving existing grid dimensions. Frontend suite: 16 passed. Live acceptance remains: restart backend, export G1 then G2 in the same subject/term, verify the same file URL with separate tabs, then re-export G1 and confirm no duplicate tab and preserved G2 summary.
- Generated report tabs are snapshots updated on export, not live views. Overlapping All Students/section/filtered snapshots must not be summed together. Empty-result exports remain disabled; use a broader nonempty scope to report current data.

## Individual progression and group removal - 2026-10-22

- Students & sections now exposes per-enrollment Eligible to proceed, Pending / INC and Repeat subject decisions with required reasons and audit history. Existing/null and new enrollments default to Pending / INC, without changing prior records. School-year and semester filters retain access to historical classlists; section moves offer only the same term.
- Carry-forward copies only current eligible members into a different subject in a later term. Original enrollments/submissions stay intact. Repeating the operation after clearing a pending member adds them to the same successor group; already-carried members are skipped and an all-already-carried request is rejected. Destination enrollments link back to the original enrollment.
- Repeat subject creates an ungrouped, Pending enrollment in a selected later-term section of the same subject. Duplicate identity matches in the destination term are rejected; no original record is moved or deleted.
- Group removal is independent of progression, including for members with submissions. Former group IDs and an audit event preserve membership history; analytics retains submission visibility and new submissions to the removed group are denied. Enrollments and groups with removal/carry history cannot be deleted through the existing management endpoints.
- Local MySQL uses ddl-auto=update: restart backend to create the new columns and former-membership table. PostgreSQL migration is Backend/scripts/progression-postgres.sql; no deployment performed. No Google configuration changes required.
- Verification: 42 backend tests passed, one live Google Sheets test skipped; 18 frontend tests passed; production frontend build completed. Live coordinator acceptance remains: restart, review two members with different outcomes, carry the eligible member, clear the pending member and carry again, then test Repeat subject and independent removal.

## Group-first progression - 2026-10-22

- Coordinator requested fewer per-student actions. Groups & advisers now contains Group progression: select a group, choose Eligible / Pending / Repeat, enter one reason, and apply to every current member in one transaction. Removed members are excluded. Counts show mixed outcomes, and the UI explicitly states that a whole-group decision replaces existing individual decisions.
- Students & sections retains individual controls for exceptions only. Carry-forward guidance now directs coordinators to the group controls first. No change to the independent meaning of group removal.
- Repeat group creates a successor group in the same subject and a later term, preserving adviser and section names and creating new enrollments for current Repeat members only. Original enrollments remain intact, new enrollments default to Pending, and duplicate repeat groups are rejected. Individual repeat remains available for exceptions.
- Backend: 44 tests passed, one live Google Sheets test skipped. Frontend: 19 tests passed, including the single-request group decision; production build succeeded. No additional database changes beyond the preceding progression migration.

## Group progression refresh correction - 2026-10-22

- User confirmed the page reset after Apply to whole group. The save callback called the full page loader, whose loading state unmounted GroupProgression and discarded its selected group and success message.
- Progression actions now refresh only group data without entering the full-page loading state. The selected group stays visible, receives server-returned progression counts, and retains save feedback. No backend changes or restart required for this correction.
- All 20 frontend tests passed. Added a page-level regression with a delayed refresh to verify that the group stays selected both during and after saving, and the updated Eligible count and success message appear.

## Import-driven semesters and archive history - 2026-10-22

- User replaced carry-forward/repeat-copy workflow with classlist imports for new enrollments. Removed carry and repeat-copy controls from Academic setup and group/individual progression. Former HTTP copy endpoints now return 410 with an instruction to import the new classlist; existing copied records remain untouched. Progression outcomes remain records, not enrollment triggers.
- Academic setup → Semesters & history lists active and finished terms. Finish semester requires confirming all subjects in the selected school year/semester. This writes a semester archive marker without deleting or changing enrollments, memberships, outcomes, submissions or requirements. Reopen removes the marker and logs the action; INC cases can then be corrected.
- History displays classlists with recorded outcomes, groups/advisers and deadlines, plus a term-scoped analytics/report-export link. Coordinator classlists/groups/dashboard/calendar and default tracking results omit archived terms. Explicit term-scoped tracking remains available for history and exports.
- Academic edits, progression changes, group edits/removal, manual enrollments, account linking, imports and new link submissions are blocked for archived semesters. Active Calendar reconciliation excludes archived semesters; users must sync again to remove existing Google Calendar events. External report files are retained.
- Local MySQL ddl-auto=update creates semester_archives on backend restart. PostgreSQL migration: Backend/scripts/semester-archive-postgres.sql. No deployment or live semester archive was performed. The existing import obtains subjects from spreadsheet rows and uses the school year/semester selected on its import page.
- Verification: 46 backend tests passed, one live Google Sheets test skipped; all 21 frontend tests passed; production frontend build succeeded. Includes archival preservation, active/history filtering, write protection, reopening, coordinator authorization and retired endpoint coverage.

Student access review: TrackingRow now includes canSubmit/submissionBlockReason; removed members and archived terms are read-only in the student UI. Tracking responses include owned academic terms for history selection. AccessService permits students to read their own past submission history after group removal, while write membership checks remain enforced. Backend: 49 tests, 0 failures, 1 live Sheets test skipped. Frontend: 23 tests passed. See docs/STUDENT_TESTING.md for live Google checks.
