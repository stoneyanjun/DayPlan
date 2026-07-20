# Kotlin + Native Android: A 10-Day Accelerated Plan for iOS / Swift Engineers (Smart-Glasses Track)

> **Time convention:** Day 0 is a 1–2 hour environment preflight. Complete it before the plan; it is **not counted** as one of the ten days. Plan 2–3 hours for each of Days 1–10.  
> **Stack:** Kotlin, Jetpack Compose, ViewModel, StateFlow, Coroutines / Flow, and a replaceable Fake Repository.  
> **Scope:** Build one narrow vertical slice: device discovery / connection state. Real BLE, camera, speech recognition, and AI chat are separate follow-up features, not parallel ten-day tasks.  
> **Companion Q bank:** `smart-glasses-android-interview-questions-en.md` (prompts); Chinese guide with model answers, Swift mappings, and design rationale: `smart-glasses-android-interview-questions-cn.md`.

## Audience, Boundaries, and Definition of Done

This plan is for engineers who already know Swift, iOS layering, domain modeling, and async/await. It is not a Kotlin or Android encyclopedia. Its purpose is to transfer existing engineering skill to a modern Android path.

At the end, you should be able to:

- Read common DTO mappers, domain models, sealed hierarchies, repositories, use cases, and basic coroutine / Flow code.
- Write Kotlin business code that is null-safe, testable, and not overloaded with language tricks.
- Deliver a runnable connection-status screen with Compose, ViewModel, and StateFlow.
- Explain the boundaries of configuration change, process death, coroutine cancellation, Context, permissions, ANR risk, and Gradle.
- Use iOS experience for **contrastive learning**, not mechanical API-name translation.

Do not claim to have “mastered Android” after ten days. Complex Flow composition, generics and reified types, modularization, real BLE GATT, performance, release work, and legacy View / Fragment apps require continued project experience.

### Explicitly out of scope for the ten days

| Defer | Why |
|---|---|
| Full real BLE GATT integration | Needs hardware, permission matrix, and protocol work; Day 9 only designs boundaries + fakes |
| CameraX / speech / AI chat | Separate vertical slices that derail the connection-status spine |
| Multi-module + full DI setup | Learn manual construction and fakes first; Hilt after Day 10 |
| Multi-screen Navigation / deep links | Single-screen slice is enough; recognize the concept, do not implement a stack |
| Legacy View / XML / Fragment UI | Learn when maintaining older apps; new code is Compose-first |
| Binder / system source / custom drawing | Interview awareness only; do not deep-dive in ten days |

Each completed day has at least:

1. A small compilable Kotlin / Android change;
2. One or two repeatable tests, or a specific runtime verification;
3. One Swift comparison plus the real semantic difference;
4. One risk or readability issue that you found and fixed.

---

## Plan Evaluation Summary (why this order)

| Strength | Note |
|---|---|
| Pure Kotlin before Android | Keeps null-safety, coroutines, lifecycle, and BLE out of one debug session |
| Narrow vertical slice | Realistic smart-glasses scenario with demonstrable boundaries |
| Swift comparisons + anti-patterns | Blocks fatal myths such as `data class ≈ struct` and `suspend ≈ background thread` |
| Acceptance criteria + AI review | Daily checkable output instead of “I read the docs” |

| Common weak spots (addressed in this revision) | How |
|---|---|
| Day 9 overloads Flow + permissions + BLE | Split into P0 (Flow + permissions) / P1 (BLE boundary design) |
| Missing classic Android interview concepts | Days 6/8 add Application, ANR, process; expanded Q bank |
| Vague testing stack | Days 5/8 name JUnit + coroutines-test; optional Turbine |
| Disconnected from interview prep | Stage checkpoints map to Q-bank items |
| Abstract package / dependency guidance | Day 6 package-structure template |
| Weak active recall | Daily output adds a 3-question oral check |

---

## Mental Models to Establish Before Migrating

Do not treat Android APIs as word-for-word replacements for Swift / iOS APIs. These differences keep affecting design:

| Familiar Swift / iOS concept | Kotlin / Android approximation | Difference that matters |
|---|---|---|
| struct + let | data class + val | A data class is still a regular class. val fixes a property reference only, and copy() is shallow; neither supplies Swift struct value semantics. |
| Optional | T?, ?., ?: | Both are null-safe. Kotlin also has Java platform types, so nullability must be normalized at boundaries. |
| async / await | suspend fun, launch, async | suspend does not mean a background thread and does not start a task. Kotlin concurrency is largely library- and scope-based. |
| MainActor | Dispatchers.Main | MainActor supplies isolation semantics; the Main dispatcher primarily schedules work. They are not equivalent race-safety mechanisms. |
| SwiftUI body | @Composable | A Composable can recompose many times. Its body should be close to pure rendering and must not start uncontrolled side effects. |
| @State / ObservableObject | remember / rememberSaveable / ViewModel + StateFlow | Remembering, configuration change, and process death are three separate problems. |
| View-controller lifecycle | Activity / Fragment / Compose lifecycle | Android can kill a process under memory pressure. Surviving rotation does not prove that state restoration is correct. |
| AppDelegate / @main | Application + Activity | Application can do process-level init; screen business state still should not live in a global singleton. |
| Main-thread block → jank | Long main-thread block → **ANR** | Android has system-level unresponsive-dialog behavior and stricter background policies. |

### Recommended package layout (from Day 6)

```text
app/src/main/java/.../
  ui/connection/     # Screen + small composables
  presentation/      # ViewModel, UiState, UiEvent
  domain/            # models, repository interfaces, use cases
  data/              # Fake / real repositories, DTOs, mappers
  di/                # Optional: manual factories; Hilt later
```

Dependency direction: **ui → presentation → domain ← data**. Domain depends on neither Android, Compose, nor Bluetooth APIs.

---

## Shared Exercise Scope and Daily Rhythm

All practice uses one narrow, realistic smart-glasses flow:

~~~text
Device list -> tap Connect -> Connecting -> Connected / PermissionRequired / Error
                                         -> Disconnect or retry
~~~

Build the domain slice in pure Kotlin on Days 1–5, then move it into Android on Days 6–10. This keeps language, architecture, lifecycle, and BLE errors out of the same debugging session.

| Time | Suggested activity |
|---|---|
| 20–30 min | Study the concept and write one Swift comparison |
| 70–90 min | Implement the smallest useful code yourself |
| 20 min | Add one or two key tests, or verify it on emulator / device |
| 15 min | Improve naming and remove unnecessary scope functions or abstractions |
| 10 min | Chinese reflection; **oral quiz of 3 checks for the day** (no notes) |
| 5 min | Optional: 3–5 simple English technical sentences |

Daily output template:

~~~text
Day X deliverable
1. Kotlin / Android code:
2. One Swift comparison:
3. Verification result (test or runtime note):
4. Semantic difference discovered today:
5. Issue found by AI / self-review and the smallest fix:
6. Question to verify actively tomorrow:
7. Oral check (3 items, pass/fail notes):
~~~

**Minimal testing stack (introduce in Stage 1)**

- JUnit 4/5 + assertions (`kotlin.test` or Truth)
- `runTest` from `kotlinx-coroutines-test`
- ViewModel tests: `MainDispatcherRule` + Fake repository
- Optional: Turbine for Flow emission sequences
- UI layer: use `createComposeRule` for Compose UI tests; learn Robolectric when you need the Android framework on the JVM, and instrumented tests (androidTest) for real-device interaction. Distinguish **unit tests** (fast, pure JVM) from **device tests** (slow, real environment).

---

# Day 0 (not part of the ten days): Environment Preflight, 1–2 Hours

## Goal

Run a modern Compose project in an emulator and remove SDK, emulator, Gradle, and IDE blockers before the actual plan begins.

## Tasks

1. Create an **Empty Activity (Compose)** project in Android Studio and run the default screen.
2. Locate AndroidManifest.xml, the app module build.gradle.kts, MainActivity.kt, ui/theme, res, and the version catalog if the project uses gradle/libs.versions.toml.
3. Add a log entry in MainActivity, find it in Logcat, and learn to filter by your app process and error level.
4. Record minSdk, targetSdk, application id, and the emulator API level. They affect permissions and hardware behavior.
5. Optional: rotate the emulator once and observe default Activity recreation; build the intuition that configuration changes happen.

## Concepts to Recognize, Not Master Yet

- An Activity hosts Android UI. Start with Compose in a new app; expect to meet Fragments and Views when maintaining older projects.
- `Application` is the process-level entry for global init; **do not** park screen business state in a custom Application singleton.
- Context is the entry point for Android resources, system services, and permissions. Do not retain an Activity Context, View, or Composable in a singleton, repository, or ViewModel. When Android Context must outlive a screen, application context is usually the only appropriate candidate.
- Gradle manages builds, dependencies, Android SDK configuration, and variants. On Day 0, identify the files; do not write complex build logic.
- The main thread owns UI work; heavy work or blocking I/O there causes jank and, if long enough, **ANR**.

## Acceptance

- The default Compose screen is visible in an emulator.
- You can sketch Activity -> Compose UI -> ViewModel -> domain / data in your own words.
- You can say roughly what minSdk and targetSdk influence (permissions, API availability, store policy direction).

---

# Stage 1: Kotlin Language and Concurrency (Days 1–5)

## Day 1: Kotlin Syntax Migration and Null Safety

### Goal

Map Swift Optionals correctly to Kotlin nullable types, and turn unreliable Java / network data into trustworthy domain models at the boundary.

### Study Focus

- val / var, default and named parameters, string templates, and if / when as expressions.
- ?., the Elvis operator ?:, smart casts, and mapNotNull. Prefer early return through ?: return.
- !! is not convenient force-unwrapping; it delays failure to an NPE. Do not use it in the exercises.
- A Java value without nullability annotations is a platform type. It is neither a reliable String nor an ordinary String?; handle it explicitly in an adapter / mapper.
- require / requireNotNull validate caller preconditions and throw IllegalArgumentException. check / checkNotNull validate internal state and throw IllegalStateException.
- lateinit var is only for a non-null reference property initialized later. Early access throws UninitializedPropertyAccessException. It is not an Optional and should not replace constructor injection. by lazy is useful for one-time lazy initialization, not for hidden expensive or failing I/O.

### Exercise: Device DTO Mapper

Implement this boundary conversion. Test a blank id, an unknown transport, a missing battery, and an out-of-range battery:

~~~kotlin
data class GlassesDeviceDto(
    val id: String?,
    val displayName: String?,
    val batteryPercent: Int?,
    val firmwareVersion: String?,
    val connectionType: String?,
    val lastSeenEpochMillis: Long?,
)

enum class Transport { BLE, USB, UNKNOWN }

data class GlassesDevice(
    val id: String,
    val displayName: String,
    val batteryPercent: Int?, // null means “unknown”; do not pretend it is 0%
    val firmwareVersion: String?,
    val transport: Transport,
    val lastSeenEpochMillis: Long?,
)

fun GlassesDeviceDto.toDomainOrNull(): GlassesDevice? {
    val normalizedId = id?.trim()?.takeIf(String::isNotEmpty) ?: return null

    return GlassesDevice(
        id = normalizedId,
        displayName = displayName?.trim().orEmpty().ifBlank { "Unnamed glasses" },
        batteryPercent = batteryPercent?.coerceIn(0, 100),
        firmwareVersion = firmwareVersion?.trim()?.takeIf(String::isNotEmpty),
        transport = when (connectionType?.lowercase()) {
            "ble" -> Transport.BLE
            "usb" -> Transport.USB
            else -> Transport.UNKNOWN
        },
        lastSeenEpochMillis = lastSeenEpochMillis,
    )
}
~~~

Keeping an absent battery as unknown matters: “the device did not report a battery level” and “the battery is at 0%” are different business facts. If the product really needs a default, make it explicit in the name and UI, such as BatteryState.Unknown, instead of silently using zero.

### Swift Comparison and Acceptance

Swift can use guard let and map for the same Optional conversion. The extra Android concern is the Java platform type. Write a Swift `toDomain() -> GlassesDevice?` comparison and explain why a Kotlin boundary cannot trust type hints alone.

**Oral check:** (1) Is `!!` the same risk as Swift `!`? (2) What is a platform type? (3) Why is `batteryPercent = null` not 0?

---

## Day 2: Modeling, Data Classes, Objects, and Extensions

### Goal

Learn Kotlin modeling tools without treating a data class as a Swift struct.

### Study Focus

- data class, copy, destructuring, primary constructors, and init. Prefer val by default.
- object for a singleton, companion object for an object associated with a class, top-level functions, extension functions, and extension properties.
- Extensions are statically resolved: a member wins over an extension with the same name. An extension cannot truly override a member or access the receiver’s private / protected members.
- A data class can contain var and mutable objects. copy() copies the first-level references only. List is a read-only interface, not a deep immutability guarantee; do not expose a MutableList still owned elsewhere as domain state.
- An @JvmInline value class can distinguish a lightweight DeviceId, but it can be boxed at generic, nullable, and Java-interoperability boundaries. It is not a general struct substitute.

### Exercise: Factory for a Valid Initial Session

~~~kotlin
@JvmInline
value class DeviceId private constructor(val value: String) {
    companion object {
        fun from(raw: String): DeviceId {
            val normalized = raw.trim()
            require(normalized.isNotEmpty()) { "device id must not be blank" }
            return DeviceId(normalized)
        }
    }
}

data class AudioStreamConfig(
    val sampleRateHz: Int,
    val channelCount: Int,
) {
    init {
        require(sampleRateHz > 0)
        require(channelCount in 1..2)
    }
}

enum class ConnectionMode { DISCONNECTED, CONNECTING, CONNECTED }

data class MeetingSession private constructor(
    val deviceId: DeviceId,
    val audioConfig: AudioStreamConfig,
    val mode: ConnectionMode,
) {
    companion object {
        fun createInitial(
            deviceId: DeviceId,
            audioConfig: AudioStreamConfig,
        ): MeetingSession = MeetingSession(
            deviceId = deviceId,
            audioConfig = audioConfig,
            mode = ConnectionMode.DISCONNECTED,
        )
    }
}

fun MeetingSession.isReadyToStream(): Boolean =
    mode == ConnectionMode.CONNECTED

fun normalizeVolume(level: Int): Int = level.coerceIn(0, 100)
~~~

The private constructor prevents callers from creating a session that starts as CONNECTED. The factory centralizes the initial state and preconditions. If creation begins to coordinate repositories, clocks, or multiple aggregates, introduce a separate factory / use case. For simple value conversion, a top-level function is clearer.

### Swift Comparison and Acceptance

Swift can use a private initializer plus static func createInitial for the same invariant. The important difference is that a Swift struct normally has value semantics when assigned or passed, while a Kotlin data class copy() is explicit and shallow. Run an experiment: mutate an inner MutableList after copy() and observe both objects; then redesign so mutable collections are not exposed.

**Oral check:** (1) Core semantic gap: data class vs struct? (2) Can extensions override? (3) When may a value class box?

---

## Day 3: Collections, Lambdas, and Readability

### Goal

Become comfortable with Kotlin collection transformations while keeping business code clear before making it concise.

### Study Focus

- map, filter, mapNotNull, flatMap, firstOrNull, any, all, sumOf, groupBy, associateBy, and fold.
- Standard collection operations are eager. Evaluate asSequence() only when a large collection or multi-step pipeline actually benefits. It resembles Swift lazy sequences and changes evaluation timing and debugging behavior.
- Use let for short nullable transformations, also for obvious side effects, apply to configure a mutable object, and run / with only when the receiver remains obvious.
- If nested scope functions make it or this unclear, use named local variables. A normal if is often clearer than a takeIf chain.

### Exercise and Acceptance

Create at least eight device records and:

- find connected devices and group them by transport or firmware major version;
- find the lowest battery and most recently active device;
- deduplicate firmware versions and build a map from DeviceId to GlassesDevice;
- turn a nullable DTO list into valid domain models.

Use mapNotNull, groupBy, and associateBy at least once each. Write Swift map / compactMap / Dictionary comparisons for two queries. In your reflection, identify one place where you deliberately did **not** use a scope function and explain why it is clearer.

**Oral check:** (1) mapNotNull vs Swift compactMap? (2) Is a read-only List immutable? (3) When is Sequence worth it?

---

## Day 4: Sealed Hierarchies, Domain Results, and UI State

### Goal

Use a closed type hierarchy for connection results and separate “what happened” from “what the screen should render.”

### Study Focus

- sealed interface / sealed class, data class, data object, and exhaustive when. data object needs Kotlin 1.9+; use object in older projects.
- A domain result is a business fact. UI state is durable, renderable screen state. One-time navigation and toast effects should not masquerade as permanent state.
- Kotlin has no checked-error mechanism that forces try at each call site as Swift throws does. Model expected business failures with a sealed result; reserve exceptions for exceptional boundaries.
- runCatching catches CancellationException. Turning it into an ordinary failure inside coroutine code breaks cancellation propagation.

### Exercise

~~~kotlin
sealed interface ConnectDeviceResult {
    data class Connected(val device: GlassesDevice) : ConnectDeviceResult
    data object BluetoothDisabled : ConnectDeviceResult
    data object DeviceNotFound : ConnectDeviceResult
    data object PermissionDenied : ConnectDeviceResult
    data class TransportFailure(val cause: Throwable) : ConnectDeviceResult
}

sealed interface DeviceUiState {
    data object Idle : DeviceUiState
    data object Connecting : DeviceUiState
    data class Connected(val device: GlassesDevice) : DeviceUiState
    data class PermissionRequired(val missing: List<String>) : DeviceUiState
    data class Error(val message: String, val canRetry: Boolean) : DeviceUiState
}

fun ConnectDeviceResult.toUiState(): DeviceUiState = when (this) {
    is ConnectDeviceResult.Connected -> DeviceUiState.Connected(device)
    ConnectDeviceResult.BluetoothDisabled ->
        DeviceUiState.Error("Bluetooth is off", canRetry = true)
    ConnectDeviceResult.DeviceNotFound ->
        DeviceUiState.Error("Device not found", canRetry = true)
    ConnectDeviceResult.PermissionDenied ->
        DeviceUiState.PermissionRequired(missing = listOf("BLUETOOTH_CONNECT"))
    is ConnectDeviceResult.TransportFailure ->
        DeviceUiState.Error("Connection failed", canRetry = true)
}
~~~

Do not put a raw Throwable into long-lived UI state, and do not let UI decide low-level error classification. Test the exhaustive when mapper and write a Swift enum-with-associated-values comparison.

**Oral check:** (1) Why separate domain result from UI state? (2) runCatching and cancellation? (3) What if sealed when misses a branch?

---

## Day 5: Coroutines and the Pure Kotlin Integration Exercise

### Goal

Learn to read and compose Kotlin coroutines, then build a fake device-connection domain slice without Android UI.

| Swift | Kotlin | Semantic point to remember |
|---|---|---|
| async func | suspend fun | Call from a coroutine / suspend context directly. suspend does not create work or guarantee a thread switch. |
| async let / task-group child | async { } + Deferred.await() | async creates a Deferred; await() is only for reading its result. |
| Task { } | launch { } | Both need a clear owner. Android user actions normally belong in viewModelScope. |
| MainActor | Dispatchers.Main | The former provides actor isolation; the latter is primarily a dispatcher. They are not equivalents. |
| task group | coroutineScope { } | Both establish and await structured children. supervisorScope lets sibling failure avoid cancelling all children automatically. |

### Study Focus

- suspend fun, coroutineScope, launch, async, await, and withContext.
- Structured concurrency: child work belongs to a scope with a correct parent Job, such as coroutineScope or viewModelScope. Do not use GlobalScope or casually create unmanaged scopes in a repository.
- Cancellation is cooperative. Cancellable suspension points usually react to cancellation; CPU loops and blocking calls need ensureActive(), isActive, yield(), or a cancellable API.
- If you broadly catch Exception, rethrow CancellationException first.
- Exceptions and supervision: in coroutineScope, one child failure cancels its siblings and propagates up; supervisorScope / SupervisorJob isolate sibling failures. launch propagates immediately, while async wraps the exception and throws it at await()—not awaiting can swallow it (see Q42).
- Use withContext(Dispatchers.IO) only for truly blocking I/O. Do not mechanically wrap a correctly implemented suspend network API. Dispatchers should be injectable for tests.

### Integration Exercise: Suggested Minimal Skeleton

~~~kotlin
interface DeviceRepository {
    suspend fun getDevice(deviceId: String): GlassesDevice?
    suspend fun getCapabilities(deviceId: String): DeviceCapabilities
    suspend fun connect(deviceId: String): ConnectDeviceResult
}

class FakeDeviceRepository(
    private val delayMillis: Long = 200,
) : DeviceRepository {
    // delay simulates I/O; tests control time with TestDispatcher
    // connect may return Connected / DeviceNotFound / PermissionDenied by deviceId
}

class ConnectDeviceUseCase(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(deviceId: String): ConnectDeviceResult {
        return coroutineScope {
            val deviceDeferred = async { repository.getDevice(deviceId) }
            val capsDeferred = async { repository.getCapabilities(deviceId) }
            val device = deviceDeferred.await() ?: return@coroutineScope ConnectDeviceResult.DeviceNotFound
            capsDeferred.await() // concurrent composition demo; merge per protocol in real code
            repository.connect(deviceId)
        }
    }
}
~~~

### Acceptance

- connect(deviceId) is a suspend fun;
- use coroutineScope to fetch device details and capability configuration concurrently, then combine them;
- simulate cancellation and verify it does not become a normal Error;
- use runTest from kotlinx-coroutines-test to write at least one or two tests each for the Day 1 mapper and the use case.

At the end of Stage 1, explain the relationship among suspend fun, launch, and async; why a suspend call does not need .await(); and why Kotlin cancellation must be deliberately preserved.

### Stage 1 checkpoint (map to Q bank)

Be ready to speak about null-safety boundaries, data-class shallow copy, sealed error modeling, and suspend / structured concurrency (see Q7, Q8, and the Kotlin deep-dive: **37 data-class equality / 38 inline+reified / 40 sealed vs enum / 42 coroutine exceptions**).  
**Oral check:** (1) launch vs async? (2) Why avoid GlobalScope? (3) How is cancellation “swallowed”?

---

# Stage 2: Android Platform, UI, and Hardware Boundaries (Days 6–10)

## Day 6: Android Project Skeleton and a Static Compose Screen

### Goal

Move the pure Kotlin domain code into an Android project and build a static device list before touching Bluetooth.

### Study Focus and Exercise

- Recognize MainActivity, setContent, @Composable, resources, themes, AndroidManifest.xml, the app module, and Gradle dependencies.
- Migrate domain / data using the package layout above; UI first reads a fake list.
- Keep dependency direction clear: UI -> ViewModel -> domain; data implements interfaces owned by domain. Domain does not depend on Android Context, Compose, or Bluetooth APIs.
- Use Context only at resource, system-service, and permission boundaries. Do not put an Activity Context or View in a ViewModel or singleton.
- Know the responsibility split between **Application** (process) and **Activity** (screen host).
- Write DeviceListScreen(devices, onDeviceClick) to show fake devices. UI components receive state and callbacks only.
- **ANR awareness:** list rendering is light; heavy work in composable initialization blocks the main thread.

### Acceptance

Explain what the UI, domain, and data layers should **not** depend on. Show the static list in an emulator.

**Oral check:** (1) Why never put Activity into a repository? (2) What belongs in Application? (3) What typically causes ANR?

---

## Day 7: Compose State, Events, and Side Effects

### Goal

Understand that Compose and SwiftUI are similar but not identical. A Composable is declarative rendering, not a one-time view-controller lifecycle callback.

### Study Focus

- State hoisting: a screen receives uiState and onEvent; deeply nested components do not mutate global state implicitly.
- Recomposition can happen many times. Do not start a request, device connection, or uncontrolled log in a Composable body.
- remember survives only while the current composition lives. rememberSaveable uses saved-instance-state machinery to retain small, serializable UI values across recreation. Neither is a database, and neither should hold large objects, secrets, or a connection.
- LaunchedEffect(key) is for composition-driven side effects. Its key must be stable and meaningful. A user tap should send an event to the ViewModel, not depend on recomposition.
- Separate durable UI state from one-time effects. This small exercise can render an error as state; model navigation and one-shot toasts as explicit effects later.
- Use LazyColumn for lists; key with a stable deviceId to avoid identity glitches.
- Stability drives recomposition performance: Compose skips recomposition based on whether a parameter type is @Stable / @Immutable. An all-val data class with immutable collections is "stable"; passing a MutableList or an unstable type breaks skipping and causes extra recompositions. When you need immutable-collection semantics, learn kotlinx.collections.immutable.

### Exercise and Acceptance

Render Idle, Connecting, Connected, PermissionRequired, and Error. Add a Connect / Disconnect event using fake behavior. Verify that:

- repositories and Context are not passed through every small Composable;
- a click does not invoke the data layer directly from a Composable body;
- after rotating the emulator, short-lived UI input is either retained or lost by design, and you can explain why.

**Oral check:** (1) Can recomposition reconnect BLE? (2) remember vs rememberSaveable? (3) How is this like / unlike SwiftUI body?

---

## Day 8: ViewModel, StateFlow, Lifecycle, and State Restoration

### Goal

Drive the Day 7 UI with a ViewModel and distinguish configuration change, collection lifecycle, and process death.

### Study Focus

- Keep MutableStateFlow private and expose read-only StateFlow. The ViewModel handles events and starts user-triggered coroutines in viewModelScope.
- Collect state in Compose with collectAsStateWithLifecycle() so an invisible UI does not keep collecting. It is a UI collection policy; it does not fix an upstream resource leak. Dependency: `androidx.lifecycle:lifecycle-runtime-compose`.
- A ViewModel normally survives an Activity configuration change such as rotation, but it is cleared when the Activity really finishes. After process death, in-memory ViewModels and StateFlows do not exist.
- SavedStateHandle is for small, recoverable UI input, navigation arguments, or a device id. It is not a database, a large device-list cache, or a Bluetooth connection. On process recreation, use the id to reload from a repository.
- **Process vs configuration change** (must be explainable):

| Scenario | Activity | ViewModel | remember | Persistence |
|---|---|---|---|---|
| Config change (e.g. rotation) | Usually recreated | Usually kept | Lost (unless saveable) | Unchanged |
| User finishes the screen | Destroyed | onCleared | Lost | Unchanged |
| Process death + restore | New process | New instance | Lost | Can be re-read |

### Exercise and Acceptance

Implement DeviceViewModel exposing DeviceUiState and onConnectClicked(deviceId). Write at least one ViewModel test with FakeDeviceRepository. Verify that:

- the policy for a second click while connecting is explicit: ignore, cancel, or serialize; choose one and test it;
- a new UI collector immediately receives the current state;
- you can explain why “state survived rotation” does not mean “state survives process death.”

**Oral check:** (1) When is viewModelScope cancelled? (2) Why StateFlow for UI state? (3) After process death, what is the restore source?

---

## Day 9: Flow, Permissions, and BLE Boundaries (prioritized)

### Goal

Model continuously changing data as Flow and leave Android permission / Bluetooth APIs in replaceable outer layers.  
**Prioritize so the day is finishable:**

| Priority | Content | Required? |
|---|---|---|
| P0 | Connection-status Flow + ViewModel mapping + lifecycle-aware collection | Yes |
| P0 | PermissionRequired UI + Activity Result simulated grant result | Yes |
| P1 | Permission matrix table (fill from minSdk / targetSdk + docs) | Prefer yes |
| P1 | BLE adapter interface + Fake (no real GATT) | Prefer yes |
| P2 | Physical BLE scan smoke test | Optional; Day 10+ backlog |

### Study Focus

- Use suspend fun for a one-off operation and Flow for changing scan results, connection status, or sensor readings. StateFlow is a hot state container with a current value; do not put every one-time UI effect into it.
- Understand cold versus hot Flow. Before stateIn, decide its scope, initial value, and SharingStarted behavior so background scanning does not continue without a consumer.
- Learn common operators as needed: map/filter to transform; flatMapLatest to cancel the previous inner flow when a new device is chosen or a rescan starts; conflate / collectLatest to keep only the latest value under high-frequency updates so the UI does not pile up stale frames (see Q43).
- On Android 12 (API 31) and later, common runtime permissions are BLUETOOTH_SCAN and BLUETOOTH_CONNECT; BLUETOOTH_ADVERTISE is also needed when advertising. Older Android scan rules differ and depend on target SDK. Build a permission matrix from minSdk, targetSdk, and current official documentation instead of memorizing one list.
- Manifest declaration is not runtime authorization. Request permissions through the Activity Result API in the UI / lifecycle layer. The ViewModel receives the result as an event and does not own a permission launcher.
- Real BluetoothGatt callbacks, scanning, and connections must unregister and close(). When wrapping callbacks in callbackFlow, release resources in awaitClose { ... }. An emulator cannot replace physical BLE testing.

### Exercise

1. Change the fake repository connection status to a Flow that emits ConnectionStatus and map it to UI state in the ViewModel.
2. Design a PermissionRequired UI state and Request Permission / Retry from Settings events.
3. Use rememberLauncherForActivityResult plus RequestMultiplePermissions to simulate a permission result and send it to the ViewModel.
4. Keep a fake / adapter outside the interface so neither domain nor UI depends directly on BluetoothGatt.

### Acceptance

Explain why declaring a Manifest permission is insufficient. List physical BLE checks including denial / re-grant, Bluetooth off, disconnect / reconnect, leaving the screen, unexpected device power loss, and resource release.

**Oral check:** (1) cold Flow vs StateFlow? (2) Why must ViewModel not own a permission launcher? (3) Why does callbackFlow need awaitClose?

---

## Day 10: Runnable Vertical Slice, Tests, and Retrospective

### Goal

Deliver a small demonstrable feature with clear boundaries, not disconnected syntax examples.

### Minimum Deliverables

1. A Compose app that runs in an emulator: device list, connect, connecting, success, missing permission, failure, and disconnect or retry.
2. A DeviceRepository interface and FakeDeviceRepository. Real BLE may remain a follow-up adapter.
3. DeviceViewModel + StateFlow + collectAsStateWithLifecycle(), plus a small SavedStateHandle restoration example.
4. At least four repeatable tests: two mappers / use cases, one ViewModel, and one error, cancellation, or Flow scenario.
5. A one-page migration retrospective: one real difference among Kotlin / Swift, Compose / SwiftUI, ViewModel / iOS state management, GC / ARC, and **process restoration**.
6. A physical-device backlog: BLE, permission matrix, reconnects, persistence, protocol, performance, battery use, and release.
7. Recommended: a **60-minute oral mock** with the interview Q bank (see suggested schedule there).

### Manual Acceptance Checklist

- [ ] No Composable calls a repository or starts a connection in its body.
- [ ] Context, Bluetooth APIs, and permission launchers stay out of domain models and ViewModels.
- [ ] Loading, success, failure, and missing-permission states are visible and distinct.
- [ ] There is no GlobalScope and no swallowed CancellationException.
- [ ] Rotation behavior is intentional, and the source of state after process death is explained.
- [ ] Tests are repeatable; conclusions about real BLE are based on physical devices, not an emulator.
- [ ] You can explain in ~90 seconds: Activity, Context, ViewModel, StateFlow, permissions, and ANR in one sentence each.

### Stage 2 checkpoint (map to Q bank)

Focus questions: **1–12 fundamentals + 26 BLE design + 33 capstone**; if time allows, add classics **44 (View rendering) / 45 (image memory)**. If you cannot answer, revisit the matching day instead of stacking more APIs.

---

# Working with AI and Self-Review

## Daily Review Prompt

~~~text
You are a senior Kotlin / Android code reviewer.
The code below is a migration exercise written by a Swift / iOS engineer.

Answer in this order:
1. Identify specific issues in null safety, data-class shallow copy, sealed when,
   coroutine scope / cancellation, Flow lifecycle, Compose side effects, Context / lifecycle, and ANR risk.
2. Flag mechanical translations from Swift or Java, and explain the risk.
3. Give the smallest useful changes. Do not rewrite everything just to show Kotlin syntax.
4. Give the two most valuable tests.
5. Explain one Android / Kotlin runtime behavior that I must verify myself.

Requirements:
<paste requirements>

Code:
<paste code>
~~~

## Self-Review Checklist

- [ ] Is nullability normalized at Java / network boundaries in a mapper?
- [ ] Did I use !!, lateinit, or nested scope functions without a real reason?
- [ ] Does a data class leak mutable collections or rely on shallow-copy behavior?
- [ ] Are domain results, renderable UI state, and one-time effects distinct?
- [ ] Does each coroutine have an owner, and does CancellationException propagate?
- [ ] Does Flow really represent ongoing changes, and is collection lifecycle-aware?
- [ ] Do Context, Activity / View, permission launchers, and BluetoothGatt stay in the Android outer layer?
- [ ] Does SavedStateHandle contain only small recoverable state, not a connection or large cache?
- [ ] Do key mappers, state transitions, and error / cancellation paths have tests?
- [ ] Did I avoid heavy work / blocking I/O on the main thread (ANR awareness)?

## Priority Path After Day 10

1. **Real BLE:** scan, GATT lifecycle, callback wrappers, reconnect policy, resource release, and physical testing across devices.
2. **Data and networking:** serialization (kotlinx.serialization / Moshi), HTTP client (OkHttp / Ktor), Room / DataStore, offline and caching strategy.
3. **Android engineering:** version catalogs, build variants, Hilt DI, modularization, CI, performance / battery / crash monitoring.
4. **UI completeness:** Navigation, deep links, accessibility, Compose UI tests; learn View / Fragment / RecyclerView when maintaining older apps.
5. **Concurrency depth:** Flow tests, sharing policy, supervision, thread and resource-leak diagnosis.
6. **Interview drill:** work through `smart-glasses-android-interview-questions-cn.md` (or EN prompts) weekly—five oral answers. Drill the Kotlin deep-dive **37–43** (equality, inline/reified, delegation, sealed, generics, coroutine exceptions, Flow operators) and the runtime classics **44/45** (View rendering, image memory).

## Official References to Prefer

- [Kotlin Null Safety](https://kotlinlang.org/docs/null-safety.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android app architecture](https://developer.android.com/topic/architecture)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Save UI states](https://developer.android.com/topic/libraries/architecture/saving-states)
- [Activity Result APIs](https://developer.android.com/training/basics/intents/result)
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Keep your app responsive (ANR)](https://developer.android.com/topic/performance/vitals/anr)
- [App startup](https://developer.android.com/topic/performance/vitals/launch-time)
