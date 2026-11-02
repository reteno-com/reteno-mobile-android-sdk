# Android SDK workflow In-app implementation plan

Status: planned; native implementation and verification are pending.
Baseline: `9a3efa9fdc1012669a3846dcb7d5a35ccccca24d`.
Reconcile the paths below if the implementation branch changes.

Use the [shared SDK contract and regression matrix](../../../esputnik-product-docs/changes/CHG-2026-0002-in-app-workflow-delivery/mobile-sdk-implementation-plan.md)
and [HTTP examples](https://ardas.atlassian.net/wiki/spaces/eS/pages/588021917).
This plan covers Android SDK changes only. Reuse existing networking, rule
evaluation, rendering, persistence, and retry infrastructure.

## Implementation steps

| Step | Android changes | Acceptance |
| --- | --- | --- |
| 1. Models and requests | Extend [InAppMessage](../../RetenoSdkCore/src/main/java/com/reteno/core/data/remote/model/iam/message/InAppMessage.kt) and existing content/interaction adapters with optional `inWorkflow`, IID, and expiry. Add discovery `includeWorkflow=true` when the complete workflow path is supported. Send workflow IID/snapshot pairs in `messages`; keep ordinary `messageInstanceIds` requests separate. | Missing/false `inWorkflow` keeps ordinary behavior. Mixed responses preserve two workflow IIDs sharing a snapshot and an ordinary message. No new delivery-method enum, event identifier, or feature flag. |
| 2. Execution state | Adapt [InAppMessageDb](../../RetenoSdkCore/src/main/java/com/reteno/core/data/local/model/iam/InAppMessageDb.kt), [InAppMessageSchema](../../RetenoSdkCore/src/main/java/com/reteno/core/data/local/database/schema/InAppMessageSchema.kt), and existing interaction persistence. Key workflow state by application/account context and IID; match content by IID plus snapshot. Preserve ordinary frequency data during upgrades. | Different IIDs retain independent personalization, pending, and shown state. Restart restores state; account/contact changes cannot expose old content or replay observations under new credentials. |
| 3. Eligibility and content | Update [IamControllerImpl](../../RetenoSdkCore/src/main/java/com/reteno/core/domain/controller/IamControllerImpl.kt) and existing validators. Skip only workflow FREQUENCY. After other conditions pass, request authorized content and apply the prepared JSON personalization map to the raw model. | Failed conditions do not fetch content. Unsupported enabled conditions defer safely. Revalidate online before first rendering, even with cached bytes; recheck absolute expiry and local shown state. Add no SDK TTL default or Velocity engine. |
| 4. Visibility and retry | Connect [IamViewImpl](../../RetenoSdkCore/src/main/java/com/reteno/core/view/iam/IamViewImpl.kt) to [InteractionRepositoryImpl](../../RetenoSdkCore/src/main/java/com/reteno/core/data/repository/InteractionRepositoryImpl.kt). Reuse the server IID for workflow display. Persist the shown marker and OPENED observation together through existing local storage, then use the existing bounded retry queue. | Only actual visibility emits OPENED. Fetch/init is not visibility. Replay the original IID, snapshot, UTC-seconds time, and payload after a lost response or restart, without redisplay. FAILED before visibility does not mark shown; dismissal remains local. |
| 5. Clicks and certification | Confirm the workflow click transport before implementing it. Extend existing CTA handling and certify native view/renderer callbacks with the same IID. Opt in to workflow discovery in released SDKs only after all required paths are verified. | Current POST CLICKED retains only the action URL downstream; existing PUT preserves CTA metadata but does not update shown state. Record the agreed transport and its tests before release. Steps 1–4 can proceed independently. |

Keep one local presentation guard for a workflow IID. Preserve distinct queued
observations rather than overwriting them by IID. A repeated discovery response
or HTTP 304 must not clear a shown marker or extend `expiresAt`. Continue
ordinary IID generation, frequency checks, and click handling unchanged.

## Verification and completion

Extend [IamControllerImplTest](../../RetenoSdkCore/src/test/java/com/reteno/core/domain/controller/IamControllerImplTest.kt),
[IamViewImplTest](../../RetenoSdkCore/src/test/java/com/reteno/core/view/iam/IamViewImplTest.kt),
rule, interaction, and database tests. Use fixed time, controlled callbacks,
and isolated persistent stores; reopen the store to verify process restart.

Cover mixed ordinary/workflow messages; same-snapshot IIDs with different
personalization; rule deferral; failed initialization; visibility; lost
acknowledgment; repeated 200/304; multiple queued observations; expiry/offline;
local concurrency; account switching; and migration of existing ordinary data.
Confirm that showing E1 leaves E2 independently available.

Run from the repository root after implementation:

```bash
./gradlew :RetenoSdkCore:testDebugUnitTest
```

Record native test and device results for presentation, background/foreground,
process death, reconnect, clicks, and dismissal. Verify renderer and supported
wrapper behavior. Completion requires reviewed Android changes, store-upgrade
and recovery evidence, and tested SDK/renderer versions. HTTP success is not a
Kafka acknowledgment; cross-device exactly-once display is not guaranteed.
No native tests or device checks were run to prepare this plan.
