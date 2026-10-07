# PR 44 Code Review — Capstone Scope Triage

PR: `feat(profile): add soft-delete account flow`

## Recommendation

For a senior design/capstone MVP, address the items marked **before merge** below. Defer the production-scale items unless the team is presenting Supabase as a supported production backend. The repository describes Supabase as an injection seam with placeholder configuration and most repositories as in-memory, so this PR should not be blocked on a full production account-erasure platform.

The focused profile test suite passed:

```text
./gradlew :shared:testAndroidHostTest --tests 'com.jdrms.bulletin.domain.profile.*' --no-daemon
```

## Address before merge

### 1. Align the deletion dialog with the behavior actually implemented

Locations:

- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/profile/presentation/DeleteProfileConfirmationDialog.kt:149-167`
- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/profile/application/ProfileUseCases.kt:133-145`

The dialog says active listings are “removed now”, messages are “gone for other students too”, campus verification can be redone to rejoin, and the action “can’t be undone”. This PR only sets `profiles.deleted_at` and signs out. It does not delete or hide listings/messages, and it does not implement a restore/rejoin flow.

For the capstone scope, the smallest correct fix is to change the copy to describe a profile soft-delete and sign-out. Do not add direct profile-to-listings/messages infrastructure coupling just to satisfy the current copy. If those side effects are desired as a showcase feature, they need a separately scoped cross-context design using ports/events and tests.

### 2. Do not let accessibility activation bypass the deliberate-delete safeguard

Location: `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/profile/presentation/DeleteProfileConfirmationDialog.kt:328-333`

The visible control requires a 1.5-second hold, but the semantic `onClick` calls `onConfirm()` immediately. A TalkBack, keyboard, or switch-access activation can therefore delete the profile without the hold. This is a correctness and usability issue, not production polish. Provide an equivalent deliberate accessible confirmation path, or replace the custom hold interaction with a standard confirmation button. Add a semantics/UI regression test.

### 3. Handle sign-out failure explicitly

Location: `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/profile/application/ProfileUseCases.kt:139-142`

`signOutUser()` is called but its `Result` is ignored. The use case reports success even if session cleanup fails. For a capstone, it is acceptable to make sign-out best-effort after the profile write, but the failure must be explicit and tested; at minimum local session state must not be presented as safely completed while cleanup failed.

### 4. Remove the presentation fallback that exposes the repository from `ManageProfile`

Locations:

- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/profile/application/ProfileUseCases.kt:83-88`
- `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/profile/presentation/ProfileViewModel.kt:54-55`

Making `ManageProfile.profileRepository` public only lets the ViewModel construct a missing use case. The manual DI container already provides `SoftDeleteProfile`. Make the dependency required in the ViewModel and update the test helper, or move construction to composition. This is worth fixing in a capstone because it demonstrates the required `presentation -> application -> domain` direction and keeps the architecture explainable during review.

### 5. Add a regression test for deletion failure and real sign-out

The current success tests do not prove that an authenticated session was actually signed out: the in-memory test user is not logged in before deletion, and the ViewModel sets its own unauthenticated state. Add tests that:

- start with an authenticated user;
- verify the auth repository has no current session after success;
- verify repository failure does not sign out;
- verify sign-out failure is surfaced or handled according to the chosen contract.

These are small, high-value tests for the feature's observable behavior.

## Address if Supabase is presented as supported

### 6. Supabase soft-delete can report success when zero rows were updated

Location: `shared/src/commonMain/kotlin/com/jdrms/bulletin/domain/profile/infrastructure/repository/SupabaseProfileRepositories.kt:75-91`

The adapter returns `Result.Success(Unit)` after the update without checking whether a row matched. If the capstone demo uses only the in-memory adapter, this can be deferred with a clear limitation in README/demo notes. If Supabase is part of the supported flow, verify the updated row or use an RPC that returns a deletion result, and add an adapter test for the no-row case.

### 7. Backend enforcement and retention policy

Do not add a large store-compliance/RLS/purge system to this capstone PR unless the team is claiming production account deletion. If Supabase is presented as production-ready, the missing pieces become a separate backend task: server-side authorization for `deleted_at`, preventing deleted users from using other contexts, auth-session revocation, and a documented retention/purge policy. The repository says the backend is not wired up, so this is scope, not an MVP blocker.

## Defer for this capstone

These are reasonable future improvements but should not block this PR's core demo:

- Replace the timestamp `String` with a validated shared time value object (`ProfileModels.kt:78-94`). Keep the current injectable clock seam documented for now.
- Enforce repeated/blank deletion checks again inside every repository adapter (`InMemoryProfileRepositories.kt:42-45`). The application use case owns the normal path; harden the repository contract later.
- Refactor the 366-line dialog into reusable design-system primitives.
- Move repeated feature-local shapes into `Theme.kt` and remove unused button color tokens.
- Remove the unused `showDeleteConfirmation` parameter and clarify `onDeleteProfile` naming.
- Add large-font, landscape, haptics, animation-scale, cache, notification, and navigation-back-stack handling. These are useful production polish, but are beyond this slice unless the capstone demo explicitly targets them.

## Important review correction

`ProfileMapper.toDto` including `deletedAt` is not, by itself, a stale-update resurrection bug in this PR: `SupabaseProfileRepository.updateProfile` uses `ProfileMapper.toUpdateDto`, and `ProfileUpdateDto` does not contain `deleted_at`. Do not prioritize that concern based only on `toDto`.

## Validation

- Focused profile tests: passed.
- Full `./gradlew check --no-daemon`: not run.
- Only this review document was updated during scope triage.
