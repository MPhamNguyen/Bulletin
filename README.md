<div align="center">

# 🎓 Bulletin
### Campus Connect Marketplace

**A student-focused marketplace for buying, selling, and connecting safely within a university community.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Course](https://img.shields.io/badge/CECS-491A/B-FFC72C?style=for-the-badge)
![Status](https://img.shields.io/badge/status-In%20Development-2563EB?style=for-the-badge)

</div>

---

## 📖 About Bulletin

**Bulletin** is a mobile campus marketplace designed to make peer-to-peer transactions between university students safer, simpler, and more convenient.

Students can discover nearby campus listings, buy, sell, or rent items, communicate directly with other students, build reputation through ratings, and receive personalized listing recommendations. Access is intended for verified university students, helping create a trusted campus-centered community.

> **Project:** Bulletin <br>
> **Course:** CECS 491A/B <br>
> **Team:** JDRMS

---

## ✨ Core Features

| Feature | Description |
| --- | --- |
| 🎓 **Student Verification** | Account creation using a university-issued email and verification flow. |
| 🛍️ **Campus Marketplace** | Create, browse, search, view, and edit marketplace listings. |
| 🔎 **Search & Filters** | Find listings using keywords, category, price, condition, distance, and sorting options. |
| 🔖 **Saved Listings** | Bookmark listings for quick access later. |
| 💬 **Real-Time Messaging** | Contact other students and access conversation history. |
| ⭐ **Ratings & Reviews** | Build community trust through user reputation and reviews. |
| 👤 **Student Profiles** | View and edit profiles containing campus and student information. |
| 🚩 **Reporting** | Report inappropriate messages for moderation. |
| ✨ **Personalized Recommendations** | Surface listings based on campus, browsing history, saved items, preferences, and interactions. |
| 📍 **Proximity Awareness** | Support campus-relevant and proximity-based discovery without exposing precise locations to other users. |

---

## 📱 User Experience

The planned application flow includes:

```text
University Verification
        ↓
Account / Login
        ↓
       Home
   ┌────┼──────────────┐
   ↓    ↓              ↓
Browse  Recommendations  Profile
   ↓                    ↓
Listing Details      User Search
   ↓                    ↓
Save / Message ←→ Student Profile
        ↓
   Ratings & Reviews
```
---

## 🧩 Functional Scope

### Account & Identity
- Create an account using a school email
- Verify the email before account creation
- Log in securely
- Delete an account and associated data
- Edit profile information
- View other student profiles
- Search for users by username, email, or full name

### Marketplace
- Create new listings with a title, description, price, category, and photos
- Browse campus listings
- Search, filter, and sort results
- View complete listing and seller details
- Edit existing listings
- Save or unsave listings

### Community & Communication
- Message other users
- View conversation history
- Send messages and photos
- Report messages
- Rate and review other users
- View reputation information

### Recommendations
Bulletin is designed to generate a **Recommended for You** feed using signals such as:

- Campus
- Browsing history
- Saved listings
- Preferences
- Past interactions
- Proximity relevance

When personalization data is insufficient, a future fallback may use popular or recent listings.

---

## 🏗️ Technical Direction

The requirements describe the following planned technology direction:

| Layer | Planned / Considered Technology |
| --- | --- |
| **Mobile Client** | Kotlin / Android |
| **Future Platform** | iOS |
| **Backend / Cloud** | Supabase, AWS, or Firebase |
| **API Integration** | AWS APIs / AWS Amplify |
| **Real-Time Communication** | Secure WebSockets (`WSS`) |
| **Client–Server Traffic** | HTTPS |
| **Email Verification** | SMTP or secure transactional email API |
| **Notifications** | Firebase Cloud Messaging or Apple Push Notification Service |
| **Location** | Mobile OS geolocation services with privacy-conscious proximity data |

---

## 🏛️ Repository Architecture

Bulletin follows a **DDD-inspired + Feature-First Clean Architecture**:

```text
shared/src/commonMain/kotlin/com/jdrms/bulletin/
├── app/                  # Application composition, root Navigation, and manual DI (AppContainer)
├── core/                 # Shared technical concerns (common, designsystem, network, database)
└── domain/               # Business domains (bounded contexts)
    ├── home/             # Personalized Feed, Ranking Engine, User Preferences
    ├── marketplace/      # Marketplace Catalog, Search, Categories, Saved Items
    ├── listings/         # Create, Post, Publish, and Manage Student Listings
    ├── messages/         # Real-Time Chat, Inbox, Message Reporting
    └── profile/          # Student Profile, Identity, .edu Verification, Reputation
```

Each business domain adheres to a 4-layer structure:
* `domain/`: Pure Kotlin entities, value objects, domain policies, and repository interfaces.
* `application/`: Single-responsibility use cases orchestrating domain workflows.
* `infrastructure/`: Repository implementations, DTOs, and mappers.
* `presentation/`: Compose screens, ViewModels, UI State, and feature UI components.

---

## 🔐 Security & Privacy

Bulletin is intended to prioritize trust and student privacy.

- 🔒 HTTPS for client–server communication
- 🔑 Secure password hashing and storage
- 🎓 University-email-based student verification
- 🛡️ Role-based access control for sensitive functionality
- 💬 WSS for secure real-time messaging
- 📍 Only necessary proximity data should be transmitted; precise locations should not be exposed to other users
- 🗃️ Sensitive data should be encrypted at rest and in transit
- 🧹 Users should be able to request account deletion
- ⭐ Rating mechanisms should resist fake reviews and manipulation
- 🚩 Content/message reporting supports moderation
- 📜 The system is intended to account for applicable university policies and student privacy requirements

---

## ⚡ Performance Goals

| Operation | Target |
| --- | ---: |
| App launch | `< 2 seconds` |
| Listing/search/profile/recommendation results | `≤ 2 seconds` |
| Rating/report submissions | `≤ 3 seconds` |
| Account creation/authentication | `≤ 5 seconds` |
| Listing/account/profile-related updates | `≤ 5 seconds` |
| API responses | `< 200 ms` |

Personalized results should also refresh as user behavior changes without noticeable lag.

---

## 🗺️ Development Roadmap

- [ ] Finalize production backend/cloud provider
- [ ] Implement university email verification
- [ ] Complete authentication and account management
- [ ] Build marketplace listing CRUD functionality
- [ ] Add search, filtering, and sorting
- [ ] Implement saved listings
- [ ] Add user search and profile management
- [ ] Implement secure real-time messaging
- [ ] Add message reporting and moderation workflow
- [ ] Implement ratings and reviews
- [ ] Add personalized recommendations
- [ ] Validate security/privacy requirements
- [ ] Benchmark performance requirements
- [ ] Prepare user guide, privacy policy, and terms of service
- [ ] Explore future iOS support

---

## 🧪 Testing Priorities

Testing should cover both successful workflows and documented failure cases, including:

- Invalid or already-used school emails
- Invalid verification codes or passwords
- Missing listing/profile fields
- Invalid prices and unsupported image uploads
- Network interruptions
- Missing/deleted listings or profiles
- Blocked-user interactions
- Duplicate bookmarks
- Failed report submissions
- Recommendation service failures or insufficient personalization data
- Performance targets for authentication, search, messaging, profiles, and recommendations

---

## 👥 Team JDRMS

| Role | Member |
| --- | --- |
| **Team Leader** | Minh Pham-Nguyen |
| **Team Member** | Sean Gallagher |
| **Team Member** | Jacob Ayoub |
| **Team Member** | Roger Carrillo |
| **Team Member** | Dominic Alfonso |

---

<div align="center">

### 🎓 Built for students. Designed around campus trust.

**Bulletin — connect locally, transact confidently.**

</div>
