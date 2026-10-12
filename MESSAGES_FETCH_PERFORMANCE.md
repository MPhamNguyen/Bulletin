# Messages Inbox Fetch Performance

## Summary

The inbox fetch takes 207-300 ms in the app. A read-only `EXPLAIN ANALYZE` of an equivalent query, run as `authenticated` with RLS active, took about **4 ms** for a representative user. The database is therefore unlikely to be the bottleneck. The remaining time sits in the HTTP round trip, Supabase/PostgREST request handling, response decoding, client-side mapping, or some combination.

**Recommendation:** add finer-grained timing before changing the schema. Indexes are not the right fix for today's data volume, but they are listed below as growth candidates.

## Request path

Bulletin is a Kotlin Multiplatform app. The shared code lives under `shared/src/commonMain/kotlin/com/jdrms/bulletin/`. Features are bounded contexts with presentation, application, domain, and infrastructure layers. The inbox belongs to the `messages` context.

- `MessagesRepository` is the repository port.
- `GetConversations` is the application use case. It coordinates sender lookup and inbox retrieval.
- `SupabaseMessagesRepository` implements the port.
- `AppContainer` binds the Supabase adapter when a Supabase client is configured, and the in-memory repository otherwise.

The README backend-status table describes configured bindings, not live-project verification.

```text
MessagesViewModel
  -> GetConversations
     -> AuthMessageSenderProvider
        -> RestoreAuthenticatedProfile (profile context)
        -> messages-owned MessageSender snapshot
     -> MessagesRepository.getConversations(sender.id)
        -> SupabaseMessagesRepository
           -> one PostgREST inbox request
           -> distinct, map DTOs to conversations, sort by update time
```

The messages context reaches the profile context only through the application-facing `RestoreAuthenticatedProfile` use case. The adapter translates the returned profile into the messages-owned `MessageSender` snapshot.

## What the app timings measure

| Timer | Scope | Notes |
| --- | --- | --- |
| `sender_resolution` | Profile restore and snapshot conversion in `AuthMessageSenderProvider` | Timing is in whole milliseconds, so `0 ms` means under 1 ms at that sample's precision. |
| `inbox_retrieval` | `SupabaseMessagesRepository.getConversations` | Includes the PostgREST request, response transfer and decoding, de-duplication, DTO mapping, and sorting. Excludes sender resolution. |

`ConsoleMessagesInstrumentation` emits duration, result count, call count, and outcome. It does not record payload contents. A 207 ms sample, or the roughly 300 ms observed earlier, is therefore an end-to-end client-side repository duration. It is not a measurement of Postgres execution.

The inbox request embeds participant and latest-message preview data in a single PostgREST request, and the latest-message relation is limited to one row per conversation. This batching already fixed the earlier 1 + 3N read pattern. The remaining latency is around that single request and its client-side processing.

## Evidence

Read-only Supabase CLI queries against the active Bulletin project reported:

| Table | Rows |
| --- | ---: |
| `conversations` | 6 |
| `conversation_participants` | 12 |
| `messages` | 19 |
| `message_reports` | 3 |

**Indexes.** Only primary keys exist:

- `conversation_participants(conversation_id, user_id)`
- `conversations(id)`
- `messages(id)`
- `message_reports(id)`

**RLS.** RLS is enabled on all four tables. Conversation, participant, and message reads use `private.is_conversation_participant`, and report reads use `private.can_access_message`. Both are stable `SECURITY DEFINER` SQL functions. The participant helper checks `(conversation_id, user_id)`, which the participant primary key covers.

**Query plan.** The plan used the participant and conversation primary keys. It sequentially scanned 19 messages to find the latest one, and 3 reports for the joined report check. Those scans are expected at this table size, and execution was still about 4 ms. The plan returned one conversation for the sampled user.

**Limits of the evidence:**

- The plan comes from an equivalent SQL query, not an `EXPLAIN` of the exact PostgREST request. It shows database cost, not an exact end-to-end comparison.
- Supabase query statistics did not include the inbox statement among the returned top statements, so there is no independent production duration for this request.
- Only one user's plan was sampled.

## Recommended next steps

### 1. Split the measurement (first priority)

Keep the existing `inbox_retrieval` total and add separate timers around:

- the Ktor request (time to response headers, then body transfer)
- body decode
- distinct, mapping, and sorting

Also record response byte size. Never log message bodies, tokens, or other private payloads.

This shows which component owns the 200-300 ms before any change is made.

### 2. Sample properly

Collect at least 10 cold and 10 warm samples on the same device and network, and compare median and p95 rather than a single opening. A first request may include DNS, TCP, and TLS setup that later requests avoid.

### 3. Isolate the app from the network

Send the same authenticated request outside the app, for example with a command-line HTTP client from the same network. If that takes similar time, the cost is network or server-side, not Ktor or decoding. If it is much faster, the cost is in the client stack or the mapping code.

### 4. Act on what the split shows

| If the data shows... | Then investigate... |
| --- | --- |
| Time to first byte dominates | Network path, Supabase project region relative to the client, and connection reuse (is the Ktor client a long-lived instance, or recreated per request?). Compare from a second device and network. |
| Body transfer or decode dominates | Payload size and shape. Review the selected columns and embedded relations, and trim anything the inbox list does not render. |
| Mapping and sorting dominate | The client-side processing. At 6 conversations this would be surprising, so it would point to a bug or an accidental heavy operation. |
| Database time grows later | The index candidates below, then the read-model option. |

### 5. Improve perceived latency if the inbox should feel instant

Keep a short-lived per-user inbox snapshot and show it immediately, then refresh from the server in the background (stale-while-revalidate). Invalidate or update the snapshot on account change and on message activity. This improves perceived speed but does not reduce the actual fetch time. It is also the only option here that helps regardless of where the latency turns out to be.

## Growth index candidates

At 19 messages, Postgres will often choose sequential scans even when indexes exist, so these will not meaningfully reduce today's 200-300 ms. Add them only when table sizes grow or a real query plan shows they help. Indexes cost writes and storage.

```sql
create index on public.conversation_participants (user_id, conversation_id);

create index on public.messages (conversation_id, created_at desc, id desc);

create index on public.message_reports (message_id);
```

- **`conversation_participants (user_id, conversation_id)`** matches the inbox's membership filter by user. The existing primary key leads with `conversation_id`, so lookups by user alone cannot use it efficiently.
- **`messages (conversation_id, created_at desc, id desc)`** lets Postgres find the newest message per conversation without scanning and sorting all of that conversation's messages.
- **`message_reports (message_id)`** supports the report-to-message relationship used for the latest-message preview.

Validate any index against a real plan before shipping it. Supabase's [query optimization guide](https://supabase.com/docs/guides/database/query-optimization) and [Index Advisor](https://supabase.com/docs/guides/database/extensions/index_advisor) cover this.

## If database time becomes meaningful later

Compare the current embedded PostgREST query against a Messages-owned SQL function or read model that selects the latest message row first, then joins its sender profile and report markers. This could reduce planning and join cost at scale. It would not remove the client-to-server round trip, so it only helps if the plan shows database time.

## Scope and safety

All project inspection and query-plan commands were read-only. No database objects, indexes, RLS policies, or data were changed. Measure the actual authenticated app request before selecting a production change.