# Android Interview Questions for Smart-Glasses Engineers

A beginner-friendly **prompt bank** for an iOS/Swift engineer moving to Android. It starts at app-level Android, not Binder, SurfaceFlinger, or HALs.

The paired Chinese guide, `smart-glasses-android-interview-questions-cn.md`, includes **model answers**, **Swift/iOS mappings**, and **design rationale**. Use this English file for mock interviews or self-check prompts; use the Chinese file to study deeply.

## How to answer well (60–90 seconds each)

1. State the **primary responsibility** of the API or component.
2. Explain its **lifecycle, ownership, or failure mode**.
3. Give one concrete **glasses example** and one **trade-off**.
4. When behavior is version-dependent, say which **Android version / `targetSdk`** you would check—do not bluff.

---

## Part I — Android app fundamentals

### 1. Activity and lifecycle `[Foundation]`

What is an `Activity`, and which lifecycle events matter when a connection-status screen becomes visible, is covered, and is destroyed?

Cover: UI host vs business logic; visibility-aware collectors / camera preview; configuration change vs process death.

### 2. Application vs Activity `[Foundation]` `[Classic]`

What is a custom `Application` class for? What should **not** live in `Application`? How does its lifetime differ from an `Activity`?

Cover: process-level init; no screen state / no BLE session ownership by default; single process vs multi-process caveats.

### 3. Context `[Foundation]`

What is `Context` used for? When application context vs activity context? How can `Context` leak memory?

Cover: resources, services, component launch; long-lived objects; Activity/View retained by singleton / static / long coroutine.

### 4. Jetpack Compose, state, and recomposition `[Foundation]`

What does “declarative UI” mean in Compose? What causes recomposition? Why state hoisting?

Cover: UI = f(state); single source of truth; no network/BLE in composable body.

### 5. ViewModel and unidirectional data flow `[Foundation]`

Why Compose + immutable `UiState` + `ViewModel`? Where should “Connect” be handled?

Cover: survives config change; not a database; events in, state out.

### 6. `StateFlow`, `LiveData`, and one-off events `[Foundation]`

When choose `StateFlow` over `LiveData` for a new Compose screen? How do one-time navigation/toast events differ from durable screen state?

Cover: hot state with current value; re-renderable state vs consumable effects.

### 7. Coroutines and structured concurrency `[Foundation]`

What does `suspend` mean? Why `viewModelScope` instead of an unowned `CoroutineScope` / `GlobalScope`?

Cover: suspend ≠ thread switch; parent Job / cancellation; cancel work when owner dies.

### 8. Dispatchers and main-thread safety `[Foundation]`

How keep BLE parse, image transform, or DB work off the main thread while updating UI safely?

Cover: IO vs Default; do not blindly wrap every suspend; preserve cancellation.

### 9. Flow and lifecycle-aware collection `[Foundation]`

What is Kotlin `Flow`? Why collect in a lifecycle-aware way from Activity / Compose?

Cover: cold vs hot; `repeatOnLifecycle` / `collectAsStateWithLifecycle`; stop UI collection when stopped.

### 10. Configuration changes, saved state, and process death `[Foundation]`

Compare `remember`, `rememberSaveable`, `ViewModel`, `SavedStateHandle`, and persistent storage for connection info.

Cover: three layers of “state survival”; reconnect from real sources after process death.

### 11. Intent, Service, and BroadcastReceiver `[Foundation]` `[Classic]`

Roles of `Intent`, `Service`, and `BroadcastReceiver`? Why not long BLE work in a receiver?

Cover: messaging / work without UI / short broadcast reactions; background limits.

### 12. Permissions and privacy `[Foundation]`

Manifest vs runtime permissions vs user revocation for camera, mic, and Bluetooth?

Cover: declare, request in context, re-check; version / targetSdk matrix; graceful degrade.

### 13. Foreground service, WorkManager, and background limits `[Foundation]`

When foreground service vs `WorkManager` for glasses sync or recording? Why is always-on service a bad default?

Cover: user-visible ongoing work vs deferrable work; notifications / service types; cancellation.

---

## Part II — Classic Android interview topics (system & engineering)

These appear often in Chinese/FAANG-style Android interviews. For smart-glasses work, aim for **correct mental model + one product example**, not framework source dumps.

### 14. ANR (Application Not Responding) `[Classic]`

What is ANR? Common causes? How would you diagnose and prevent it in a glasses companion app?

Cover: main thread blocked; Input / Broadcast / Service timeouts (directional knowledge); StrictMode, traces, move work off main; never block main on GATT/network.

### 15. Main thread, Handler, Looper, MessageQueue `[Classic]`

How does the Android main thread process messages? When would you still use `Handler` in a modern Kotlin app?

Cover: Looper + MessageQueue; main Looper; Handler for bridging Java callbacks / frame-ish work; prefer coroutines for new code; do not leak Handler with outer Activity.

### 16. Process priority, LMK, and “why was my app killed?” `[Classic]`

Why can Android kill your process even if the user likes your app? How does foreground / service / cached process priority roughly work?

Cover: system owns memory; visibility and service type matter; design for recovery, not immortality.

### 17. Memory leaks and common patterns `[Classic]`

List common Android leak patterns and how you would find them. How does this differ from Swift ARC retain cycles?

Cover: Activity Context in singleton; static View; non-static inner class; unregistered listeners; LeakCanary; GC does not fix retained graphs.

### 18. Startup: cold / warm / hot `[Classic]`

Define cold, warm, and hot start. What would you measure for a glasses app’s first connect screen?

Cover: process creation cost; Application + first Activity; avoid heavy init on main; baseline profiles / App Startup library as awareness.

### 19. Task, back stack, and launch modes (awareness) `[Classic]`

What is a task / back stack at a high level? When do `singleTop` / `singleTask` matter? Do Compose apps still need this?

Cover: system navigation model still exists under Compose; deep links and multi-Activity products; prefer single Activity + Navigation unless multi-Activity is required.

### 20. Fragment (legacy awareness) `[Classic]`

What problem did Fragment solve historically? Why might a modern Compose app still meet Fragments?

Cover: modular UI / lifecycle inside Activity; Navigation / ViewPager legacy; Compose Navigation as alternative; do not rewrite greenfield in Fragments.

### 21. View system vs Compose: RecyclerView vs LazyColumn `[Classic]`

What is RecyclerView for? How does LazyColumn compare? When might you still need Views?

Cover: recycling, adapters, DiffUtil awareness; LazyColumn composition; interop `AndroidView`; camera preview surface edge cases.

### 22. SharedPreferences vs DataStore vs Room `[Classic]`

When each? Why is SharedPreferences discouraged for new code in many teams?

Cover: prefs vs structured data; async DataStore; Room/SQLite; not for live BLE state.

### 23. Serialization: Serializable, Parcelable, JSON `[Classic]`

What is `Parcelable` for? When JSON / kotlinx.serialization? Why not put large objects in `Bundle` / `SavedStateHandle`?

Cover: IPC / Intent extras; size limits; domain models stay out of Intent bags when possible.

### 24. Gradle, Manifest, and build variants `[Classic]`

What do Gradle and `AndroidManifest.xml` each control? Why product flavors for hardware products?

Cover: dependencies, SDK, signing, variants; component/permission declaration; dev vs prod endpoints.

### 25. Debugging toolkit `[Classic]`

How do you use Logcat, breakpoints, Layout Inspector / Compose Inspector, Profiler, and `adb` for a reconnect bug?

Cover: structured logs + state machine history; reproduce first; Perfetto for deep timing.

---

## Part III — Smart-glasses practical extensions

### 26. BLE connection design `[Glasses]`

Sketch a reliable phone-to-glasses BLE design: states, failure handling, ownership.

Cover: central/peripheral roles; state machine; serialize GATT; retry/backoff; permission/BT-off; state stream to UI.

### 27. Camera integration `[Glasses]`

When CameraX vs Camera2? How keep preview + analysis responsive?

Cover: lifecycle-aware CameraX first; backpressure; close buffers; release with owner lifecycle.

### 28. Repository, use cases, and DI `[Glasses]`

Separate `GlassesConnectionRepository`, use case, and Compose `ViewModel`. What does DI buy?

Cover: UDF layers; use case only when real policy; Hilt/fakes; lifetimes.

### 29. Local persistence for companion apps `[Glasses]`

What in DataStore vs Room? What never trust after process death?

Cover: preferred device id vs session history; re-validate live connection.

### 30. Power, thermal, and responsiveness `[Glasses]`

Practical choices that reduce drain/heat without making glasses feel slow.

Cover: event-driven; duty-cycle scan; stop sensors; measure first; latency budget.

### 31. Debugging reconnect storms `[Glasses]`

“App reconnects forever and battery drains”—how diagnose and prove the fix?

Cover: timeline, state transitions, tests, metrics, privacy-safe logs.

### 32. Privacy and security `[Glasses]`

Protect camera, mic, location, and first-person data.

Cover: least privilege; Keystore for keys; retention/deletion; export components carefully.

### 33. Capstone: connection-status screen `[Glasses]`

Design discover-and-connect with Compose, ViewModel, coroutines, StateFlow. Cover rotation, BT-off, permission denial, process recreation.

Cover: UiState + events; lifecycle collection; durable ids only; re-query real world after restore.

---

## Part IV — Deep comparison prompts (for study, not trivia)

Use these after Part I–III. Prefer the Chinese guide for full model answers.

### 34. Android design philosophy vs iOS `[Design]`

In your own words: what does Android optimize for that forces “recoverable process + component contracts”? How should that change an iOS engineer’s instincts?

### 35. Kotlin design philosophy vs Swift `[Design]`

Contrast Kotlin’s JVM/Android pragmatism (interop, coroutines-as-library, GC) with Swift’s value semantics, ARC, and compiler-checked concurrency. Give one code-level trap for each direction of migration.

### 36. Compose vs SwiftUI runtime semantics `[Design]`

What is shared (“UI as a function of state”) and what is not (host lifecycle, state containers, side-effect APIs)?

---

## Part V — Kotlin language deep-dive and extended classics

Use these after Part I–III to close common gaps. 37–43 are Kotlin/coroutine language questions that appear constantly in Chinese/FAANG-style Android rounds; 44–45 are classic Android runtime topics. Prefer the Chinese guide for full model answers.

### 37. Data-class equality: equals / hashCode / copy `[Classic]` `[Kotlin]`

What does a `data class` generate? When are two instances equal—is it a deep comparison? Compare with Swift.

Cover: only primary-constructor properties count (body properties do not); shallow `copy()`; `==` (structural) vs `===` (referential); Array member pitfall; safe as an immutable map key; Swift `Equatable`/`Hashable` synthesis vs value semantics.

### 38. inline / reified / crossinline / noinline `[Classic]` `[Kotlin]`

Why do inline functions exist? Why must `reified` pair with `inline`? When do you need crossinline / noinline?

Cover: lambda-object + virtual-call elimination and non-local return; JVM type erasure and recovering `T` at runtime; bytecode bloat if overused; Swift has no `reified` because Swift generics are not erased.

### 39. Property delegation: by lazy / lateinit / Delegates `[Classic]` `[Kotlin]`

What do `by lazy`, `lateinit`, `Delegates.observable`, and framework delegates (`by viewModels()`, `by remember`) solve? How do they relate to Swift property wrappers?

Cover: `getValue`/`setValue` convention; `lazy` vs `lateinit` constraints (val vs var, primitives, nullability); `by remember` ≈ `@State`; Swift property wrappers as the closest analogue.

### 40. sealed vs enum vs abstract `[Classic]` `[Kotlin]`

When do you pick each? Why sealed over enum for results / UI state? Compare with a Swift enum.

Cover: enum = fixed same-shape singletons; sealed = closed set of differently-shaped subtypes + exhaustive `when`; abstract = open, non-exhaustive; Swift enum-with-associated-values ≈ Kotlin sealed hierarchy, while Kotlin `enum` ≈ a Swift enum without associated values.

### 41. Generics: variance (in / out), erasure, reified `[Classic]` `[Kotlin]`

What are `out` and `in`? What does erasure prevent, and how do you work around it? Compare with Swift generics.

Cover: declaration-site variance vs Java wildcards (`? extends` / `? super`); star projection `<*>`; erasure → need `reified` or a `Class`/`KClass` token; Swift generics are not erased, so no `reified` is needed (`some` / `any` solve a different problem).

### 42. Coroutine exceptions: SupervisorJob, handler, async timing `[Classic]` `[Kotlin]`

How does failure propagate in `coroutineScope`? What changes with `supervisorScope` / `SupervisorJob`? When does `async` throw vs `launch`? Why is `CancellationException` special?

Cover: sibling cancellation vs supervised isolation; `async` throws at `await()` (can be swallowed if never awaited); `CoroutineExceptionHandler` is a last-resort root handler, not business error handling; always rethrow `CancellationException`.

### 43. Flow operators and backpressure `[Classic]` `[Kotlin]`

What are the common operators? When `flatMapLatest`? How do `buffer` / `conflate` / `collectLatest` handle a fast producer?

Cover: map/filter/transform, combine vs zip, flatMapConcat/Merge/Latest; suspension as natural backpressure; conflate/collectLatest keep only the latest; `stateIn`/`shareIn` sharing; Combine `switchToLatest` analogue and cold-vs-push difference.

### 44. View rendering: measure / layout / draw `[Classic]`

How does the classic View pipeline work? What is the difference between `invalidate()` and `requestLayout()`? Does a Compose app still need this?

Cover: measure → layout → draw; `invalidate` = redraw only, `requestLayout` = re-measure/layout (more expensive); deep layout hierarchies cost more; UIKit `setNeedsLayout` / `setNeedsDisplay` analogue; awareness helps with `AndroidView` interop and camera Surface/TextureView.

### 45. Bitmap and image memory `[Classic]` `[Glasses]`

Why do Bitmaps cause OOM? How do you manage them? What do image loaders do? How do you keep a glasses camera stream from exploding memory?

Cover: bytes = width × height × bytes-per-pixel (ARGB_8888 = 4B); downsample (`inSampleSize` / target resolution) and pixel format (RGB_565); recycle / reuse; Glide/Coil caching + lifecycle-aware cancellation; `ImageProxy.close()` and latest-frame backpressure for analysis; iOS `ImageIO` downsampling analogue.

---

## Suggested sessions

### 60-minute Android fundamentals

| Segment | Questions | Time |
|---|---:|---:|
| Hosts & context | 1–3 | 12 min |
| UI & state | 4–6, 10 | 12 min |
| Concurrency | 7–9 | 10 min |
| Components & background | 11–13 | 10 min |
| Classic ANR / leaks | 14, 17 | 8 min |
| Glasses or capstone | 26 or 33 | 8 min |

### 45-minute classic system round

Questions **14–18, 22–24** + one glasses scenario (**26** or **30**).

### 45-minute Kotlin language deep-dive

Questions **37–43** (equality, inline/reified, delegation, sealed, generics, coroutine exceptions, Flow operators). Pair each answer with the Swift contrast.

### 90-minute full mock (hiring-style)

1–13 core (40 min) → 14, 16, 17 classic (15 min) → 26 + 33 design (25 min) → 34 design philosophy (10 min).

---

## Mapping to the 10-day plan (`Kot.md` / `Kot_Cn.md`)

| Plan day | Best practice questions |
|---|---|
| Day 0–1 | 2, 3, 17 (Context/Application/leak intuition) |
| Day 4–5 | 7, 8 (coroutines) |
| Day 6–7 | 1, 4, 21 |
| Day 8 | 5, 6, 9, 10 |
| Day 9 | 12, 26, parts of 11/13 |
| Day 10 | 14, 33, 34–36 |
| After plan | 15, 16, 18–20, 22–25, 27–32; Kotlin deep-dive 37–43; runtime classics 44–45 |

---

## Useful official starting points

- [Android app architecture](https://developer.android.com/topic/architecture)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Lifecycle-aware coroutine collection](https://developer.android.com/topic/libraries/architecture/coroutines)
- [Permissions](https://developer.android.com/guide/topics/permissions/overview)
- [Background work overview](https://developer.android.com/develop/background-work/background-tasks)
- [ANR](https://developer.android.com/topic/performance/vitals/anr)
- [App startup](https://developer.android.com/topic/performance/vitals/launch-time)
- [Bluetooth Low Energy](https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview)
- [CameraX](https://developer.android.com/media/camera/camerax)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Room](https://developer.android.com/training/data-storage/room)
