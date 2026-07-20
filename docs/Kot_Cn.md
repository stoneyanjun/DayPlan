# Kotlin + Android 原生开发：给 iOS / Swift 工程师的 10 天加速学习计划（智能眼镜方向）

> **时间口径**：Day 0 是 1–2 小时的环境预热，建议在正式开始前完成，**不计入**后面的 10 天；Day 1–10 每天约 2–3 小时。  
> **技术路线**：Kotlin、Jetpack Compose、ViewModel、StateFlow、Coroutines / Flow，以及可替换的 Fake Repository。  
> **练习范围**：只完成“设备发现 / 连接状态”垂直切片。真实 BLE、相机、语音识别和 AI 对话是后续独立功能，不要在十天内并行实现。  
> **配套题库**：`smart-glasses-android-interview-questions-cn.md`（含答案、Swift 对照、设计理念）；英文提问版见 `smart-glasses-android-interview-questions-en.md`。

## 适用对象、边界与完成标准

本计划面向已有 Swift、iOS 工程分层、业务建模和 async/await 经验的工程师。它不是 Kotlin 或 Android 的百科全书，而是把已有能力迁移到一条现代 Android 路径。

完成后，你应能：

- 读懂 DTO mapper、领域模型、sealed hierarchy、repository、use case 和基础 coroutine / Flow 代码。
- 写出空安全、可测试、不过度依赖语法糖的 Kotlin 业务代码。
- 用 Compose + ViewModel + StateFlow 交付可运行的设备连接状态页面。
- 说清配置变更、进程死亡、协程取消、Context、权限、ANR 风险边界和 Gradle 的职责。
- 用 iOS 经验做**对照学习**，而不是机械翻译 API 名。

十天后不应声称“掌握 Android”。复杂 Flow、泛型与 reified、模块化、真实 BLE GATT、性能、发布、遗留 View / Fragment 工程，仍需要在项目中继续练习。

### 十天内明确不做（避免范围爆炸）

| 暂缓项 | 原因 |
|---|---|
| 真实 BLE GATT 全流程联调 | 依赖真机、权限矩阵与硬件协议；Day 9 只做边界与 fake |
| CameraX / 语音 / AI 对话 | 各自是独立垂直切片，会冲掉连接状态主线 |
| 多模块 + 完整 DI 工程化 | 先学会手工构造与 fake；Hilt 放 Day 10 后 |
| Navigation 多页面深链 | 本计划单屏垂直切片即可；概念可认识，不实现栈 |
| 遗留 View / XML / Fragment 手写 UI | 维护旧项目时再补；新代码以 Compose 为主 |
| Binder / 系统源码 / 自定义 View 绘制 | 面试可认识名词；十天内不深挖 |

每天的“完成”至少包含：

1. 一小段可编译的 Kotlin / Android 代码；
2. 1–2 个可重复运行的测试，或一次明确的运行验证；
3. 一个 Swift 对照，以及真正的语义差异；
4. 一条主动发现并修正的风险或可读性问题。

---

## 计划评估摘要（为什么按这个顺序学）

| 优点 | 说明 |
|---|---|
| 先纯 Kotlin 再 Android | 避免把空安全、协程、生命周期、BLE 混在一次调试里 |
| 窄垂直切片 | 智能眼镜场景真实，但边界清楚，可演示 |
| Swift 对照 + 反模式表 | 防止 `data class ≈ struct`、`suspend ≈ 后台线程` 等致命误解 |
| 验收与 AI review | 每天有可检查产出，而不是只“看过文档” |

| 常见薄弱点（本修订已加强） | 处理方式 |
|---|---|
| Day 9 同时塞进 Flow + 权限 + BLE | 拆成 P0（Flow + 权限）/ P1（BLE 边界设计） |
| 缺经典 Android 面试概念 | Day 6/8 增加 Application、ANR、进程；题库扩充 |
| 测试栈不具体 | Day 5/8 明确 JUnit + coroutines-test；可选 Turbine |
| 与面试题脱节 | 阶段检查点对接题库题号 |
| 包结构与依赖方向偏抽象 | Day 6 给出推荐包结构模板 |
| 缺“主动回忆” | 每日输出增加 3 题口述检查 |

---

## 迁移前先建立的心智模型

不要把 Android API 当成 Swift / iOS 的逐字替换。下面这些差异会反复影响设计：

| Swift / iOS 中的熟悉概念 | Kotlin / Android 中的近似物 | 不能忽略的差异 |
|---|---|---|
| struct + let | data class + val | data class 仍是普通 class；val 只固定属性引用，copy() 是浅拷贝，不提供 Swift struct 的值语义。 |
| Optional | T?、?.、?: | 两者都空安全；Kotlin 还要面对 Java 的 platform type，必须在边界收敛。 |
| async / await | suspend fun、launch、async | suspend 不等于后台线程，也不会自动启动任务；Kotlin 的并发能力主要来自库和 scope。 |
| MainActor | Dispatchers.Main | MainActor 有隔离语义；Main dispatcher 主要解决调度，不能等同于数据竞争保护。 |
| SwiftUI body | @Composable | Composable 可因重组多次执行，函数体应接近纯渲染，不能直接启动不受控副作用。 |
| @State / ObservableObject | remember / rememberSaveable / ViewModel + StateFlow | 记忆、配置变更和进程死亡是三层不同的问题。 |
| View controller 生命周期 | Activity / Fragment / Compose 生命周期 | Android 还会因内存压力杀掉进程；旋转后仍正常，不等于状态恢复正确。 |
| AppDelegate / @main | Application + Activity | Application 可做进程级初始化；业务状态仍不应塞进全局单例。 |
| 阻塞主线程 → 卡顿 | 主线程阻塞过久 → **ANR** | Android 对主线程无响应有系统级对话框与更严格的后台策略。 |

### 推荐工程包结构（Day 6 起采用）

```text
app/src/main/java/.../
  ui/connection/     # Screen + 小组件
  presentation/      # ViewModel、UiState、UiEvent
  domain/            # model、repository 接口、use case
  data/              # Fake / 真实 repository、DTO、mapper
  di/                # 可选：手工 factory；Hilt 放后期
```

依赖方向：**ui → presentation → domain ← data**。domain 不依赖 Android、Compose、Bluetooth API。

---

## 统一练习范围与每日节奏

全程围绕一个窄而真实的智能眼镜用例：

~~~text
设备列表 -> 点击连接 -> Connecting -> Connected / PermissionRequired / Error
                                      -> 断开或重试
~~~

Day 1–5 先在纯 Kotlin 中完成领域切片；Day 6–10 再放入 Android 应用。这样不会把语言、架构、生命周期和 BLE 错误混在同一次调试里。

| 时间 | 建议活动 |
|---|---|
| 20–30 分钟 | 阅读当天概念，写出一个 Swift 对照 |
| 70–90 分钟 | 亲手完成当天最小代码，而不是只看示例 |
| 20 分钟 | 写 1–2 个关键测试，或在模拟器 / 真机验证 |
| 15 分钟 | 重构命名、删除不必要的 scope function 或抽象 |
| 10 分钟 | 中文复盘；**口述 3 道当天检查题**（不看笔记） |
| 5 分钟 | 可选：写 3–5 句英文技术总结 |

每日输出模板：

~~~text
Day X 输出
1. Kotlin / Android 代码：
2. 一个 Swift 对照：
3. 验证结果（测试或运行说明）：
4. 今天发现的语义差异：
5. AI / 自查发现的问题与最小修复：
6. 明天要主动验证的问题：
7. 口述检查（3 题，对/错简述）：
~~~

**测试最小栈（阶段一即可引入）**

- JUnit 4/5 + 断言（Kotlin 可用 `kotlin.test` 或 Truth）
- `kotlinx-coroutines-test` 的 `runTest`
- ViewModel 测试：`MainDispatcherRule` + Fake repository
- 可选：Turbine 断言 Flow 发射序列
- UI 层：Compose 用 `createComposeRule` 写 UI 测试；需要 Android 框架但想跑在 JVM 上时了解 Robolectric，真实设备交互用 instrumented 测试（androidTest）。区分**单元测试**（快、纯 JVM）与**设备测试**（慢、真实环境）。

---

# Day 0（不计入 10 天）：环境预热，1–2 小时

## 目标

让现代 Compose 项目在模拟器运行，提前排除 SDK、模拟器、Gradle 和 IDE 问题。

## 操作

1. 用 Android Studio 创建 **Empty Activity（Compose）** 项目，并运行默认页面。
2. 找到 AndroidManifest.xml、app module 的 build.gradle.kts、MainActivity.kt、ui/theme、res，以及版本目录（若项目使用 gradle/libs.versions.toml）。
3. 在 MainActivity 添加一条日志，在 Logcat 看到它；学会筛选应用进程和错误级别。
4. 记录项目的 minSdk、targetSdk、application id 和模拟器 API 级别；它们会影响权限和硬件行为。
5. （可选）在模拟器上试一次旋转，观察默认 Activity 重建；先建立“配置变更会发生”的直觉。

## 只需建立、不要求精通的概念

- Activity 是 Android UI 的宿主；新项目优先 Compose，但日后维护旧项目仍会遇到 Fragment 和 View。
- `Application` 是进程级入口，可做全局初始化；**不要**把屏幕业务状态塞进自定义 Application 单例。
- Context 是资源、系统服务、权限等 Android 环境的入口。不要把 Activity Context、View 或 Composable 存进单例、repository 或 ViewModel；需要长期持有 Android Context 时，通常只考虑 application context。
- Gradle 管理构建、依赖、Android SDK 配置和变体。Day 0 只认识位置，不手写复杂构建逻辑。
- 主线程负责 UI；在主线程做重计算或阻塞 I/O 会卡顿，严重时触发 **ANR**。

## 验收

- 模拟器上能看到默认 Compose 页面。
- 能用自己的话画出“Activity -> Compose UI -> ViewModel -> domain / data”的粗略图。
- 能说出 minSdk / targetSdk 各影响什么（权限、API 可用性、商店要求等方向即可）。

---

# 阶段一：Kotlin 语言与并发（Day 1–5）

## Day 1：Kotlin 语法迁移与空安全

### 目标

正确地从 Swift Optional 迁移到 Kotlin nullable type，并在 Java / 网络 API 边界把不可靠数据转换为可信领域模型。

### 学习重点

- val / var、默认参数、命名参数、字符串模板；if 和 when 都是表达式。
- ?.、Elvis 运算符 ?:、smart cast、mapNotNull；优先早返回 ?: return。
- !! 不是“方便的强制解包”，而是把失败推迟为 NPE；练习阶段一律不用。
- Java 未标注 nullability 的返回值是 platform type。它既不是可靠的 String，也不是普通 String?；在 adapter / mapper 层显式处理。
- require / requireNotNull 用于调用方参数前置条件（IllegalArgumentException）；check / checkNotNull 用于内部状态断言（IllegalStateException）。
- lateinit var 只能用于稍后初始化的非空引用属性。提前读取会抛 UninitializedPropertyAccessException；它不是 Optional，也不应替代构造器注入。by lazy 适合一次性惰性初始化，但不要把昂贵或可失败 I/O 藏进去。

### 练习：设备 DTO mapper

实现下面的边界转换，并为“空 id、未知传输方式、空电量、越界电量”写测试：

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
    val batteryPercent: Int?, // null 表示“未知”，不要伪装成 0%
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

这里把空电量保留为未知，是因为“设备未上报”与“电量为 0”是不同业务事实。若产品确实要求默认值，默认值必须在命名和 UI 中可见，例如 BatteryState.Unknown，而不是悄悄改成 0。

### Swift 对照与验收

Swift 中同样可用 guard let 和 map 处理 Optional；不同点是 Kotlin mapper 往往还要防御 Java platform type。写一个 Swift `toDomain() -> GlassesDevice?` 对照，并能解释：为什么 Android 边界处的 nullability 不能只相信类型提示。

**口述检查：** (1) `!!` 与 Swift `!` 风险是否相同？(2) platform type 是什么？(3) 为何 `batteryPercent = null` 不等于 0？

---

## Day 2：建模、data class、object、extension

### 目标

学习 Kotlin 的建模工具，同时避免把 data class 当成 Swift struct。

### 学习重点

- data class、copy、解构声明、主构造器和 init；默认使用 val。
- object（单例）、companion object（依附在类上的对象）、top-level function，以及 extension function / property。
- extension 是静态解析：成员方法优先于同名 extension；extension 不能真正覆写成员，也不能访问 receiver 的 private / protected 成员。
- data class 可以含 var 和可变对象；copy() 只复制第一层引用。List 是只读接口，不保证深度不可变；不要把仍由外部持有的 MutableList 暴露为领域状态。
- @JvmInline value class 可用于 DeviceId 这类轻量类型区分，但在泛型、nullable、Java 互操作等边界可能装箱；不要把它误当成通用 struct 替代品。

### 练习：由工厂创建合法初始会话

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

私有构造器让调用方不能误建一个初始即 CONNECTED 的会话；factory 集中初始状态和前置条件。若创建过程开始协调 repository、时钟或多个聚合，再引入独立 factory / use case；只是转换值时，top-level function 更简单。

### Swift 对照与验收

Swift 可用 private initializer + static func createInitial 达到相同的不变式。真正不同的是：Swift struct 赋值 / 传参通常拥有值语义，而 Kotlin data class 的 copy() 是显式浅拷贝。写一个实验：copy() 后修改内部 MutableList，观察两个对象都看到变化；再改成不暴露可变集合的设计。

**口述检查：** (1) data class 与 struct 的核心语义差？(2) extension 能否 override？(3) value class 何时可能装箱？

---

## Day 3：集合、lambda 与可读性

### 目标

熟悉 Kotlin 集合转换，但让业务代码先清楚、再简洁。

### 学习重点

- map、filter、mapNotNull、flatMap、firstOrNull、any、all、sumOf、groupBy、associateBy、fold。
- 标准集合操作默认 eager。只在大集合或多步流水线确实受益时评估 asSequence()；它类似 Swift 的 lazy sequence，也会改变求值时机和调试体验。
- let 适合短小 nullable 转换；also 只放显而易见的副作用；apply 用于配置可变对象；run / with 只在 receiver 能保持清晰时使用。
- 一旦嵌套 scope function 让 it / this 指向不明，立刻改为具名局部变量。普通 if 通常比 takeIf 链更易读。

### 练习与验收

构造至少 8 条设备记录，完成：

- 找到已连接设备，按传输方式或固件主版本分组；
- 找出电量最低、最近活跃的设备；
- 去重固件版本，转成以 DeviceId 为键、GlassesDevice 为值的 map；
- 从 nullable DTO 列表得到有效领域模型列表。

至少各使用一次 mapNotNull、groupBy、associateBy，并为其中两个查询写 Swift map / compactMap / Dictionary 对照。复盘中指出一段你**没有**使用 scope function 的代码，以及为什么这样更清楚。

**口述检查：** (1) mapNotNull 与 Swift compactMap？(2) List 只读是否等于不可变？(3) 何时才用 Sequence？

---

## Day 4：sealed hierarchy、领域结果与 UI 状态

### 目标

用封闭的类型集合描述连接结果，清楚区分“发生了什么”和“页面应该显示什么”。

### 学习重点

- sealed interface / sealed class、data class、data object 与完整 when。data object 需要 Kotlin 1.9+；旧工程用普通 object。
- 领域结果是业务事实；UI state 是可渲染的稳定页面状态；一次性导航、toast 等 effect 不应伪装成永久 state。
- Kotlin 没有 Swift throws 那样要求每个调用点写 try 的 checked-error 机制。预期业务失败通常用 sealed result；异常留给真正异常的边界。
- runCatching 会捕获 CancellationException。协程里若把它变成普通失败并吞掉，会破坏取消传播。

### 练习

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

不要把原始 Throwable 放进长寿命 UI state，也不要让 UI 直接决定底层错误分类。为完整 when mapper 写测试，并写一段 Swift enum associated values 对照。

**口述检查：** (1) domain result 与 UI state 为何分离？(2) runCatching 与取消？(3) sealed when 漏分支编译会怎样？

---

## Day 5：Coroutines 与纯 Kotlin 综合验收

### 目标

理解 Kotlin 协程的阅读与组合方式，完成不依赖 Android UI 的 fake 设备连接领域切片。

| Swift | Kotlin | 必须记住的语义 |
|---|---|---|
| async func | suspend fun | 在协程 / suspend 上下文中直接调用；suspend 本身不创建任务，也不保证切线程。 |
| async let / task-group child | async { } + Deferred.await() | async 创建 Deferred，只有读取它的结果时才 await()。 |
| Task { } | launch { } | 都需要清晰 owner；Android 用户操作通常由 viewModelScope 承担。 |
| MainActor | Dispatchers.Main | 前者有 actor isolation，后者主要是 dispatcher；两者不等价。 |
| task group | coroutineScope { } | 都建立并等待结构化子任务；supervisorScope 则允许兄弟失败不自动取消全部。 |

### 学习重点

- suspend fun、coroutineScope、launch、async、await、withContext。
- 结构化并发：子任务应归属 coroutineScope、viewModelScope 等有正确父 Job 的 scope。不要用 GlobalScope，也不要在 repository 随手创建无人管理的 scope。
- 取消是协作式的。可取消挂起函数通常会响应取消；CPU 循环或阻塞调用需要 ensureActive()、isActive、yield() 或改用可取消 API。
- 若写宽泛的 catch (Exception)，必须优先重新抛出 CancellationException。
- 异常与监督：coroutineScope 中任一子任务失败会取消兄弟并向上抛；supervisorScope / SupervisorJob 让兄弟失败互不牵连。launch 的异常立即向上传播，async 的异常被封装、在 await() 时才抛出——不 await 可能被吞（见题库 42）。
- withContext(Dispatchers.IO) 只包装真实阻塞 I/O；正确实现的 suspend 网络 API 不应被机械再包一层。dispatcher 应可注入，以便测试。

### 综合练习：建议最小骨架

~~~kotlin
interface DeviceRepository {
    suspend fun getDevice(deviceId: String): GlassesDevice?
    suspend fun getCapabilities(deviceId: String): DeviceCapabilities
    suspend fun connect(deviceId: String): ConnectDeviceResult
}

class FakeDeviceRepository(
    private val delayMillis: Long = 200,
) : DeviceRepository {
    // 用 delay 模拟 I/O；测试用 TestDispatcher 控制时间
    // connect 可按 deviceId 返回 Connected / DeviceNotFound / PermissionDenied
}

class ConnectDeviceUseCase(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(deviceId: String): ConnectDeviceResult {
        return coroutineScope {
            val deviceDeferred = async { repository.getDevice(deviceId) }
            val capsDeferred = async { repository.getCapabilities(deviceId) }
            val device = deviceDeferred.await() ?: return@coroutineScope ConnectDeviceResult.DeviceNotFound
            capsDeferred.await() // 演示并发组合；真实逻辑按协议合并
            repository.connect(deviceId)
        }
    }
}
~~~

### 验收

- connect(deviceId) 是 suspend fun；
- 用 coroutineScope 并发读取设备详情和能力配置，再组合结果；
- 模拟取消，确认它没有变成普通 Error；
- 用 kotlinx-coroutines-test 的 runTest 为 Day 1 mapper 和 use case 各写至少 1–2 个测试。

阶段一结束时，你应能解释：suspend fun、launch 与 async 的关系；为什么 suspend 调用没有 .await()；以及 Kotlin 中取消为何必须显式保留。

### 阶段一检查点（对照题库）

能口述回答题库 **Day1–5 相关**：空安全边界、data class 浅拷贝、sealed 错误建模、suspend 与结构化并发（见面试题 7、8，以及 Kotlin 深潜 **37 data class 相等性 / 38 inline+reified / 40 sealed vs enum / 42 协程异常**）。  
**口述检查：** (1) launch 与 async 区别？(2) 为何不用 GlobalScope？(3) 取消如何被“吞掉”？

---

# 阶段二：Android 平台、UI 与硬件边界（Day 6–10）

## Day 6：Android 工程骨架与静态 Compose 页面

### 目标

将纯 Kotlin 领域代码放进 Android 项目，完成静态设备列表，而不是立刻接入蓝牙。

### 学习重点与练习

- 认识 MainActivity、setContent、@Composable、资源、主题、AndroidManifest.xml、app module 与 Gradle 依赖。
- 按上文包结构迁移 domain / data；UI 先只读 fake 列表。
- 用清晰的依赖方向组织代码：UI -> ViewModel -> domain；data 实现 domain 定义的接口。domain 不依赖 Android Context、Compose 或 Bluetooth API。
- Context 只在资源、系统服务、权限等边界使用。不要把 Activity Context / View 放入 ViewModel 或单例，以免泄漏。
- 认识 **Application** 与 **Activity** 的职责差：前者进程级，后者屏幕宿主。
- 写出 DeviceListScreen(devices, onDeviceClick)，先显示 fake 设备；UI 组件只接收状态和回调。
- **ANR 意识**：列表渲染本身轻量；若在 composable 初始化里做重计算，会卡主线程。

### 验收

能说明 UI、domain、data 三层各自**不应该**依赖什么；模拟器能展示静态列表。

**口述检查：** (1) 为何不能把 Activity 放进 repository？(2) Application 适合放什么？(3) ANR 通常因什么触发？

---

## Day 7：Compose 状态、事件与副作用

### 目标

理解 Compose 与 SwiftUI 相似但不等价：Composable 是声明式渲染函数，不是只执行一次的 view controller 生命周期回调。

### 学习重点

- state hoisting：screen 接收 uiState 与 onEvent，深层组件不隐式修改全局状态。
- 重组可能多次发生；不要在 Composable 函数体直接发请求、连接设备或写不可控日志。
- remember 只在当前 composition 存活时记忆；rememberSaveable 使用 saved-instance-state 机制跨重建保存少量、可序列化 UI 值。两者都不是数据库，也不应保存大对象、密钥或连接实例。
- LaunchedEffect(key) 只处理由 composition 驱动的副作用，key 必须稳定且有明确含义。用户点击连接应发 event 给 ViewModel，而不是靠重组触发。
- 将持久 UI state 与一次性 effect 分开。简单练习可先把错误显示为 state；导航、一次性 toast 等在下一步用明确 effect 机制表达。
- 列表用 LazyColumn；key 使用稳定 deviceId，避免错位重组。
- 稳定性影响重组性能：Compose 依据参数类型是否 @Stable / @Immutable 决定能否跳过重组。全 val 的 data class + 不可变集合更“稳定”；把 MutableList 或不稳定类型传进 composable 会破坏跳过优化，导致多余重组。需要不可变集合语义时了解 kotlinx.collections.immutable（见题库 43 之外的稳定性主题）。

### 练习与验收

渲染 Idle、Connecting、Connected、PermissionRequired、Error，加“连接 / 断开”事件并保持 fake 行为。确认：

- repository 和 Context 没有被一路传进每个小 Composable；
- 点击事件不会在 Composable 函数体直接调用 data layer；
- 旋转模拟器后，短暂 UI 输入按预期保留或丢失，并能解释原因。

**口述检查：** (1) recomposition 会不会重新连 BLE？(2) remember 与 rememberSaveable？(3) 与 SwiftUI body 的异同？

---

## Day 8：ViewModel、StateFlow、生命周期与状态恢复

### 目标

让 ViewModel 驱动 Day 7 的 UI，并区别配置变更、订阅生命周期和进程死亡。

### 学习重点

- 私有 MutableStateFlow、对外只读 StateFlow；ViewModel 处理事件并在 viewModelScope 启动用户触发的协程。
- Compose 中以 collectAsStateWithLifecycle() 收集 state，避免不可见 UI 仍持续收集。它是 UI 收集策略，不会替你修复上游资源泄漏。依赖：`androidx.lifecycle:lifecycle-runtime-compose`。
- ViewModel 通常可跨 Activity 配置变更（例如旋转），但在 Activity 真正结束时会被清除；进程死亡后内存中的 ViewModel 和 StateFlow 都不存在。
- SavedStateHandle 适合保存少量、可恢复的 UI 输入、导航参数或设备 id。不要把它当数据库、缓存大型设备列表或保存 Bluetooth 连接；进程重建时应依靠 id 从 repository 重新加载。
- **进程与配置变更对照表**（务必能讲清）：

| 场景 | Activity | ViewModel | remember | 持久化 |
|---|---|---|---|---|
| 旋转等配置变更 | 通常重建 | 通常保留 | 丢失（除非 saveable） | 不动 |
| 用户返回退出 | 销毁 | onCleared | 丢失 | 不动 |
| 系统杀进程后恢复 | 新进程 | 新建 | 丢失 | 可读回 |

### 练习与验收

实现 DeviceViewModel，暴露 DeviceUiState 和 onConnectClicked(deviceId)。写至少一个使用 FakeDeviceRepository 的 ViewModel 测试，并验证：

- 连接中再次点击的策略明确（忽略、取消、或串行化，三者任选其一但要测试）；
- 新 UI collector 订阅后能立即得到当前状态；
- 解释“旋转后状态还在”为什么不等于“被系统杀进程后可恢复”。

**口述检查：** (1) viewModelScope 何时取消？(2) StateFlow 为何适合 UI state？(3) 进程死亡后从哪恢复？

---

## Day 9：Flow、权限与 BLE 边界（分层优先级）

### 目标

把持续变化的数据建模为 Flow，并把 Android 权限和 Bluetooth API 留在可替换的外层。  
**本日拆优先级，避免一次做不完：**

| 优先级 | 内容 | 必须完成？ |
|---|---|---|
| P0 | 连接状态 Flow + ViewModel 映射 + 生命周期收集 | 是 |
| P0 | PermissionRequired UI + Activity Result 模拟授权结果 | 是 |
| P1 | 权限矩阵表（按 minSdk / targetSdk 查文档填写） | 尽量 |
| P1 | BLE adapter 接口设计 + Fake 实现（不接真 GATT） | 尽量 |
| P2 | 真机 BLE 扫描试跑 | 可选，列入 Day 10 后 backlog |

### 学习重点

- 一次性操作用 suspend fun；连续变化的扫描结果、连接状态、传感器读数用 Flow。StateFlow 是持有当前值的 hot state container，不要把所有一次性 UI effect 都塞进去。
- 了解 cold Flow 与 hot Flow 的区别；使用 stateIn 前先决定 scope、初值和 SharingStarted，避免后台无意义地持续扫描。
- 常用操作符按需了解：map/filter 变换；flatMapLatest 在“选了新设备 / 重新扫描”时取消上一个内层流；conflate / collectLatest 面对高频状态更新只保留最新值，避免 UI 堆积过期帧（见题库 43）。
- Android 12（API 31）及以上常用 BLUETOOTH_SCAN、BLUETOOTH_CONNECT（有广播需求时还有 BLUETOOTH_ADVERTISE）运行时权限。旧 Android 的扫描权限规则不同，且与 target SDK 有关；根据 minSdk、targetSdk 和官方文档建立权限矩阵，不要死记一组权限。
- Manifest 声明不等于已获授权。使用 Activity Result API 在 UI / lifecycle 层请求权限；ViewModel 接收结果事件，不直接持有 permission launcher。
- 真实 BluetoothGatt 回调、扫描和连接都必须取消注册并 close()。如果用 callbackFlow 封装回调，必须在 awaitClose { ... } 中释放资源。模拟器不能替代 BLE 真机测试。

### 练习

1. 将 fake repository 的连接状态改为一个输出 ConnectionStatus 的 Flow，由 ViewModel 映射为 UI state。
2. 设计 PermissionRequired UI 与“请求权限 / 设置页重试”事件。
3. 用 rememberLauncherForActivityResult + RequestMultiplePermissions 模拟权限结果；将结果发给 ViewModel。
4. 在接口外实现一个 fake / adapter，确保 domain 与 UI 都不直接依赖 BluetoothGatt。

### 验收

能说出为什么“声明了 Manifest 权限”还不足够；能列出真机 BLE 验证至少包括：权限拒绝 / 再授权、蓝牙关闭、断开重连、页面离开、设备异常断电和资源释放。

**口述检查：** (1) cold Flow 与 StateFlow？(2) 为何 ViewModel 不持有 permission launcher？(3) callbackFlow 为何必须 awaitClose？

---

## Day 10：可运行垂直切片、测试与复盘

### 目标

交付一个可演示、边界清楚的小功能，而不是一组互不相连的语法示例。

### 最小交付

1. 一个可在模拟器运行的 Compose 应用：设备列表、连接、连接中、成功、权限不足、失败、断开或重试。
2. DeviceRepository interface 和 FakeDeviceRepository；真实 BLE 仍可留为 adapter 的待办。
3. DeviceViewModel + StateFlow + collectAsStateWithLifecycle()，以及少量 SavedStateHandle 状态恢复示例。
4. 至少 4 个可重复运行的测试：2 个 mapper / use case、1 个 ViewModel、1 个错误或取消 / Flow 场景。
5. 一页迁移复盘：Kotlin / Swift、Compose / SwiftUI、ViewModel / iOS 状态管理、GC / ARC、**进程恢复** 各写一个真实差异。
6. 一份真机待办：BLE、权限矩阵、断线重连、持久化、协议、性能、耗电与发布。
7. （建议）用面试题库做 **60 分钟模拟口述**（见题库建议日程）。

### 手工验收清单

- [ ] Composable 不直接调用 repository，也不在函数体发起连接。
- [ ] Context、Bluetooth API 和权限 launcher 不进入 domain model / ViewModel。
- [ ] 加载、成功、失败、权限不足的状态可见且互不混淆。
- [ ] 没有 GlobalScope，也没有吞掉 CancellationException。
- [ ] 旋转后状态行为符合设计；同时说明进程死亡后的重建来源。
- [ ] 测试可重复运行；真实 BLE 结论只基于真机，而不是模拟器。
- [ ] 能用 90 秒说清：Activity、Context、ViewModel、StateFlow、权限、ANR 各一句。

### 阶段二检查点（对照题库）

重点题：**1–12 基础 + 26 BLE 设计 + 33 综合设计**；行有余力再看经典补充 **44（View 渲染）/ 45（图像内存）**。答不出时回看对应 Day，而不是继续堆新 API。

---

# AI 协作与自查

## 每日 review prompt

~~~text
你是资深 Kotlin / Android code reviewer。
以下代码来自一位 Swift / iOS 工程师的迁移练习。

请按顺序回答：
1. 指出 null safety、data class 浅拷贝、sealed when、协程 scope / cancellation、
   Flow 生命周期、Compose 副作用、Context / 生命周期、ANR 风险方面的具体问题。
2. 标出哪些写法是 Swift 或 Java 的机械翻译，并解释风险。
3. 给出最小修改建议；不要为了展示 Kotlin 语法糖而整体重写。
4. 给出最有价值的 2 个测试。
5. 说明一个我必须亲自验证的 Android / Kotlin 运行时行为。

需求：
<粘贴需求>

代码：
<粘贴代码>
~~~

## 自检 checklist

- [ ] Java / 网络边界的 nullable 是否已在 mapper 中收敛？
- [ ] 是否不必要地用了 !!、lateinit 或嵌套 scope function？
- [ ] data class 是否泄漏了可变集合或依赖了浅拷贝？
- [ ] 领域结果、可渲染 UI state、一次性 effect 是否分开？
- [ ] 协程是否有 owner，并让 CancellationException 继续传播？
- [ ] Flow 是否真的代表持续变化的数据，收集是否有生命周期意识？
- [ ] Context、Activity / View、permission launcher、BluetoothGatt 是否只在 Android 外层？
- [ ] SavedStateHandle 是否只保存小而可恢复的状态，而非连接或大缓存？
- [ ] 关键 mapper、状态迁移、错误 / 取消路径是否有测试？
- [ ] 主线程是否避免了重计算 / 阻塞 I/O（ANR 意识）？

## Day 10 之后的优先路线

1. **真实 BLE**：扫描、GATT 生命周期、回调封装、重连策略、资源释放、多机型真机测试。
2. **数据与网络**：序列化（kotlinx.serialization / Moshi）、HTTP client（OkHttp / Ktor）、Room / DataStore、离线与缓存策略。
3. **Android 工程化**：version catalog、build variant、Hilt DI、模块化、CI、性能 / 耗电 / 崩溃监控。
4. **UI 完整性**：Navigation、深链、无障碍、Compose UI test；维护旧项目时再补 View / Fragment / RecyclerView。
5. **并发深化**：Flow 测试、共享策略、异常监督、线程与资源泄漏诊断。
6. **面试巩固**：完整过一遍 `smart-glasses-android-interview-questions-cn.md`，每周口述 5 题；语言层重点刷 Kotlin 深潜 **37–43**（相等性、inline/reified、委托、sealed、泛型、协程异常、Flow 操作符），运行时补 **44/45**（View 渲染、图像内存）。

## 推荐优先查阅的官方资料

- [Kotlin Null safety](https://kotlinlang.org/docs/null-safety.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android app architecture](https://developer.android.com/topic/architecture)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Save UI states](https://developer.android.com/topic/libraries/architecture/saving-states)
- [Activity Result APIs](https://developer.android.com/training/basics/intents/result)
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Keep your app responsive (ANR)](https://developer.android.com/topic/performance/vitals/anr)
- [App startup](https://developer.android.com/topic/performance/vitals/launch-time)
