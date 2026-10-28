# Bulletin Supabase schema analysis

Assessed on October 28, 2026 using Supabase CLI 2.120.0 against the linked **Bulletin** project (`qullzprtorshyqkxuvac`, `us-east-2`, PostgreSQL 17). The CLI reported the project as `ACTIVE_HEALTHY`; that is a service status, not an application readiness assessment.

**The proposed README correction is substantially correct:** this checkout has no `supabase/migrations/`, and using the app requires an already provisioned database. However, the linked project retains six migration records with SQL statements, including a baseline. The missing files are recoverable history, rather than evidence that the schema has no versioned definition anywhere. The deployed policies also have gaps that prevent describing the backend as fully verified or production-ready.

## Repository and migration findings

The tracked `supabase/` directory contains `config.toml`, confirmation and recovery email templates, `seed-users.js`, and three seed CSV files. There are no tracked migration files and no migration directory on disk. The seed files populate data; they do not replace schema, policy, function, or bucket provisioning.

`supabase/.temp/` is ignored and holds machine-local linking information. `project_id = "bulletin"` in `config.toml` names the local project; the hosted target was established separately through `supabase projects list`. Local configuration is explicitly not a snapshot of hosted Auth settings.

The current documentation is inconsistent:

- `README.md`, under **Configure Supabase**, still says to apply `supabase/migrations/`.
- Its **Backend Status** section correctly explains that the Microsoft boundary SQL is stored remotely and older migration files are absent.
- `AGENTS.md` calls `supabase/migrations/` the schema source of truth and requires future database changes there. That is the intended repository policy, but the current checkout does not satisfy it.

`supabase migration list` returned these versions only on the remote side. A read-only query of `supabase_migrations.schema_migrations` confirmed the names and nonempty statement arrays:

| Version | Name | Stored SQL statement entries |
| --- | --- | ---: |
| `20261013040619` | `bulletin_baseline` | 146 |
| `20261013050000` | `message_inbox_broadcast` | 4 |
| `20261017000100` | `microsoft_identity_policy` | 9 |
| `20261018214751` | `listing_images` | 1 |
| `20261018223321` | `listing_images_storage_policy_fix` | 1 |
| `20261027000000` | `microsoft_identity_boundary` | 1 |

These are statement-array counts, not counts of objects or guarantees that a migration contains only one SQL operation. The version timestamps and history records are not independent proof of when each deployment occurred. A history record also does not prove that subsequent manual changes have been captured.

## Deployed database overview

The linked database has **10 public base tables, 45 public-table policies, 17 public functions, and 29 public indexes**. No public views or materialized views were returned. Every public base table has RLS enabled; none has `FORCE ROW LEVEL SECURITY` enabled. Elevated owners and privileged server operations therefore need their own authorization discipline.

| Table | Responsibility and important fields | Integrity and relationships |
| --- | --- | --- |
| `profiles` | Student profile: `id`, `email`, `full_name`, `university`, `bio`, `major`, `avatar_url`, verification, deletion and admin flags | ID references `auth.users`; unique email. Profile creation and verification synchronization use an Auth trigger. |
| `listings` | Catalog and seller inventory: `name`, `user_id`, `category`, `condition`, `price`, `description`, `is_sold`, `buyer_id`, timestamps | Seller and buyer reference profiles. Category and condition have allowed-value checks. |
| `listing_images` | Image metadata: `listing_id`, `object_path`, `position`, crop scale and offsets | Listing FK cascades on deletion; unique object path and listing/position pair. Positions 0–4 cap metadata at five images per listing. Crop scale is 1–4; offsets are −1–1. |
| `listing_bookmarks` | Saved listings: `user_id`, `listing_id`, `created_at` | Composite primary key prevents duplicate saves; user and listing FKs cascade on deletion. |
| `conversations` | Thread identity and optional listing reference | Listing deletion sets `listing_id` to null, preserving the conversation. |
| `conversation_participants` | Membership, read timestamp, hidden state, history visibility cutoff | Composite conversation/user primary key; references conversations and profiles. |
| `messages` | Thread messages: sender, body, creation/edit/deletion timestamps | Conversation and sender FKs; body length check 1–1000 characters. Editing is handled through an RPC with additional validation. |
| `reviews` | Reputation: reviewer, reviewee, score, comment, optional listing | Score 1–5; FKs to profiles and listings; one review per non-null listing. A partial index separately permits only one profile review per reviewer/reviewee pair. |
| `reports` | Moderation queue for user, listing, or message reports | `report_type` enum is `user`, `message`, `listing`; check requires exactly the corresponding target. Status is `open`, `in_review`, `resolved`, or `dismissed`. Partial unique indexes prevent duplicate active reports per reporter and target. |
| `user_blocks` | Directed block relationships | Composite blocker/blocked primary key; no self-blocking; both users reference `auth.users`. |

Several listing fields remain nullable, including seller, name, price, and sold state. No listing price check appeared in the catalog constraints. Those definitions are weaker than many UI/domain expectations; adapters and domain validation remain necessary, and direct API writes need separate boundary tests.

`profiles` contains no precise location column, and none appeared in the other public tables. This establishes the inspected schema shape only; it does not assess free-text content, image metadata, logs, or other systems.

The profile adapter first queries `reviews_with_names` and falls back to `reviews`. That view is absent from this project, so the documented fallback path matters. Do not list the view as part of the deployed schema.

## How the application uses Supabase

`AppContainer` delegates backend selection to `InfrastructureModule` and the feature modules. Configured bindings use Supabase for listings, listing images, marketplace reads and reviews, bookmarks, reports, messages, profiles, profile photos, blocks, and authentication/session access. Home reads through the marketplace listing source; Home preferences and swipe decisions remain process-local. Normal Android runtime disables implicit in-memory fallback.

The main path is screen → ViewModel → application use case/port → context-owned adapter → PostgREST, Storage, Auth, or Realtime. The database is shared across bounded contexts: listings owns seller mutations while marketplace reads the same listing records; messages refers to listing and profile identities; moderation reports can target all three kinds of entity. Shared SQL therefore needs boundary tests across these flows.

The Android app uses an anon/public project key and a signed-in user's JWT. The separate Python admin portal uses privileged server operations, so its authorization checks are another boundary; ordinary client RLS cannot substitute for them. No binding was changed by this analysis.

## RLS and authorization approach

The broad design combines operation-specific permissive policies with restrictive account checks. Permissive policies combine with OR, while restrictive policies add AND conditions. RLS applies to rows, and table/column privileges still matter. This distinction is central to the review-policy issue below. See [PostgreSQL row security](https://www.postgresql.org/docs/17/ddl-rowsecurity.html) and [Supabase RLS guidance](https://supabase.com/docs/guides/database/postgres/row-level-security).

| Area | Installed policy behavior |
| --- | --- |
| Account gate | All ten public tables have restrictive `bulletin_active_account` policies for `authenticated`. The same gate exists on Storage objects and Realtime messages. |
| Profiles | A user can read their own profile, verified profiles, or a profile sharing a conversation. Own-row insert/update policies apply; updates preserve verification. A trigger prevents clients from changing admin status. |
| Listings | Authenticated reads are narrowed by restrictive block and active-seller checks. Inserts, updates, and deletes require the acting seller to own the row. |
| Listing image metadata | Authenticated reads; seller ownership for insert/update/delete; restrictive active-owner visibility. Parent listing visibility also affects policies that query listings. |
| Bookmarks | Read, insert, and delete require `user_id = auth.uid()`. |
| Conversations and membership | Reads require membership. Creation, hide, restore, and mark-read operations use security-definer RPCs rather than broad client mutation policies. |
| Messages | Reads require membership and the per-user history cutoff. Inserts require the authenticated sender and membership, plus active recipients. No direct client UPDATE policy is installed; editing uses `edit_message`. |
| Reports | Users insert their own reports and read their own reports. Message reports additionally require membership in the message conversation. No authenticated update/delete grants were returned for reports. |
| Reviews | Authenticated reads; two overlapping permissive insert policies, discussed below. |
| Blocks | Users see relationships involving themselves; only the blocker creates or deletes the relationship. |

The private `bulletin_account_is_active` function checks that `auth.users` exists and `banned_until` is absent or expired. It does **not** check `profiles.deleted_at`, email confirmation, or profile verification. Consequently “active” in these policies means “existing and not currently banned”; it must not be described as a complete account eligibility or deletion check.

No permissive anonymous policy was returned for public base-table reads. Broad SQL grants still exist on many tables, but normal anonymous row reads have no matching permissive authorization. This does not cover public Storage URLs or function execution.

## Storage buckets

| Bucket | Access model | Maximum object size | Allowed MIME types | Client write authorization |
| --- | --- | ---: | --- | --- |
| `listing_imgs` | Public | 5,242,880 bytes, or 5 MiB | JPEG, PNG, WebP | Insert/delete require the first path folder to match a listing owned by the actor. |
| `pfp` | Public | 5,242,880 bytes, or 5 MiB | JPEG, PNG, WebP | Insert/update require the exact path `<auth.uid()>/avatar`. A SELECT policy permits the actor's own avatar folder. |

`storage.objects` has RLS enabled and six policies, including the restrictive account gate. No profile-photo DELETE policy or listing-image UPDATE/SELECT policy was returned. That matches the principal upload/public-URL workflows, but future deletion, listing, or overwrite flows need corresponding verification.

**Public images are accessible to anyone possessing the URL**, even if the viewer is blocked or signed out. The object policies still govern mutations, but they do not make public file retrieval private. Account suspension or hiding a listing is therefore not equivalent to revoking an existing image URL. See [Supabase bucket access models](https://supabase.com/docs/guides/storage/buckets/fundamentals).

Listing image rows cascade with a deleted listing; that does not itself establish deletion of the stored bytes. The admin code explicitly deletes exact object paths separately and reports partial cleanup failures. Schema provisioning must include bucket records and policies, not only `public` table DDL.

## Auth functions and Realtime

The deployed Auth triggers include `tr_enforce_signup_rules` on `auth.users`, `tr_enforce_auth_email_change`, profile provisioning through `handle_new_user`, and `tr_enforce_microsoft_identity` before inserts/updates on `auth.identities`.

The installed Microsoft identity function checks explicit provider-derived `custom_claims.xms_edov`, verified email matching the account email, conflicting password/other-provider identities, and a soft-deleted profile. Password signup applies the `.edu` policy; Azure signup follows a different rule. This verifies installed SQL, not successful Microsoft OAuth callbacks or hosted provider settings.

The local config enables the Before User Created hook at `public.enforce_edu_email_signup` and disables Azure locally. The function exists remotely, but its existence does not prove that hosted Auth has enabled the hook. Provider credentials, Entra claims, redirect allow lists, and live sign-in were not validated here.

Realtime publishes `public.messages` and `public.conversation_participants`. Two policies on `realtime.messages` constrain inbox broadcast receipt to `inbox:<auth.uid()>` and apply the active-account gate. Message insert/body-edit triggers send a private `conversation-changed` invalidation containing the conversation ID, not message text. Live subscriber delivery and reconnect behavior require integration tests.

`edit_message` additionally enforces sender ownership, visible membership, an edit window under 15 minutes, moderation locks, optimistic concurrency, and trimmed nonempty content with the Kotlin UTF-16 size limit. These protections illustrate why a table-only schema summary would miss important backend behavior.

## Findings that need follow-up

### Review insertion can bypass the buyer-only policy

`Students can submit reviews for other peers` and `reviews_insert_buyer_only` are both **PERMISSIVE INSERT** policies for `authenticated`. The first accepts an authenticated verified reviewer reviewing someone else; it does not require a purchase or validate the listing relationship. Because the policies OR together, adding the second policy does not make all listing reviews buyer-only.

From the installed definitions, a verified active user could satisfy the broader policy while supplying an unreviewed listing that they did not buy and an unrelated reviewee, subject to foreign keys and uniqueness. This is a static authorization finding; no fabricated review was submitted. Reconcile profile reviews versus sold-listing reviews explicitly, then make the alternatives mutually scoped or use a restrictive listing eligibility condition. Add tests for unsold listings, non-buyers, seller mismatch, self-review, and valid legacy profile reviews.

### Conversation creation trusts the supplied seller relationship

`get_or_create_listing_conversation` is security-definer and calls `private.bulletin_require_listing_contact`. The helper checks an active actor, requester identity, non-self contact, a non-null listing ID, and an active supplied seller. It does **not** check that the supplied seller owns the listing, that the caller can view the listing, or that the two users are not blocked. The lookup helper only searches existing conversation membership; it does not add those checks.

The listing FK establishes existence, not seller ownership. A direct RPC request can therefore reach a creation path without the relationship checks suggested by its name. The installed message INSERT policies also lack a block-relationship check, so blocking is not a demonstrated server-enforced messaging prohibition. Establish the intended block behavior and enforce listing contact invariants inside the privileged operation. Test forged seller IDs, blocked actors, unavailable listings, and concurrent legitimate creation.

The anonymous EXECUTE grant is unnecessary exposure, but it is not evidence of successful anonymous creation: the helper rejects a missing active authenticated account.

### Profile row access also exposes email and internal flags

`authenticated` has table-level SELECT on `profiles`. The read policies return entire eligible rows, including `email`, `is_admin`, and `deleted_at`; they do not provide a public-field projection. A client choosing fewer fields does not prevent another client requesting them. Confirm whether email disclosure is intended. If it is not, introduce an appropriately authorized public projection or column privileges while preserving private self-profile access. No profile records or email values were read during this audit.

### SQL grants are broader than client operations require

Nine public tables grant `anon` and `authenticated` TRUNCATE, REFERENCES, and TRIGGER, alongside other privileges. `reports` has the narrower authenticated SELECT/INSERT grants. PostgreSQL does not apply row security to TRUNCATE. This is a database privilege concern; it does not establish an available destructive HTTP endpoint. Revoke privileges outside the supported client contract, and verify RPC and server dependencies in a disposable environment. See [PostgreSQL RLS limitations](https://www.postgresql.org/docs/17/ddl-rowsecurity.html).

### Security advisor results

`supabase db advisors --linked --type security` completed successfully and returned **10 warnings, with no ERROR-level entries**:

| Warning group | Count | Objects or setting |
| --- | ---: | --- |
| Mutable function search path | 3 | `record_conversation_history_cutoff`, `stamp_message_created_at`, `set_listing_updated_at` |
| Anonymous execution of a security-definer function | 1 | `get_or_create_listing_conversation` |
| Authenticated execution of security-definer functions | 5 | `edit_message`, `get_or_create_listing_conversation`, `hide_conversation`, `mark_conversation_read`, `restore_conversation` |
| Leaked password protection disabled | 1 | Hosted Auth setting reported by the advisor |

The five authenticated RPCs are intentional application entry points; their warnings call for reviewing function authorization and grants rather than blindly revoking access. Fix mutable search paths with explicit schema qualification and a suitable fixed path. Evaluate leaked-password protection for the target deployment. A successful advisor command is not a passing security audit; it did not report the business-policy gaps identified above. See [Supabase advisors](https://supabase.com/docs/guides/observability/advisors) and [database function security](https://supabase.com/docs/guides/database/functions).

## Suggested README wording

> **Database provisioning:** This checkout does not include `supabase/migrations/` and cannot provision a fresh Bulletin database by itself. Configure the app against an already provisioned Supabase project. The linked project's migration history contains six migrations, including `bulletin_baseline` and `microsoft_identity_boundary`; local migration files remain absent. Hosted schema, functions, RLS, storage buckets and policies, and Auth settings must be reconciled before connecting another project. `supabase/config.toml` is local development configuration and does not establish hosted configuration.

> **Backend overview:** The inspected project has ten public tables covering profiles, listings and images, bookmarks, conversations and membership, messages, reviews, reports, and blocks. All ten have RLS enabled. Policies use authenticated ownership or conversation membership and restrictive suspension checks. Two public image buckets, `listing_imgs` and `pfp`, allow JPEG, PNG, and WebP files up to 5 MiB, with ownership checks for supported writes. Public image URLs remain publicly accessible. Review insertion and conversation authorization need additional hardening; schema inspection does not establish production readiness.

This wording replaces the impossible instruction to apply missing migration files. Keep the existing Backend Status qualifications about unverified live application flows. The report does not revise the README or change its configured bindings.

## Restoring a reproducible schema

1. Recover historical files into an isolated working directory using `supabase migration fetch --linked`. The installed CLI supports this command, and every observed migration has stored SQL entries. Inspect recovered SQL before adding it to the repository; do not assume history is a complete snapshot of the current database. See [CLI migration fetch](https://supabase.com/docs/reference/cli/supabase-migration-fetch).
2. Compare a schema-only export of the current database with the recovered history, including `public` and `private` functions, Auth triggers, grants, policies, Realtime publication, and Storage configuration. Account separately for managed Supabase schemas and bucket configuration; never commit live application records, credentials, or Auth identities.
3. Replay the reviewed migrations against a disposable local stack. This is where to determine whether the recorded baseline really provisions a clean database and whether the seed tools still match the schema.
4. Add actor-based SQL/API tests for anonymous access, self versus other users, verified/unverified and banned/deleted accounts, buyers/sellers, participants/nonparticipants, blocks, moderation locks, and storage ownership. Include denial cases for the findings above.
5. Version subsequent fixes in `supabase/migrations/`, update provisioning documentation and `AGENTS.md` consistently, and reconcile the existing hosted history before deploying. Do not repair migration history merely to hide missing files.

These are follow-up actions. No migrations were fetched, applied, repaired, reset, or pushed during this analysis, and hosted application state and settings were not changed.

## Verification and scope

Successful CLI checks: `supabase --version`, `supabase projects list --output json`, `supabase migration list`, read-only `supabase db query --linked --file ... --output json` catalog queries, and `supabase db advisors --linked --type security --output json`. CLI help also confirmed query and migration-fetch capabilities.

The SQL queries used `BEGIN READ ONLY` and `ROLLBACK`. They inspected catalogs, bucket configuration, and migration names/statement counts; they did not read application records, message contents, user identities, or stored objects. CLI authentication initialized its temporary login role, and CLI startup needed filesystem permission for its normal user-level telemetry/configuration.

Useful read-only checks to repeat through `supabase db query --linked --file <file.sql> --output json` are:

```sql
BEGIN READ ONLY;
SELECT version, name, cardinality(statements) AS statement_entries
FROM supabase_migrations.schema_migrations ORDER BY version;

SELECT c.relname, c.relrowsecurity, c.relforcerowsecurity
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind = 'r'
ORDER BY c.relname;

SELECT schemaname, tablename, policyname, permissive, roles,
       cmd, qual, with_check
FROM pg_policies
WHERE schemaname IN ('public', 'storage', 'realtime')
ORDER BY schemaname, tablename, policyname;

SELECT id, public, file_size_limit, allowed_mime_types
FROM storage.buckets ORDER BY id;
ROLLBACK;
```

No Gradle build or application tests were run because the only repository change is this report. No live actor-based access tests, clean-database replay, OAuth sign-in, Storage upload/download, Realtime integration, or performance advisor run was performed. The existing README's earlier SQL regression results were not rerun. Policy findings above follow from installed definitions and grants, rather than writes or reads impersonating real users.
