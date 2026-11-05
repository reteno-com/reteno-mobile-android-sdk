# Android SDK InApp delivery plan and implementation

Status: the November 4, 2026 contract refinement is committed and pushed. Native
certification and release are pending. Earlier six-pass review records cover
the previous contract.

This document covers the Android SDK implementation, API expectations, and
acceptance checks. Source and test references resolve within this repository.

## Implementation and acceptance

| Area | Current implementation | Acceptance |
| --- | --- | --- |
| Discovery | Optional IID, absolute expiry, and backend priority. No workflow flag or query opt-in. | Mixed ordinary/server entries, distinct IIDs sharing a snapshot, legacy response defaults. Backend isolates unsupported SDK versions. |
| Content | Received IID selects online IID/snapshot content authorization. Ordinary snapshots retain their existing content request. | Original IID, raw snapshot, prepared per-delivery personalization; no cross-account or offline first display. |
| Rules and priority | Evaluate the prepared rules normally. Higher backend priority wins among eligible candidates; default priority is 10. | Priority 20 beats 10, failed eligibility permits fallback, and asynchronous eligibility cannot invert priority. Visible UI is not preempted. |
| Local state | Existing account-scoped storage retains IID, expiry, suppression, and separate queued observations. | Restart, identity switch, repeated discovery, and lost acknowledgment do not redisplay a completed IID. |
| Observations | Standalone InApp OPENED/CLICKED/FAILED use POST. `serverAssignedIid` reflects received versus generated identity and is retained with retries. | Preserve original IID/time/body and complete CTA; missing/false provenance follows ordinary processing. Push transport stays unchanged. |
| Failure | OPENED, CLICKED, and FAILED establish local suppression for a received IID. | Server synchronously ends contact availability; local state survives ACK loss, queue exhaustion, and reopen. |

The SDK has no workflow business mode, TTL default, or Velocity engine. It
applies the sender's prepared JSON values and reports OPENED only after actual
visibility. A content fetch or initialization alone is not OPENED. Dismissal
stays local. For a server-assigned IID, HTTP 200 follows server delivery
completion and observation publication. A failed or lost response does not
restore eligibility. Retry the original IID, time, and body through the
existing persisted queue.

## Remaining acceptance

Run native tests, render on devices, certify supported wrappers and renderer
versions, and exercise sender → SDK → analytics with real services. Verify
upgrade from released local storage, account isolation, pause/resume,
backgrounding, native loading failures, and late CTA events. Configure backend
minimum SDK versions only after those versions are certified. This plan does
not authorize publication or deployment.

## Android implementation and verification

[FetchInAppMessagesAction](../../RetenoSdkCore/src/main/java/com/reteno/core/internal/action/iam/FetchInAppMessagesAction.kt),
[ShowInAppAction](../../RetenoSdkCore/src/main/java/com/reteno/core/internal/action/iam/ShowInAppAction.kt),
and [InAppPresenter](../../RetenoSdkCore/src/main/java/com/reteno/core/internal/iam/InAppPresenter.kt)
use the existing coroutine Actions, DI graph, queue, and presenter.

Version 12 storage keeps the existing physical column names and upgrade order.
No further schema migration is introduced by this refinement. Queued account
context retains IID provenance. Ordinary frequency history remains intact.
Server-delivery content is not persisted for offline display. Priority is
retained in memory for conditional responses; a fresh process fetches a full
list before using cached server deliveries, without changing stored formats.

The observation sync uses the existing queue, three delivery attempts, bounded
backoff, Retry-After, and context-bound authentication. Completing or exhausting
an observation preserves the local consumed marker until expiry/cleanup.
POSTPONE preserves an undisplayed candidate; SKIP emits no OPENED. Actual native
presentation completion establishes visibility; push keeps its existing path.

## Review findings and resolutions

The November 4 contract-alignment and follow-up reviews identified the following
Android defects. All are fixed in the committed implementation. A subsequent
review also found that profile updates could change delivery ownership; the
fix keeps ownership stable for the same identified account.

| Finding | Failure scenario | Resolution |
| --- | --- | --- |
| Eligibility fallback stops too early | The preferred delivery becomes unavailable during content authorization or presentation preparation, and the next eligible message is never offered. | Selection retains the candidate list and continues after a rejected delivery. Cancellation, a busy presenter, postponement, or visible UI do not trigger a competing display. |
| Cached deliveries bypass fresh discovery | After restart, cached server deliveries can use stale rules or default priority before discovery succeeds. | Server deliveries wait for fresh discovery; ordinary offline messages remain available. Conditional responses retain the current process's priorities, and refreshed expiry cannot extend the received deadline. |
| Queued ordinary clicks lose account ownership | A queued click can be retried under a different account, or account metadata can incorrectly mark a generated IID as server-assigned. | Persist the original account context independently of IID provenance and replay with that context. Ordinary clicks retain generated-IID provenance. |
| Pause/resume loses fallback candidates | Only the preferred message is postponed. If it expires or is rejected after resume, the ordinary candidate from the same trigger is lost; a changed account can receive the postponed offer. | Retain candidates, the original account, and the rejection continuation through pause/resume, including renderer preparation. Resume re-evaluates eligibility and abandons offers from another account. Push postponement keeps its existing behavior. |
| Profile updates change delivery ownership | Updating or clearing email or phone for the same external user ID strands queued observations and loses local suppression after rediscovery. | Identified accounts use the access key, complete device ID, and external user ID for ownership. Anonymous contexts retain contact discrimination. Profile updates preserve retries and suppression across restart. |

The fixes use the existing
[discovery action](../../RetenoSdkCore/src/main/java/com/reteno/core/internal/action/iam/FetchInAppMessagesAction.kt),
[selection action](../../RetenoSdkCore/src/main/java/com/reteno/core/internal/action/iam/ShowInAppAction.kt),
[pause action](../../RetenoSdkCore/src/main/java/com/reteno/core/internal/action/iam/PauseInAppMessagesAction.kt),
and [presenter](../../RetenoSdkCore/src/main/java/com/reteno/core/internal/iam/InAppPresenter.kt).
They add no public SDK API or further storage migration.

Regression coverage includes
[delivery behavior](../../RetenoSdkCore/src/test/java/com/reteno/core/internal/action/iam/InAppDeliveryTest.kt),
[observation replay](../../RetenoSdkCore/src/test/java/com/reteno/core/internal/action/sync/PushInAppInteractionsStepTest.kt),
[selection](../../RetenoSdkCore/src/test/java/com/reteno/core/internal/action/iam/ShowInAppActionTest.kt),
[postponed state](../../RetenoSdkCore/src/test/java/com/reteno/core/internal/iam/InAppMessagesStateTest.kt),
and [presentation](../../RetenoSdkCore/src/test/java/com/reteno/core/internal/iam/InAppPresenterTest.kt).
The final pause/resume run verifies expiry fallback, rejection after resume,
account changes, and pauses during renderer preparation. The
[HTTP client tests](../../RetenoSdkCore/src/test/java/com/reteno/core/data/remote/api/RestClientImplTest.kt)
exercise real owner calculation and request authorization through profile and
identity changes. The
[storage tests](../../RetenoSdkCore/src/test/java/com/reteno/core/data/local/database/manager/WorkflowStorageTest.kt)
verify pending observations and suppression after clearing profile contacts and
reopening SQLite and identity settings.

## Verification results

| Check | Result |
| --- | --- |
| Kotlin 1.8 / JDK 17 compilation | 678 source and test files compile with resolved dependencies and temporary resource stubs. This is not an AGP build. |
| Contract-alignment JVM tests | 363 Actions, rules, presenter, HTTP, and sync tests passed through a standalone JUnit launcher before the final pause/resume fix. |
| Latest pause/resume regression run | 148 focused tests passed after the fix. Three new tests reproduced the defect before implementation; an additional test covers renderer preparation. This suite overlaps the earlier run; counts are not cumulative. |
| Latest owner-context regression run | 36 HTTP tests and 7 SQLite tests passed after the fix, including eight new tests. The profile authorization and persistent-state regressions failed before implementation. All 678 source and test files compile with Kotlin 1.8 and JDK 17 through the standalone harness. Counts overlap earlier runs. |
| SQLite | 39 Robolectric tests passed, including terminal failure and historical schema upgrades. The runner uses OS resources, without the app manifest. No storage code changed in the final pause/resume fix. |
| Native Gradle/Kover | Blocked before compilation: Android SDK location is missing. |
| Devices and cross-service flow | Pending. |

Run with JDK 17 and a configured Android SDK:

```bash
./gradlew :RetenoSdkCore:testDebugUnitTest :RetenoSdkCore:koverVerifyDebug --configure-on-demand --no-watch-fs
```
