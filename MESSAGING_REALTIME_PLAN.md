# Messaging Realtime and History Plan

## Goal

Deliver live updates for an open conversation and make message history load in bounded pages. Realtime and pagination solve different problems: subscriptions deliver new messages, while cursor pagination retrieves older history. Implement both as separate capabilities that work together.

## Current state

- `AppContainer` selects `SupabaseMessagesRepository` when a Supabase client is configured and otherwise can use `InMemoryMessagesRepository`.
- The shared module includes Supabase Auth and PostgREST, but no Realtime dependency or message subscription.
- `GetConversationMessages` calls `MessagesRepository.getMessages`; the Supabase adapter currently fetches the full conversation ordered by `created_at`, then `id` ascending.
- The messaging ViewModel reloads conversation data after sending. It does not observe messages sent by another participant.
- README messaging copy calls the feature real-time, which overstates the behavior currently implemented.
- Research supplied for this plan reports that the active Supabase project publishes `messages` inserts through `supabase_realtime`, has RLS enabled, and limits SELECT and INSERT to participants. The research also reports there is no composite index for the message ordering. Recheck these facts against the target project and committed schema before changing or documenting backend behavior.
- This checkout has no `supabase/migrations/` directory and no README Backend status table. A schema dump was made outside the repository at `/private/tmp/bulletin-public-schema.sql`; it is research output, not a versioned migration.

## Ownership and boundaries

- **Context:** `messages` owns the feature and its language.
- **Aggregate:** `Conversation` is the aggregate root; message insertion must continue to respect its participant and conversation invariants. A received event is untrusted input and must be decoded, scoped to the active conversation, and deduplicated before it is shown.
- **Use cases:** application use cases coordinate initial page loading, older-page loading, and subscription lifecycle through messages-owned ports. Presentation observes use-case output and forwards user actions; it does not call the repository or Supabase client.
- **Infrastructure:** Supabase Realtime and PostgREST implementations stay in messages infrastructure. Bind them in `AppContainer`. Keep the in-memory adapter behaviorally aligned with repository paging and observation contracts.
- **Context crossings:** none are required. Use the current messages context's conversation and sender IDs; the Supabase Auth session remains the source of authenticated identity.
- **Current call-path check:** `MessagesViewModel` is 114 lines, `MessagesScreen` 188, and `MessagesUiState` 14. They are within repository size limits at planning time. Recheck before implementation. Do not add Realtime transport handling directly to the screen.

## Proposed design

### 1. Live updates

1. Add the Supabase Kotlin Realtime dependency using the existing version-catalog convention and verify the API for the repository's pinned Supabase version.
2. Define a messages-owned subscription port that observes inserts for one `ConversationId` and exposes domain `Message` values or an explicit stream failure. Provide an in-memory implementation for tests and local fallback.
3. Implement the Supabase adapter with an authenticated Realtime channel filtered to the active conversation. Decode payloads through a messages infrastructure mapper; do not expose Supabase payload DTOs to application or presentation.
4. Start the subscription when a conversation is selected and stop/cancel the previous subscription when selection changes or the screen-scoped ViewModel is cleared. Handle channel status and recoverable subscription errors without silently implying that the stream remains connected.
5. Merge received messages in stable `(timestamp, id)` order and deduplicate by `MessageId`. This must tolerate overlap between a page response, the sender's PostgREST response, and a Realtime insert. Keep sending through the existing use case; a successful send should update the local UI from its returned message, with Realtime duplicates ignored, rather than requiring a full-history reload.
6. Continue relying on authenticated participant RLS and server-side Realtime authorization. Verify a participant can receive events only for conversations they belong to, and that a nonparticipant cannot subscribe/read events. Do not treat a client-side filter as access control.

### 2. Cursor-paginated history

1. Replace the unbounded `getMessages` contract with an explicit message page query/result, including a stable cursor based on both `created_at` and `id` and an indication of whether older history remains.
2. Load the newest page first, then request older pages as the user reaches the start of the conversation. Preserve chronological display order even if the database query fetches newest-first.
3. Use a strict lexicographic cursor over `(created_at, id)` so equal timestamps do not skip or repeat rows. Keep cursor semantics identical in Supabase and in-memory implementations.
4. Add a composite database index supporting the conversation filter and stable message ordering, likely `(conversation_id, created_at, id)`, after confirming the deployed schema and query plan. Commit any schema change as a migration under `supabase/migrations/`; do not rely on a dashboard-only change.
5. Make Realtime inserts and page loads converge: merge by ID, keep stable order, and ensure a new insert arriving while an older page is loading cannot be lost or duplicated.

## Implementation sequence

1. **Baseline and schema:** inspect `AppContainer`, the complete messages call path, configured Supabase version/docs, and the target project's schema/RLS/publication. Establish or add versioned schema migrations and a README Backend status table as required by repository instructions. Record whether messaging is Supabase-backed or in-memory in the README.
2. **Contracts and tests:** add repository/subscription contracts and deterministic in-memory implementations. Add tests for authorization scope, ordering, cursor boundaries with tied timestamps, empty/final pages, stream lifecycle, duplicate events, and page/event races.
3. **Pagination:** add the migration/index, Supabase query and mapper behavior, then implement page loading in the application and presentation layers. Keep adapters covered by the same repository contract suite.
4. **Realtime:** add the Supabase channel adapter and DTO mapping, wire the subscription through `AppContainer` and a focused application API, then connect selection lifecycle and message merging to the ViewModel. Add adapter tests using the available test seam and ViewModel tests for switching conversations, cancellation, errors, and deduplication.
5. **Copy and status:** update README feature language so it claims real-time behavior only after the client integration and access behavior are verified. Keep Backend status aligned with actual `AppContainer` bindings. Document the migration and verification evidence.
6. **Acceptance checks:** run the relevant common/host tests, repository contract tests against both adapters, Detekt, and the Android build when available. Verify against a real Supabase project that authenticated participants receive inserts only for their conversations and that pagination has no gaps or duplicates. Report any unavailable checks precisely.

## Acceptance criteria

- A participant viewing a conversation sees another participant's newly committed message without reopening or manually refreshing the conversation.
- Leaving or switching conversations stops delivery to the old conversation; event delivery and rendered messages remain participant-scoped.
- Sending displays the returned message promptly and does not duplicate it when its Realtime event arrives.
- Initial history is bounded; older messages load on demand in stable chronological order.
- Equal `created_at` timestamps do not produce gaps or duplicate rows across page boundaries.
- Messages arriving during paging are retained and appear once in the correct order.
- Supabase and in-memory repository implementations satisfy the same contract tests.
- Domain/application code remains independent of Supabase SDK and transport DTOs; all expected adapter failures are surfaced through the established `Result`/flow error handling.
- Any schema or RLS change is versioned, tested, and reflected in the README backend status; no claim of production readiness is made without real-project verification.

## Open verification items

- Confirm Realtime authorization behavior for the project's actual policies and authenticated client configuration, including reconnect and channel cleanup behavior.
- Confirm the deployed schema, existing indexes, and query plan against the exact target project; the supplied research says no matching composite index exists.
- Confirm the pinned Supabase Kotlin Realtime APIs and the test seams supported by that version before selecting concrete interfaces.
- Resolve the repository's schema-history gap by establishing committed migrations that accurately represent deployed messaging objects before adding an index/policy change.
