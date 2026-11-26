# Changelog

## 2.12.1

### Fixed

- Apps whose `Application` doesn't implement `Configuration.Provider` no longer crash with "WorkManager needs to be
  initialized via a ContentProvider#onCreate() or an Application#onCreate()" when Android starts the process only to
  run a scheduled WorkManager job (`SystemJobService`), for example the SDK's periodic background sync after the app
  was swiped away (since 2.11.0). The SDK initializes WorkManager while it is created at app startup again, before
  the system can start the job's service, instead of later on a background thread.

## 2.12.0

### Added

- `Reteno.useDeviceLanguage()` stops using the `languageCode` set in user attributes as the device's language and
  returns to automatic detection. The contact keeps the last `languageCode` it was given. Without such a
  `languageCode` the call does nothing.

### Changed

- The device's `languageCode` follows the language the app shows instead of `Locale.getDefault()`, which Android
  resets to the system language on every configuration change, so the device language flipped between the app's and
  the system's and the device was sent again each time. The language is detected once per process, on the first app
  foreground, after the visible screen has stayed resumed for a second, and every device update in that process uses
  it; the device is sent again only when it changed. In order: a `languageCode` set in user attributes; the app's own
  configuration language when it differs from the system's (per-app language on Android 13+,
  `AppCompatDelegate.setApplicationLocales`); the visible screen's language when it differs from the system's (an
  in-app language on Android 12 and lower, or one the app sets on its activities); otherwise `Locale.getDefault()`
  when it differs from the system's, kept until the system language changes; otherwise the system language. A device
  updated before any screen was shown (a push in the background) uses the last detected language. Detected languages
  without a region get one (`uk` → `uk-UA`), and Unicode extensions such as Android 14 regional preferences
  (`en-US-u-mu-celsius`) are no longer sent. The device is registered after the language is detected; in-app
  messages are fetched without waiting for it.
- A `languageCode` set with `setUserAttributes`, `setMultiAccountUserAttributes` or `setAnonymousUserAttributes`
  also becomes the device's language and wins over detection until `useDeviceLanguage()` is called. It must contain
  a region (`de-AT`); a code without one (`de`, `de_AT`) is dropped with a warning in the log and the other attributes
  are still saved. Valid codes are sent in canonical case and without Unicode extensions (`de-at` → `de-AT`,
  `de-AT-u-mu-celsius` → `de-AT`).

### Fixed

- Responses sent with `Content-Encoding: gzip` are unpacked before they are read. Requests with a body (POST, PUT)
  ask for gzip explicitly, so Android's HTTP stack passed such answers on still compressed and they could not be
  read.
- Opening the app from a push notification on Android 12+ (apps targeting API 31+) shows the app's own splash screen
  again. The activity that handles the click (`RetenoNotificationClickedActivity`) is now translucent and finishes
  before it opens the app or the link, so Android shows the splash screen of the opened activity (for example
  `Theme.SplashScreen` with `installSplashScreen()`); it used to show a splash screen built from the application theme
  and hand it over to the launch activity. Apps that want the click activity to use their own theme can still
  override it in their manifest (`tools:replace="android:theme"`).
- On Android 13+ the splash screen opened from a push notification shows the app icon again, not only the
  background colour: the SDK opens the app or the link with
  `ActivityOptions.setSplashScreenStyle(SPLASH_SCREEN_STYLE_ICON)`. Android 12/12L have no such option and still show
  the background colour only.

## 2.11.0

The SDK internals were rebuilt on Kotlin coroutines. The public API (`Reteno`, `RetenoConfig`, `AppInbox`,
`Recommendation`, `RetenoNotifications`, callbacks and models) is unchanged; deprecated entry points still work as
before. Classes that were public only for technical reasons and were never part of the documented API were removed
(see **Removed**). Stored data keeps its format: the local database schema is unchanged and upgrades from every
earlier release are tested.

### Changed

- All work runs on coroutines instead of the internal `OperationQueue` threads. Calls to `Reteno`, `AppInbox` and
  `Recommendation` made before `Reteno.initWithConfig(...)` are held until it is called and then run in call order,
  even when a configuration saved by an earlier launch is available; they used to run as soon as that saved
  configuration was applied. Push handling in the background (delivery statuses, background sync) still starts from
  the saved configuration, so it works before the app calls `initWithConfig`. `RetenoInternalImpl.isInitialized`
  still turns true once the saved configuration is applied; it does not mean `Reteno` calls run yet.
- Data sync runs as one ordered pipeline (device → user → interactions → events → inbox status → recommendation events
  → wrapped links → in-app interactions → log events). A step that fails with a repeatable error (network, 5xx, 429)
  stops the rest of the run and is retried on the next cycle; other failures no longer block the pipeline.
- `PushDataWorker` reports its result only after the sync finished, waits for SDK initialization and stops
  rescheduling once there is nothing left to send.
- Log events are uploaded again (they were stored but never sent) and cleaned up afterwards.
- Log events recorded before `Reteno.initWithConfig(...)` are stored right away instead of being held until
  initialization.
- In-app messages: display-rule in-apps are no longer blocked while push in-apps are paused; a show is counted only
  after the in-app was actually displayed; the stored ETag is sent with the in-app list request; an in-app whose
  activity was not ready when the app went to background is presented on the next activity start.
- In-apps targeted by time spent in the app are shown in every session; they used to be shown only in the first
  session after install, because the counter carried over time from earlier sessions.
- Without network, in-app messages and their content saved by an earlier launch are used, so event-triggered and timed
  in-apps still show offline; they used to be dropped when the in-app list could not be loaded.
- Links opened from an in-app without a current activity start from the application context instead of being dropped.
- In-app custom data is broadcast only to receivers in the host app's own package.
- On Android versions the SDK does not support (below 8.0, API 26), `setUserAttributes`,
  `setMultiAccountUserAttributes` and `setAnonymousUserAttributes` now validate their input like on supported versions
  and throw `IllegalArgumentException` for a blank `externalUserId` or an invalid `marketId`; they used to return
  silently. Valid calls are still ignored there.
- Swiping a slide-up in-app away: a fling toward the edge without moving the message first now dismisses it toward
  that edge.
- Copying a config with `RetenoConfig.Builder(config)` or `RetenoConfig.Builder(config, accessKey)` now keeps every
  setting, including `platform` and `sessionDuration`.
- Market id validation message reads "max length is 64".
- Logging: SDK logs no longer contain the access key, request URLs or bodies, emails, phone numbers, external ids,
  device ids, FCM tokens or image URLs. In-app JavaScript console messages go through the SDK logger (only when debug
  logging is on) with personal data masked, instead of always being written to logcat.

### Fixed

- Sync could stall until the next 30-second tick, send duplicates or run two pipelines at once.
- Wrapped links were sent twice or lost; `http://` links blocked the link queue forever (they are dropped now).
- Initialization never completed when device registration threw (for example before Firebase was initialized), so
  every later API call waited forever.
- Validation errors for `externalUserId` and `marketId` crashed the app on the main thread instead of reaching the
  caller.
- User attributes the server rejected (for example an unknown custom field) were stored as already sent, so later
  updates left out every attribute of the rejected request. The rejected update is now dropped and later updates are
  compared with what the server actually accepted.
- After switching to another `externalUserId` (or device id), only the attributes that differed from the previous user
  were sent, so the new contact missed the rest. The first update for a new user now sends all its attributes.
- Users switched while the device was offline were merged into one request for the last user: earlier users never got
  their attributes and the last one received theirs. Pending updates are now sent per user in the order they were set
  (up to five users per sync, the rest on the next one); anonymous attributes set before identifying a user are sent
  on their own, as they are online.
- `SessionStarted` could be stored before the `SessionEnded` of the previous session; they are now logged in order.
- The device was registered with the last stored notification state (off on a fresh install), so it was sent again a
  moment later once the push module reported the real state. The device is now saved with the state read from the
  system (app notifications and the default Reteno channel) and sent once.
- Taps on in-app buttons that also close the in-app were not recorded: the in-app reports the close before the click,
  and the click was dropped once the in-app was gone. Push in-apps never registered a click this way. A click that
  follows the close of the same in-app is now recorded and its link or custom data handled; if another in-app
  is already on screen, the click is only recorded.
- Typed recommendations (`fetchRecommendation` with your own `RecomBase` class) came back with every field empty in
  minified release builds, because R8 removed the fields Gson fills. The SDK's consumer rules now keep the fields and
  constructors of every `RecomBase` implementation; the classes can still be renamed.
- Waiting for a custom device id or for the device id could poll forever.
- The default notification channel config ignored defaults for missing fields and could throw for a missing id or
  name.
- Personalised in-app content inserted user attributes without escaping; values are now JSON-escaped before they reach
  the in-app WebView.
- In-app message position was never read back from local storage.
- Databases created by an early 2.x build that shipped database version 9 without the in-app position column now get
  the column on upgrade; saving in-app messages failed silently for them.
- `RetenoNotifications.requestNotificationPermission()` could wait forever when the current activity is a plain
  `Activity`, when two requests overlapped, or when the activity was recreated (for example rotated) while the dialog
  was open. Such requests now answer `false`.
- A permission request that was cancelled, or whose dialog could not be launched, no longer receives the answer of a
  later dialog; looking for the current activity for a permission request now always reads activity state on the main
  thread.
- A failing internal lifecycle listener no longer stops the others from being notified.
- In-app WebViews no longer allow file and content access (they defaulted to on below Android 11).
- Concurrent writes to the local database no longer share one buffer between calls.
- User updates were sent with no changes after attributes without custom fields were stored.
- Times saved by the local database are read as UTC (they were read in the device time zone) and parsed without a
  shared, non-thread-safe formatter.
- Cleaning up old link clicks compared times in two different text formats and deleted clicks from the cutoff day up
  to a day early.
- A slide-up in-app removed from the screen while it was still settling after a swipe crashed the app.
- Upgrading the local database no longer reports a "duplicate column" error for every column that already exists; only
  migrations that fail for another reason are reported.
- A stored row that could not be parsed (for example broken JSON) stopped every later row of that table from being
  read and sent, on every sync. Such a row is now removed and the rest are read.
- Automatic screen tracking missed the fragments of an activity that had started but not yet resumed when tracking was
  enabled (for example the first activity, when the SDK finishes initializing during its start). While switching
  activities, the SDK also treated the previous, already paused activity as resumed.

### Deprecated (unchanged)

- `RetenoImpl(application)`, `RetenoImpl(application, accessKey, config)`, the `RetenoConfig(...)` function and
  `RetenoApplication` stay deprecated. Use `Reteno.initWithConfig(RetenoConfig.Builder()...build())` and
  `Reteno.instance`. As before, `RetenoImpl(application, accessKey)` and `RetenoConfig()` without an access key throw
  `IllegalArgumentException`; the README setup guide now shows the current API.

### Removed

These classes and members were public only for technical reasons. Apps that used them need to switch to the public
API.

- Dependency wiring: `ServiceLocator`, `RetenoInternalImpl.serviceLocator`, `RetenoInternalImpl.isInitializedByUser`,
  `RetenoInternalImpl.swapInstance`, `com.reteno.core.di.provider.*` (including `RetenoConfigProvider`),
  `com.reteno.core.di.base.*` (`Provider`, `ProviderNewInstance`, `ProviderStrongReference`,
  `ProviderWeakReference`).
- Threading: `OperationQueue`.
- Former controllers, repositories and workers: `ContactController`, `ContactRepository`, `ScheduleController`,
  `LogEventRepository`, `AppLifecycleController`, `InteractionController`, `DeeplinkController`,
  `InteractionRepository`, `DeeplinkRepository`, `InteractionCacheStore`, `UpdateUserAttributesAction`,
  `UpdateMultiAccountUserAttributesAction`.
- In-app internals: `FrequencyRuleValidator`, `ScheduleRuleValidator`, `RuleEventValidator`, `FrequencyRuleType`,
  `DisplayRuleType`, `InAppMessagesList`, `EventWaitingQueue`, `EventWaitingForInApp`, `InAppMessage.notifyShown`,
  `SegmentRule.shouldCheckStatus`, `AsyncRuleRetryParams.shouldRetry` / `STANDARD_RETRY_TIME`,
  `VerticalSwipeDismissBehavior`, `com.reteno.core.view.iam.container.slideup.SwipeDismissBehavior`.
- Utilities: `Util.fromRemote`, `isNonRepeatableError`, `toJsonOrNull`, `SchedulerUtils.getOutdatedTime`, the
  `bufferUntil` helper, `SqlStateEncrypt`, `Device.Companion` (`createDevice`), and the constants
  `com.reteno.core.util.TAG`, `PROP_KEY_DEBUG_VIEW`, `PROP_VALUE_DEBUG_VIEW_ENABLE`, `THREAD_PREFIX_NAME`.
- `ConfigRepository.getAppPackageName` / `getAppPackageInfo`.
- `RetenoInternalFacade.logRetenoEvent` / `RetenoInternalImpl.logRetenoEvent`.
- `AndroidActivityAware.requireActivity()` (protected, unused).
- `BuildConfig.SQL_PASSWORD` of the core library.
- `Util.isEncryptedDatabase` (unused since the encrypted database is no longer dropped by hand).

### Other

- `RetenoLogEvent.version` defaults to `null`; the SDK fills it in when the event is recorded.
- A stored device row without a category reads as `MOBILE`.
