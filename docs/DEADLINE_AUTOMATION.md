# Deadline automation on Vercel and Render Free

Automation is disabled by default. Enable it on ONE backend only. Keep it disabled locally when local and production share a database.

## Render environment

- `DEADLINE_AUTOMATION_ENABLED=true`
- `GOOGLE_DEADLINE_SENDER_EMAIL=intelli.track0@gmail.com`: the designated IntelliTrack Gmail address. This is also the application default when the variable is absent. An explicitly empty deployment variable must be removed or set to this address.
- `DEADLINE_AUTOMATION_TRIGGER_SECRET`: a randomly generated secret of at least 32 characters.
- Keep the existing Google OAuth client, production callback URI and stable token encryption key configured.

Enable Gmail API in the same Google project. Add `https://www.googleapis.com/auth/gmail.send` to the consent configuration and add the sender as a test user if the consent app is in testing. In IntelliTrack, sign in as administrator, open System configuration, and Connect Gmail sender. Grant consent using the exact configured address. Enabling the API alone does not authorize sending.

Students and coordinators connect Calendar once using their existing calendar page. The worker reconciles their full set of active assigned deadlines, including changes, enrollment removal and semester archival. It uses the existing app-owned calendar and stable event IDs. Manual sync remains available. Adviser and administrator calendars are not supported by the current calendar connection flow.

## Free Render scheduling

Render Free sleeps after inactivity. Its in-process timer cannot run while asleep. Configure an external scheduler to POST every five minutes to:

`https://YOUR-BACKEND.onrender.com/api/automation/deadlines/run`

Include header `X-Automation-Key` with the secret. Do not put the secret in a URL, frontend code or repository. Allow for cold starts and retries. The worker also runs every five minutes while awake. A run processes up to five emails and three calendars; large backlogs take additional runs. Reminders are eventual, not an exact-time delivery guarantee. Consider an always-on worker when volume grows.

## Behavior and QA

- New, updated and archived requirements queue emails to currently linked enrolled students only when automation is enabled at the time of the change. Enabling automation does not label old requirements as new.
- Unsubmitted active requirements get a reminder within 24 hours, within one hour, and after becoming overdue. Only the currently applicable window is queued; each window is sent once per requirement revision. Existing overdue requirements can produce one overdue email when enabled.
- Before delivery, the worker rechecks assignment, revision, semester and submission status. Stale messages are cancelled.
- A delivery error becomes REVIEW because Google may have accepted the email before a network timeout. It is not automatically retried. Check the sender Sent folder; the admin screen shows review counts. There is no manual resend UI yet.
- Sender and Calendar refresh tokens stay encrypted on the backend. Revoked/expired consent requires reconnection. Calendar failures retry later.
- This feature does not change the existing SMTP-based password reset email service.

Test with your own linked student account first: new deadline, extension, submission before reminder, requirement archival, semester archival and revoked consent. Check Calendar for updates without duplicates and Gmail Sent for deliveries. Never enable local and production automation against the same database. No external scheduler or live email delivery is provisioned by these code changes.

References: https://render.com/docs/free and https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/send
