# Bulletin Microsoft SSO Implementation Roadmap

Snapshot basis: the analysis dated 2026-10-17. The roadmap below is retained as the original design record. The implementation record here supersedes conflicting eligibility and v1 scope statements in that roadmap.

## Implementation record — 2026-10-17

### MVP policy agreed during implementation

- Microsoft sign-in accepts **any Microsoft account with a verified email**, including personal accounts and non-`.edu` emails, through the `common` endpoint. Password registration continues to require `.edu`.
- Microsoft is additive. Password sign-in, signup, confirmation, and recovery remain available.
- Reject missing or unverified Microsoft emails. There is no extra email-code fallback for this flow.
- No linking or unlinking UI in v1. A server trigger rejects Azure identity insertion into an account with another provider or password, and rejects adding another provider to a Microsoft account. An app check alone would not prevent automatic linking.
- Microsoft display name is presentation data, with `Student` as fallback. University starts empty and may be edited by the user; it is never proof of campus membership.
- Microsoft email changes must match a verified attached identity. Password email changes must remain `.edu`. Existing emails are not retroactively rewritten. Soft-deleted profile emails and aliases remain reserved.
- V1 uses Supabase's normal session and existing RLS at `aal1`. It adds no Supabase MFA challenge or requirement. Microsoft-side MFA does not establish Supabase `aal2`.
- Sign-out clears the Supabase session and account profile cache. The Microsoft browser session remains; each new OAuth request asks Microsoft to show its account picker.

### Implemented in the repository

| Area | Implementation | Evidence / limitation |
| --- | --- | --- |
| Profile domain/application | Provider-neutral external-auth port, typed outcomes, verified-identity policy, application use case | Deterministic adapter and ViewModel tests; in-memory auth explicitly returns unavailable |
| Supabase adapter | Azure OAuth, `email` scope, PKCE, `prompt=select_account`; fetch server-owned identity data before importing the session | Pinned SDK 3.1.4 sources inspected; test asserts request parameters and code exchange |
| Profile/session | Same profile read/recovery path as password sign-in; rejects mixed identities and deleted profiles; strips provider credentials before persisting | Fake HTTP tests cover failure, cancellation, profile retry, missing/unverified email and collisions |
| Android callback | Exact `com.jdrms.bulletin.auth://oauth/callback`, persisted pending request and PKCE verifier, 5-minute expiry, one-time callback consumption, cold-start resume through the sign-in ViewModel | Portable tests cover unsolicited, replayed, expired, malformed and ambiguous callbacks; actual device lifecycle matrix remains pending |
| Android storage | AES-GCM with Android Keystore; project-specific private preferences; migrate prior SDK session and erase legacy plaintext session/verifier; backups disabled | Compiles; Keystore migration and restart behavior still need device verification |
| UI | Working Microsoft action, disabled during progress, cancellation copy without an error banner, fixed safe failure messages | ViewModel tests cover shared session, repeated taps and every typed rejection |
| Flag | `MICROSOFT_SSO_ENABLED=false` by default; read using existing Gradle configuration inputs | Build-time switch requires a rebuilt release for UI rollback; provider disablement is the server-side switch |
| Backend | Exported public/private DDL, auth triggers, storage policies/configuration and Realtime membership; new verified identity/collision/email-change rules; paired policy rollback | Fresh isolated local database and SQL tests; **not applied to the live project** |

The sign-in UI uses `MaterialTheme` colors, typography and shapes, including `surface`, `onSurface`, `outlineVariant`, `errorContainer` and `onErrorContainer`. No theme tokens were added. Profile and seller badges now use confirmed verification status instead of inferring it from `.edu`; university cards explicitly label affiliation as user-provided.

The SDK defaults to implicit flow and unencrypted Settings persistence; neither default is relied on by Android now. SDK 3.1.4 writes the PKCE verifier asynchronously, so the adapter waits for durable verifier storage before opening the browser. A returning cold-start flow preserves that verifier instead of generating another request. Callback tokens and URLs are never logged. The SDK response can contain provider token fields transiently; they are discarded before importing or storing the Supabase session. The app does not call Microsoft Graph.

### Observed live configuration (read-only inspection)

Project: `qullzprtorshyqkxuvac`. No live SQL, provider settings or Auth settings were changed.

- `before_user_created` is enabled and points to `public.enforce_edu_email_signup`; both the hook and insert trigger currently enforce `.edu` for all new accounts.
- Site URL is `http://localhost:3000`; current redirect allow-list contains `bulletin://auth/callback`, which does **not** match the new Android return URI.
- Manual identity linking is disabled. [Supabase documents automatic OAuth linking by email](https://supabase.com/docs/guides/auth/auth-identity-linking) separately; the new `auth.identities` trigger is required to reject it for v1.
- TOTP enrollment and verification are enabled, phone MFA is disabled; no existing RLS policy mentions AAL. This does not demonstrate that any users have MFA enrolled or that Auth requires it.
- Entra registration, credentials, `xms_edov` behavior, issuer/audience enforcement in the deployed Auth version, Azure provider enablement, real collisions and refresh/revocation behavior remain unverified. CLI config pull does not establish all provider settings.
- Remote migration history contains `20261013040619` and `20261013050000`, whose original files are absent from this checkout. The new snapshot must be reconciled with that history before a remote `db push`; do not replay the baseline over existing tables or delete history to bypass the mismatch.

### Migration files and local validation

1. `supabase/migrations/20261017000000_remote_schema_baseline.sql`: schema-only live export of public/private objects, plus custom auth triggers and storage policies omitted by the normal dump.
2. `supabase/migrations/20261017000001_storage_and_realtime_baseline.sql`: existing bucket definitions and Realtime publication membership; contains no user objects or message rows.
3. `supabase/migrations/20261017000100_microsoft_identity_policy.sql`: MVP provider eligibility, verified identity guard, automatic-link rejection, email-change protection and Microsoft profile defaults.
4. `supabase/rollbacks/20261017000100_microsoft_identity_policy.sql`: restores the original hook/signup/profile functions and removes the two new guards. The schema baseline is retained.
5. `supabase/tests/microsoft_identity_policy.sql`: transactional pgTAP fixtures, including password/Microsoft message RLS parity. Run only against local or staging environments.

For a fresh local project:

```bash
supabase start
supabase test db
```

The local config leaves Azure disabled. Enabling OAuth requires separate local/staging credentials; do not put them in source control. Normal Kotlin tests make no live OAuth calls.

### Checks completed

- `./gradlew :shared:testAndroidHostTest --no-daemon`: 651 tests, zero failures/errors/skips in the latest shared host-test report.
- `./gradlew :shared:detekt :androidApp:detekt --no-daemon`: passed after formatting fixes.
- `./gradlew check :androidApp:assembleDebug --no-daemon`: passed, including Android lint.
- Fresh isolated `supabase start` / `db reset`: all three migrations applied successfully.
- `supabase test db`: 20 checks passed, including after rollback/reapplication and after rebuilding from all migrations.
- Policy rollback executed in the isolated database; original hook behavior and absence of the new guards were checked before reapplication.
- Callback URI consistency checked across manifest, SDK, browser boundary and local Supabase configuration.
- `git diff --check`: passed. No production writes or real OAuth/device tests were performed.

### Enablement procedure and remaining gates

1. Create a separate staging Supabase project and Entra registration. Register the Entra app for organizational **and personal** accounts and configure Supabase's Azure Tenant URL as `https://login.microsoftonline.com/common`.
2. Apply the repository migrations to the fresh staging database. For an existing project, first reconcile its schema and migration history against the exported baseline. Preserve existing history and review the diff; the baseline is not an upgrade script.
3. In Entra, register `https://<staging-project>.supabase.co/auth/v1/callback` as the **Web** return URL. Configure the optional `email` and `xms_edov` ID token claims as described in the [Supabase Azure provider documentation](https://supabase.com/docs/guides/auth/social-login/auth-azure). Keep the client secret only in Supabase/server environment configuration. Assign an app/secret owner and record expiry/rotation dates in the team's credential system.
4. Enable Azure in staging Supabase with its client ID and secret. Add the exact app URI `com.jdrms.bulletin.auth://oauth/callback` to Auth's redirect allow-list. A custom scheme is used because there is no app-owned HTTPS domain/verified App Link configuration in the repository.
5. Build Android with `MICROSOFT_SSO_ENABLED=true` only against that prepared environment. Verify a personal account, an organizational account, missing/unverified email, existing-password email and alias collisions. Verify that `identity_data.email_verified` reflects the intended Microsoft verification signal; a configured claim and a real negative case are required. A default/fallback provider verification value is not proof that `xms_edov` was supplied.
6. Run the device matrix: back/close browser, offline, double tap, rotate, process killed during browser step, cold callback, restart/refresh, profile retry, legacy session migration and sign-out/restart. Portable tests do not close these device checks.
7. Before production cohorts, reconcile live migrations, apply/review the policy delta, verify actual Auth/RLS parity, set production credentials and redirect, and establish outcome monitoring/alerts. There is no new analytics backend or dashboard in this change.
8. Roll back UI by rebuilding with the flag false; disable Azure server-side if immediate new-sign-in shutdown is needed. Rehearse these provider/session behaviors in staging. Disabling the provider does not invalidate already-issued access tokens. The existing password-recovery flow is the proposed escape path for Microsoft-only users and must be verified with a real account before rollout. The no-link policy rejects later Microsoft identity updates once an account has a password; recovery therefore transitions that account to password access under v1 policy.

The policy rollback was applied and its previous hook behavior checked on the isolated local database, then the policy migration was reapplied and all SQL checks passed again. It was not applied to production. Production rollout, real provider sign-in, dashboards, secret ownership and device signoff remain outstanding; the original roadmap's full definition of done is **not** claimed.

**Labels used throughout**

- **\[Confirmed\]**: stated in the analysis.
- **\[Verify\]**: an assumption that must be tested before it is relied on.
- **\[Decision\]**: an open product or engineering choice the team must make.

## 0. Framing

### Confirmed starting point

- **\[Confirmed\]** Bulletin is a Kotlin Multiplatform codebase whose only current client is Android. Shared auth, business rules and Compose UI live under `shared`.
- **\[Confirmed\]** `AuthRepository` is bound to `SupabaseAuthRepository` in `AppContainer` (manual DI) when the Supabase client is configured. Supabase Kotlin is `3.1.4`, with Auth, PostgREST, Realtime and Storage installed.
- **\[Confirmed\]** The sign-in screen is email/password only. The `AuthRepository` port has password login, registration, email-code verification, password recovery and sign-out. It has **no** OAuth/SSO or callback operation.
- **\[Confirmed\]** `getCurrentUserId()` and `getCurrentUser()` restore a user only when `emailConfirmedAt != null`. Profiles are created from the confirmed Supabase user when absent, reading `full_name` and `university` from auth metadata opportunistically.
- **\[Confirmed\]** The Android manifest has only the launcher intent filter, with no OAuth callback.
- **\[Confirmed\]** The live Bulletin project (`qullzprtorshyqkxuvac`) has a `BEFORE INSERT` trigger on `auth.users` calling `public.enforce_signup_rules()`. It rejects emails failing `public.is_valid_edu_email()` and emails that normalize to an active profile email. It does not fire on update or on each sign-in.
- **\[Confirmed\]** `public.enforce_edu_email_signup(jsonb)` exists, but its registration as a live Auth hook is unverified.
- **\[Confirmed\]** There is no `supabase/migrations/` directory and no `supabase/config.toml` in the checkout, so backend state is not reproducible from the repo.
- **\[Confirmed\]** The `.edu` rule means suffix eligibility only. It is not campus membership. The six-digit email code proves mailbox possession. Microsoft OAuth can replace that step only if Supabase receives a trusted, verified email.
- **\[Confirmed\]** There is no app-level MFA/AAL handling. OAuth sign-in is `aal1` by itself in Supabase.
- **\[Confirmed\]** Provider settings, redirect allow-list, `xms_edov` configuration, Entra app and MFA dashboard settings were not inspected.

### Design stance

1. **Supabase Auth is the OAuth broker and the single source of auth truth.** The app holds only a Supabase session. It never holds Microsoft tokens, never calls Microsoft Graph and never sees the Entra client secret.
2. **Microsoft-specific code lives in one place:** the infrastructure adapter (`SupabaseAuthRepository` or a sibling in profile infrastructure), plus the Android callback wiring. Domain, application and UI layers see a provider-neutral "start external sign-in / result" contract and never see Supabase SDK types.
3. **Provider identities live in Supabase's identity records** attached to the auth user (the analysis says Supabase "records an OAuth identity"). Bulletin adds no parallel identity table and no second session store, unless Phase 5 proves it needs one **\[Decision\]**.
4. **Authorization stays server-side.** Successful SSO yields a normal Supabase session. RLS and server checks are unchanged and must not be loosened for SSO users.
5. **Password login is not touched** until the exit gate in Phase 8.

## Phase 0: Baseline, backend reconciliation and decisions

### Goals and scope

Make backend state reproducible, and settle the decisions that shape every later phase. No user-facing change.

### Architecture and data flow

None changes. This phase exists because the live trigger and helper function are "remote-only" and cannot be tested or rolled back from the repo.

### Tasks

- **Backend:** export the live `tr_enforce_signup_rules` trigger, `enforce_signup_rules()`, `is_valid_edu_email()`, `enforce_edu_email_signup(jsonb)`, and the profile unique indexes into versioned `supabase/migrations/`. Add `supabase/config.toml` without secrets. Reconcile the README, which describes a migrations directory that does not exist.
- **Backend \[Verify\]:** from the Supabase Dashboard, record whether the Auth hook is registered, the current Site URL and redirect allow-list, whether MFA is enabled or required, whether any RLS policy checks AAL, and the current account-linking behavior.
- **Team:** resolve the blocking decisions listed in "Key decisions still needed" (tenant scope, Microsoft-only vs additive, university derivation, MFA stance).
- **Test environment:** create a non-production Supabase project and a test Entra tenant, mirrored from the reconciled migrations.

### Security and privacy

Do not copy user rows, secrets or provider credentials into the repo. Migrations contain DDL only.

### Dependencies and sequencing

First in the sequence. Phases 1 and 2 depend on a reproducible staging backend.

### Acceptance criteria and tests

- A fresh staging project built only from repo migrations reproduces the trigger behavior: a non-`.edu` insert into `auth.users` fails; a duplicate active-profile email fails.
- A written record of the dashboard settings above exists and is reviewed.

### Risks and rollback

Migration drift against production. Diff the migration output against a fresh schema dump before applying anything to production. Phase 0 changes nothing in production.

## Phase 1: Provider selection, configuration and environments

### Goals and scope

Enable Microsoft as the only new provider through Supabase's Azure (Microsoft) provider, in staging first.

### Architecture and data flow

Android app → system browser → Microsoft (Entra) → Supabase Auth callback → return URI to app → Supabase client completes the session. Supabase holds the client secret and performs the code exchange. **\[Verify\]** that the pinned Kotlin SDK `3.1.4` performs PKCE and state handling on Android as the analysis assumes ("provided by the auth SDK").

### Tasks

- **Provider (Entra):** register the app. Apply the tenant/account-type decision. Add the Supabase callback URL as a redirect URI. Add the optional `xms_edov` claim.
- **Supabase:** enable the Azure provider with client ID and secret, set the Azure Tenant URL, and add the app return URI(s) to the redirect allow-list. Request the `email` scope (required by Supabase).
- **Environments:** separate Entra apps and Supabase projects for staging and production. Use separate client secrets. Record a secret rotation owner and date.
- **App:** no secret goes into app config or `.env.example`. Only non-secret redirect values, if any.

### Security and privacy

- Client secret exists only in Supabase configuration.
- Request minimal scopes. The analysis says to avoid unnecessary scopes.
- Redirect URIs must match exactly across Entra, Supabase and the Android return URI. Prefer a verified HTTPS App Link; if using a custom scheme, scope it narrowly **\[Decision\]**.
- **Issuer/audience:** Supabase, as broker, validates the Microsoft token. **\[Verify\]** that the Tenant URL setting pins the issuer to the intended tenant rather than a multi-tenant or common endpoint, and that the audience is the Bulletin Entra app. The analysis does not describe this behavior in detail, so confirm it from Supabase's current provider documentation and by test.

### Dependencies and sequencing

Needs Phase 0 staging project and the tenant-scope decision.

### Acceptance criteria and tests

- Staging: a manual browser OAuth round trip with a test `.edu` account in the intended tenant creates a Supabase auth user with an identity record.
- `xms_edov` is present in the issued token for the intended account types, and Supabase's resulting `email_confirmed_at` is observed for both a verified and an unverified case **\[Verify\]**.
- A personal Microsoft account and an account from a different tenant are tested against the chosen tenant scope, and the results recorded.

### Risks and rollback

Entra email claim behavior varies by tenant/app configuration. Rollback is to disable the Azure provider in Supabase. Existing password users are unaffected.

## Phase 2: Backend eligibility and identity policy

### Goals and scope

Make the server the security boundary for who may hold an account, and close the gaps the analysis found around SSO.

### Architecture and data flow

First-time OAuth sign-in inserts into `auth.users`. The existing `BEFORE INSERT` trigger runs and either admits or rejects it. Everything after that is a normal Supabase session governed by RLS.

### Tasks

- **Keep** the insert trigger as the `.edu` guard. The client-side `StudentEmail.isUniversityEmail` is not a security boundary.
- **Tenant scope \[Decision\]:** if one campus is intended, enforce it server-side through the Entra tenant configuration and, if needed, an explicit server-side allow-list or issuer check. `.edu` alone does not establish tenant membership.
- **Email-change gap:** the insert trigger does not cover updates. Decide whether `.edu` must remain true after creation. If yes, add server-side enforcement for auth email changes and verify whether the helper Auth hook is registered and covers those events **\[Verify\]**.
- **Verified-email requirement:** gate app access on Supabase returning a confirmed email derived from a verified Microsoft claim. If the verification signal is missing or false, fail closed or fall back to the existing six-digit code path **\[Decision\]**. Do not rely on a `.edu` suffix or a populated `email_confirmed_at` alone.
- **Existing-profile collision \[Verify\]:** the trigger rejects an email that normalizes to an active profile email. Test what a Microsoft sign-in with an email matching an existing password profile does: either the trigger rejects the insert, or Supabase links to the existing user. The analysis says linking behavior depends on provider and account state. The outcome must be known before Phase 5.
- **MFA \[Decision\]:** state explicitly whether Supabase `aal2` is required for anyone. The analysis says Microsoft MFA is not automatically Supabase `aal2`. If the product requires a second factor, that needs a challenge flow in the app and AAL checks in RLS. This is out of scope for v1 unless decided otherwise, and the omission must be documented.
- **Migrations:** add every change as a checked-in migration with a rollback script.

### Security and privacy

- Never use client-supplied metadata (`full_name`, `university`) as proof of eligibility or ownership.
- Rejected sign-ins must leave no usable session and no orphan profile.
- Soft-deleted profiles must not be reactivated implicitly by SSO **\[Decision\]**.

### Dependencies and sequencing

Depends on Phase 1 test results. Gates Phase 3's production exposure.

### Acceptance criteria and tests

SQL-level tests against staging:

- Non-`.edu` Microsoft user is rejected at insert.
- `.edu` user in a wrong tenant is rejected, if tenant scope is enforced.
- Unverified-email user is rejected or routed to fallback verification.
- Email change to a non-`.edu` address is rejected, if the decision requires it.
- Soft-deleted and duplicate-email cases behave as decided.

### Risks and rollback

Tightening rules could lock out legitimate existing users. Make new rules apply to inserts and email changes only, not to every sign-in. Roll back with the paired down-migration.

## Phase 3: Android sign-in, cancellation and failure states

### Goals and scope

Add the Microsoft sign-in action to the existing sign-in experience, behind a feature flag, without changing password login.

### Architecture and data flow

```
SignInScreen (Microsoft button)
  → SignInViewModel
  → application use case (new: external sign-in)
  → AuthRepository port (new operation + result type)
  → Supabase adapter (Supabase Kotlin Auth OAuth, Azure provider)
  → system browser → Microsoft → Supabase → return URI
  → Android callback → Supabase client completes session
  → same confirmed-user + profile load path as password login
```

The new port operation returns a provider-neutral result: signed in, cancelled, or failed with a typed reason. No Supabase or Microsoft types cross the port.

### Tasks

- **Presentation:** add the Microsoft action to `SignInScreen.kt` and state to `SignInViewModel`: idle, in-progress, cancelled, error. Disable the button while in progress.
- **Application/domain:** define the port operation and result model. Cancellation is a distinct outcome, not an error. Security rejections (ineligible, unverified, wrong tenant) are distinct from technical failures so the UI and telemetry can tell them apart.
- **Infrastructure:** implement with the Supabase Kotlin Auth SDK. **\[Verify\]** the exact API and Android requirements for `3.1.4` before coding. This includes how the browser launch and callback delivery work.
- **Android entry:** add the callback intent filter to the manifest and deliver the callback to the initialized Supabase client. Handle process death and a returning-app cold start **\[Verify\]**.
- **Composition:** wire any platform launcher or callback boundary in `AppContainer` or the Android composition boundary, consistent with current manual DI.
- **Flag:** hide the Microsoft button unless the flag is on **\[Decision: flag mechanism\]**; the analysis does not describe an existing flag system.

### Security and privacy

- Never log authorization codes, tokens, callback URLs or query values. Log only coarse outcome categories.
- Reject a callback that does not correspond to an in-flight request initiated by the app. **\[Verify\]** the SDK enforces state/PKCE matching.
- Do not display raw provider error text that could include identifiers; map to fixed messages.
- User-facing copy must distinguish "you cancelled" from "this account is not eligible".

### Dependencies and sequencing

Depends on Phase 1 (working provider in staging). Production exposure is gated by Phase 2.

### Acceptance criteria and tests

- Unit tests with a deterministic fake `AuthRepository`: success, cancel, ineligible, unverified email, missing email, network failure, expired or replayed callback.
- ViewModel tests: state transitions, button disabled while in progress, cancellation returns to idle with no error banner.
- Manual device tests: browser back/close, app killed during browser step, airplane mode, second tap during progress.
- Password login tests pass unchanged.

### Risks and rollback

Callback misrouting on Android. Turn the flag off to hide the button; the manifest filter is inert without an initiated flow.

## Phase 4: Token validation and Supabase session handling

### Goals and scope

Ensure SSO users enter the same session model as password users, with no duplicated auth state.

### Architecture and data flow

The only session is the Supabase session. After callback, the adapter resolves the Supabase user and applies the existing `emailConfirmedAt != null` restoration rule. The profile is loaded or created through the existing path. App code must not hold a parallel "is signed in via Microsoft" flag; if it needs the provider, it reads it from the Supabase user's identities.

### Tasks

- Route SSO success through the same code that establishes the authenticated profile/session after password login and session restore.
- Profile creation must be idempotent. Define population of `full_name` (from Microsoft claims, or prompted) and `university` (derived from a trusted tenant/domain mapping, or user input) **\[Decision\]**. Today `university` is read from metadata opportunistically, which is not trustworthy for SSO.
- Decide handling of a missing display name or changed claims on later sign-ins.
- Preserve soft-delete handling per the analysis.

### Security and privacy (token validation specifics)

- **Provider tokens:** validated by Supabase as broker (signature, issuer, audience, expiry). The app does not validate Microsoft tokens itself and should not request them. **\[Verify\]** against Supabase documentation and staging tests; the analysis does not detail these checks.
- **Supabase JWT:** validated by Supabase services (PostgREST, Realtime, Storage) and used by RLS. Expiry and refresh are handled by the Supabase client; do not add custom token parsing in the app.
- **Replay:** rely on PKCE and state for the authorization code. **\[Verify\]** the SDK enables them for this flow. Do not add custom replay logic.
- **Credential storage:** the Supabase SDK stores the session. **\[Verify\]** where and how on Android (encrypted or not) and decide whether that meets policy **\[Decision\]**. Do not store tokens in app preferences, logs or crash reports.
- **Authorization:** RLS is unchanged. Add a test proving an SSO session gets exactly the same data access as a password session for the same profile.

### Dependencies and sequencing

Depends on Phases 2 and 3.

### Acceptance criteria and tests

- SSO and password sessions are indistinguishable to the app after sign-in aside from the identity record.
- Restart restores the SSO session; an expired session refreshes or signs out cleanly.
- Second sign-in does not create a second profile.
- RLS test: an SSO user cannot read or write another user's rows.

### Risks and rollback

Half-created state (auth user exists, profile creation fails). Make profile creation retryable on next restore. Rollback is the flag plus provider disablement; existing SSO-created users would then need a defined path, see Phase 8.

## Phase 5: Mapping identities and account linking

### Goals and scope

Define exactly how a Microsoft identity maps to a Bulletin account, with no implicit linking.

### Architecture and data flow

One Bulletin profile corresponds to one Supabase auth user. A provider identity is an attached record on that auth user (Supabase-owned). Bulletin does not mirror it into its own table unless a concrete need is shown **\[Decision\]**.

### Tasks

- **Identity key:** the provider subject recorded by Supabase is the identity, never the email string. Email is an attribute used for eligibility and display only.
- **First sign-in, no existing account:** create a new auth user and profile (Phases 2 and 4).
- **Email matches an existing password account:** do not link by email. **\[Verify\]** Supabase's actual behavior (reject via trigger, auto-link, or create a second user) and if auto-linking can occur, disable or neutralize it. The analysis explicitly warns not to assume safe or consistent linking. The user-facing outcome must be a clear message directing them to sign in with their existing method and link from settings.
- **Explicit linking flow:** an already-signed-in user (re-authenticated with their current method) starts "Link Microsoft" from settings. Microsoft returns a verified identity. Only then is it attached. **\[Verify\]** that Supabase Kotlin `3.1.4` and the project's settings support manual identity linking; the analysis does not say.
- **Unlinking:** **\[Verify\]** availability. Rule: block unlinking if it would leave no usable sign-in method. Require recent re-authentication. After unlinking, existing sessions are revoked or re-validated per Phase 7.
- **Conflicts:** a Microsoft identity already attached to a different Bulletin account must be rejected with a generic message that does not reveal the other account.

### Security and privacy

- Linking requires proof of both identities: an authenticated current session and a verified provider identity.
- Don't disclose whether an email is registered (account enumeration) in unauthenticated error messages **\[Decision\]** if consistent with current password flow behavior, which the analysis does not describe.
- Log link/unlink events with account id and timestamp, without tokens or emails.

### Dependencies and sequencing

Depends on Phase 4 and on the Phase 2 collision test results. If explicit linking is not supported, ship v1 with no linking and rejection messaging (see Decisions).

### Acceptance criteria and tests

- Microsoft sign-in with an email equal to an existing password account never silently gives access to that account.
- Explicit link succeeds only with an authenticated session plus a verified Microsoft identity.
- Link of an already-linked identity is rejected.
- Unlink of the last method is blocked.
- Soft-deleted profile, alias email and changed-email cases behave as decided.

### Risks and rollback

Account takeover through implicit linking is the primary risk; treat any test failure here as a release blocker. Rollback: disable the link UI via flag; existing links remain valid.

## Phase 6: School-email verification and sign-in eligibility

### Goals and scope

Separate the three checks the analysis distinguishes and decide how each applies to SSO.

### Architecture and data flow

| Check                     | Mechanism for Microsoft SSO                                                                                     |
| ------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `.edu` eligibility        | Existing `auth.users` insert trigger (plus email-change enforcement if decided)                                 |
| Mailbox/account ownership | Verified Microsoft email via `xms_edov`, trusted by Supabase; otherwise fallback to the existing six-digit code |
| Campus/tenant membership  | Entra tenant configuration and/or server-side allow-list; not implied by `.edu`                                 |

### Tasks

- Do not send a second Bulletin email code to a user whose Microsoft email Supabase confirmed from a verified claim, unless product policy requires it **\[Decision\]**.
- Implement the fail-closed or fallback behavior from Phase 2 in the app as a distinct result ("verification required"), reusing the existing `verifyEmail()` path rather than building a new one.
- Keep the app-side `.edu` check for password registration only. It is a UX nicety for that path and is not the SSO guard.
- Password sign-in continues not to apply `.edu` (accounts are restricted at creation), per the analysis.

### Security and privacy

Never grant a verified account because the address ends in `.edu` or because `email_confirmed_at` is populated. Confirm the verification signal first.

### Dependencies and sequencing

Depends on Phases 1 and 2 verification results.

### Acceptance criteria and tests

- Verified `.edu` Microsoft user: no extra code step, reaches authenticated root.
- Unverified-email Microsoft user: no verified session; rejected or sent to fallback per decision.
- Missing email claim: rejected with a clear, non-technical message.

### Risks and rollback

Tenants differ in whether they return a verification signal. If the intended institution's tenant does not, the fallback path becomes the main path; know this before launch.

## Phase 7: Logout and session cleanup

### Goals and scope

Signing out or removing a method fully ends access, without leaving app-side residue.

### Architecture and data flow

The existing sign-out operation ends the Supabase session. SSO adds no separate app session to clear.

### Tasks

- Confirm `signOut` clears the Supabase session on the device for SSO users, and clear any app caches keyed by user as the password path already does.
- **\[Decision\]** whether sign-out should also end the Microsoft browser session. The analysis does not cover this. Browser-based flows may auto-reuse the Microsoft session at next sign-in, which matters on shared devices. Evaluate account-picker or prompt behavior in the provider call **\[Verify\]**.
- **Revocation \[Verify\]:** test what happens to other devices' sessions after unlinking or deleting an account. Supabase refresh-token behavior and scope-based sign-out options need confirmation.
- Soft-deleted profile: ensure SSO restore does not resurrect access.
- Remove or avoid persisting any in-flight OAuth state after completion or cancellation.

### Security and privacy

No tokens in logs or crash reports. Verify that app backups do not include the session store **\[Verify\]**.

### Dependencies and sequencing

Can proceed alongside Phase 4; must finish before wide rollout.

### Acceptance criteria and tests

- After sign-out, restart does not restore a session; protected calls fail.
- Sign-in after sign-out requires a fresh flow; behavior of the Microsoft browser session matches the decision.
- Unlink or account deletion invalidates access as documented.

### Risks and rollback

Shared-device account reuse. Mitigate by choice of provider prompt behavior once verified.

## Phase 8: Flagged rollout, monitoring and rollback

### Goals and scope

Expose SSO gradually, observe it, and keep a clean retreat.

### Architecture and data flow

The flag controls only the visibility of the Microsoft actions in the app. Backend rules and provider enablement are separate switches.

### Tasks

- **Stages:** (1) staging with test accounts; (2) internal/production allow-listed accounts; (3) a small percentage or one campus **\[Decision\]**; (4) general availability.
- **Flag mechanism \[Decision\]:** the analysis does not describe existing remote config. If none exists, a build-time flag requires a release to change, which weakens rollback; choose accordingly.
- **Monitoring:** count by outcome only: started, succeeded, cancelled, ineligible, unverified, missing email, network failure, callback failure; plus profile-creation failures and duplicate-email rejections. Use Supabase Auth logs for server-side view. Alert on a spike in ineligible, unverified or callback failures. No tokens, emails or codes in telemetry.
- **Rollback levers, in order:** flag off (hides UI); disable the Azure provider in Supabase (stops new SSO sign-ins); revert tightening migrations via down-scripts. Define what happens to users who only have a Microsoft identity (no password): they need password recovery or must be told the plan **\[Decision\]**.
- **Exit gate for password login:** do not remove or hide it until SSO has run in production for an agreed period with acceptable metrics and no open security findings. Whether password login is ever retired is a product decision. Enabling OAuth does not disable password signup, so if "Microsoft-only" is chosen, disable it explicitly in Supabase **\[Decision\]**.

### Security and privacy

Rollout cohorts must not be inferred from email domain in logs. Keep client-secret rotation and Entra app ownership documented.

### Dependencies and sequencing

Requires Phases 2 through 7 acceptance criteria met for the stage being opened.

### Acceptance criteria and tests

- Flag off in staging hides all SSO UI without affecting password flows.
- Rollback rehearsal completed in staging: provider disabled, existing sessions and password users behave as documented.
- Dashboards and alerts exist before stage 2.

### Risks and rollback

Users who signed up only via Microsoft are stranded if the provider is disabled. Rehearse and document their recovery path before stage 3.

## Recommended implementation sequence (critical path)

1. **Gate A, baseline:** Phase 0 complete. Staging backend reproducible from migrations; dashboard settings recorded; blocking decisions made.
2. **Gate B, provider proof:** Phase 1 complete. A manual staging round trip works; `xms_edov` and tenant behavior are known for intended account types.
3. **Gate C, server policy:** Phase 2 complete. Eligibility, verified-email, collision and email-change behavior tested at the database/Auth level.
4. **In parallel after Gate B:** Phase 3 (app UI, port, callback) and Phase 7 groundwork. Phase 4 follows Phase 3 and needs Gate C for production use.
5. **Phase 5 and Phase 6:** linking and verification fallbacks. Phase 5 is skippable for v1 only if the collision behavior is verified safe and rejection messaging ships.
6. **Gate D, security review:** token handling, storage, logging, RLS parity test, and linking tests reviewed.
7. **Phase 8:** staged rollout; password-login exit gate only after sustained healthy production metrics.

## Key decisions still needed

1. Which Entra account types are allowed: one campus tenant, any organizational tenant, or also personal accounts? Is there a single intended institution?
2. Is Microsoft additive to password login, or does the product intend "Microsoft only" (and when)?
3. Must `.edu` remain true after account creation (email-change enforcement)? Should the Auth hook be registered?
4. If a Microsoft email lacks a verified signal, do we reject or fall back to the six-digit code?
5. How is `university` derived for SSO users (tenant mapping or user input), and where does `full_name` come from?
6. Is Supabase `aal2` required for any users? Is Microsoft-side MFA considered sufficient?
7. Is explicit account linking in v1, or rejection messaging only? Is unlinking in scope?
8. What happens to an SSO-only user if the provider must be disabled?
9. Should sign-out also end the Microsoft browser session?
10. Return URI type: verified HTTPS App Link or custom scheme?
11. Feature-flag mechanism and rollout cohorts.
12. Are existing non-`.edu` users (if any) grandfathered, and may soft-deleted profiles be reactivated through SSO?

## Definition of done

**Functional**

- A first-time eligible Microsoft user signs in on Android, gets one confirmed Supabase session and one profile, and reaches the authenticated root.
- Cancellation, failure, ineligible, unverified and missing-email outcomes are distinct, visible and recoverable.
- Existing password login, registration and recovery behave identically to before.
- Sign-out ends the session; restart does not restore it.

**Security**

- No account is linked or accessed by email match alone; test evidence for every collision case.
- Non-`.edu` and (if decided) wrong-tenant users are rejected server-side with no leftover session or profile.
- Verified-email requirement is demonstrated with both verified and unverified test accounts.
- No client secret, token, code or callback value appears in the repo, app config, logs or telemetry.
- RLS parity test passes: SSO and password sessions have identical authorization.
- Every **\[Verify\]** item above is closed with recorded evidence or converted to a documented accepted risk.

**Testing**

- Unit tests cover the port result mapping, ViewModel states and profile idempotency using a deterministic fake; normal test runs make no live OAuth calls.
- SQL tests cover trigger, email-change and duplicate rules against a migration-built staging project.
- A manual device test matrix (cancel, kill, offline, double-tap, cold-start callback) is executed and signed off.

**Rollout**

- Backend DDL and Auth configuration steps are versioned and a fresh environment can be built from the repo.
- Flag, provider-disable and migration-revert rollback paths have been rehearsed in staging.
- Dashboards and alerts exist for outcome categories before the first production cohort.
- Password login removal, if ever pursued, is a separate decision made after the agreed production soak period with no open security findings.
