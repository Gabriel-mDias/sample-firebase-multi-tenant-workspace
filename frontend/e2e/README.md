# T029 integration against the real Firebase test project

`FirebaseEndToEndIntegrationTest` runs the real backend (`AppApplication`), a disposable PostgreSQL 16
from Testcontainers and the Angular app in Chromium against the shared Firebase test project
`sample-firebase-multi-tenant`. The Auth Emulator is not used here; the Compose/emulator in
`environments/` stays an optional demo/offline setup.

## Running locally

1. Use JDK 21 with Docker active and the published SDK repository configured externally.
2. In `frontend`, run `npm ci` and `npx playwright install chromium`.
3. Point ADC at the service account JSON of the test project, kept outside the checkout. Never copy
   its path, contents or e-mail into the repository, logs, commits or issues:

   ```powershell
   $env:GOOGLE_APPLICATION_CREDENTIALS = '<path outside the repository>'
   Remove-Item Env:FIREBASE_AUTH_EMULATOR_HOST -ErrorAction SilentlyContinue
   ```

4. From `backend`:

   ```powershell
   .\mvnw.cmd -pl app-deploy -am '-Dtest=FirebaseRealTestPreflightTest,FirebaseEndToEndIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false test
   ```

Keep port 4301 free; Playwright starts the real Angular app there. The test invokes Playwright with
`node node_modules/@playwright/test/cli.js test`, so it needs no shell wrapper and works on Windows
and Linux.

The public Web app configuration (`apiKey`, `authDomain`, `appId`) is read at runtime from the Firebase
management API with ADC and handed only to the browser run; nothing is versioned. If the ADC cannot
list Web apps, set `T029_FIREBASE_API_KEY`, `T029_FIREBASE_AUTH_DOMAIN` and `T029_FIREBASE_APP_ID`
(optionally `T029_FIREBASE_APP_ID` alone to choose the app). The automation never registers an app.

## Preflight, namespace and cleanup

- The preflight is fail-closed and runs before Docker, the network and any mutation. It fails with a
  clear message when `GOOGLE_APPLICATION_CREDENTIALS` is absent or unreadable, when its `project_id`
  is not `sample-firebase-multi-tenant`, when `FIREBASE_AUTH_EMULATOR_HOST` is set, or unless the IAM
  `testIamPermissions` check positively confirms every `firebaseauth.users.*` permission used (a disabled IAM API,
  a transport error or a partial grant also refuse the run before any mutation). Messages never print a path, key, token or service account.
- Every execution has a unique run ID (a UUID fragment, plus `GITHUB_RUN_ID`/`GITHUB_RUN_ATTEMPT`
  when present). Synthetic accounts use the reserved prefix `gems-t029-<run>-<case>` for e-mail
  (`@example.com`) and UID. The SDK plans a random UID, so the harness pins it to the namespace UID before
  any command runs and the test asserts the format of the account actually created. Passwords are random and
  live only in memory and the process environment; Playwright sets them through `locator.evaluate` so
  they never appear as an action argument in a call log.
- Created UIDs are registered and deleted in `finally`; the run then asserts that its namespace is
  empty. A sweep before the suite deletes only automation-prefixed accounts older than 2 hours from
  other runs whose UID and e-mail both carry the prefix, never anything outside it.
- Only synthetic accounts of the namespace are created or deleted. No other project setting changes.

## Quotas and serialization

The suite is serialized (`workers: 1`, sequential Java steps) and never retries with backoff. A 429 or
`RESOURCE_EXHAUSTED` fails the run explicitly (`Quota do Firebase excedida`); wait and rerun. The only
bounded wait is a read-after-write poll of 5 seconds after a remote revoke, disable or delete, and it
never retries a 503. Run the full suite sparingly: each run creates and deletes about 12 accounts.

## Pull requests from forks

Secrets are not available to fork pull requests. CI sets `T029_FIREBASE_REAL=skip` there: the test is
ignored through a JUnit assumption and the log carries an explicit `T029 AVISO` line. This is not a
PASS. On the main repository, a missing secret must fail the job. Any value other than `skip` keeps the
fail-closed preflight.

## Provider outage

The 503 scenario never touches the shared project. A second local backend instance gets an Admin app
whose HTTP transport points to an unreachable loopback proxy. The test proves a `503` with the generic
`{"code":"PROVIDER_UNAVAILABLE"}` body and no leakage, the durable local block while the remote command
is in `RETRY`, and the recovery once the real provider path is used again.

## Out of automation

Google OAuth (popup and first-link race) and password reset e-mail delivery need a human; follow
`specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md`. Deterministic tests with a
fake verifier still cover the Google provider and denials. The synthetic multi-company user logs in
with e-mail and password in these scenarios.

## What the suite covers

HTTP and browser checks run without replacing the application, verifier, administration gateway,
authorization resolver or persistence. ID tokens are real; the backend verifies them with
`checkRevoked`, and real Admin revoke, disable and delete must make it answer 401. Delayed responses
fetch the actual endpoint before release, and token races delay the native Firebase user's
`getIdToken`. The Beta transport failure case is explicitly simulated.

The native child uses one real servlet worker thread. A test-only outer filter poisons incoming
tenant/security/actor state and checks cleanup after successful, denied and invalid-UUID requests. The
Admin CREATE race forwards to the real Admin adapter, pauses before returning, and advances the
persisted version with an adversarial SQL writer; the real worker must reject its stale ACK.

The parent JVM snapshots `role.guard.ts` and asserts it is unchanged after the child finishes.
Traces, video, credential screenshots and raw request logging are disabled; the reporter emits
sanitized scenario results. Disposable Playwright metadata uses `.angular/t029-playwright`, which is
excluded from formatting and source checks.
