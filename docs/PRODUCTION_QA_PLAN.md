# IntelliTrack — Production QA Test Plan

| Document field | Value |
| --- | --- |
| Plan date | October 25, 2026 |
| Application | IntelliTrack Submission Tracker |
| Frontend | https://intellitrack-pied.vercel.app |
| Backend | https://intellitrack-backend-smaj.onrender.com |
| Release branch | `revised` |
| Baseline code commit | `4a8bd2e` — verify the deployed commit before each run |
| Baseline frontend deployment | `dpl_A3zf3vcDNdiNCqJd89gF4C9ximPR` — includes corrected production API URL |
| Database | New Supabase production database; preserve existing accounts |
| Timezone | Asia/Manila (Philippine time) |
| Test type | Manual functional, integration, authorization, usability, and release regression |
| Execution status | NOT RUN — this document is a plan, not evidence of passing QA |
| Tester / execution date | ____________________ |
| Actual frontend / backend release IDs | ____________________ |

## 1. Rules and preparation

1. Hard-refresh the website. Open DevTools **Network** and **Console**; enable Preserve log during a reproduction. Requests must target Render, never `localhost:8080`.
2. Confirm Vercel is Ready and Render has completed startup for the intended release. Record both commit IDs; a reachable backend alone does not prove it runs the latest code.
3. Use separate browser profiles for each role. Two incognito windows in the same browser may share a session; do not assume they are isolated.
4. Use controlled test accounts and prefix all academic data with `QA-`. Use only email inboxes you own or have explicit permission to test.
5. Keep automatic deadline delivery disabled until the controlled email tests. Connect Gmail separately. Never enable local and production automation against the same database.
6. Do **not** run the legacy submission reset script on the new Supabase database. Do not clear all tables to prepare QA.
7. Run destructive operations only on disposable QA records. Revoked consent, simulated provider failures, large imports, scheduler failure, and infrastructure outage tests belong in an isolated test environment where stated.
8. Capture errors without passwords, JWTs, cookies, refresh tokens, encryption keys, database credentials, or private document contents. HAR files can contain these: sanitize before sharing.
9. Stop the affected workflow immediately for cross-user data exposure, role escalation, unintended deletion, or unintended email delivery. Do not continue creating notifications while investigating.

## 2. Account and test-data register

These are **test account labels**, not existing credentials. Fill in the real accounts from production User Management. This plan does not supply or change passwords.

| Label | Required account / access | Actual email or account ID |
| --- | --- | --- |
| ADMIN-A | Existing administrator; manages users and sender connection | __________ |
| COORD-A | Coordinator, with access to QA academic setup | __________ |
| ADVISER-A | Adviser assigned only to QA-GROUP-A | __________ |
| ADVISER-B | Adviser assigned only to QA-GROUP-B | __________ |
| STUDENT-A | Linked student in QA-GROUP-A | __________ |
| STUDENT-B | Second linked student in QA-GROUP-A | __________ |
| STUDENT-C | Linked student in QA-GROUP-B | __________ |
| STUDENT-U | Google student not yet linked to an enrollment | __________ |
| FIRST-STAFF | Controlled Google email pre-created as coordinator/adviser; never used to sign in yet | __________ |
| MAIL-SENDER | Designated outgoing account: `intelli.track0@gmail.com` | Confirm access: ______ |
| SHEET-OWNER | Google account owning a private QA classlist | __________ |

Use the approved subjects IT332 / Capstone 1 and IT411 / Capstone 2 with disposable QA sections, groups, and academic terms. Do not invent course codes for positive tests. At least one student should appear in two subjects to test scope. Keep unrelated real records out of the fixtures.

| Fixture | Purpose |
| --- | --- |
| TERM-A / TERM-B | Distinct valid academic terms; archive TERM-B only at the end |
| QA-SUBJECT-A / QA-SUBJECT-B | Test labels for IT332 / IT411; detect subject leakage and filter errors |
| QA-GROUP-A / QA-GROUP-B | Detect adviser and student isolation failures |
| QA-FUTURE | Deadline more than 24 hours ahead |
| QA-DAY | Deadline between 1 and 24 hours ahead |
| QA-HOUR | Deadline less than 1 hour ahead |
| QA-OVERDUE | Past deadline on a disposable requirement |
| DOC-A / DOC-B | Two valid Google Docs links accessible under the submission policy |
| DOC-PRIVATE | A private document to test access validation |
| LOCAL-VALID | Small `.xlsx` classlist with controlled students |
| SHEET-VALID | Private Google Sheet containing the same rows |
| LOCAL-INVALID | Separate workbook with bad email/ID, missing required data, and duplicates |

Classlist columns A–G: **Student ID, Full Name, Email, Subject Name, Subject Code, Section, Group Name**. Row 1 contains headers. Use valid IDs such as `26-0001-001`, and Google sign-in emails for account linking. Group Name is optional. Use separate terms when comparing cloud/local imports so one does not mask the other's result.

## 3. Execution order and priorities

Run sections 4–7 first, then 8–11, then 12–14. Archive and cleanup tests run last. Repeat the smoke set after every deployment or configuration fix.

- **P0 — release gate:** production availability, login, role/data isolation, and core submission integrity. Any failure blocks release acceptance.
- **P1 — required:** role workflows, imports, reporting, Google connections, and automation if included in the demonstration.
- **P2 — polish/resilience:** presentation, accessibility, and optional edge cases. Record all failures and decide explicitly whether to defer them.

Status values: **NOT RUN, PASS, FAIL, BLOCKED, N/A**. Missing credentials, unavailable provider consent, or an unconfigured scheduler mean BLOCKED, not PASS. N/A requires a written scope reason.

Smoke set: DEP-01–04, AUTH-01–04, SEC-01–03, IMP-01, SUB-01–03, ADV-01, ADM-01, GOO-01.

## 4. Deployment and connectivity

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| DEP-01 | P0 | Confirm deployment IDs; open home and hard-refresh. Inspect login request destination without submitting credentials to an unfamiliar host. | Current release loads; API calls use the intended Render origin, not localhost; no missing JS/CSS. |
| DEP-02 | P0 | Directly open `/admin/system`, `/student/submissions`, and `/coordinator/classlist`; refresh each while signed in with the matching role. | SPA routes load; no Vercel 404, blank page, or HTML returned for a JS asset. |
| DEP-03 | P0 | Perform one legitimate login from the production domain; inspect OPTIONS/POST requests. | CORS succeeds; response is readable by the frontend. A server response is distinguished from network failure. |
| DEP-04 | P0 | Observe Render startup and exercise a read-only dashboard action. | Startup completes, schema queries succeed, and no repeated 500s occur. Record the deployed backend commit. |
| DEP-05 | P1 | After normal Render inactivity, reopen the app and wait for the backend to wake. Record elapsed time. | Loading recovers without corrupting session/data; cold-start delay is recorded separately from application errors. No exact warm-up time is assumed. |
| DEP-06 | P1 | Refresh after creating one QA record; reopen in another session and, during a planned deployment, recheck afterward. | Stored records persist across reloads/redeploys; database is the intended new Supabase instance. |

## 5. Login, sessions, and onboarding

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| AUTH-01 | P0 | Sign in using each of the four roles, one isolated profile per role. Use `/student/login` for students and `/staff/login` for staff. | Correct role workspace opens; wrong-portal sign-in is rejected by the backend. |
| AUTH-02 | P0 | Submit a wrong password, nonexistent account, and blank fields. | Clear validation or credential error; no authenticated session, privilege grant, or crash. |
| AUTH-03 | P0 | Sign in, refresh, open a permitted nested route, log out, then use browser Back and refresh. | Session survives authorized refresh; logout prevents renewed access to protected data. |
| AUTH-04 | P0 | In separate profiles, test student Google sign-in and FIRST-STAFF Google sign-in after admin pre-creates the exact email and role. | Existing account is linked correctly; staff reaches its assigned workspace; no duplicate account or automatic staff privilege from email alone. |
| AUTH-05 | P1 | Sign in with STUDENT-U, whose Google email has no classlist enrollment. | Safe empty/onboarding state; no other student's groups, submissions, or deadlines. |
| AUTH-06 | P1 | Import STUDENT-U's exact email; sign out/in and refresh group/enrollment pages. | The account links to its own enrollment; only intended subjects/groups become visible. |
| AUTH-07 | P1 | Cancel Google consent, then retry; test a wrong Google account where a designated sender is required. | Cancellation is recoverable; wrong sender is rejected without replacing the legitimate sender. |
| AUTH-08 | P1 | Exercise an expired/invalid session using an isolated test profile; trigger concurrent protected reads. Do not edit shared tokens or production JWT settings. | A valid refresh restores access, or the app returns to login cleanly; no endless refresh loop or stale-user data. |
| AUTH-09 | P1 | Admin changes a disposable staff user's role while that user is signed in; attempt old-role actions and sign in again. | Backend enforces the updated authorization/session policy; old privileges are not retained. |
| AUTH-10 | P1 | Request password reset for a controlled password account and an unknown email; use the valid link once, then reuse it. | No account-existence disclosure; valid reset works once; old password fails; reused/expired token fails. SMTP must be configured separately from Gmail sender integration. |

## 6. Authorization and data isolation

For API checks, use DevTools with your own controlled test accounts and known QA record IDs only. Do not enumerate real users or paste authentication tokens into reports. A forbidden resource should return 403 or a non-disclosing 404; unauthenticated access should be rejected. A hidden navigation item alone is not proof of authorization.

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| SEC-01 | P0 | As each role, manually navigate to every other role's route family. | Unauthorized workspace is denied; no protected data flashes or remains available. |
| SEC-02 | P0 | As STUDENT-A, request STUDENT-B/C's known QA submission, version, or notification through the relevant UI/API request. | Other students' private records and update actions are inaccessible. |
| SEC-03 | P0 | As ADVISER-A, change a known QA group filter/ID to QA-GROUP-B; try direct detail/review requests. | No access to ADVISER-B's group or students, including export/summary/detail paths where offered. |
| SEC-04 | P0 | In a logged-out profile, replay a protected read/write request without credentials. | Server rejects it; no data returned and no state change. |
| SEC-05 | P0 | As student/adviser, attempt a known admin/coordinator-only request on disposable QA data: user role change, classlist import, deadline edit, or sender connection. | Server rejects every unauthorized action, regardless of UI visibility. |
| SEC-06 | P1 | With an authorized operator, verify Supabase public application tables have intended RLS/API grants. Check application access afterward. | Browser anon/authenticated Data API cannot expose application users or Google token records; privileged backend remains functional. Do not test by publishing service credentials. |
| SEC-07 | P1 | Inspect browser storage, Network responses, page source, and errors. | No backend/client secrets, database credentials, or Google refresh tokens reach the browser. Public API URLs are expected. |

## 7. Administrator

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| ADM-01 | P0 | Open Overview, User Management, System configuration, Deadlines, and Analytics. | Pages load for admin; Overview shows website/account activity, distinct from submission analytics. |
| ADM-02 | P1 | Create/update a disposable user; try duplicate email and invalid required fields; reload. | Valid changes persist, invalid changes are rejected clearly, and duplicate identities are not created. |
| ADM-03 | P1 | Compare user totals against known accounts; sign in with an additional controlled account and refresh activity after its update window. | Counts reconcile with definitions. Authenticated activity is not presented as exact live-online presence; updates may be throttled up to five minutes. |
| ADM-04 | P1 | Inspect system configuration and Gmail status. | Environment and sender are accurate; missing Google settings are named without showing secret values; disabled automation is distinct from disconnected Gmail. |
| ADM-05 | P1 | Use submission analytics filters and compare with known QA submissions. | Counts and rows represent the selected academic data; admin overview metrics are not substituted for submission metrics. |

## 8. Coordinator, academic setup, and imports

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| COORD-01 | P1 | Create QA subjects/sections/groups in TERM-A; assign ADVISER-A/B; reload. | Associations persist and become visible to the correct students/advisers only. |
| COORD-02 | P1 | Create a requirement for QA-SUBJECT-A with an explicit Manila deadline. | Intended enrolled students receive it; QA-SUBJECT-B does not acquire it accidentally. |
| COORD-03 | P1 | Change title/deadline on a QA requirement; refresh student and adviser views. | Updated values agree across roles; prior submission history is retained. |
| COORD-04 | P1 | Import/enroll a student in TERM-B as well; switch term filters. | Terms remain distinct; new enrollment does not overwrite older academic history. |
| IMP-01 | P0 | Import LOCAL-VALID `.xlsx` into the selected QA term. Review summary and classlist. | Expected student rows, sections, subjects, and groups appear; success/error totals match actual outcomes. |
| IMP-02 | P1 | Import a valid `.xls` copy; then choose a non-Excel file and attempt import. | Supported workbook works; unsupported type is rejected and a previously selected file is not silently submitted instead. |
| IMP-03 | P1 | Import LOCAL-INVALID with missing/invalid fields and duplicate rows. | Row errors are actionable; totals reconcile; no malformed enrollment or silent duplicate is created. Record whether valid rows were accepted. |
| IMP-04 | P1 | Repeat the same valid import in the same term; compare row/account/group counts. | No duplicate students/enrollments/groups; existing-record handling is clear in the result. |
| IMP-05 | P1 | Import names with punctuation, whitespace, blank optional groups, and formatted student IDs. | Names/IDs are preserved, required data is validated, and optional grouping behaves consistently. |
| IMP-06 | P1 | Choose Attach classlist > Attach cloud files; connect SHEET-OWNER and select SHEET-VALID privately from Drive with no worksheet name. | Read-only Drive/Sheets consent completes; first worksheet imports through the same validation as local Excel. |
| IMP-07 | P1 | Import a named second worksheet containing spaces or an apostrophe in its name. | Correct worksheet is read; IDs and columns are preserved. |
| IMP-08 | P1 | Try a sheet whose access was removed after selection, nonexistent tab, and empty sheet. Using the import API with controlled credentials, try invalid Google and non-Google URLs. | Clear failure with no unintended import; backend does not fetch arbitrary submitted URLs. |
| IMP-09 | P2 | In isolated test data, exercise 5,000 student rows and then 5,001 cloud rows. | Supported limit is processed with accurate outcomes; excess rows are rejected clearly rather than silently truncated. |
| IMP-10 | P1 | Switch local/cloud sources, reconnect/cancel Google, and retry after a validation error. | UI recovers; correct source and academic term are used; no double submission from repeated clicks. |

## 9. Student submissions and deadline status

Record actual server submission times. Use separate requirements for timing cases; do not change the machine clock or production database timestamps.

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| SUB-01 | P0 | STUDENT-A submits DOC-A to an assigned requirement before its deadline; refresh and sign in again. | Submission persists for STUDENT-A with correct requirement, document link, first timestamp, and on-time status. |
| SUB-02 | P0 | STUDENT-B in the same group has not submitted; inspect both accounts and coordinator analytics. | A's submission does not falsely mark B as submitted. Compliance is per student × requirement. |
| SUB-03 | P0 | STUDENT-A replaces DOC-A with DOC-B; inspect history and first-submitted value. | Current link changes, historical version remains, and original first-submitted timestamp is preserved. |
| SUB-04 | P1 | First submit to QA-OVERDUE after its deadline. | Submission is late, not on time; dashboards agree. |
| SUB-05 | P1 | Submit on time, allow deadline to pass, then replace the link. | First submission remains on time; later replacement is recorded separately and does not rewrite original compliance. |
| SUB-06 | P1 | Leave requirements unsubmitted across future, due-today, and overdue times. | Displayed status/counts follow Manila time; due-today counts may overlap other categories and should not be assumed mutually exclusive. |
| SUB-07 | P1 | Enter empty, malformed, non-Google-Docs, and inaccessible/private document links. | Validation follows the documented document-access policy, gives an actionable error, and does not silently accept an unreadable submission. |
| SUB-08 | P1 | Submit to a requirement for which the student has no enrollment; use a known QA ID. | Backend denies the action; no submission is created. |
| SUB-09 | P1 | Double-click submit; repeat after a slow response; inspect current record and history. | No duplicate current submission or conflicting state. Document the replacement/version behavior if a second intentional submission is accepted. |
| SUB-10 | P1 | Open a saved form, archive/remove its QA assignment in another authorized profile, then submit. | Stale action is rejected or reconciled safely; no orphaned submission is created. |
| SUB-11 | P2 | Compare a deadline near midnight using a browser with another display timezone. | UI still clearly represents Philippine deadline time; date is not shifted unintentionally. |

## 10. Adviser, analytics, and reporting

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| ADV-01 | P0 | ADVISER-A opens dashboard/groups/review with QA-GROUP-A assigned. | Only assigned academic data appears; empty assignments produce a useful empty state. |
| ADV-02 | P1 | Open an assigned student's document and submission history; use feedback controls if exposed. | Correct student's versions/document appear; authorized feedback saves and reloads. If feedback is not exposed, record N/A for that substep. |
| ADV-03 | P1 | Coordinator reassigns a disposable group from A to B; refresh both advisers and retry old detail requests. | Old adviser loses access and new adviser gains the intended group; no stale authorization. |
| REP-01 | P1 | Apply term, subject, section, group, requirement, adviser, status, and student search individually and in combination. Clear filters. | Rows and counts match fixtures; clear/reset restores intended scope; no cross-term leakage. |
| REP-02 | P1 | Compare student, adviser, coordinator, and admin counts for the same permitted data. | Results agree after applying equivalent scope; distinct students are not confused with student × requirement assignments. |
| REP-03 | P1 | Connect reports; export one subject/term; inspect sheet columns, rows, totals, and timestamps. | Export represents the selected data and Manila time; links refer to correct documents. |
| REP-04 | P1 | Repeat export for the same subject/term; change filters; export a different subject/term. | Same report workbook is reused as designed; other subject/term reports remain separate; data is not mixed or duplicated. |
| REP-05 | P1 | Export with no matches or without the required single subject/term selection. | Clear guidance; no misleading empty/mixed report is silently produced. |
| REP-06 | P1 | Generate AI summary with known overdue/late/on-time fixtures, then change filters. | Summary is consistent with visible scope; no other users' data or invented document-content analysis. Provider errors are recoverable and do not block core records. |

## 11. Google connections, Calendar, and notifications

Gmail sender, Sheets imports, reports, and Calendar use separate connection purposes. A successful connection for one does not prove the others work. Administrator/adviser Calendar connections are not supported by the current flow; test Calendar with students and coordinators.

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| GOO-01 | P1 | ADMIN-A opens System configuration and connects the designated Gmail sender. | Consent returns to production, sender shows connected, and credentials remain on backend. `Automation disabled` may still correctly appear. |
| GOO-02 | P1 | As a student/coordinator, connect Calendar and manually sync active assigned deadlines. | App-owned IntelliTrack calendar contains correct titles, dates, Manila times, and intended assignments. |
| GOO-03 | P1 | Sync repeatedly, extend a QA deadline, then sync again. | Existing event updates without duplicates; unrelated personal events remain untouched. |
| GOO-04 | P1 | Remove a QA enrollment or archive a requirement/term; sync again. | Stale app-owned events are reconciled; other active assigned deadlines remain. |
| GOO-05 | P1 | Compare STUDENT-A and C calendars with separate assignments; inspect coordinator calendar. | Each student sees only assigned deadlines; coordinator receives its intended monitoring scope. |
| GOO-06 | P1 | In a controlled account, revoke Google consent and retry, then reconnect. | Clear reconnect guidance; no false success; reconnection restores that integration. Do not revoke the shared sender during others' testing. |
| GOO-07 | P1 | Inspect in-app notifications after supported events; mark one read and reload. | Correct recipient/content; read state persists; notification requests do not loop on expired authentication. |

### Automatic delivery gate

Run AUTO cases only when the worker is intentionally enabled on production, the designated sender is connected, recipients are controlled, and the protected external scheduler is configured. Otherwise mark these cases BLOCKED. Calendar connection alone does not provision a scheduler.

The worker processes up to five emails and three calendars per run. Its normal interval is five minutes while awake. Allow multiple runs for a backlog and record queue depth/time; this is eventual processing, not guaranteed exact-minute delivery.

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| AUTO-01 | P1 | With automation enabled, create a QA deadline for linked controlled students; run/wait for the worker. | Intended students receive one new-deadline email; unrelated/unlinked students do not. Connected calendars update. |
| AUTO-02 | P1 | Extend the deadline; run the worker twice. | Updated email/event reflects new details; repeated processing does not duplicate the same notification/event. |
| AUTO-03 | P1 | For an unsubmitted QA-DAY requirement, process within the 1–24-hour window twice. | One applicable 24-hour reminder per requirement revision; no repeat on the second run. |
| AUTO-04 | P1 | Use QA-HOUR and QA-OVERDUE requirements; process twice. | Applicable one-hour/overdue reminder is sent once per revision, not every poll. Only the current reminder window is queued. |
| AUTO-05 | P1 | Submit before a pending reminder is delivered; run the worker. | Stale reminder is cancelled; submitted work does not receive an unsubmitted reminder. |
| AUTO-06 | P1 | Before delivery, remove a QA enrollment, change a deadline revision, or archive its term. | Worker rechecks scope/revision; obsolete messages are cancelled. Archive notices follow supported event behavior, not stale reminder behavior. |
| AUTO-07 | P1 | Observe the external scheduler through a natural Render sleep/cold start; inspect run records and pending queue. | Authenticated trigger wakes/reaches the service and work eventually processes. An in-process timer alone is not accepted as proof. |
| AUTO-08 | P0 | Operator sends trigger request without/with an invalid automation key, then with the valid configured key. Do not put key in URL or evidence. | Unauthorized trigger rejected; authorized trigger works. Actual delivery occurs only against controlled fixtures. |
| AUTO-09 | P1 | In isolated test environment simulate uncertain provider delivery/failure; inspect review count and sender Sent folder. | Uncertain email goes to REVIEW and is not blindly retried; no duplicate email from automatic retries. Manual resend UI is not currently available. |
| AUTO-10 | P1 | With automation disabled, connect Gmail and create a QA deadline; later enable only under controlled conditions. | Gmail connection works independently. Disabled changes are not retroactively labelled new; existing overdue work can receive a reminder after enabling. |

## 12. Archive and history — run last

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| HIST-01 | P1 | Record TERM-B fixture counts/history; archive only this disposable term. | It moves out of active work; archived records/history remain accessible to authorized roles. |
| HIST-02 | P1 | Try to submit/change archived work; open historical submissions and versions. | Archive restrictions are enforced without deleting historical evidence. |
| HIST-03 | P1 | Import a later term after archival and inspect groups/members. | No unintended automatic carry-forward or merging with old term; new enrollments reflect the new import. |
| HIST-04 | P1 | After archival, inspect analytics filters, reminders, and connected calendars. | Archived work is excluded from active reminders/calendars; explicitly selected historical analytics remain consistent. |

## 13. Desktop UI, accessibility, and resilience

| ID | Priority | Preconditions and steps | Expected result |
| --- | --- | --- | --- |
| UI-01 | P1 | In Chrome and Edge, exercise every role at 1920×1080 and 1366×768, normal browser zoom. | Desktop navigation, tables, actions, and dialogs are usable; no overlapping content or clipped primary buttons. |
| UI-02 | P2 | Resize desktop window near 1200px; try 125% and 200% zoom; open DevTools docked. | Layout remains usable with deliberate scrolling/reflow; no inaccessible controls. This is website testing, not a native mobile requirement. |
| UI-03 | P2 | Check logo on navbar/login/favicon; test long names, empty tables, and long document titles. | No white logo rectangle, broken image, uncontrolled overflow, or misleading empty state. |
| UI-04 | P1 | Use Tab/Shift+Tab/Enter/Escape through login, filters, imports, and dialogs. | Logical focus, visible focus indicator, labelled inputs, accessible errors, and no keyboard trap. |
| UI-05 | P1 | Throttle network; submit once; temporarily go offline using DevTools, then recover. | Loading prevents accidental duplicates; errors preserve useful input; recovery does not falsely claim a save. |
| UI-06 | P1 | Review Console/Network after the smoke set and compare response sizes for populated pages. | No uncaught exceptions, repeated 500s, infinite polling failures, or unnecessary sensitive data. Record page-load/action times. |
| UI-07 | P2 | Two authorized testers edit the same disposable record from stale views. | Behavior is understandable; no partial corruption. Record any last-write-wins behavior as a limitation if conflict detection is absent. |

## 14. Test execution record

Create one row per test case. Split cases into subcases such as AUTH-01-A/B/C/D when testing each role. A case passes only after **all** required substeps pass. Include expected negative outcomes; a deliberately rejected bad password is a pass, not a defect.

| Run ID | Case ID / subcase | Tester | Role / fixture | Environment / commit | Status | Actual result | Evidence link | Defect ID | Tested at (PHT) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| QA-001 | DEP-01 | | | | NOT RUN | | | | |

Record missing setup as BLOCKED with the exact dependency. Do not mark tests passed from this document, unit-test results, or another role's successful session.

## 15. Defect report template

```text
Defect ID: IT-QA-001
Title: [Role/page] concise observable problem
Case ID:
Severity: S1 / S2 / S3 / S4
Priority: P0 / P1 / P2
Environment: production URL + frontend/backend commit/deployment IDs
Browser/version, screen size, zoom:
Account role and QA record IDs (no passwords/tokens):
Preconditions:
Steps to reproduce:
1.
2.
3.
Expected result:
Actual result:
Frequency: every time / intermittent (e.g. 2 of 5)
Occurred at (Asia/Manila):
Network endpoint + HTTP status (sanitized):
Backend error excerpt / request correlation ID (sanitized):
Screenshot or recording:
Data impact / workaround:
Assigned owner:
Fix commit:
Retest result/date:
Related regression cases:
```

Severity: **S1 Critical** = role/data breach, data loss, unintended mass delivery, or total outage. **S2 Major** = essential workflow unusable with no reasonable workaround. **S3 Moderate** = incorrect/limited behavior with a workaround. **S4 Minor** = cosmetic or wording issue. Severity describes impact; priority describes repair order.

## 16. Acceptance and QA sign-off

- All P0 cases pass; zero open S1 or S2 defects.
- All in-scope P1 cases pass. Blocked Gmail/Calendar/scheduler tests mean those features are **not verified**, even when the core site is usable.
- P2 failures have an owner and explicit accepted/deferred disposition.
- Both deployments are recorded and intended; no localhost API requests, unhandled errors, or unauthorized data exposure.
- Counts, original submission times, history, and term boundaries reconcile with the fixture register.
- A second tester repeats critical isolation and submission cases when possible.
- Fixed defects are retested with related regression cases; repeat the smoke set after a release/configuration change.

| Sign-off field | Result |
| --- | --- |
| Total test/subcase executions | |
| Passed / Failed / Blocked / Not Run / N/A | |
| Open defects by severity | |
| Core application accepted? | Yes / No |
| Google integrations accepted? | Yes / No / Blocked |
| Automation accepted? | Yes / No / Blocked |
| Deferred items and reasons | |
| Tester / reviewer / date | |
| Final decision | READY FOR DEMO / CONDITIONAL / NOT READY |

## 17. Cleanup

Export/save sanitized test evidence before cleanup. Remove or archive only identified QA fixtures through supported workflows; preserve accounts and records needed for the presentation. Reconcile QA calendar events using the app, check pending emails, and leave automation in the agreed final state. Do not delete shared personal calendar events, reset the new database, or run an unreviewed bulk cleanup query. Retain at least one prepared example of on-time, late, and overdue work for the demonstration.

## 18. November QA corrections and regression cases

The updated tester DOCX was reviewed on November 2. Its reported passes are tester evidence, not independent live verification by the coding agent. SEC-06, SUB-09 and SUB-10 remain uncertain. SUB-08 reports a missing submission board; repeat its direct unauthorized API check before marking it PASS. Several cloud-import, Google consent, automation, archive and resilience cases remain blank or untested. Keep those separate from confirmed results.

G01/G02 are off-semester section identifiers, distinct from on-semester G1/G2. Do not merge them. Use IT332 (Capstone 1, normally semester 2) and IT411 (Capstone 2, normally semester 1) as the two valid subjects in fixtures; use separate terms, sections and groups for isolation rather than inventing QA course codes. IT411 in 2026-2027 / 1 can follow IT332 in 2025-2026 / 2; IT332 in 2026-2027 / 1 is off-semester.

| ID | Priority | Steps | Expected result | Production status |
| --- | --- | --- | --- | --- |
| REG-01 | P0 | Import IT322 / Capstone 1, then IT411 / Capstone 1. Repeat via cloud. | Row errors; no new invalid subject or enrollment. | NOT RUN |
| REG-02 | P1 | Import valid IT332 records using G01 and G1 for different students; inspect identifiers and term badges. | Both identifiers preserved separately; mismatched section/term convention warns without merging. | NOT RUN |
| REG-03 | P1 | Attach classlist > cloud files; reconnect Google; search a private sheet, load more results, select it and import. Repeat local .xlsx. | Correct connected account, file and term; both sources use identical validation. | NOT RUN |
| REG-04 | P0 | Try student credentials through staff login and each staff role through student login, with password and Google. | No tokens issued through wrong portal; no staff grant or new account from unregistered staff Google sign-in. | NOT RUN |
| REG-05 | P0 | Use the correct portal for all roles, including first-time pre-created staff Google account. | Correct workspace; enrollment linking still works for students. | NOT RUN |
| REG-06 | P1 | Assign adviser A, repeat save, reassign B before queued delivery. | No repeat assignment notice; A's stale queued email cancelled; B sees its own group. | NOT RUN |
| REG-07 | P1 | Enable controlled automation; run twice after 08:00; refresh analytics repeatedly. | One daily digest per coordinator/adviser with scoped counts; no refresh or sign-in email. | NOT RUN |
| REG-08 | P0 | Reassign group before adviser digest delivery; inspect recipients and content. | Former adviser cannot receive private counts/details for removed group. | NOT RUN |
| REG-09 | P1 | Submit and replace a document; inspect adviser in-app notices and next daily digest; repeat student deadline reminder cases. | Correct assigned adviser notified; versions counted as one updated record in digest; student reminder deduplication preserved. | NOT RUN |

Do not mark Gmail or automation accepted until production sender consent, environment flag and external scheduler have been tested together with controlled recipients.

### Calendar and analytics follow-up

| ID | Priority | Steps | Expected result | Production status |
| --- | --- | --- | --- | --- |
| REG-10 | P1 | Assign three distinct deliverables with identical deadlines to one linked student. Repeat in the coordinator calendar with requirements across both subjects. Open that month, select the day, check subject filters, sync twice and inspect each connected Google Calendar. | All requirements in the selected scope appear separately in the date cell and agenda; Google contains separate stable events, without duplicates. Coordinator Google sync includes all active subjects regardless of page filters. | NOT RUN |
| REG-11 | P1 | Keep email delivery disabled, connect Calendar once, change a deadline and wait for the production worker. | Calendar updates automatically without email delivery or manual sync. Confirm the external scheduler reaches Render during sleep. | NOT RUN |
| REG-12 | P1 | Open analytics, switch IT332/IT411, choose Groups and drill into individual records. | Subject, term, group and requirement filters remain consistent; incompatible prior group filters clear on subject change. | NOT RUN |
| REG-13 | P1 | Export IT332 and IT411 separately, then repeat a section export. | Separate subject/term workbooks; repeated identical scope updates its snapshot. | NOT RUN |

Calendar automation is controlled by `CALENDAR_AUTOMATION_ENABLED`, independently of `DEADLINE_AUTOMATION_ENABLED`. Production defaults to calendar automation on; local MySQL defaults off. Worker/scheduler tests still require controlled calendars and actual deployment verification.

### Enrollment correction, import chooser and Google setup

| ID | Priority | Steps | Expected result | Production status |
| --- | --- | --- | --- | --- |
| REG-14 | P1 | Move an unused mistaken IT411 or legacy IT322 enrollment to an active IT332 section; inspect destination choices across terms. Repeat with an existing target-subject enrollment and with group/progression history. | Approved active subjects/sections are available, including G01 and G1 separately. Duplicate destination enrollment is rejected. A subject/term change with history is blocked with guidance to add a separate enrollment. Archived terms remain protected. | NOT RUN |
| REG-15 | P1 | Press Import classlist; choose device, cancel, reopen and choose Google Drive. Select a sheet and import. | A compact chooser opens first; device selection uses the OS file picker, cloud selection uses a modal. No permanent expanded source menu. Selection alone never imports records. | NOT RUN |
| REG-16 | P0 | Sign in as student, coordinator and admin without Google tools connected. Approve the setup offer once; repeat login. Decline in another controlled profile. | Student connects Calendar; coordinator connects Calendar/imports/reports; admin connects imports/reports. Gmail sender is never included. Connected users are not prompted again. Deferring leaves core login usable; feature-specific connection remains available. | NOT RUN |
| REG-17 | P0 | Start coordinator tool consent, change the disposable account role before callback, then complete. | Completion rejects the stale role bundle; no staff integration is installed for the downgraded account. | NOT RUN |

Google setup is an optional offer after authentication, followed by Google's consent screen. The browser remembers a deferred offer per account and role. Reconnection can still be required after revoked permissions or expired provider access. This does not enable Gmail delivery or provision the Render scheduler.

### Welcome emails

With Gmail connected, email automation enabled and the worker configured, newly imported enrollments queue one welcome per enrollment. Local Excel and Google Sheets share this flow. The recipient is the imported email, including students who have not signed in yet. Existing records are not bulk-emailed when enabling automation.

| ID | Priority | Steps | Expected result | Production status |
| --- | --- | --- | --- | --- |
| REG-18 | P1 | Import controlled students, including one without an account, via local and cloud sources. Process the queue, repeat the import and process again. | Each new enrollment receives its subject, section, term and production student-login link. Duplicate/skipped rows produce no repeated welcome. | NOT RUN |
| REG-19 | P1 | Before welcome delivery, remove the enrollment, move it to a different section or change its email; also test an archived term. | The obsolete queued welcome is cancelled. No stale enrollment details are delivered. | NOT RUN |
| REG-20 | P1 | Create an adviser, assign a group, process the queue, and repeat the same assignment. Reassign a group before delivery in a separate case. | Account welcome and assignment notice contain the staff-login link, never a password. Repeated unchanged assignment does not resend; former adviser does not receive stale assignment details. | NOT RUN |
| REG-21 | P1 | Disable email automation, import a controlled enrollment and create an adviser, then re-enable. | Those disabled-period actions do not generate retroactive welcome emails. Calendar automation remains independent. | NOT RUN |
