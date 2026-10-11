# AGENTS.md

These instructions apply throughout the Bulletin repository. A more specific `AGENTS.md` may add guidance for its subtree.

## How to work

- Start from the requested outcome. Inspect the relevant code, tests, `README.md`, and configuration before changing them.
- Make the smallest coherent change that solves the problem. Preserve unrelated user changes; do not reformat or refactor unrelated code.
- Follow existing conventions where they serve the design. If a convention makes the requested behavior unsafe or difficult to maintain, improve it in the affected area and explain why.
- Keep implementation, tests, documentation, and claims about the product in sync. Do not present planned or unverified work as complete.
- State what you changed, what you ran, the results, and any checks that could not run. Do not claim a build, test, or integration passed without running it.

For a business change, identify the owning context, the invariant or user intent, and the path from UI or API to
domain and adapter. For larger changes, note any context crossings, schema or RLS migrations, and tests before
implementing. Keep working notes proportional to the task; do not create repository files just for agent notes.

## Project facts and source of truth

Bulletin is a Kotlin Multiplatform campus marketplace. Android is the current application; shared business logic and Compose UI live in `shared`.

| Concern | Source of truth |
| --- | --- |
| Backend state per repository | `README.md` **Backend status** table and actual bindings in `AppContainer` |
| Shared UI tokens and components | `shared/src/commonMain/kotlin/com/jdrms/bulletin/core/designsystem/Theme.kt` and `Components.kt` |
| Dependency versions | `gradle/libs.versions.toml` |
| Static analysis | `config/detekt/detekt.yml` and the Gradle checks |
| Database schema, RLS, storage rules | `supabase/migrations/` |

Repositories are being migrated from in-memory implementations to Supabase adapters. Check the binding and status table before saying a context is live. Update the table when a binding changes. An adapter is not production-ready merely because code exists: verify its configuration, failure behavior, tests, and the relevant real environment before making that claim. In-memory implementations can remain as test fakes or a documented local development option.

Do not edit generated output or machine-local files such as `build/`, `.gradle/`, `.kotlin/`, `.idea/`, `local.properties`, or `.env`.

## Architecture and ownership

The documented bounded contexts are `home`, `marketplace`, `listings`, `messages`, and `profile`. Before moving a concept or adding a context, establish its responsibility and vocabulary in `README.md` and decide how it interacts with existing contexts. Treat the existing `recommendations` coupling to marketplace types as legacy debt, not a pattern to extend.

Business code under `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/` uses these responsibilities:

| Layer | Owns | Depends on |
| --- | --- | --- |
| `domain/` | Domain types, invariants, policies, repository contracts | Kotlin and domain-neutral `core` primitives |
| `application/` | Use cases, orchestration, application ports | Domain and shared primitives |
| `infrastructure/` | Supabase and local adapters, DTOs, persistence mapping | The inward contracts it implements |
| `presentation/` | ViewModels, UI state, Compose screens | Application behavior and UI primitives |

- Keep domain rules in domain types or policies. A screen, ViewModel, mapper, or database constraint alone must not decide a business invariant.
- Keep transport and storage types out of domain and presentation APIs. Application code uses ports rather than concrete infrastructure. Bind concrete implementations in `AppContainer`.
- A screen renders state and sends events to its ViewModel. A ViewModel coordinates a user flow through use cases; it does not call repositories or implement business policy. Keep navigation in the navigation layer and identity in a single session abstraction rather than inventing fallback user IDs.
- A context owns its aggregates, repositories, DTOs, and vocabulary. Do not import another context's entity, repository, DTO, mapper, ViewModel, or screen. Cross contexts through a typed ID when only identity is needed, or through a consumer-owned port, snapshot, event, or translation at the boundary.
- Express meaningful state changes through named operations that protect invariants. Use value objects and sealed states when they clarify the domain; avoid turning every value or small operation into a new abstraction.
- Use `com.jdrms.bulletin.core.common.Result` for expected failures. Keep I/O suspendable and off blocking coroutine paths. Keep platform-specific code in `androidMain` behind a suitable boundary when shared code needs it.

## Cohesion and repository limits

Design for a clear responsibility first. Before extending a class, ask:

1. Can you describe what it owns in one sentence? What kind of change would require editing it?
2. Do its methods, state, and dependencies support that responsibility, or form unrelated clusters?
3. Can a reader find a behavior by name, and can its tests focus on observable behavior without excessive setup?

A long file, growing constructor, wide public surface, repeated orchestration, many unrelated state flags, or frequent
changes for unrelated reasons is a prompt to investigate. A short file can still mix responsibilities. A cohesive
sealed hierarchy or mapper may reasonably be long. Extract along an actual responsibility boundary; avoid
`FooPart1`, pass-through layers, one-use wrappers, and vague `Manager`/`Helper` types. Keep related small types
together when separating them would obscure the concept.

The repository also has these **maximums**. They are constraints to satisfy, not targets for class design:

| Unit | Maximum |
| --- | ---: |
| Kotlin file | 400 lines |
| Composable screen file | 300 lines |
| ViewModel | 250 lines |
| Function | 40 lines |

The more specific limit applies alongside the Kotlin file limit. Check the actual Detekt configuration and CI for
how each limit is enforced. If a changed unit is over a limit, split at a meaningful responsibility or named step
and verify that behavior stays the same. Do not scatter one responsibility into numbered files just to pass a count.
If a cohesive design cannot meet a limit without harmful fragmentation, explain the conflict and propose a specific
policy and tooling change for review; do not silently ignore the limit. Do not add structural `@Suppress` entries or
Detekt baseline entries to hide a new violation.

When touching a legacy design problem, fix the part needed for the requested behavior and its tests. If the fix would
require a wider ownership decision or risky migration, finish safe independent work and explain the decision needed.
Do not silently extend a known boundary violation. Report unrelated problems only when they affect the change or
its verification.

## Supabase, privacy, and configuration

- Version table, RLS, and storage changes in `supabase/migrations/`; include matching adapter, DTO, mapper, and tests as needed. Keep the `README.md` backend status and `AppContainer` bindings aligned.
- Enable RLS on client-reachable tables and test policies for the actors and operations the feature supports. Domain invariants still belong in the domain.
- Client code uses only the Supabase anon key. Keep service-role keys, credentials, tokens, real user data, and machine configuration out of the repository. `.env.example` contains placeholders only.
- Never log passwords, session tokens, message contents, or precise locations. Keep tokens in the platform's secure storage abstraction. Other users must not be able to read message content or precise locations through the backend; enforce those restrictions with policies or other server-side controls.
- Translate backend failures at the adapter boundary into explicit application/domain failures. Retry only safe, idempotent operations with bounded attempts. Model required offline behavior explicitly instead of hiding it in an adapter.
- When changing an adapter or repository contract, verify the in-memory and Supabase implementations agree on observable behavior. Add shared contract tests where they give useful coverage. Live Supabase tests require explicit configuration and are opt-in.

## Compose and Kotlin

- Before changing visual UI, read `Theme.kt`. Use `MaterialTheme` colors, typography, and shapes; add reusable tokens centrally. Check `Components.kt` before duplicating a shared component. Use a local value for content-specific geometry when that is clearer.
- Keep composables driven by state. Represent multi-step flows with explicit states when combinations of flags could become invalid. Keep independent loading and error states separate when users can see or act on them independently.
- Scope a ViewModel to the screen or flow it serves. Keep transient snackbar messages as one-shot events rather than timer-cleared state; keep inline field errors in state. Obtain the acting user through the session abstraction, never a placeholder or a default ID.
- Expose ViewModel state read-only; use `MutableStateFlow.update` and `viewModelScope` when working with the repository's coroutine-based state pattern. Extract stateless sub-composables when a screen contains distinct layout concerns. Keep functions at a consistent level of abstraction.
- Follow the Kotlin style and Detekt configuration already in the repository. Add dependency versions through `gradle/libs.versions.toml`.
- Prefer manual dependency injection as used by `AppContainer` unless a broader change justifies changing the composition approach.

## Verification

Choose tests based on the behavior and risk of the change:

- For changed domain rules, validation, state transitions, mapping, error handling, or orchestration, test observable behavior, meaningful failure cases, and boundaries. A bug fix should include a regression test that fails without the fix.
- For changed repository behavior, check the contract against affected implementations. Test adapter translations and failures with deterministic fakes or a fake HTTP engine when relevant.
- Visual-only, copy, documentation, and mechanical changes do not need artificial unit tests. If UI event handling or state behavior changes, test the owning ViewModel or use case.
- Keep portable tests in `shared/src/commonTest` and Android-dependent tests in `shared/src/androidHostTest`. Do not make normal tests depend on real credentials, live services, wall-clock timing, or network availability.

Use the committed Gradle wrapper. Run the smallest relevant test while iterating; run broader checks appropriate to the change and available environment before handoff:

```bash
./gradlew :shared:testAndroidHostTest --no-daemon
./gradlew :shared:detekt :androidApp:detekt --no-daemon
./gradlew :androidApp:assembleDebug --no-daemon
./gradlew check --no-daemon
```

Builds require a configured Android SDK and JDK 21. If a check cannot run, identify the actual blocker and report it; run checks that remain available. Inspect `git status` and the final diff for unrelated edits, generated files, secrets, stale comments, and unintended API changes. `./gradlew format` changes files, so review its diff if used.

## Handoff

Summarize the behavior changed and the affected context. List the checks run and their outcomes, any environment blocker, and any material remaining risk. For backend changes, name the affected repository's current binding and relevant migration; for UI changes, name the theme tokens used or added. Keep the report proportional to the change.