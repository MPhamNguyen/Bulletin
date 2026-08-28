# AGENTS.md

These instructions apply to the entire repository unless a more specific `AGENTS.md` is added below a subdirectory.

## Project overview

Bulletin is an in-development campus marketplace. It is a Kotlin Multiplatform project that currently ships an Android application, with shared business logic and Compose UI in the `shared` module. The production backend is not wired up yet: most repositories are in-memory implementations, and `SupabaseConfig` is currently only an injection seam with placeholder values.

Keep implementation claims, documentation, and tests aligned with that current state. Do not describe a mocked or planned integration as production-ready.

Domain-driven design is mandatory for business changes. In this document, a business change is any change that adds or
alters domain behavior, business invariants, application orchestration, or data flow across a bounded-context boundary.
Documentation, formatting, build/configuration maintenance, mechanical refactors that preserve behavior, and visual-only
UI or copy polish are not business changes. When a change mixes exempt and business work, apply the business-change rules
to the affected behavior. Treat architectural boundary violations as defects, not optional cleanup.

## Repository map

- `androidApp/`: thin Android application shell, manifest, resources, and `MainActivity`.
- `shared/src/commonMain/`: shared Compose UI, application composition, core utilities, and business domains.
- `shared/src/androidMain/`: Android `actual` implementations for platform abstractions.
- `shared/src/commonTest/`: portable domain, repository, use-case, and ViewModel tests.
- `shared/src/androidHostTest/`: tests that require an Android host runtime.
- `config/detekt/detekt.yml`: static-analysis and formatting rules.
- `gradle/libs.versions.toml`: the single version catalog for plugins and dependencies.
- `.github/workflows/`: full/targeted Gradle checks and the standalone Detekt scan.

Do not edit or commit generated or machine-local content such as `build/`, `.gradle/`, `.kotlin/`, `.idea/`, `local.properties`, `.env`, or OS metadata.

## Domain-driven design rules (mandatory)

The current bounded contexts are `identity`, `marketplace`, `messaging`, `reputation`, and `recommendations`. Do not add a new context or move a concept between contexts without documenting its responsibility, language, ownership, and integrations.

Business code is organized by bounded context under `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/`. Each context has four layers:

- `domain/`: aggregates, entities, value objects, domain services, policies, domain events, and repository ports.
- `application/`: commands, queries, and focused use cases that coordinate domain objects and transaction boundaries.
- `infrastructure/`: DTOs, persistence/network adapters, mappers, and repository implementations.
- `presentation/`: immutable UI state, ViewModels, Compose screens, and feature UI.

The allowed dependency direction is `presentation -> application -> domain`. Infrastructure may depend inward to implement ports owned by the domain/application layers. The domain layer depends only on Kotlin and deliberately shared, domain-neutral primitives from `core`; it must never import Compose, Android APIs, ViewModels, DTOs, mappers, network/database clients, or concrete repositories. Application code must not import concrete infrastructure. Presentation code must not call repositories directly.

### Model the domain, not storage or screens

- Use the language from Bulletin's requirements consistently in types, methods, tests, and UI copy. Avoid vague additions such as `Manager`, `Helper`, `Util`, `Data`, or generic CRUD names when a domain term or use-case verb exists.
- Identify the aggregate root before adding state-changing behavior. External code may reference an aggregate by its typed ID, but only the aggregate root may authorize changes inside its consistency boundary.
- Put invariants in aggregate methods, value-object construction, or domain policies. Do not rely on a screen, ViewModel, DTO mapper, or database constraint as the only business validation.
- Prefer named domain operations such as `publish`, `reserve`, `report`, or `verify` over public mutable state or arbitrary `data class.copy`. Do not use `copy` to bypass an invariant.
- Make invalid states unrepresentable where practical with typed IDs, value objects, sealed types, and validated factories. Do not pass raw `String`, `Double`, or `Boolean` through multiple layers when it represents a domain concept with rules.
- Use a domain service only for stateless domain behavior that does not naturally belong to one entity or value object. Domain services must not perform I/O.
- Emit a domain event when a completed domain action must trigger work outside its aggregate or bounded context. Handle orchestration outside the aggregate; do not make aggregates call each other.

### Keep bounded contexts independent

- A context owns its entities, value objects, repository ports, DTOs, and vocabulary. Never import another context's infrastructure, DTO, mapper, ViewModel, or screen.
- Do not share aggregate entities or repository interfaces across contexts. Integrate through an application-facing port, an immutable published read model/event, or an anti-corruption mapper owned by the consuming context.
- A stable typed identifier from another context may cross a boundary when identity is all that is needed. Do not navigate from that ID to another context's aggregate from inside the domain model.
- Translate external and cross-context data at the boundary. Transport and persistence models must not leak into domain or presentation APIs.
- Existing direct coupling from `recommendations` to marketplace `Listing`, `ListingRepository`, `ListingDto`, and `ListingMapper` is legacy debt, not a pattern. Do not add more such imports. When changing that integration, introduce a recommendations-owned port and recommendation input snapshot, then map marketplace data at the boundary.
- Note pre-existing boundary violations encountered outside the requested scope in the handoff, including their paths, but
  do not fix them or broaden the patch unless asked. Never use a legacy violation as precedent. If the in-scope change must
  touch one, avoid worsening it and stop to propose a scoped boundary-safe change when that is not possible.

### Aggregates, repositories, and use cases

- Define repositories around aggregate roots and domain-oriented operations. Do not create generic DAO/base-repository abstractions or expose storage tables, DTOs, or unbounded mutable collections.
- Repository interfaces live in the owning context's domain layer; implementations live in infrastructure. Return domain objects or explicit domain results.
- Keep application use cases small and named for one user/business intent. They may load aggregates, invoke domain behavior, persist through ports, and publish events; business decisions remain in the domain.
- Use `com.jdrms.bulletin.core.common.Result` for expected business/application failures. Reserve thrown exceptions for programming errors or invalid value construction that cannot proceed.
- Keep infrastructure replaceable. In-memory and future Supabase adapters must obey the same domain contract and must not force backend-specific concepts into the model.

### Required workflow for business changes

Before implementation, record the following in the task's visible working notes. An AI agent must put it in a commentary
update; a human contributor may use the PR or change description. Do not add a scratch file to the repository unless the
task explicitly requests one.

1. The owning bounded context and the domain terms being used.
2. The aggregate root, entities/value objects, and invariants affected.
3. The command/query or use case that initiates the behavior.
4. The repository ports or domain events required.
5. Every bounded-context crossing and the translation mechanism used.

Then implement from the domain outward: domain model and tests, application use case, infrastructure adapter, presentation state/UI, and finally composition. If a requested design violates a boundary, stop and propose a domain-safe alternative instead of implementing the violation.

Example (architecture only, not a statement of implemented behavior): to add “reserve a listing,” identify `marketplace`
as the owner and `Listing` as the aggregate root; define and test the reservation invariants; initiate the behavior through
a focused `ReserveListing` command/use case and the marketplace-owned listing repository port; persist it with an
infrastructure adapter; expose it through immutable ViewModel state; then wire it in composition. If identity data is
needed, translate it through a marketplace-owned application port or immutable snapshot rather than importing identity
entities or repositories.

Place reusable code in `commonMain`. Add Android-specific code to `androidMain` only when a common implementation is impossible; expose platform behavior through an `expect`/`actual` boundary in `core/common`. Keep `androidApp` limited to platform startup and Android resources.

When adding a feature or vertical slice:

1. Add or extend the aggregate, value objects, domain behavior, events, and repository port.
2. Prove its invariants and state transitions with domain tests.
3. Add a focused application command/query use case.
4. Implement the adapter, DTO, and anti-corruption mapper in `infrastructure` when I/O is involved.
5. Model screen state explicitly and expose it from a ViewModel as read-only `StateFlow`.
6. Wire dependencies in `AppContainer`; update `AppDestination` and the root `App` only for top-level navigation.

The project intentionally uses manual dependency injection. Do not introduce a DI framework for a local change. Repository implementations should be replaceable through their domain interfaces so the current in-memory implementations can later be exchanged for real services.

## Kotlin and Compose conventions

- Follow the official Kotlin style configured in `gradle.properties` and the repository Detekt rules.
- Keep lines at or below 120 characters unless an unavoidable identifier or URL makes that impractical.
- Match the surrounding file's import and declaration style; Compose wildcard imports and PascalCase `@Composable` functions are explicitly permitted.
- Use value objects and domain policies for business invariants. Constructors may reject invalid values with `require`; recoverable workflow failures should use `com.jdrms.bulletin.core.common.Result`.
- Keep repository and use-case I/O `suspend`. Do not block coroutine threads.
- In ViewModels, mutate private `MutableStateFlow` values with `update`, expose them with `asStateFlow`, and launch asynchronous work in `viewModelScope`.
- Keep composables state-driven. Put business decisions in policies/use cases and state transitions in ViewModels, not in screen functions.
- Follow the mandatory design-system workflow below before creating or changing visual UI.
- Add dependencies and versions through `gradle/libs.versions.toml`; do not hard-code dependency versions in module build files.

## Design-system source of truth (mandatory)

Before making any visual or interaction design choice, open and follow:

`shared/src/commonMain/kotlin/com/jdrms/bulletin/core/designsystem/Theme.kt`

This file is the source of truth for Bulletin's theme. `BulletinTheme` and its `MaterialTheme` values must drive feature UI; a screen must not invent an independent palette or visual language.

- In design plans, working notes, and handoff summaries, point to `Theme.kt` and name the existing theme token being reused or the theme-level token that must be added.
- Read `Theme.kt` before choosing colors, typography, shapes, elevation, spacing, surface treatment, or light/dark behavior—even when the requested UI supplies its own visual reference.
- Consume colors through `MaterialTheme.colorScheme` rather than hard-coded `Color(...)` values in features. The current palette is defined by `LightColors` in `Theme.kt`.
- Consume typography and shapes through `MaterialTheme.typography` and `MaterialTheme.shapes`. If Bulletin needs a custom typography, shape, spacing, or elevation scale, define the centralized tokens in or alongside `Theme.kt` first, then consume them from features.
- Put reusable themed UI primitives in `shared/src/commonMain/kotlin/com/jdrms/bulletin/core/designsystem/Components.kt`. Check `SectionHeader` and `BulletinCard` before adding a feature-local equivalent.
- Keep feature composables concerned with layout and domain-specific content. Do not duplicate theme values or shared component styling across screens.
- A one-off value is acceptable only when it represents content-specific geometry rather than a reusable design token; document the reason in the code review or handoff.
- Any intentional change to the application's visual language must update `Theme.kt` first and include a visual review of all affected shared components and screens.

A UI change fails review if it introduces hard-coded feature colors, duplicates an existing design-system component, or makes a design decision without first grounding it in `Theme.kt`.

## Configuration and security

- Use `.env.example` only for documented placeholders. Never commit `.env`, service-role keys, private tokens, credentials, or real user data.
- Treat Supabase anonymous keys as configuration even if they are client-visible. Never substitute a privileged key into client code.
- Do not log passwords, session tokens, message contents, or precise user locations.
- Preserve the product's privacy model: proximity features must not expose precise locations to other users.
- Do not silently replace the in-memory repositories with network-backed implementations. Backend work must include explicit configuration, error handling, and tests.

## Build, analysis, and tests

Use the committed Gradle wrapper from the repository root. The build requires an Android SDK, and the committed Gradle daemon criteria request JDK 21.

```bash
./gradlew :shared:testAndroidHostTest --no-daemon
./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :shared:detekt :androidApp:detekt --no-daemon
./gradlew check --no-daemon
```

For a single domain suite, use the same class filters as CI, for example:

```bash
./gradlew :shared:testAndroidHostTest --tests "com.jdrms.bulletin.domain.marketplace.*" --no-daemon
```

Run the smallest relevant task while iterating, then run `./gradlew check --no-daemon` before handoff when the environment supports it. Run `./gradlew :androidApp:assembleDebug --no-daemon` for Android application, manifest, resource, dependency, or integration changes.

`./gradlew format` is a mutating task: it runs Detekt with auto-correction in subprojects and normalizes final newlines in supported project files. Use it intentionally and inspect the resulting diff. Detekt may warn that version 1.23.8 was built against an older Kotlin compiler; warnings are not permission to ignore reported rule violations.

## Testing expectations

Every change that adds or changes executable logic must add or update unit tests in the same patch. Logic includes validation, branching, calculations, transformations, mapping, filtering, sorting, ranking, state transitions, coroutine behavior, error handling, repository behavior, and ViewModel event handling. A logic change without unit tests is incomplete and must not be handed off.

- Write tests before or alongside the implementation so the desired behavior and boundaries are explicit.
- Test observable behavior and domain rules rather than private methods or implementation details.
- Cover the successful path, expected failure paths, and meaningful boundary values for every changed behavior.
- Every bug fix requires a regression test that would fail before the fix and pass afterward.
- A visual-only Compose change may omit a unit test only when it changes no event handling, state transition, semantics, formatting logic, or business behavior. If any of those change, test the ViewModel, use case, formatter, or state reducer that owns the behavior.
- If logic is difficult to unit test because it is coupled to Android, time, randomness, storage, or networking, introduce an interface or deterministic seam and test through it. Test difficulty is a design signal, not a reason to skip coverage.
- Put platform-independent tests in the matching package under `shared/src/commonTest`.
- Put tests that require Android APIs or resources in `shared/src/androidHostTest`.
- Use `kotlin.test` assertions and `kotlinx.coroutines.test.runTest` for suspend behavior.
- Prefer deterministic fakes or the existing in-memory repositories. Common tests must not depend on live services, real credentials, wall-clock timing, or network availability.
- When changing a repository contract, update every implementation, use case, DI binding, and affected test in the same change.
- Run the narrowest affected unit-test task while iterating and record the result. Before handoff, run the broader `./gradlew check --no-daemon` when the environment supports it; if it does not, report the exact blocker without claiming the tests passed.

### DDD acceptance gate

A business change is not complete unless all of the following are true:

- Every added or changed piece of logic has a corresponding unit test.
- Domain tests prove every added or changed invariant and aggregate state transition.
- Application tests prove orchestration and expected failures with fake/in-memory ports.
- Mapper/adapter tests cover any new external or cross-context translation.
- The dependency direction defined under “Domain-driven design rules” is preserved in every changed path.
- The canonical rules under “Keep bounded contexts independent” are satisfied; no new cross-context dependency is
  introduced.
- Names in code and tests use the bounded context's ubiquitous language.

Review boundary imports before handoff:

```bash
rg -n '^import (android\.|androidx\.|.*\.infrastructure\.)' \
  shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/*/domain
rg -n '\.infrastructure\.' \
  shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/*/application
rg -n '\.infrastructure\.' \
  shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/*/presentation
rg -n '^import com\.jdrms\.bulletin\.domain\.' \
  shared/src/commonMain/kotlin/com/jdrms/bulletin/domain
```

The first three commands must return no violations. Review every result from the fourth command for a cross-context
dependency; follow “Keep bounded contexts independent” when assessing results, and report out-of-scope legacy violations
as directed there.

## Change hygiene

- Inspect `git status` before editing and preserve unrelated user changes.
- Keep patches scoped; avoid drive-by reformatting or unrelated dependency upgrades.
- Never edit generated outputs to fix source behavior.
- If a new domain or source path should participate in targeted CI, update the path filters in `.github/workflows/ci.yml`.
- Keep `README.md` architecture or roadmap statements synchronized when a change materially alters the product scope or repository structure.
- Before handoff, review the diff for secrets, generated files, accidental API changes, and stale comments. Report which validation commands ran and any environmental blocker.
