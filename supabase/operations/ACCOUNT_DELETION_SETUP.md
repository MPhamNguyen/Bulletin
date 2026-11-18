# Scheduled account deletion

This is an incremental deployment for the existing Bulletin hosted schema, including
the moderation guards and message-retention migration. It is not a database baseline.
Nothing is scheduled by installing the migration. No Android changes are included.

## 1. Install the migration

In your Supabase project's **SQL Editor**, paste and run the entire file
`supabase/migrations/20261119000100_schedule_account_deletion.sql` once.
It runs in a transaction and stops if the prerequisite retention helpers are missing.
Do not re-run earlier migrations. If it fails, save the exact error before proceeding.

This adds private jobs, an object manifest, service-role-only RPCs, restoration/report
guards, and restrictive Storage write policies. Existing read policies and cascades
are preserved. New soft-deletion timestamps come from the database, even when the
app supplies a device timestamp. Existing timestamps are preserved: review any old,
incorrectly backdated requests before activating the worker.

## 2. Configure the worker secret

Generate a dedicated unpredictable secret locally, or run this in SQL Editor:

```sql
select gen_random_uuid()::text || gen_random_uuid()::text as cron_secret;
```

Copy the result into **Edge Functions → Secrets** as
`ACCOUNT_DELETION_CRON_SECRET`. Keep it private; do not put it in Git or the app.
The function uses Supabase's automatically supplied `SUPABASE_URL` and
`SUPABASE_SERVICE_ROLE_KEY` server environment variables. No client receives that key.

## 3. Deploy the Edge Function

In **Edge Functions → Deploy a new function → Via Editor**, name it
`purge-deleted-accounts`. Add these two files, preserving their names:

- `supabase/functions/purge-deleted-accounts/index.ts`
- `supabase/functions/purge-deleted-accounts/worker.ts`

Deploy. In the function settings, **disable Verify JWT**: the handler authenticates
every request using `x-deletion-secret` before privileged I/O. Leaving JWT verification
enabled prevents Cron's custom-secret requests from reaching the handler.
The corresponding CLI configuration is already in `supabase/config.toml`.

If using the CLI instead of the browser editor, from the repository run:

```sh
supabase functions deploy purge-deleted-accounts --project-ref YOUR_PROJECT_REF --no-verify-jwt
```

Do not push the whole local Supabase config to the hosted project.

## 4. Run a preview

Use the function Dashboard's **Test** panel with method **POST**, body
`{"dry_run":true}`, and header `x-deletion-secret` containing your secret.
Expect HTTP 200 with `dry_run`, `eligible`, `held`, and `finalizing` counts.
Missing `dry_run` also previews. Preview does not create jobs, freeze accounts,
delete files, or delete accounts. It does not test live Storage removal or Auth deletion.

You can also inspect the age cutoff without creating data:

```sql
select id, deleted_at, deleted_at + interval '30 days' as eligible_after
from public.profiles
where deleted_at is not null
order by deleted_at;
select public.account_purge_preview();
```

## 5. Store Cron configuration in Vault

Enable **pg_cron** (Cron) and **pg_net** in Database → Extensions / Integrations.
In **Vault**, create these two secrets:

| Vault name | Value |
| --- | --- |
| `account_deletion_project_url` | Your project URL, such as `https://YOUR_PROJECT_REF.supabase.co`, without a trailing slash |
| `account_deletion_cron_secret` | The same value as the Edge Function's `ACCOUNT_DELETION_CRON_SECRET` |

Use Vault's UI so secrets do not enter saved SQL scripts. Then run
`supabase/operations/account-deletion-cron.sql` in SQL Editor. It schedules hourly
preview requests at minute zero, in UTC. It replaces only the named Bulletin job.
Keep one Vault entry per name.

## 6. Enable actual deletion

After a successful function preview, run this statement in SQL Editor:

```sql
update cron.job
set command = replace(command, '"dry_run":true', '"dry_run":false')
where jobname = 'bulletin-account-purge';
```

The next hourly run can irreversibly remove data. To run it immediately, use the
function Test panel with `{"dry_run":false}` and the same header. Each invocation
processes at most **one account** and 100 files per Storage request, with a 70-second
work budget plus in-flight HTTP calls. Large accounts continue in later runs.
Hourly scheduling therefore handles at most 24 accounts/day, before retries. For
a larger backlog, change the Cron schedule to `*/5 * * * *` (five minutes);
leases prevent concurrent invocations from claiming the same account.

## Behavior and retries

- Accounts become eligible at `deleted_at + 30 days`. Actual deletion occurs in a
  later scheduled run, subject to backlog and moderation holds.
- An `open` or `in_review` report **about** the account blocks claiming it. Listing
  and message subjects are derived using the existing database helpers. Being the
  reporter does not create a hold. Moderation event completion follows report status.
- Restoration remains possible until the worker claims the account. Claiming and
  moderation writes share the existing transaction lock: if the active report wins,
  cleanup waits; if cleanup wins, new/reopened active reports against that subject
  are rejected. This is the explicit point at which irreversible cleanup begins.
- Once claimed, restoration and listing changes are blocked, even after a failure.
  Do not clear job records to restore an account after partial file removal.
- Storage inventory includes objects owned by the user in **any bucket**, their
  `pfp/<user-id>/...` files, and `listing_imgs/<listing-id>/...` plus image metadata
  for listings they own. It uses metadata reads only; deletion uses the Storage API.
  Files with no owner, no known listing metadata, and no recognized prefix cannot
  be attributed to the account and need a separate orphan audit.
- Auth deletion happens only after the manifest and a fresh Storage inventory are
  empty. The Admin Auth API performs a hard delete. Existing database guards,
  cascades, review anonymization and conversation retention determine row cleanup.
  Remaining members keep retained conversations, which the deployed app renders
  read-only with **User Not Found**. This worker does not replace those migrations.
- Storage/Auth errors retain the job and schedule a retry after one hour. A crashed
  invocation's lease expires after 15 minutes. Missing files and already-deleted Auth
  users are handled on retry. No restore window reopens when a lease expires.
- Existing authenticated Storage policies remain in force, with additional finalizing
  checks. Trusted service-role writers and signed-upload workflows can bypass RLS;
  they must stop writes for finalizing accounts. Do not add such uploads without
  integrating this hold. A fresh inventory and Auth trigger catch late metadata,
  but PostgreSQL and Storage HTTP calls are not a single distributed transaction.

## Monitor and pause

```sql
select jobid, jobname, schedule, active from cron.job
where jobname='bulletin-account-purge';
select status, return_message, start_time from cron.job_run_details
where jobid in (select jobid from cron.job where jobname='bulletin-account-purge')
order by start_time desc limit 20;
select id, status_code, timed_out, error_msg, created from net._http_response
order by created desc limit 20;
select user_id, requested_at, started_at, completed_at, attempts, lease_until,
       retry_after, last_error
from private.account_purge_jobs order by started_at desc limit 50;
```

Cron success means the HTTP request was enqueued; check the HTTP status and Edge
Function logs too. Errors expose only stage/status, not message contents or paths.
For persistent failures, inspect the database constraint/Auth/Storage error in the
respective service logs; do not disable moderation guards to force deletion.
Job metadata is private and retained for operational recovery; choose an audit
retention period and delete completed job rows when no longer needed.

To stop future invocations:

```sql
select cron.unschedule('bulletin-account-purge');
```

An invocation already running may finish. Do not delete the function or jobs while
cleanup is partially complete unless you have a recovery plan.

## Developer verification

`deno check supabase/functions/purge-deleted-accounts/index.ts` and
`deno test supabase/functions/purge-deleted-accounts/worker_test.ts` need no credentials.
`supabase/tests/account_purge.test.sql` is an opt-in disposable-database test and is
not an installation step. Never run fixture tests against the hosted production DB.
The repository does not contain the full hosted baseline needed for a fresh reset.
