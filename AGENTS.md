# AGENTS.md

These instructions apply to the entire repository unless a more specific `AGENTS.md` is added below a subdirectory.

## TL;DR: the rules that matter most

1. **Be honest about state.** Track per-repository status (in-memory vs. Supabase-backed) in the **Backend status**
   table in `README.md`. Never describe an integration as production-ready unless it has an adapter, error handling,
   and tests, and has been verified against a real Supabase project. Never describe planned work as done.
2. **Dependencies point inward:** `presentation -> application -> domain`; infrastructure implements ports. See
   "Architecture rules" (the single canonical statement).
3. **Contexts are independent.** No importing another context's entities, repositories, DTOs, mappers, ViewModels, or
   screens. Cross by typed ID, published read model/event, or a consumer-owned anti-corruption mapper.
4. **Invariants live in the domain,** never only in a screen, ViewModel, mapper, or database constraint. RLS and
   database constraints are defense in depth, not a substitute.
5. **Logic changes need unit tests in the same patch.** Bug fixes need a regression test.
6. **Respect the size limits.** Never suppress them. If the file you must change is over a limit, extract first.
7. **Fix nonconforming code you touch;** report (do not fix) violations you only noticed.
8. **UI uses `Theme.kt`.** No hard-coded colors, no feature-local palettes.
9. **Never commit secrets, generated files, or machine-local files.** Do not log passwords, tokens, message contents,
   or precise locations. The client uses only the Supabase anon key.
10. **Schema is code.** Database schema, RLS policies, and storage rules are versioned in the repository. No
    dashboard-only changes.
11. **Report honestly.** State which commands ran, which did not, and why.

Everything below expands on these. If two sections seem to conflict, the TL;DR priority order above breaks the tie.

## Project overview

Bulletin is an in-development campus marketplace. It is a Kotlin Multiplatform project that currently ships an Android
application, with shared business logic and Compose UI in the `shared` module.

Supabase is connected as the backend. Repositories migrate from in-memory to Supabase-backed adapters per bounded
context, so a given context may be live, partially migrated, or still in-memory. The **Backend status** table in
`README.md` is the source of truth for which repositories are Supabase-backed; check it (and `AppContainer`) before
making claims about what is live, and update it in the same patch whenever a binding changes. In-memory
implementations remain as test fakes and as a fallback for local development.

Keep implementation claims, documentation, and tests aligned with that current state.

Domain-driven design is mandatory for business changes. A **business change** is any change that adds or alters domain
behavior, business invariants, application orchestration, or data flow across a bounded-context boundary.
Documentation, formatting, build/configuration maintenance, mechanical refactors that preserve behavior, and
visual-only UI or copy polish are not business changes. When a change mixes exempt and business work, apply the
business-change rules to the affected behavior. Treat architectural boundary violations as defects, not optional
cleanup.

## Repository map

- `androidApp/`: thin Android application shell, manifest, resources, and `MainActivity`.
- `shared/src/commonMain/`: shared Compose UI, application composition, core utilities, and business domains.
- `shared/src/androidMain/`: Android `actual` implementations for platform abstractions.
- `shared/src/commonTest/`: portable domain, repository, use-case, and ViewModel tests.
- `shared/src/androidHostTest/`: tests that require an Android host runtime.
- `supabase/migrations/`: versioned schema, RLS policies, and storage rules (see "Supabase backend").
- `config/detekt/detekt.yml`: static-analysis and formatting rules.
- `gradle/libs.versions.toml`: the single version catalog for plugins and dependencies.
- `.github/workflows/`: full/targeted Gradle checks and the standalone Detekt scan.

Do not edit or commit generated or machine-local content such as `build/`, `.gradle/`, `.kotlin/`, `.idea/`,
`local.properties`, `.env`, or OS metadata.

## Architecture rules (canonical)

The current bounded contexts are `home`, `marketplace`, `listings`, `messages`, and `profile`. Do not add a new context
or move a concept between contexts without documenting its responsibility, language, ownership, and integrations.

Business code is organized by bounded context under
`shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/`. Each context has four layers:

- `domain/`: aggregates, entities, value objects, domain services, policies, domain events, and repository ports.
- `application/`: commands, queries, and focused use cases that coordinate domain objects and transaction boundaries.
- `infrastructure/`: DTOs, persistence/network adapters, mappers, and repository implementations.
- `presentation/`: immutable UI state, ViewModels, Compose screens, and feature UI.

### Dependency direction and call flow

This is the one canonical statement of layering. Other sections refer to it rather than restating it.

| Tier | Layers | Responsibility |
|---|---|---|
| Display | `presentation/` | Renders immutable state, forwards user events to ViewModels. |
| Business | `application/` + `domain/` | Use cases orchestrate; aggregates, value objects, and policies decide. |
| Data/API | `infrastructure/` | Talks to Supabase, network, database, and platform storage; maps to and from domain objects. |

- Allowed direction: `presentation -> application -> domain`. Infrastructure depends inward to implement ports owned by
  the domain/application layers. Do not skip tiers.
- **Domain** depends only on Kotlin and deliberately shared, domain-neutral primitives from `core`. It must never
  import Compose, Android APIs, ViewModels, DTOs, mappers, network/database clients, or concrete repositories.
- **Application** must not import concrete infrastructure. It reaches data only through ports it or the domain owns.
- **Presentation** reaches business behavior only through application use cases (via its ViewModel). Screens and
  ViewModels never call repositories, DTOs, mappers, or clients, and never make business decisions.
- **Infrastructure** implements ports, is the only tier that knows about external services and transport formats,
  never calls up into use cases/ViewModels/UI, and returns domain objects or explicit domain results, never DTOs.
- Concrete adapters are bound to ports only in `AppContainer`.

### Keep bounded contexts independent

- A context owns its entities, value objects, repository ports, DTOs, and vocabulary. Never import another context's
  infrastructure, DTO, mapper, ViewModel, or screen.
- Do not share aggregate entities or repository interfaces across contexts. Integrate through an application-facing
  port, an immutable published read model/event, or an anti-corruption mapper owned by the consuming context.
- A stable typed identifier from another context may cross a boundary when identity is all that is needed. Do not
  navigate from that ID to another context's aggregate from inside the domain model.
- Translate external and cross-context data at the boundary. Transport and persistence models must not leak into
  domain or presentation APIs.
- Existing direct coupling from `recommendations` to marketplace `Listing`, `ListingRepository`, `ListingDto`, and
  `ListingMapper` is legacy debt, not a pattern. Do not add more such imports. `recommendations` is not one of the
  documented bounded contexts above; when changing that integration, either document it as a context (responsibility,
  language, ownership, integrations) or fold it into an existing one, introduce a recommendations-owned port and
  recommendation input snapshot, and map marketplace data at the boundary.
- Violations encountered outside the requested scope are noted in the handoff with paths, not fixed, and never used as
  precedent. Code you are modifying is not "outside the requested scope": see "Fix nonconforming code you touch".

### Model the domain, not storage or screens

- Use the language from Bulletin's requirements consistently in types, methods, tests, and UI copy. Reuse terms already
  present in the context's code and `README.md`; avoid vague names such as `Manager`, `Helper`, `Util`, `Data`, or
  generic CRUD names when a domain term or use-case verb exists.
- Identify the aggregate root before adding state-changing behavior. External code may reference an aggregate by its
  typed ID, but only the aggregate root may authorize changes inside its consistency boundary.
- Put invariants in aggregate methods, value-object construction, or domain policies.
- Prefer named domain operations such as `publish`, `reserve`, `report`, or `verify` over public mutable state or
  arbitrary `data class.copy`. Do not use `copy` to bypass an invariant.
- Make invalid states unrepresentable where practical with typed IDs, value objects, sealed types, and validated
  factories. Do not pass raw `String`, `Double`, or `Boolean` through multiple layers when it represents a domain
  concept with rules.
- Use a domain service only for stateless domain behavior that does not naturally belong to one entity or value object.
  Domain services must not perform I/O.
- Emit a domain event when a completed domain action must trigger work outside its aggregate or bounded context. Handle
  orchestration outside the aggregate; do not make aggregates call each other.

### Aggregates, repositories, and use cases

- Define repositories around aggregate roots and domain-oriented operations. Do not create generic DAO/base-repository
  abstractions or expose storage tables, DTOs, or unbounded mutable collections.
- Repository interfaces live in the owning context's domain layer; implementations live in infrastructure.
- Keep use cases small and named for one user/business intent. They may load aggregates, invoke domain behavior,
  persist through ports, and publish events; business decisions remain in the domain.
- Use `com.jdrms.bulletin.core.common.Result` for expected business/application failures. Reserve thrown exceptions
  for programming errors or invalid value construction that cannot proceed.
- Keep infrastructure replaceable. In-memory and Supabase adapters must obey the same domain contract, and both must
  pass the same repository contract tests (see "Testing expectations"). Backend-specific concepts (row IDs, Postgres
  error codes, PostgREST filters, RLS details) must not leak into the domain or application layers.

## Supabase backend

Supabase is the production backend. These rules apply to every Supabase-backed adapter.

### Schema, RLS, and storage

- Schema, RLS policies, and storage rules are versioned under `supabase/migrations/`. Do not make dashboard-only
  changes; if one was made out of band, capture it as a migration in the same patch.
- Any patch that changes a table, column, policy, or storage rule includes the migration, the matching DTO/mapper
  changes, and tests. Call out every schema or RLS change in the handoff.
- RLS is defense in depth, not a substitute for domain invariants. Invariants still live in the domain.
- Every table reachable from the client has RLS enabled. A table without a policy is a defect, not a convenience.

### Binding an adapter

- A Supabase adapter replaces an in-memory one only in `AppContainer`, in a patch that also includes: error mapping
  to `Result`, adapter and mapper tests, a passing run of the repository contract tests against both implementations,
  and an update to the **Backend status** table in `README.md`.
- Do not half-migrate an aggregate. All repositories that participate in one aggregate's consistency boundary move
  together.
- Do not let the in-memory and Supabase adapters drift. Behavior change in one requires the same change (or a
  documented reason) in the other, enforced by the shared contract tests.

### Auth and session

- The session port (see "Ownership rules") is backed by Supabase Auth. Token storage, refresh, and expiry are handled
  in infrastructure and surfaced to the app only as session state.
- Never log or persist tokens outside the platform's secure storage abstraction.

### Error mapping, offline, and retries

- Adapters map transport and backend failures into explicit domain/application failures via `Result`: network
  unavailable, auth expired, not found, conflict, permission denied, and unexpected. Raw exceptions, HTTP status
  codes, and Postgres error codes never cross the port.
- Retry only idempotent operations, with bounded attempts and backoff owned by infrastructure. Use cases must not
  implement retry loops. If a feature needs offline behavior, model it explicitly in the domain/application layers
  rather than hiding it in an adapter.

### Environments and configuration

- Dev and prod use separate Supabase projects. Local and CI builds default to dev; prod credentials are injected only
  by the release pipeline.
- Inject configuration through `SupabaseConfig`, populated from untracked local configuration or CI secrets. Document
  placeholders in `.env.example` only.
- The client uses only the anon key. Service-role keys never appear in client code, CI logs, test fixtures, or the
  repository.

### Privacy enforced server-side

- Precise locations must be unreadable by other users at the database level (RLS or column-level protection), not
  merely hidden by the client. Proximity features expose only coarse or derived values to other users.
- Message contents are readable only by conversation participants, enforced by RLS.

## Workflow for business changes

### Step 1: Record working notes before implementing

An AI agent puts these in a commentary update; a human contributor may use the PR or change description. Do not add a
scratch file to the repository unless the task explicitly requests one.

**Fast path.** A change qualifies when all of these hold: it touches one bounded context and one aggregate; it changes
roughly 100 or fewer non-test lines; it adds no new port, domain event, or context crossing; it changes no schema or
RLS policy; and no touched file is over a size limit. Record one short note covering: owning context,
aggregate/invariant affected, the use case that initiates it, and the tests you will add. Then proceed.

**Full path.** For everything else, record:

1. The owning bounded context and the domain terms being used.
2. The aggregate root, entities/value objects, and invariants affected.
3. The command/query or use case that initiates the behavior.
4. The repository ports or domain events required.
5. Every bounded-context crossing and the translation mechanism used.
6. The ViewModel(s) and use case(s) that will own the behavior, and their current size against "Cohesion and size
   limits".
7. Any existing code on the call path that does not conform to these rules, and how it will be brought into
   conformance.
8. Any schema, RLS, or storage change, the migration that carries it, and whether each affected repository is
   in-memory or Supabase-backed.

Both paths still require the tests and the acceptance gate below.

### Step 2: Implement from the domain outward

1. Add or extend the aggregate, value objects, domain behavior, events, and repository port.
2. Prove its invariants and state transitions with domain tests.
3. Add a focused application command/query use case.
4. Implement the adapter, DTO, and anti-corruption mapper in `infrastructure` when I/O is involved, with the
   migration for any schema or RLS change.
5. Model screen state explicitly and expose it from a focused ViewModel as read-only `StateFlow`. Check the size limits
   first; if the feature does not fit an existing ViewModel within its limits, create a new one.
6. Wire dependencies in `AppContainer`; update `AppDestination` and the root `App` only for top-level navigation.

If a requested design violates a boundary, stop and propose a domain-safe alternative instead of implementing the
violation.

Example (architecture only, not a statement of implemented behavior): to add "reserve a listing," identify
`marketplace` as the owner and `Listing` as the aggregate root; define and test the reservation invariants; initiate
the behavior through a focused `ReserveListing` use case and the marketplace-owned listing repository port; persist it
with an infrastructure adapter (and a migration if the schema changes); expose it through immutable ViewModel state;
then wire it in composition. If identity data is needed, translate it through a marketplace-owned application port or
immutable snapshot rather than importing identity entities or repositories.

Place reusable code in `commonMain`. Add Android-specific code to `androidMain` only when a common implementation is
impossible; expose platform behavior through an `expect`/`actual` boundary in `core/common`. Keep `androidApp` limited
to platform startup and Android resources.

The project intentionally uses manual dependency injection. Do not introduce a DI framework for a local change.

## Fix nonconforming code you touch (mandatory)

Do not build on top of nonconforming code you are already editing. Bring it into conformance in the same patch.

Nonconforming code includes, for example:

- Presentation code that calls a repository, DTO, mapper, or other infrastructure directly, or that makes business
  decisions in a screen or ViewModel.
- Application code that imports concrete infrastructure, or domain code that imports Compose, Android APIs, DTOs, or
  concrete repositories.
- Business invariants that live only in a screen, ViewModel, mapper, RLS policy, or database constraint.
- Cross-context imports of another context's entities, repositories, DTOs, mappers, ViewModels, or screens.
- Classes or files over the limits in "Cohesion and size limits".
- Anything else that violates a mandatory rule in this document.

How to apply this:

1. **Scope is what you touch.** The code you modify and the direct call path of the behavior you are changing (screen ->
   ViewModel -> use case -> domain -> port -> adapter) must conform when you finish. This is in scope and is not a
   "drive-by refactor" under Change hygiene.
2. **Fix it in the right place.** Move logic to the layer that owns it, introduce the missing use case or port, add the
   anti-corruption mapper, or split the class. Do not wrap, re-export, or annotate around the violation.
3. **Keep fixes behavior-preserving** unless the task asks otherwise, and cover moved or corrected logic with tests in
   the same patch.
4. **Record the nonconformance and planned fix** in the working notes before implementing; report each fix in the
   handoff with paths.
5. **Do not copy or extend a nonconforming pattern,** even if it is the only local precedent.
6. **Do not widen the patch to untouched code.** Violations you only encountered, in files you did not need to change,
   are reported with paths, not fixed.

### When to stop and propose instead (escape hatch)

Stop before implementing and propose a scoped, boundary-safe alternative if **any** of these is true:

- The conforming fix needs a new bounded context or a cross-context ownership decision.
- The conformance work would touch more than one bounded context beyond the one the task targets.
- The conformance work would be more than about 3x the size of the requested change, or would push the non-test diff
  past roughly 400 changed lines.
- The fix cannot be made behavior-preserving, or cannot be covered by tests within the patch.

A proposal states: what is nonconforming, the smallest safe slice that could ship now, and what is deferred. Do not
implement on top of the violation while waiting.

## Cohesion and size limits (mandatory)

A class that grows past these limits is a design defect, not a style nit. Split it before adding to it.

### Hard limits

| Unit | Limit |
|---|---|
| ViewModel | ≤ 250 lines, ≤ 6 constructor dependencies, ≤ 15 public functions |
| UI state class | ≤ 15 properties |
| Use case | one public `invoke`, one user/business intent |
| Any Kotlin file | ≤ 400 lines |
| Composable screen file | ≤ 300 lines; extract stateless sub-composables |
| Function | ≤ 40 lines, cyclomatic complexity ≤ 15 |

Never use `@Suppress` (or a Detekt baseline entry) for `LargeClass`, `LongParameterList`, `TooManyFunctions`,
`LongMethod`, `CyclomaticComplexMethod`, or `ComplexCondition`. These rules are fixed by splitting, not silencing. The
Detekt baseline may only shrink, never grow.

### Ownership rules

- One ViewModel per screen or flow, with one state class. A ViewModel that serves several screens or flows is wrong.
- Scope ViewModels to the destination they serve (edit screen, registration flow, review dialog), not the whole
  feature.
- A ViewModel orchestrates one flow. It must not hold session logic, a navigation stack, transient message timers, or
  unrelated flows.
- **Session** (who is the current user) is a single app-scoped application port that other ViewModels observe, backed
  by Supabase Auth (see "Supabase backend"). Do not re-derive it per ViewModel. Do not use placeholder fallback IDs
  such as `UserId("current_student")`.
- **Navigation** belongs to the navigation layer. Sub-screens are destinations, not fields on UI state; no hand-written
  `openX`/`closeX` method pairs or `returnTo` state.
- **Transient messages** (flash/snackbar) are one-shot events through an injected messenger, not state fields cleared
  by timers. Inline field errors stay in state.
- **Multi-step workflows** (registration, verification, checkout) are modeled as a sealed state machine, not a set of
  booleans and nullable fields.
- Each independent async operation has its own loading state. One shared `isLoading` for unrelated operations is not
  allowed.
- Reviewer, actor, and current-user identity come from the session port, never from default arguments.
- Optional dependencies are not nullable constructor parameters. Require them, or bind a no-op implementation (null
  object) in `AppContainer`.
- A constructor property must not share a name with a method on the same class.

### Split triggers (any one means extract before continuing)

1. The same 3+ step orchestration appears in more than two places. Extract an application use case.
2. Two groups of properties or methods never read each other's state. Extract a second class.
3. Constructor dependencies cluster into groups that are never used together.
4. A new feature would add a new `isX`/`showX`/`xError` field to a state class already near its limit.
5. A class has more than one reason to change (e.g. auth, editing, and reviews all changing in one file).

### Extract before extend

If the file you must change already exceeds a limit above, do not add to it. First extract the part you are about to
modify into its own class (behavior-preserving, tests moved with it), then make the change in the new class. This
extraction is in scope and is not a "drive-by refactor." Report the extraction in the handoff. Pre-existing oversized
files you did not need to touch are reported, not fixed.

## Kotlin and Compose conventions

- Follow the official Kotlin style configured in `gradle.properties` and the repository Detekt rules.
- Keep lines at or below 120 characters unless an unavoidable identifier or URL makes that impractical.
- Match the surrounding file's import and declaration style; Compose wildcard imports and PascalCase `@Composable`
  functions are explicitly permitted.
- Constructors may reject invalid values with `require`; recoverable workflow failures use
  `com.jdrms.bulletin.core.common.Result`.
- Keep repository and use-case I/O `suspend`. Do not block coroutine threads.
- In ViewModels, mutate private `MutableStateFlow` values with `update`, expose them with `asStateFlow`, and launch
  asynchronous work in `viewModelScope`.
- Keep composables state-driven. Business decisions belong in policies/use cases and state transitions in ViewModels.
- Follow the design-system rules below before creating or changing visual UI.
- Add dependencies and versions through `gradle/libs.versions.toml`; do not hard-code versions in module build files.

## Design-system source of truth (mandatory)

Before making any visual or interaction design choice, open and follow:

`shared/src/commonMain/kotlin/com/jdrms/bulletin/core/designsystem/Theme.kt`

`BulletinTheme` and its `MaterialTheme` values must drive feature UI; a screen must not invent an independent palette
or visual language.

- In design plans, working notes, and handoff summaries, point to `Theme.kt` and name the existing theme token being
  reused or the theme-level token that must be added.
- Read `Theme.kt` before choosing colors, typography, shapes, elevation, spacing, surface treatment, or light/dark
  behavior, even when the requested UI supplies its own visual reference.
- Consume colors through `MaterialTheme.colorScheme` rather than hard-coded `Color(...)` values in features. The
  current palette is defined by `LightColors` in `Theme.kt`.
- Consume typography and shapes through `MaterialTheme.typography` and `MaterialTheme.shapes`. If Bulletin needs a
  custom typography, shape, spacing, or elevation scale, define the centralized tokens in or alongside `Theme.kt`
  first, then consume them from features.
- Put reusable themed UI primitives in
  `shared/src/commonMain/kotlin/com/jdrms/bulletin/core/designsystem/Components.kt`. Check `SectionHeader` and
  `BulletinCard` before adding a feature-local equivalent.
- Keep feature composables concerned with layout and domain-specific content.
- A one-off value is acceptable only when it represents content-specific geometry rather than a reusable design token;
  document the reason in the code review or handoff.
- Any intentional change to the application's visual language must update `Theme.kt` first and include a visual review
  of all affected shared components and screens.

A UI change fails review if it introduces hard-coded feature colors, duplicates an existing design-system component, or
makes a design decision without first grounding it in `Theme.kt`.

## Configuration and security

- Use `.env.example` only for documented placeholders. Never commit `.env`, service-role keys, private tokens,
  credentials, or real user data.
- Treat Supabase anonymous keys as configuration even if they are client-visible. Never substitute a privileged key
  into client code.
- Do not log passwords, session tokens, message contents, or precise user locations.
- Preserve the product's privacy model: proximity features must not expose precise locations to other users, and this
  is enforced at the database level, not only in the client (see "Supabase backend").
- Do not silently swap an in-memory repository for a Supabase-backed one, or the reverse. Every binding change in
  `AppContainer` follows "Binding an adapter" and updates the **Backend status** table in `README.md`.
- Backend work must include explicit configuration, error handling, and tests.

## Build, analysis, and tests

Use the committed Gradle wrapper from the repository root. The build requires an Android SDK, and the committed Gradle
daemon criteria request JDK 21.

```bash
./gradlew :shared:testAndroidHostTest --no-daemon
./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :shared:detekt :androidApp:detekt --no-daemon
./gradlew check --no-daemon
```

For a single domain suite, use the same class filters as CI:

```bash
./gradlew :shared:testAndroidHostTest --tests "com.jdrms.bulletin.domain.marketplace.*" --no-daemon
```

Run the smallest relevant task while iterating, then `./gradlew check --no-daemon` before handoff when the environment
supports it. Run `./gradlew :androidApp:assembleDebug --no-daemon` for Android application, manifest, resource,
dependency, or integration changes.

Tests against a live or local Supabase instance are opt-in. They run through a separate Gradle task (for example
`./gradlew :shared:integrationTest`), require explicit configuration, and are never part of `check`.

**If Gradle cannot run** (no Android SDK, no JDK 21, no network): report the exact blocker, run every check that still
works (the boundary and size `rg`/`find` commands below, and any standalone Detekt invocation), and state clearly that
tests were not executed. Never claim tests passed that did not run.

`./gradlew format` is a mutating task: it runs Detekt with auto-correction in subprojects and normalizes final newlines
in supported project files. Use it intentionally and inspect the resulting diff. Detekt may warn that version 1.23.8
was built against an older Kotlin compiler; warnings are not permission to ignore reported rule violations.

Detekt enforces the size and complexity limits (`LargeClass`, `TooManyFunctions`, `LongParameterList`, `LongMethod`,
`CyclomaticComplexMethod`, `NestedBlockDepth`) and forbids suppressing them (`ForbiddenSuppress`). Do not loosen these
thresholds in `config/detekt/detekt.yml`, and do not add entries to the Detekt baseline.

## Testing expectations

Every change that adds or changes executable logic must add or update unit tests in the same patch. Logic includes
validation, branching, calculations, transformations, mapping, filtering, sorting, ranking, state transitions,
coroutine behavior, error handling, repository behavior, and ViewModel event handling. A logic change without unit
tests is incomplete and must not be handed off.

- Write tests before or alongside the implementation so the desired behavior and boundaries are explicit.
- Test observable behavior and domain rules rather than private methods or implementation details.
- Cover the successful path, expected failure paths, and meaningful boundary values for every changed behavior.
- Every bug fix requires a regression test that would fail before the fix and pass afterward.
- A visual-only Compose change may omit a unit test only when it changes no event handling, state transition,
  semantics, formatting logic, or business behavior. Otherwise test the ViewModel, use case, formatter, or reducer that
  owns the behavior.
- If logic is hard to unit test because it is coupled to Android, time, randomness, storage, or networking, introduce
  an interface or deterministic seam and test through it. Test difficulty is a design signal, not a reason to skip
  coverage.
- When splitting a class, move its tests with the behavior and keep each new class testable with 2-3 fakes. If a test
  needs more than ~6 fakes, the class is still too broad.
- Logic moved between layers to fix nonconforming code needs tests in its new home, at the layer that now owns it.

**Repository contract tests.** Each repository port has one contract test suite in `commonTest`, parameterized over the
implementation. The in-memory adapter and the Supabase adapter must both pass it. A new or changed repository behavior
updates the contract suite first.

**Supabase adapter tests.** Test adapters with a fake HTTP engine (for example Ktor `MockEngine`) covering success,
auth-expired, conflict, permission-denied, not-found, and network-failure paths, and assert the resulting `Result`
failures. Mapper tests cover every DTO-to-domain and domain-to-DTO translation, including malformed or missing fields.

**Where and how:**

- Platform-independent tests go in the matching package under `shared/src/commonTest`; tests requiring Android APIs or
  resources go in `shared/src/androidHostTest`.
- Name the test file `<ClassUnderTest>Test` and mirror the production package. Name test methods after the behavior and
  condition (for example, `reserve_fails_when_listing_already_reserved`), in the language of the bounded context.
- Use `kotlin.test` assertions and `kotlinx.coroutines.test.runTest` for suspend behavior.
- Prefer deterministic fakes or the existing in-memory repositories. Common tests must not depend on live services,
  real credentials, wall-clock timing, or network availability.
- When changing a repository contract, update every implementation, use case, DI binding, and affected test in the same
  change.

### DDD acceptance gate

A business change is not complete unless all of the following are true:

- Every added or changed piece of logic has a corresponding unit test.
- Domain tests prove every added or changed invariant and aggregate state transition.
- Application tests prove orchestration and expected failures with fake/in-memory ports.
- Mapper/adapter tests cover any new external or cross-context translation.
- Repository contract tests pass against every implementation of a changed port.
- Any schema, RLS, or storage change ships as a migration under `supabase/migrations/`.
- The dependency direction and call flow in "Architecture rules" hold on every changed path.
- No new cross-context dependency is introduced.
- Any nonconforming code that was modified, or that sits on the changed call path, has been brought into conformance,
  or the blocker is reported with a proposed alternative.
- Names in code and tests use the bounded context's existing vocabulary.
- No changed class exceeds the size limits, and no size-related `@Suppress` was added.
- Any state class touched has no new boolean flag where a sealed state or separate flow state would fit.
- The **Backend status** table in `README.md` matches the bindings in `AppContainer`.

### Pre-handoff checks

Boundary imports:

```bash
rg -n '^import (android\.|androidx\.|.*\.infrastructure\.)' \
  shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/*/domain
rg -n '\.infrastructure\.' \
  shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/*/application
rg -n '\.infrastructure\.' \
  shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/*/presentation
```

These three must return no violations.

Cross-context imports (this one needs review, so filter it to files you changed):

```bash
git diff --name-only | rg 'domain/(\w+)/' | xargs -r rg -n '^import com\.jdrms\.bulletin\.domain\.'
```

For each result, confirm the imported context matches the file's own context. Any mismatch in a file you modified is
fixed per "Fix nonconforming code you touch"; mismatches elsewhere (run the unfiltered
`rg -n '^import com\.jdrms\.bulletin\.domain\.' shared/src/commonMain/kotlin/com/jdrms/bulletin/domain` if needed)
are reported with paths, not fixed.

Size and suppression:

```bash
# Any Kotlin file over 400 lines (must return nothing)
find shared/src -name '*.kt' -print0 | xargs -0 wc -l | awk '$1 > 400 && $2 != "total"'
# Size-rule suppressions (must return nothing)
rg -n '@Suppress\(.*(LargeClass|LongParameterList|TooManyFunctions|LongMethod)' shared/src androidApp/src
```

Secrets (must return nothing):

```bash
# Service-role keys or privileged tokens in client code or config
rg -n -i 'service[_-]?role' shared/src androidApp/src .env.example
```

## Change hygiene

- Inspect `git status` before editing and preserve unrelated user changes.
- Keep patches scoped; avoid drive-by reformatting or unrelated dependency upgrades. Extracting code out of an
  oversized class you must modify, and bringing code you are modifying into conformance, are in scope.
- Never edit generated outputs to fix source behavior.
- If a new domain or source path should participate in targeted CI, update the path filters in
  `.github/workflows/ci.yml`. Changes under `supabase/migrations/` should trigger the relevant checks.
- Keep `README.md` architecture, roadmap, and **Backend status** statements synchronized when a change materially
  alters the product scope, repository structure, or which repositories are Supabase-backed.

## Handoff report

Before handoff, review the diff for secrets, generated files, accidental API changes, and stale comments. Then report:

1. What changed, by bounded context.
2. Which validation commands ran, their results, and any environmental blocker.
3. Each conformance fix or extraction made to touched code, with paths.
4. Each out-of-scope violation noticed but not fixed, with paths.
5. Any proposal made under the escape hatch, and what was deferred.
6. For UI changes, the `Theme.kt` tokens reused or added.
7. For each affected repository, whether it is in-memory or Supabase-backed, plus any schema, RLS, or storage change
   and the migration that carries it.