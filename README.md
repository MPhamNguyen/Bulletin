<div align="center">

# 🎓 Bulletin
### Campus Connect Marketplace

**A student-focused marketplace for buying, selling, and connecting safely within a university community.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Backend](https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Course](https://img.shields.io/badge/CECS-491A%2FB-FFC72C?style=for-the-badge)
![Status](https://img.shields.io/badge/status-In%20Development-2563EB?style=for-the-badge)

</div>

---

## 📖 About Bulletin

**Bulletin** is a Kotlin Multiplatform campus marketplace that makes peer-to-peer transactions between university students safer, simpler, and more convenient. Android is the current application platform; shared business logic and Compose UI live in the `shared` module.

Students can discover listings, buy or sell items, message other students, build reputation through reviews, and manage their profiles. Accounts are tied to university email addresses, and the repository also includes a small web **admin portal** for moderating reports.

> **Project:** Bulletin
> **Course:** CECS 491A/B
> **Team:** JDRMS

---

## ✨ Core Features

| Feature | Description |
| --- | --- |
| 🎓 **Student Verification** | Sign up with a `.edu` email and confirm it before logging in (Supabase Auth). Password recovery is supported. |
| 🏠 **Home Discovery** | Swipe-style feed of listings. Swipe right to save, left to pass, or up to message the seller; buttons and accessibility actions do the same. |
| 🛍️ **Marketplace** | Browse, search, filter, and sort listings; view listing and seller details. |
| ➕ **Listings** | Create, edit, and manage your own listings, upload images, and mark items as sold. |
| 🔖 **Saved Listings** | Bookmark listings and revisit them from your profile. |
| 💬 **Messaging** | Start conversations from a listing, view the inbox and thread history, edit messages, and see read status. Inbox updates arrive in real time. |
| ⭐ **Ratings & Reviews** | Leave and view reviews tied to sold listings. |
| 👤 **Profiles** | View and edit student profiles, including profile photos. |
| 🚩 **Reporting & Blocking** | Report users, listings, or messages, and block or unblock users. |
| 🌗 **Theme** | Light/dark theme preference. |
| 🛡️ **Admin Portal** | Web tool for reviewing reports and suspending or restoring accounts (see [Admin Portal](#%EF%B8%8F-admin-portal)). |

> Feature availability depends on backend configuration. This README does not claim production readiness.

---

## 📱 App Flow

The app has three root screens: **Sign in**, **Create profile**, and the **Main** app. Main has five bottom-navigation tabs:

```text
Sign Up / Sign In  →  Create Profile  →  Main
                                          │
        ┌──────────┬───────────┬──────────┼──────────┐
        ↓          ↓           ↓          ↓          ↓
      Home       Market       Post       Inbox     Profile
   (swipe feed)  (browse,   (create    (conversations, (settings, edit account,
                  search,    listings)   messages)       saved, my listings,
                  seller                                 blocked users, ...)
                  profiles)
```

---

## 🏗️ Tech Stack

| Layer | Implementation |
| --- | --- |
| **Language / UI** | Kotlin 2.4, Jetpack Compose via Compose Multiplatform |
| **Shared code** | Kotlin Multiplatform library in `shared` (`commonMain`, `androidMain`) |
| **Android app** | Thin host in `androidApp` (min SDK 24, target/compile SDK 36) |
| **Backend** | Supabase (Auth, PostgREST, Realtime, Storage) via `supabase-kt` |
| **Images** | Coil 3 with the Ktor network engine |
| **Dependency injection** | Manual composition in `AppContainer` |
| **Static analysis** | Detekt with formatting rules |
| **Build** | Gradle wrapper with the version catalog in `gradle/libs.versions.toml` |
| **Admin portal** | Python 3.13 + Flask, deployed on Vercel |

---

## 🏛️ Repository Layout

```text
Bulletin/
├── androidApp/            # Android application (Activity, Application, manifest, build flags)
├── shared/                # KMP module: all business logic and Compose UI
├── admin/                 # Flask admin portal (Python)
├── supabase/              # Local Supabase config, email templates, seed data and script
├── config/detekt/         # Detekt rules
├── .github/workflows/     # CI (ci.yml, detekt.yml, stale-branch cleanup)
├── AGENTS.md              # Contribution rules for AI agents and humans
└── CODEOWNERS
```

### Architecture

Bulletin follows a **DDD-inspired, feature-first clean architecture** inside `shared/src/commonMain/kotlin/com/jdrms/bulletin/`:

```text
├── app/              # Composition root (AppContainer, feature modules in di/), app shell and navigation,
│                     # theme preference, and cross-context integration adapters (integration/)
├── core/             # Shared technical concerns: design system, Supabase config/auth storage, common utilities
└── domain/           # Bounded contexts
    ├── home/         # Swipe-based discovery feed, swipe decisions, preferences
    ├── marketplace/  # Catalog, search, bookmarks, listing and seller-profile reviews, listing/user reporting
    ├── listings/     # Creating, editing, and managing a student's own listings
    ├── messages/     # Conversations, inbox, message sending/editing, message reporting
    ├── profile/      # Auth and sessions, profiles and photos, reviews on profiles, blocking, user reports
    └── reporting/    # Shared report vocabulary only (ReportType, ReportStatus, ReportReason)
```

The five main contexts (`home`, `marketplace`, `listings`, `messages`, `profile`) each use four layers, all as packages within `shared` (not separate Gradle modules). `reporting` is the exception: it only contains `domain/model` types shared by the others.

| Layer | Owns |
| --- | --- |
| `domain/` | Domain types, invariants, policies, repository contracts |
| `application/` | Use cases and orchestration |
| `infrastructure/` | Supabase and in-memory adapters, DTOs, mappers |
| `presentation/` | Compose screens, ViewModels, UI state |

Contexts do not import each other's entities, repositories, or DTOs. Cross-context wiring lives in `app/integration` and in the feature modules under `app/di`. See [`AGENTS.md`](AGENTS.md) for the full rules, including file and function size limits enforced by Detekt.

### Backend Binding

Dependency injection is manual. `AppContainer` composes `HomeModule`, `ListingsModule`, `MarketplaceModule`, `MessagesModule`, and `ProfileModule`; `InfrastructureModule` owns the lazily created Supabase client.

| Context | Runtime binding |
| --- | --- |
| Marketplace, listings, listing images (Storage), listing reports | Supabase adapters |
| Messages (including realtime thread and inbox observers) | Supabase adapters |
| Profile, auth/session, profile photos, user reports, user blocks | Supabase adapters |
| Home | Supabase adapters and local memory. The feed reads through `MarketplaceHomeRepository` (marketplace listing source), swipe-right saves via the marketplace bookmark adapter, and swipe-up messages via the messages adapter. Swipe decisions (which cards were saved or passed) and Home preferences are kept in memory per app process. |
| Theme and marketplace layout preferences | Android local preference stores |

If Supabase is not configured, the normal Android runtime **fails to initialize** backend-backed contexts: in-memory fallback is disabled by default. In-memory implementations exist for tests, previews, and explicitly enabled local use.

> These bindings describe what the code does, not that every flow has been verified against a deployed Supabase project.

---

## 🔧 Local Setup

### Requirements

- JDK 21
- Android SDK (compile SDK 36) configured for your environment, e.g. via Android Studio
- Git

### 1. Configure Supabase

The Android build reads settings from a root `.env` file, then environment variables, then Gradle properties. Copy `.env.example` to `.env` and fill in your project values:

```dotenv
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_API_KEY=your-anon-public-key
SUPABASE_IS_CONNECTED=true
```

- Use **only** the anon/public key in the app. Never commit a service-role key, credentials, or real user data. `.env` is git-ignored.
- `SUPABASE_IS_CONNECTED` defaults to `false`, which leaves backend-backed features unavailable.
- This checkout has no `supabase/migrations/` folder and **cannot provision a fresh Bulletin database by itself**. Point the app at an already provisioned Supabase project. See [Database (Supabase)](#%EF%B8%8F-database-supabase) below.

### 2. Build and run

Open the project in Android Studio and run the **Bulletin** configuration, or build from the command line:

```bash
./gradlew :androidApp:assembleDebug --no-daemon
```

### Microsoft sign-in (optional)

Microsoft sign-in is a build-time feature flag, **on in debug builds and off in release builds** by default. Override it with `-PMICROSOFT_SSO_ENABLED=true|false` or the `MICROSOFT_SSO_ENABLED` environment variable (the value must be exactly `true` or `false`; `.env` is not read for this flag). Only enable it against a Supabase project that has been prepared for it: Azure provider credentials, the `com.jdrms.bulletin.auth://oauth/callback` redirect allow-list entry, and the matching server-side identity policy. Password sign-up always requires a `.edu` address.

### Local Supabase and seed data (optional)

`supabase/config.toml` configures a local Supabase stack for the Supabase CLI (email confirmations on, and a before-user-created hook pointing at `public.enforce_edu_email_signup`). `supabase/templates/` holds the confirmation and recovery email templates.

`supabase/seed-users.js` loads the CSVs in `supabase/seedData/` (profiles, listings, reviews) into a project. It reads `SUPABASE_URL` and `SUPABASE_API_KEY` from `.env`, and each seed profile's `id` must be the UUID of an Auth user you created manually.

---

## 🗄️ Database (Supabase)

### Provisioning

The schema, functions, RLS policies, storage buckets and Auth settings live in the hosted Supabase project, not in this repository. The linked project's migration history holds six migrations (including `bulletin_baseline` and `microsoft_identity_boundary`), but the migration files are not checked in. `supabase/config.toml` is local development configuration only and does not describe the hosted project. Before connecting a different project, its schema, functions, RLS, storage buckets and policies, and Auth settings must be reconciled with the hosted one.

Restoring a reproducible schema (recover history with `supabase migration fetch --linked`, compare against the live schema, replay on a disposable local stack, then version future changes in `supabase/migrations/`) is still open work; see the roadmap.

### Overview

The inspected project (PostgreSQL 17) has ten public tables, all with RLS enabled:

| Table | Purpose |
| --- | --- |
| `profiles` | Student profile; ID references `auth.users`, created by an Auth trigger |
| `listings` | Seller inventory, with sold state and optional buyer |
| `listing_images` | Image metadata and crop settings (up to five per listing) |
| `listing_bookmarks` | Saved listings |
| `conversations`, `conversation_participants` | Message threads, membership, read and hidden state |
| `messages` | Thread messages (1 to 1000 characters; edits go through an RPC) |
| `reviews` | Ratings (1 to 5) between students, optionally tied to a listing |
| `reports` | Moderation queue for user, listing, and message reports (`open`, `in_review`, `resolved`, `dismissed`) |
| `user_blocks` | Directed block relationships |

**Authorization approach:** permissive policies scope access to the signed-in owner or conversation members, and a restrictive account check applies to every table, Storage, and Realtime. Conversation creation, hide/restore, mark-read, and message editing run through security-definer RPCs rather than direct table writes. Auth triggers enforce the `.edu` signup rule and the Microsoft identity policy.

**Storage:** two public buckets, `listing_imgs` and `pfp`, accept JPEG, PNG, and WebP up to 5 MiB, with ownership checks on uploads. Public image URLs are retrievable by anyone who has the link, regardless of sign-in or blocking.

**Realtime:** `messages` and `conversation_participants` are published, and a private per-user inbox channel sends conversation-changed signals (conversation ID only, never message text).

> This is an inspection of the deployed definitions, not a production-readiness claim. Known follow-ups include tightening review-insert and conversation-creation authorization, narrowing broad table grants and profile column exposure, fixing mutable function search paths, and enabling leaked-password protection. The Microsoft sign-in path and live actor-based access tests have not been verified.

---

## ✅ Checks and CI

```bash
./gradlew :shared:testAndroidHostTest --no-daemon          # unit tests
./gradlew :shared:detekt :androidApp:detekt --no-daemon    # static analysis
./gradlew :androidApp:assembleDebug --no-daemon            # debug build
./gradlew check --no-daemon                                # everything
```

Normal tests do not need live credentials or network access; live Supabase tests are opt-in.

To run one slice of the host tests the way CI shards them (set both properties or neither):

```bash
./gradlew :shared:testAndroidHostTest -PtestShardCount=4 -PtestShardIndex=0 --no-daemon
```

GitHub Actions (`.github/workflows/`):

| Workflow | Purpose |
| --- | --- |
| `ci.yml` | Decides scope from the diff against `main`, then runs size checks, static analysis, domain/shared tests (sharded on full runs), and admin tests. A change to `reporting` also tests its consumers (marketplace, messages, profile). Full runs happen on `main`, the merge queue, the weekly schedule, and manual dispatch. The required gate is **Build and Test**. |
| `detekt.yml` | Detekt CLI scan with SARIF upload for code scanning (complements the type-resolved Detekt run inside `check`) |
| `remove-stale-branches.yml` | Daily cleanup of branches with no open PR after 28 days of inactivity (14 to mark stale, 14 more to delete) |

---

## 🛡️ Admin Portal

`admin/` is a small Flask app for moderators, deployed on Vercel with **Root Directory set to `admin`**. It talks to Supabase from the server only.

**What it does**

- Sign in with a Supabase account whose `profiles.is_admin` is true
- Review reports (filter by status and type) and change a report's status: `open`, `in_review`, `resolved`, `dismissed`
- View the reported conversation, listing, and user context for a report
- Delete a reported listing
- Suspend or restore an account, with a duration
- Browse a user directory and view individual users

**Configuration** (see `admin/.env.example`; set these as environment variables, nothing is loaded from a file automatically):

| Variable | Notes |
| --- | --- |
| `SUPABASE_URL` | Project URL |
| `SUPABASE_ANON_KEY` | Anon key, used for admin sign-in |
| `SUPABASE_SERVICE_ROLE_KEY` | **Server-only.** Never put it in HTML, browser code, or the Android app |
| `ADMIN_SECRET_KEY` | At least 32 random characters (`python -c "import secrets; print(secrets.token_hex(32))"`) |
| `ADMIN_LOCAL_HTTP` | `true` only for local HTTP development; otherwise cookies are secure-only |

**Run locally**

```bash
cd admin
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# export the variables above in your shell, then:
python -m flask --app app run
```

**Test**

```bash
cd admin
python -B -m unittest discover -s tests -v
```

---

## 🔐 Security & Privacy

- Client-server traffic uses HTTPS through the Supabase client.
- The Android app uses only the Supabase anon/public key; the service-role key lives only in the admin portal's server environment.
- Database access must be protected by Row Level Security and storage policies.
- Message contents and precise locations must not be exposed to unauthorized users. The inspected schema has no precise-location column.
- Listing and profile images are in public buckets, so anyone with an image URL can fetch it.
- Credentials, session tokens, message contents, and precise locations must never be logged.
- The admin portal uses signed, HTTP-only, same-site cookies with CSRF protection.

These are implementation requirements and project goals; this README does not claim a production security audit.

---

## 🗺️ Development Roadmap

**Where we are:** the core student marketplace is implemented in code: sign-up and sign-in with `.edu` verification, marketplace browse and search, listings, saved items, messaging, reviews, reporting and blocking, swipe-based Home discovery, and an admin moderation portal. Everything is backed by Supabase adapters and covered by CI. Checked items are implemented in the codebase; they have not been audited against the live project. What remains is mostly hardening, user search, and the recommendation work.

- [x] Validate authentication, email confirmation, and account lifecycle against the configured Supabase project *(implemented in code)*
- [ ] Verify database RLS and storage policies for supported actors and operations *(policies inspected, RLS enabled on all ten tables; known gaps still to close)*
- [x] Complete and validate marketplace listing workflows *(implemented in code)*
- [x] Complete and validate search, filters, sorting, and saved listings *(implemented in code; search loading and consistency fixes are in the backlog)*
- [ ] Validate user search and profile management *(profile management implemented; there is no user search in the app yet)*
- [x] Validate messaging, inbox updates, reporting, and blocking *(implemented in code)*
- [x] Validate sold-listing review workflows *(implemented in code)*
- [ ] Develop and validate personalized home recommendations *(Home feed uses a basic ranking policy; the recommendation epics are in the backlog)*
- [ ] Measure performance against project targets
- [ ] Prepare user guide, privacy policy, and terms of service
- [ ] Explore future iOS support

**Also planned:** recover and check in the Supabase migration history, verify Microsoft sign-in end to end, persist Home swipe decisions and preferences, and add admin analytics and a warning system.

---

## 🤝 Contributing

Read [`AGENTS.md`](AGENTS.md) before making changes. In short: keep changes small and focused, respect context and layer boundaries, add tests for changed behavior, keep this README's backend binding table in sync with `AppContainer`, and run the checks above before opening a PR.

---

## 👥 Team JDRMS

| Role | Member |
| --- | --- |
| **Team Leader** | Minh Pham-Nguyen |
| **Team Member** | Sean Gallagher |
| **Team Member** | Jacob Ayoub |
| **Team Member** | Roger Carrillo |
| **G.O.A.T** | Dominic Alfonso |

---

<div align="center">

### 🎓 Built for students. Designed around campus trust.

**Bulletin — connect locally, transact confidently.**

</div>