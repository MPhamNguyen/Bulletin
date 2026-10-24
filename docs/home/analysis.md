# Home swipe and messaging analysis

This describes the code on the Home swipe branch. The earlier analysis and step-2 ADR are unavailable; no historical ADR is reconstructed here. Backend bindings are code facts, not live Supabase verification.

## Semantics and data

Right saves through marketplace bookmarks; left passes. Decisions use the container-scoped `InMemorySwipeDecisionRepository`, clear on account change, and do not persist across process death. Guest Pass advances the loaded deck without recording a decision; Save and messaging require an account and configured ports.

Start Over replays loaded cards, clears Pass decisions, preserves Save exclusions, and disables immediate prefetch. Refresh fetches again while retaining session decisions. `HomeDeckPolicy` filters the displayed deck but preserves `nextCursor` from the unfiltered repository page. It continues past empty eligible pages, bounded at three consecutive empty pages. The Home source is `MarketplaceHomeRepository` over the configured marketplace listing source; preferences remain in memory.

## Ownership and handshakes

`HomeViewModel` owns feed loading and delegates card commands to `HomeCardInteraction`. `CardStateMachine` defines Idle, Swiping, Peeking, Open, Sending and Leaving; rejected events leave state unchanged. `HomeInteractionState` holds capabilities, draft/error/receipt, departure and pending effects. `HomeInteractionStore` rejects stale listing, viewer and lifecycle work.

`HomeSaveInteraction` records Save and requests an optimistic departure, then calls `SaveListingPort`. Only success emits `SaveConfirmed`. UI celebration waits until that listing's departure ends, invokes the app's Profile celebration callback when a target exists (or scale is zero), and acknowledges the effect ID. Missing geometry consumes the effect without an animated celebration. Failure offers a listing-specific Retry. Showing an offer acknowledges its effect independently from retry ownership; saved UI bookkeeping survives recreation with a retained VM. Destination changes dismiss offers and remove only the failed SAVE exclusion, preserving a later PASS. A new process does not restore retry authority or session decisions.

The UI acknowledges a matching departure ID after animation; only then does the deck advance. Message success stays Sending until the UI acknowledges its sent receipt dwell, then requests a SENT departure. Duplicate or stale acknowledgement IDs cannot advance another card. Failure restores the original draft with the tray open. Quick replies only fill the draft; button or IME Send explicitly submits. There is no flick-to-send.

Home owns `RecordSwipeDecision`, `SendQuickMessage`, `SellerMessage` (trimmed, nonblank, at most 200 characters), `SaveListingPort` and `ContactSellerPort`. The integration adapters call existing marketplace bookmark and messaging use cases. `MessagesHomeContactSellerAdapter` resolves seller identity from listing ID, checks ownership, availability and visibility, and removes the `listing:` prefix for messaging's listing reference. Home does not receive a marketplace entity or seller ID. `AppContainer` binds the adapters; other contexts' implementations and schema are unchanged.

## Input, accessibility and motion

The gesture wrapper sits outside the transformed card. Photo-zone taps retain the 38% left/right zones; motion past touch slop locks an axis and cancels child clicks. Horizontal distance or velocity resolves Save/Pass; short drags snap back. Leaving ignores further gestures and commands. A guest right swipe snaps back; guest Pass advances.

An upward drag or fast flick opens the tray without sending. Expanded descriptions consume vertical movement when they can scroll, retain ownership through the boundary and release fling, and leave the tray closed. A new drag at the boundary can open the tray; collapsed descriptions allow tray dragging. Horizontal swipes over descriptions remain card swipes. Description scrolling uses raw child deltas and its own fling, without forwarding nested scroll to the navbar.

Idle cards expose available Save, Pass and Message seller custom actions and focused arrow-key equivalents. Unavailable actions are omitted. Polite announcements cover confirmed Save, Pass, sent receipt, failures and account notices. Composer/card focus follows tray opening/closing. Back, predictive Back and Escape close an open tray; Sending and Leaving delegate Back. Keyboard insets reserve navbar space, keep the composer visible and pause photo playback while the keyboard is visible; closing the tray clears focus and hides it.

Android observes animator duration scale and rereads it on resume. Invalid settings fall back to 1; positive scales multiply durations, and zero completes motion and receipt handshakes without animation. `HomeMotionClock` avoids applying the system scale twice. Scale zero retains the celebration haptic request. Theme colors/typography/shapes and `BulletinFeedCardDefaults` supply styling; the card retains the 8 dp margin and theme large shape.

## Limitations and remaining checks

- `MessageSeller` has no idempotency key. Explicit retry after an ambiguous failure can duplicate a delivered message; there is no automatic send retry.
- Deck operations still call `getOrThrow()` on decision storage. A future fallible persistent adapter needs explicit failure handling.
- Save departure is optimistic: the card can animate toward Profile before a Save failure is known. Celebration requires confirmed success.
- Guest Pass has no recorded exclusion. Drafts and pending interaction work are deliberately transient.
- Saved-state composition recreation, Activity recreation mid-drag and a fresh VM are automated seams; actual OS process kill/relaunch mid-drag remains manual.
- TalkBack traversal/announcements, physical keyboard behavior, physical-device gestures/IME/rotation, haptic feel, crop/blur and animation appearance require manual checks. Emulator semantics and synthetic key events do not verify these.
- Retained step-30–33 crash buffers contain two Compose exit-easing failures near progress 1 and emulator UWB crashes, beyond the fixture failures. Passing later suites does not establish that the easing failure cannot recur.
