<div align="center">

# 🎓 Bulletin
### Campus Connect Marketplace

**A student-focused marketplace for buying, selling, and connecting safely within a university community.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Course](https://img.shields.io/badge/CECS-491A%2FB-FFC72C?style=for-the-badge)
![Status](https://img.shields.io/badge/status-In%20Development-2563EB?style=for-the-badge)

</div>

---

## 📖 About Bulletin

**Bulletin** is a Kotlin Multiplatform campus marketplace designed to make peer-to-peer transactions between university students safer, simpler, and more convenient. Android is the current application platform; shared business logic and Compose UI live in `shared`.

Students can discover campus listings, buy or sell items, communicate with other students, build reputation through reviews, and manage their profiles. The app supports university-email verification and privacy-conscious campus discovery.

> **Project:** Bulletin
> **Course:** CECS 491A/B
> **Team:** JDRMS

---

## ✨ Core Features

| Feature | Description |
| --- | --- |
| 🎓 **Student Verification** | University-email registration and email confirmation through Supabase Auth. |
| 🛍️ **Campus Marketplace** | Browse listings and create, view, edit, or mark listings as sold. |
| 🔎 **Search & Filters** | Search listings and refine results with marketplace filters and sorting. |
| 🔖 **Saved Listings** | Bookmark listings for quick access later. |
| 💬 **Messaging** | Start conversations, view inbox and thread history, and report messages. |
| ⭐ **Ratings & Reviews** | Submit and view reviews associated with completed/sold listings. |
| 👤 **Student Profiles** | View and edit student profile information and profile photos. |
| 🚩 **Reporting & Blocking** | Report users or messages and block users. |
| 📍 **Campus Discovery** | Support campus-relevant discovery without exposing precise locations to other users. |
| 🏠 **Home Feed** | Browse marketplace listings and pass items for the current session. |

> Feature availability depends on backend configuration. Recommendation personalization and production readiness are not claimed by the repository status below.

---

## 📱 User Experience

The main product flow connects account setup with marketplace, messaging, and profile features:

```text
University Email Registration
             ↓
      Email Confirmation
             ↓
        Account / Login
             ↓
            Home
       ┌─────┼──────┐
       ↓     ↓      ↓
    Browse  Search  Profile
       ↓             ↓
 Listing Details  User Search
       ↓             ↓
Save / Message ←→ Student Profile
       ↓
 Reviews & Ratings
```

---

## 🧩 Functional Scope

### Account & Identity

- Register and sign in with a university email
- Confirm email ownership before login
- Request password recovery
- Edit a profile and view other student profiles
- Maintain a session through the authentication boundary

### Marketplace

- Create, browse, search, filter, and sort listings
- View listing and seller details
- Edit or mark a listing as sold
- Save and remove saved listings
- Submit reviews associated with sold listings

### Community & Communication

- Start conversations and view inbox/thread history
- Send messages
- Report messages or users
- Block and unblock users
- View profile and review information

### Home & Recommendations

The home feed is bound to `MarketplaceHomeRepository`, which reads through the shared marketplace visibility rules; home preferences remain in memory. Left swipe passes a listing for the current session, including across refresh and pagination. Passes have no undo UI and are cleared when the signed-in identity changes or the app session ends. Right swipe continues to use marketplace bookmarks. Personalized ranking and popular/recent fallback behavior remain product direction, not a verified live backend feature.

---

## 🏗️ Technical Direction

| Layer | Current implementation |
| --- | --- |
| **Mobile app** | Android with Kotlin and Jetpack Compose |
| **Shared code** | Kotlin Multiplatform and Compose Multiplatform in `shared` |
| **Backend** | Supabase client adapters for configured backend-backed contexts |
| **Authentication** | Supabase Auth adapter, with university-email validation in shared business logic |
| **Persistence** | Supabase PostgREST and Storage adapters; in-memory repositories for tests and local/inspection use |
| **Dependency injection** | Manual composition in `AppContainer` |
| **Build system** | Gradle wrapper and version catalog in `gradle/libs.versions.toml` |

---

## 🏛️ Repository Architecture

Bulletin follows a **DDD-inspired, feature-first clean architecture**:

```text
shared/src/commonMain/kotlin/com/jdrms/bulletin/
├── app/                  # Application composition, navigation, and manual DI (AppContainer)
├── core/                 # Shared technical concerns, design system, networking, database
└── domain/               # Business contexts and their layers
    ├── home/             # Home feed and preferences
    ├── marketplace/      # Marketplace catalog, search, bookmarks, reviews
    ├── listings/         # Create and manage student listings
    ├── messages/         # Conversations, inbox, and reporting
    └── profile/          # Student profiles, authentication, reputation
```

Business contexts use four layers:

- `domain/`: Domain types, policies, and repository contracts.
- `application/`: Use cases and workflow orchestration.
- `infrastructure/`: Supabase and in-memory adapters, DTOs, and mapping.
- `presentation/`: Compose screens, ViewModels, and UI state.

### Backend Status

`AppContainer` uses Supabase adapters when a configured client is available. In the normal Android runtime, backend-backed contexts fail to initialize if Supabase is not configured; in-memory fallback is disabled by default. Tests and inspection flows can use in-memory implementations, and local fallback can be explicitly enabled. Home reads through the marketplace listing source; home preferences and session passes remain in memory. These bindings describe code paths, not verification against a deployed Supabase project.

| Repository or port | Configured binding | In-memory use | Live project verification |
| --- | --- | --- | --- |
| Home feed | Supabase marketplace listing source when configured | Marketplace listing source with in-memory fallback | Not verified here; home preferences remain in-memory |
| Marketplace repository and listing source, including sold-listing review submission | Supabase adapters | In-memory/composite adapters | Not verified here |
| Listings repository, including buyer attribution when marking sold | Supabase adapter | In-memory adapter | Not verified here |
| Messages repository, thread observer, and inbox observer | Supabase adapters | In-memory repository/observers | Inbox broadcast tested locally; not verified against the live project |
| Profile repository | Supabase adapter | In-memory adapter | Not verified here |
| User report repository | Supabase adapter | In-memory adapter | Not verified here |
| User block repository | Supabase adapter | In-memory adapter | Not verified here |
| Profile photo repository | Supabase adapter | In-memory adapter | Not verified here |
| Auth and session repositories | Supabase Auth adapter | In-memory auth adapter | Not verified here |
| Android theme preference | Platform preference store | In-memory store for common/preview use | Not applicable |

Supabase schema, RLS, and storage policies are versioned under `supabase/migrations/`. Review those migrations and configure the matching project before using backend features. No live project validation is claimed here.

---

## 🔧 Local Setup

### Requirements

- JDK 21
- Android SDK configured for your environment
- Git

### Configure Supabase

The Android build reads configuration from a root `.env` file, environment variables, or Gradle properties. Copy `.env.example` to `.env` and set your Supabase project values:

```dotenv
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_API_KEY=your-anon-public-key
SUPABASE_IS_CONNECTED=true
```

The app must use only the Supabase anon/public key. Never put a service-role key, credentials, or real user data in the repository. Ensure the Supabase project has the schema and policies in `supabase/migrations/` applied.

### Build and Checks

```bash
./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :shared:testAndroidHostTest --no-daemon
./gradlew :shared:detekt :androidApp:detekt --no-daemon
```

Live Supabase tests are opt-in and require explicit configuration. Regular tests should not require live credentials or network access.

---

## 🔐 Security & Privacy

- Client-server traffic uses HTTPS through the backend client.
- Client applications use the Supabase anon/public key, never a service-role key.
- Database access must be protected by Row Level Security and storage policies.
- Message contents and precise locations must not be exposed to unauthorized users.
- Avoid logging credentials, session tokens, message contents, or precise locations.
- Account deletion, moderation, and privacy requirements must be validated against the configured backend and applicable policies.

These are implementation requirements and project goals; this README does not claim a production security audit.

---

## ⚡ Performance Goals

The following are project targets, not measured guarantees:

| Operation | Target |
| --- | ---: |
| App launch | `< 2 seconds` |
| Listing, search, profile, and feed results | `≤ 2 seconds` |
| Rating and report submissions | `≤ 3 seconds` |
| Account creation and authentication | `≤ 5 seconds` |
| Listing and profile updates | `≤ 5 seconds` |
| API responses | `< 200 ms` |

---

## 🗺️ Development Roadmap

- [ ] Validate authentication, email confirmation, and account lifecycle against the configured Supabase project
- [ ] Verify database RLS and storage policies for supported actors and operations
- [ ] Complete and validate marketplace listing workflows
- [ ] Complete and validate search, filters, sorting, and saved listings
- [ ] Validate user search and profile management
- [ ] Validate messaging, inbox updates, reporting, and blocking
- [ ] Validate sold-listing review workflows
- [ ] Develop and validate personalized home recommendations
- [ ] Measure performance against project targets
- [ ] Prepare user guide, privacy policy, and terms of service
- [ ] Explore future iOS support

---

## 🧪 Testing Priorities

Test successful workflows and meaningful failure cases, including:

- Invalid, unconfirmed, or already-registered email addresses
- Invalid credentials and password recovery failures
- Missing listing or profile fields and invalid prices
- Missing or deleted listings and profiles
- Network interruptions and backend errors
- Unauthorized access to messages, reports, profile data, or photos
- Blocked-user interactions and duplicate bookmarks
- Failed report and review submissions
- Recommendation behavior when personalization data is unavailable

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
