# 智能眼镜 Android 入门面试题：完整答案、Swift/iOS 对照与设计理念

面向 **Swift / iOS 工程师转 Android** 的学习型题库。  
英文提问版：`smart-glasses-android-interview-questions-en.md`。  
学习计划：`Kot_Cn.md` / `Kot.md`。

本文件目标：

1. **完善答案**：不是关键词列表，而是 60–90 秒可口述的组织方式；
2. **对照 Swift / iOS**：有对应就说清对应与差异，没有对应就说明“不可硬映射”；
3. **设计理念**：把“Android 为什么这样设计”和“Swift/iOS 为什么那样设计”放在同一比较轴上。

刻意从**应用层**开始：生命周期、状态、协程、权限、后台限制、BLE 场景。能讲清这些，比背 Binder / SurfaceFlinger 名词更有价值。系统细节出现在「经典题」中，也以**心智模型**为主。

---

## 使用方式

1. 先不看答案，口述 60–90 秒；
2. 对照参考回答，补「责任 / 生命周期 / 眼镜例子 / 取舍」四要素；
3. 读 Swift 对照，刻意找**语义差**，不要记“等于某某 API”；
4. 读设计理念段，用自己的话复述一分钟。

版本敏感点（权限、前台服务、后台执行）不要死记版本号。更专业的说法是：

> “我会按当前 `targetSdk` 与目标 Android 版本查官方行为，再实现兼容分支。”

---

## 先建立一张心智地图

```text
Compose UI  <── renders ──  immutable UiState / StateFlow
    │                              ▲
 user event                         │ state stream
    ▼                              │
ViewModel ── coordinates ── use case / repository
                                    │
                 BLE / Camera / network / Room / DataStore / OS APIs
```

| 组件 | 一句话责任 |
|---|---|
| `Activity` | UI 入口 / 宿主与 Android 生命周期对接 |
| Compose | 根据状态渲染；不拥有长任务 |
| `ViewModel` | 屏幕级状态与用户操作协调 |
| Repository | 隐藏 BLE / 网络 / 存储来源 |
| 系统 | 可停组件、可杀进程——**可恢复**是默认假设 |

---

# 第一部分：Android 应用基础

## 1. Activity 与生命周期

**题目：** 什么是 `Activity`？连接状态页从可见、被遮挡到销毁时，哪些生命周期问题最重要？

### 参考回答

`Activity` 是 Android **带窗口的 UI 入口 / 宿主**：创建窗口、承接交互，把 Compose 或传统 View 放进屏幕。它不是整个 app，更不应成为 BLE、网络和业务规则的巨型容器。

与可见性相关的资源要跟生命周期对齐：页面 `onStop` 后，停止只为该页服务的动画、UI 状态收集、相机 preview。粗略时间线：`onStart` 后可见，`onResume` 后通常可交互；`onStop` 后不应再推不可见 UI 更新。真正长期的连接策略属于 repository 或合规的后台工作，而不是绑死在某一个 Activity 实例上。

必须区分两件事：

1. **配置变更**（如旋转）常会重建 Activity，但 `ViewModel` 通常还在；
2. **进程被杀**后，内存中的 Activity 与 ViewModel 都可能不存在，只能从持久化数据 + 真实设备状态恢复。

不要把 `onDestroy()` 当成“一定会被调用的最终清理回调”——进程可能直接被杀。

**眼镜例子：** 预览页离开时关闭 preview/analyzer；用户明确开启的持续录制，应由符合前台规则的组件拥有，而不是“这个 Activity 还活着”。

### Swift / iOS 对照

| iOS | Android | 差异 |
|---|---|---|
| `UIViewController` / SwiftUI 场景宿主 | `Activity` | 都是 UI 宿主；Android 配置变更重建更“日常” |
| SwiftUI `View` | `@Composable` | 都是短生命周期描述，不适合持有长任务 |
| app 被挂起 / 终止 | 进程回收 + 组件销毁 | iOS 也会杀 app；Android 用组件 + 进程优先级更显性 |

### 设计理念（短）

Android 把 UI 拆成可被系统重建的组件，而不是假设“一个 controller 从启动活到卸载”。**可重建**是一等公民。

---

## 2. Application 与 Activity `[经典]`

**题目：** 自定义 `Application` 用来做什么？什么**不该**放进 `Application`？它和 `Activity` 生命周期有何不同？

### 参考回答

`Application` 代表 **app 进程级**对象，在大多数单进程 app 中先于任何 Activity 创建，进程结束时一起消失。适合：依赖图根初始化、崩溃上报、轻量全局配置、与“屏幕无关”的注册。

**不该**放：当前连接 UI 状态、某个屏幕的 ViewModel 逻辑、默认假设“永远在线”的 BLE 会话、大量同步初始化（拖慢冷启动、占主线程）。

`Activity` 跟**一个界面实例**绑定，可因配置变更重建；`Application` 在同一进程内通常只有一个（多进程时每个进程各有一个，这是坑）。业务上的“当前已连接设备”应以 repository 的 source of truth 为准，而不是 `MyApp.currentDevice` 全局 var。

### Swift / iOS 对照

- 粗略像 `UIApplication` + `AppDelegate` / `@main` App 的初始化位置。
- iOS 也常有人滥用单例；两边正确做法都是：**组合根（composition root）清晰，屏幕状态不进全局**。

### 设计理念

开放系统上多个组件（Activity/Service/Receiver）可独立被拉起，需要一个进程级钩子，但系统**不保证**你的进程常驻——所以 Application 是初始化点，不是永生状态机。

---

## 3. Context

**题目：** `Context` 做什么？何时 application / activity context？如何泄漏？

### 参考回答

`Context` 是访问 Android 环境的入口：资源、主题、系统服务、启动组件、权限相关能力等。它不是“什么都能塞”的万能依赖，也不应传入纯 domain 模型。

- **Application context**：生命周期接近进程，适合长寿命对象拿系统服务、建库、非 UI 的 manager。
- **Activity context**：带窗口 / 主题 / 当前 UI 环境，适合 Dialog、部分需要 Activity 的权限/UI API。

泄漏典型路径：singleton / 静态字段 / 长生命周期 coroutine / 未注销监听 持有已销毁的 `Activity` 或 `View`，导致整棵 UI 图无法被 GC。原则：**长寿对象只持 application context；UI 回调有取消点；View 不进 data 层。**

### Swift / iOS 对照

- 无严格同名类型；可联想“全局应用环境 vs 当前界面环境”。
- Swift 常见 retain cycle（闭包/delegate）；Android 常见“长寿引用钉住 Activity”，靠 GC 也收不回。

### 设计理念

Android 用 Context 把“系统能力”与组件绑定，避免全局静态乱取服务；代价是必须理解**哪一种 Context 的生命周期**。

---

## 4. Compose、状态与重组

**题目：** 声明式 UI？什么触发 recomposition？为何 state hoisting？

### 参考回答

声明式 UI：描述“给定状态页面长什么样”，而不是命令式改控件。某个 composable 读到的可观察 state 变化后，Compose 可重新执行受影响部分——**recomposition**。这不是整页必刷，也不是允许在函数体里随便发网络请求。

State hoisting：可复用子组件接收状态、向上抛事件（`isConnected` + `onConnectClick`），单一事实来源清晰，便于预览与测试。副作用放 ViewModel 或带明确 key 的 effect API，不放 composable 函数体。

**眼镜例子：** `ConnectionCard` 只知道状态文案与按钮，不知道 GATT。

### Swift / iOS 对照

- 接近 SwiftUI：`body` 随状态重算。
- **不是** API 一一替换：Compose 挂在 Activity/Lifecycle/ViewModel/Flow 上；SwiftUI 挂在 property wrapper / Observation / Scene 上。

### 设计理念

两边都把 UI 变成状态的函数，把“怎么画”交给框架；Android 额外要求你处理**宿主重建**与**进程恢复**。

---

## 5. ViewModel 与单向数据流

**题目：** 为何 Compose + 不可变 `UiState` + ViewModel？“连接”点击在哪处理？

### 参考回答

推荐 UDF：UI 渲染不可变 `UiState` → 用户事件进 ViewModel → 调 use case/repository → 新状态出来。页面“为何显示 Connecting”可追溯到明确状态，而不是散落的 mutable 标志。

`ViewModel` 持有**屏幕级**展示状态，通常跨配置变更保留；它不是数据库，不持有 `Activity`/`View`。点击：`viewModel.onConnectClicked()`，由 ViewModel 决定校验权限、发起连接或映射错误。

```kotlin
data class ConnectionUiState(
    val status: Status = Status.Disconnected,
    val batteryPercent: Int? = null,
    val errorMessage: String? = null,
)

class ConnectionViewModel(
    private val repository: GlassesConnectionRepository,
) : ViewModel() {
    val uiState: StateFlow<ConnectionUiState> = repository.state
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionUiState())

    fun onConnectClicked() {
        viewModelScope.launch { repository.connect() }
    }
}
```

### Swift / iOS 对照

- 类似 `@StateObject` / Observation 模型 + MVVM。
- Swift 常用 `@MainActor` 表达 UI 隔离；Android 用 `viewModelScope` + Main + lifecycle 协作。

### 设计理念

在“Activity 会被重建”的前提下，把状态上移到比 UI 实例活得更长的对象，同时**仍短于**进程与数据库——这是 ViewModel 的定位。

---

## 6. StateFlow、LiveData 与一次性事件

**题目：** 新 Compose 页何时选 StateFlow？导航/Toast 为何不能当持久 state？

### 参考回答

`StateFlow`：有当前值的 hot 状态容器，契合 coroutine/Compose，新 collector 立刻拿到最新 UI state。`LiveData` 在遗留代码常见，自带 lifecycle 感知；新代码通常统一 Flow 生态更顺。

可重复渲染的：连接状态、电量、错误文案。一次性的：导航、短暂 Toast、弹一次权限说明——若当普通 state 存着，旋转/重收集可能**重放**。需要明确“消费语义”（显式状态机转换或有规则的 event 通道），不要迷信神秘的 SingleLiveEvent 黑盒。

### Swift / iOS 对照

- 状态属性 ≈ `@Published` / Observation；alert/sheet 也应由可推理状态驱动，避免 `body` 多次触发副作用。

### 设计理念

**状态可重放，效应要设计消费规则**——声明式 UI 的共同纪律。

---

## 7. Coroutine、`suspend` 与结构化并发

**题目：** `suspend` 是什么？为何优先 `viewModelScope`？

### 参考回答

`suspend`：可在不阻塞调用线程的情况下挂起再继续；**不等于**自动切后台线程，也**不等于**启动任务。必须在 coroutine 或其它 suspend 中调用。

结构化并发：任务有父子 Job；父取消则子取消；错误边界更清晰。`viewModelScope` 在 ViewModel 清除时取消子任务，避免连接请求变成孤儿。`GlobalScope` / 无主 scope 是面试减分项。宽 `catch (Exception)` 必须先 rethrow `CancellationException`。

### Swift / iOS 对照

| Swift | Kotlin | 关键差 |
|---|---|---|
| `async` 函数 | `suspend fun` | Kotlin 在 suspend 上下文**直接调用**，不写 `await` |
| `await` 调用点 | 仅 `Deferred.await()` 等 | 语法直觉不同 |
| `Task { }` | `launch` | 都要 owner |
| task group | `coroutineScope` | 结构化等待 |
| actor / isolation | 主要靠库 + 规范 | Swift 编译器参与更深 |

### 设计理念

Kotlin 把并发能力主要放在 **库（kotlinx.coroutines）** 以服务 JVM/多平台；Swift 把更多并发安全放进**语言与编译器**。迁移时最大坑是：把 `suspend` 当成 GCD 后台队列。

---

## 8. Dispatcher 与主线程安全

**题目：** 解析 BLE、图像变换、数据库如何不阻塞主线程又安全更新 UI？

### 参考回答

- 阻塞 I/O → 常 `Dispatchers.IO`（或库已处理则不再包）。
- CPU 密集 → `Dispatchers.Default` 或有界执行器。
- UI 状态更新在 UI 所属链路（ViewModel 的 Main 安全约定 + Compose）。

不要机械 `withContext(IO)` 包所有 suspend。相机分析要有背压（只保留最新帧）。结果映射成小的 `UiState`，不要把大 bitmap 塞进 state。

### Swift / iOS 对照

- 类似非 MainActor 工作 + 回主线程/`@MainActor` 更新 UI。
- Swift 用 actor/`Sendable` 更积极查 data race；Kotlin 更靠纪律 + 不可变数据 + 正确 scope。

### 设计理念

移动 UI 线程必须响应输入；Android 用 ANR 把“主线程太久”上升为**系统级失败模式**。

---

## 9. Flow 与生命周期感知收集

**题目：** Flow 是什么？为何 lifecycle-aware 收集？

### 参考回答

`Flow` 是异步数据流。普通 `flow {}` 多为 **cold**（每个 collector 启动自己的生产）；`StateFlow` 为 **hot** 且有当前值。冷流误用会导致重复扫描 BLE。

UI 用 `repeatOnLifecycle` / `collectAsStateWithLifecycle`：页面不可见时停 UI 收集，回来再收最新状态。注意：产品若要求后台维持连接，应由 repository/合规服务拥有，**不能**靠隐藏页面 collector 偷活。

### Swift / iOS 对照

- 像 `AsyncSequence` / Combine，但 cold/hot 与生命周期 API 不同。
- SwiftUI `.task` 也要考虑取消。

### 设计理念

流式数据 + 组件可见性 = 必须把**收集生命周期**和**业务资源生命周期**分开建模。

---

## 10. 配置变更、保存状态与进程死亡

**题目：** `remember` / `rememberSaveable` / `ViewModel` / `SavedStateHandle` / 持久化各存什么？

### 参考回答

| 机制 | 适合 | 不适合 |
|---|---|---|
| `remember` | 当前 composition 内轻量 UI 记忆 | 跨进程、大对象、连接 |
| `rememberSaveable` | 小、可序列化 UI 值（tab、输入） | 密钥、GATT、大列表 |
| `ViewModel` | 屏幕状态，跨配置变更 | 当数据库；进程死亡后不可靠 |
| `SavedStateHandle` | 少量可 Bundle 的恢复数据（deviceId） | 大缓存、连接对象 |
| DataStore/Room | 偏好、离线业务数据 | 假装“仍已连接” |

恢复后必须重查：权限、蓝牙开关、真实连接。不要持久化 `isConnected = true` 就当真。

### Swift / iOS 对照

- 粗映射：`@State`、`@SceneStorage`、屏幕模型、`UserDefaults`/DB。
- iOS 也会被系统终止；好习惯都是**可恢复输入 + 真实源重建运行态**。

### 设计理念

Android 把“状态能活多久”拆成多层，逼你显式选择——这是开放设备内存压力下的工程答案。

---

## 11. Intent、Service、BroadcastReceiver `[经典]`

**题目：** 三者角色？为何长 BLE 不能放 receiver？

### 参考回答

- **Intent**：请求执行动作或携带目标的消息（显式组件或隐式匹配）。
- **Service**：无 UI 的组件，承载某类工作；**不是线程**，回调默认主线程，耗时仍要协程/线程池。
- **BroadcastReceiver**：短时响应广播。

`onReceive` 应尽快结束：可更新状态或调度合规工作，不可做长扫描/重网络。后台限制仍在，组件类型 ≠ 永久运行许可。

### Swift / iOS 对照

- 无完整对等物。Intent 有点像 URL/deep link + 系统 action；Receiver 不完全是 `NotificationCenter`；后台要对照 background modes / `BGTaskScheduler` 重新学。

### 设计理念

Android 是**组件化、可跨 app 协作**的系统：用清单契约注册能力，用 Intent 路由。扩展性强，边界成本高（导出、权限、输入校验）。

---

## 12. 权限与隐私

**题目：** Manifest、运行时授权、用户撤销如何配合（相机/麦/蓝牙）？

### 参考回答

Manifest 声明可能用到的权限；危险权限在用户触发相关功能时运行时请求；**每次**敏感操作前再检查（设置里可撤）。无权限要可解释降级，不要静默失败。

蓝牙权限随 Android 版本与 `targetSdk` 变化（新系统常见 `BLUETOOTH_SCAN`/`CONNECT` 等；旧规则可能涉及位置）。面试应说“查矩阵 + 真机测”，而不是背死一张表。

### Swift / iOS 对照

- iOS：usage description + 授权状态；Android 还多了 manifest 合并、组件导出、targetSdk 行为分支。
- 共同原则：最小权限、可解释时机、撤销后立即尊重。

### 设计理念

权限是用户与系统对 app 能力的**持续合约**，不是 onboarding 一次勾选。

---

## 13. 前台服务、WorkManager、后台限制

**题目：** 同步/录制何时 FGS、何时 WorkManager？为何常驻 service 差？

### 参考回答

- **前台服务**：用户可感知、进行中、时间敏感（明确开始的录制/导航）；需可见通知，并符合 service type/权限/启动限制。
- **WorkManager**：可延后、要约束与重试的可靠任务（充电+网络后上传日志）；不保证精确立刻执行。

常驻 service 耗电、发热、触碰策略，并掩盖需求不清。优先事件驱动、超时、做完即停。

### Swift / iOS 对照

- 类比 foreground + 特定 background modes + `BGTaskScheduler`；允许场景与用户可见性不同。
- 共同点：OS 保留调度权。

### 设计理念

电池与多任务是公共资源；Android 用可见性与配额把“后台”从默认能力变成**特权**。

---

# 第二部分：经典 Android 面试题（系统与工程）

## 14. ANR `[经典]`

**题目：** 什么是 ANR？常见原因？眼镜 companion 如何预防与排查？

### 参考回答

ANR（Application Not Responding）：主线程过久无法处理输入/特定生命周期工作，系统弹出无响应。常见原因：主线程同步 IO、重计算、锁等待、在主线程做 BLE/网络、错误地 `Thread.sleep`、同步 Binder 过慢等。

预防：重活离开主线程；启动路径轻量；严格区分 UI 与 repository 工作；用超时与取消。排查：复现路径、ANR traces、StrictMode（调试）、Profiler/Perfetto、Logcat 时间线。眼镜场景：**永远不要在主线程同步等待 GATT 回调**。

### Swift / iOS 对照

- iOS 主线程卡顿也会掉帧/看门狗杀进程，但“ANR 对话框”是 Android 用户更熟悉的形态。
- 两边纪律相同：主线程只做轻量 UI。

### 设计理念

把“卡死”从体验问题升级为**可被系统判定的故障**，倒逼架构分层。

---

## 15. 主线程、Handler、Looper、MessageQueue `[经典]`

**题目：** 主线程如何处理消息？现代 Kotlin 何时还用 Handler？

### 参考回答

主线程有 **Looper** 循环读取 **MessageQueue**；**Handler** 向该队列投递 Message/Runnable。这是 Android 早期并发与 UI 更新的基础设施，也是“必须在主线程碰 View”的底层原因之一。

现代 Kotlin：优先 coroutine。仍可能见到 Handler：Java 回调线程切主线程、与 Choreographer/帧相关、遗留 API。注意非静态内部 Handler 持有外部 Activity 的泄漏。不要用 Handler 重写一套生命周期混乱的“自研框架”。

### Swift / iOS 对照

- 类似 run loop + 主队列派发；GCD/`MainActor` 是更高层习惯。
- iOS 开发者很少直接碰 CFRunLoop 细节；Android 面试却常问 Handler 机制——知其结构即可。

### 设计理念

单线程 UI + 消息泵是经典 GUI 模型；Android 将其标准化为 Looper 体系，再在其上长出 Lifecycle/Compose。

---

## 16. 进程优先级与“为何被杀” `[经典]`

**题目：** 用户喜欢你的 app，系统为何仍可杀进程？前台/服务/缓存进程大致如何？

### 参考回答

Android 为多 app 共享有限内存，由 LMK 等策略回收。大致：前台可见进程优先级高；有合规前台服务的进程较高；纯后台/空缓存进程最先被杀。具体数值会变，面试重点是：

**不要假设进程永生；设计可恢复。** 连接状态、扫描、重试都要能在冷启动后从磁盘 + 设备真实状态重建。

### Swift / iOS 对照

- iOS jetsam 也会杀后台；Android 的进程/组件优先级叙事更常出现在面试。
- 产品结论相同：关键状态可恢复。

### 设计理念

开放多任务系统的核心契约：**系统拥有最终资源调度权，app 拥有恢复责任。**

---

## 17. 内存泄漏常见模式 `[经典]`

**题目：** 常见泄漏？如何查？与 Swift ARC 循环有何不同？

### 参考回答

常见：singleton 持 Activity Context；静态 View；非静态内部类/匿名回调；未注销 Listener/Receiver；协程/线程持有 UI；WebView 等重对象用法不当。

工具：LeakCanary、Profiler、mat/hprof（进阶）、代码审查生命周期。修复：application context、弱引用慎用（更优先理清 owner）、取消注册、`viewModelScope`、不把 View 传入长寿层。

与 Swift：ARC 用 `weak/unowned` 打断环；Android ART **GC** 能收无引用图，但**仍被长寿对象引用的 Activity 不会收**——不是“有 GC 就不会漏”。

### Swift / iOS 对照

| | Swift | Android |
|---|---|---|
| 机制 | ARC 引用计数 | GC |
| 典型坑 | 闭包/delegate 环 | Context/监听长寿引用 |
| 修复直觉 | weak self | 缩短引用寿命 + 取消 |

### 设计理念

内存安全 ≠ 内存正确。平台不同，**所有权建模**都是架构核心。

---

## 18. 冷/温/热启动 `[经典]`

**题目：** 定义？眼镜首屏连接页你量什么？

### 参考回答

- **冷启动**：进程不在，从创建进程 + Application + 首 Activity 开始。
- **温启动**：进程在，Activity 被销毁后重建等（说法随文档微调，能区分“是否新建进程”即可）。
- **热启动**：进程与 Activity 多在，从后台回前台。

眼镜 companion 可量：点图标到首帧、到可点“扫描/连接”、到展示上次设备。手段：启动追踪、避免 Application 重初始化、懒加载 BLE 栈、Baseline Profile 等（认识即可）。**主线程同步初始化是大敌。**

### Swift / iOS 对照

- 类似 cold/warm launch 讨论；Instruments 时间分析。
- Android 多了 Application/多组件入口导致的初始化分叉。

---

## 19. Task、返回栈、启动模式（认知） `[经典]`

**题目：** task/back stack？`singleTop`/`singleTask`？Compose 还要吗？

### 参考回答

Task 与 back stack 是系统管理 Activity 导航历史的模型。启动模式影响“是否复用实例、是否清栈”。现代单 Activity + Compose Navigation 可减少多 Activity 复杂度，但 deep link、多入口、与其它 app 协作时仍会碰到 Intent flags 与 task 行为。

新项目默认：单 Activity；需要时再学 launchMode。面试能说清“系统仍按 Activity 任务栈管理，Compose 导航是 app 内层”即可。

### Swift / iOS 对照

- 类似 UINavigationController 栈 / 多 window scene，但模型不同。
- 不要把 launchMode 翻译成 `UINavigationController` 动画。

---

## 20. Fragment（遗留认知） `[经典]`

**题目：** Fragment 解决过什么？现代 Compose 为何仍会遇到？

### 参考回答

Fragment 曾用于 Activity 内模块化 UI、生命周期片段、平板多窗格、ViewPager 等。现代 greenfield 可用 Compose + Navigation 替代大量 Fragment UI。但仍会在遗留代码、部分 Navigation 集成、SDK 示例中出现。

转职策略：**会读、会改边界，不主动用 Fragment 重写新功能。**

### Swift / iOS 对照

- 有点像 child view controller 容器，但生命周期规则更绕。
- SwiftUI 也有 UIKit 宿主遗留问题——两边都是“新 UI 框架 + 旧容器共存”。

---

## 21. RecyclerView vs LazyColumn `[经典]`

**题目：** RecyclerView 做什么？LazyColumn 如何比？何时仍要 View？

### 参考回答

RecyclerView：View 体系下高效列表，回收复用 item 视图；常配合 Adapter、LayoutManager，DiffUtil/ListAdapter 做差分更新（认识即可）。

LazyColumn：Compose 惰性列表，按可见性组合 item，用稳定 key 保身份。新 UI 优先 LazyColumn。仍要 View 的情况：复杂 Surface/TextureView 相机预览、成熟自定义 View、渐进迁移时用 `AndroidView` 互操作。

### Swift / iOS 对照

- `UITableView`/`UICollectionView` vs SwiftUI `List`/`LazyVStack`。
- 都关心复用/身份/差分；API 形态不同。

---

## 22. SharedPreferences vs DataStore vs Room `[经典]`

**题目：** 各用何时？为何新代码常弃 SharedPreferences？

### 参考回答

- **SharedPreferences**：老式键值；API 易误用（主线程、apply/commit 语义、类型弱）。
- **DataStore**：现代偏好/小型 typed 数据，异步、更一致。
- **Room**：SQLite 上的结构化数据、查询、关系。

实时 BLE 连接状态**不**以三者为唯一真相。密钥不要明文塞偏好。

### Swift / iOS 对照

- DataStore ≈ 更现代的 `UserDefaults` 角色；Room ≈ Core Data/SwiftData/SQLite 角色。

---

## 23. Serializable、Parcelable、JSON `[经典]`

**题目：** Parcelable 做什么？JSON 何时用？为何不要把大对象塞 Bundle？

### 参考回答

`Parcelable` 用于 Android **IPC / Intent extras / 部分进程内传递** 的高效打包（相对 Java Serializable 更安卓化）。业务网络层用 JSON + kotlinx.serialization/Moshi 等。

`Bundle`/`SavedStateHandle` 有大小与类型限制；大列表、bitmap、连接对象不应塞进去。应存 **id**，再从 repository 加载。

### Swift / iOS 对照

- 类似 `NSCoding`/`Codable` 分场景；Intent extras 更像有约束的跨组件信封，不是通用归档。

---

## 24. Gradle、Manifest、构建变体 `[经典]`

**题目：** 各管什么？硬件产品为何要 flavor？

### 参考回答

- **Gradle**：模块、依赖、SDK/构建选项、任务、签名输入、variant。
- **Manifest**：组件、权限、metadata、部分能力声明；多模块会 **manifest merge**。

product flavor：开发固件 endpoint、日志级别、applicationId 后缀、feature flag 与正式包隔离。密钥走 CI/secret，不进仓库。

### Swift / iOS 对照

- Xcode configurations/schemes/targets + `Info.plist`/entitlements。
- Manifest 还承担组件发现与跨 app 契约，不只是“属性列表”。

---

## 25. 调试工具箱 `[经典]`

**题目：** 重连 bug 怎么用 Logcat、断点、Inspector、Profiler、`adb`？

### 参考回答

先复现与时间线：权限、蓝牙开关、状态机迁移、重试原因。Logcat 过滤进程/标签；断点看线程；Compose/Layout Inspector 看 UI 树；Profiler 看 CPU/内存；深时序用 Perfetto。`adb` 装包、权限、模拟配置变更。

没有状态机日志的“一直重连”只能靠猜——先补可观测性。

### Swift / iOS 对照

- Xcode console / Instruments / os_log / LLDB；Android 多了 adb 与设备碎片矩阵。

---

# 第三部分：智能眼镜场景

## 26. BLE 连接设计

**题目：** 手机到眼镜可靠 BLE：状态、失败、职责边界？

### 参考回答

常见 companion：手机 central/GATT client，眼镜 peripheral（以硬件为准）。用状态机，例如：

`Idle → Scanning → Connecting → DiscoveringServices → Ready`，以及 `Reconnecting` / `Failed` / `Disconnected`，并带原因（权限、BT 关、超时、超距、认证失败）。

Repository/BLE manager：扫描、GATT 回调、**操作串行队列**、重连退避、资源 close。ViewModel：投影 UI。UI：只订 `StateFlow`。`callbackFlow` 必须在 `awaitClose` 释放。模拟器不能替代真机。

**取舍：** 低延迟控制可能要保连；24h 全功率扫描不可接受——按任务与佩戴状态调 duty cycle。

### Swift / iOS 对照

- CoreBluetooth 同样要状态机与串行化；权限/后台/厂商差异在 Android 更吵。

---

## 27. 相机：CameraX 与 Camera2

**题目：** 何时 X 足够？如何保 preview+分析流畅？

### 参考回答

CameraX：高层、生命周期友好，preview/拍照/录像/分析优先。Camera2：底层 session/request，特殊硬件/手动控制/多 surface 才上。

分析路径背压：只留最新帧；`ImageProxy` 及时 `close()`；少拷贝。打开/绑定/释放跟 owner 生命周期走。

### Swift / iOS 对照

- AVFoundation session + output；问题本质是帧率/缓冲/权限/争用。

---

## 28. Repository、use case、DI

**题目：** 如何分工？DI 带来什么？

### 参考回答

UI 渲染与发事件；ViewModel 屏幕协调；Repository 藏数据源。Use case 仅在有真实策略（重试上限、多仓库协调、权限策略）时引入，避免空转发层。

DI（如 Hilt）：显式生命周期与实现替换，便于 fake 测试。DI 不自动产生好架构。

### Swift / iOS 对照

- protocol + 构造注入 / 组合根；意图相同。

---

## 29. 本地持久化（companion）

**题目：** DataStore vs Room？进程死后什么不可信？

### 参考回答

DataStore：偏好设备 ID、单位、开关。Room：会话历史、诊断、离线队列。  
**不可信：** 内存里的“已连接”、过期 GATT、仅 UI 保存的布尔。恢复后重验蓝牙与协议。

### Swift / iOS 对照

- 同 22；密钥与媒体要单独策略。

---

## 30. 功耗、热、响应

**题目：** 如何降耗散热又不觉得慢？

### 参考回答

先定预算：端到端延迟、平均功耗、温升。事件驱动代替狂轮询；扫描 duty cycle；离开功能即停相机/麦/识别；最低可用采样率/分辨率。禁无限重试、无界队列、泄漏 wake lock、无意义 recomposition。用真机指标验证，不靠感觉砍帧。

### Swift / iOS 对照

- Instruments Energy；Android 硬件矩阵更大，更需降级与遥测。

---

## 31. 重连风暴排查

**题目：** “一直重连又耗电”怎么定位并证明修好？

### 参考回答

可复现步骤 + 结构化日志（状态迁移、原因、backoff）。Profiler/电量相关证据。单测状态机与退避；真机/instrumented 覆盖权限与生命周期；上线指标：失败率、重试次数、崩溃；日志禁带用户音视频。

### Swift / iOS 对照

- 同样：假设 → 证据 → 修复 → 回归；工具链不同。

---

## 32. 隐私与安全

**题目：** 如何保护相机/麦/位置/第一人称数据？

### 参考回答

最小权限；录制/上传可感知可关；传输保护；**Keystore 管密钥**（不是随便存 token 的大筐）；敏感数据加密存储；日志与导出组件最小暴露；保留与删除策略产品化。

### Swift / iOS 对照

- Keystore ≠ Keychain 简单等同：Keystore 偏密钥；Keychain 可存凭据；Secure Enclave 偏硬件密钥。原则相通：最小化、同意、可删、加密。

---

## 33. 综合题：发现并连接页

**题目：** Compose + ViewModel + coroutine + StateFlow 设计；旋转、BT 关、拒权、进程重建？

### 参考回答

```text
Compose screen
  ├─ renders: UiState(permissions, bluetooth, scan, connection, error)
  └─ emits: Scan / DeviceClick / PermissionResult / Retry
              │
              ▼
ViewModel ── policy, cancel, map errors
              │
              ▼
Repository ── BLE callbacks + queue + retry
              │
              ▼
DataStore/Room ── durable preferred device id
```

- `collectAsStateWithLifecycle` 收集；
- 显式状态：无权限 / BT 关 / 扫描中 / 连接中 / 已连接 / 可重试错误；
- 连接幂等：重复点击不建多会话；
- 旋转：ViewModel 常在，重收集当前 state；
- 进程重建：读 preferred id → 重查权限与真实连接 → 必要时用户确认重连；
- 后台长连另设计合规 owner。

### Swift / iOS 对照

- 像 SwiftUI + VM + CoreBluetooth 状态发布；迁移关键是接受 Android 的恢复与权限模型，而不是改类名。

---

# 第四部分：设计理念深潜（Android × iOS × Kotlin × Swift）

> 先把比较轴放对，避免“Android 和 Swift 的理念”这种层级错乱。

| 比较轴 | 比什么 | 不能推出什么 |
|---|---|---|
| Android ↔ iOS | 生命周期、权限、后台、硬件生态、组件契约、分发 | 不能归因于 Kotlin/Swift 语言 |
| Kotlin ↔ Swift | 空安全、内存、并发、类型、互操作 | 换语言不会自动获得对方平台生命周期 |
| Compose ↔ SwiftUI | 声明式、状态驱动、组合 | 宿主与生态不同 |

---

## 34. Android 设计理念 vs iOS

### 参考回答（可直接口述）

Android 面向**开放设备、多厂商、多 app 抢资源**的现实。系统必须能回收内存、限制后台、按组件启动应用。因此架构默认假设：

1. **进程与组件可死**：UI 与内存状态随时可能没；
2. **能力靠契约获得**：Manifest、权限、Intent、Service 类型；
3. **恢复优于永生**：repository + 持久化 + 重连策略；
4. **能力探测与降级**：API level、feature、真机差异是功能一部分。

iOS 同样会挂起/终止，并强调隐私与后台边界，但软硬件与分发更受控，开发者更常在较稳定的设备矩阵上优化。iOS 的整合感强；Android 的组件化与跨 app 协作更居中心。

**对 iOS 工程师的心智切换：**

| 旧直觉 | 新直觉 |
|---|---|
| 这个 VC/Screen 会一直在 | 这个 UI 实例可能重建，进程可能没 |
| 先把功能跑通再补恢复 | 恢复路径是功能的一部分 |
| 后台模式申请后大致能做 | 后台是配额与可见性约束下的特权 |
| 一台机测差不多 | 版本 × 厂商 × 芯片矩阵 |

### 眼镜场景放大点

持续 BLE + 相机 + 麦 + 传感器把电量、热、隐私变成**架构约束**：状态机、duty cycle、前台可见性、数据最小化必须写进设计，而不是发布前优化。

---

## 35. Kotlin 设计理念 vs Swift

### 参考回答

**Kotlin**：从 JVM/Java/Android 务实现代化出发——互操作第一、空安全、少样板、data/sealed/extension、协程以库交付、GC 管内存。目标是让现有生态低风险变现代。

**Swift**：从安全、表达力、接近原生性能的系统语言出发——值语义优先、ARC、Optional、协议导向、`async/await`+actor 与编译期竞争检查、ABI 稳定压力大。

### 代码级陷阱（双向）

| 方向 | 陷阱 |
|---|---|
| Swift → Kotlin | 把 `data class` 当 `struct` 值语义；忽视 `copy` 浅拷贝；以为 `suspend` 会换线程；忽视 Java platform type |
| Kotlin → Swift | 忽视值拷贝带来的开销/语义；低估 `Sendable`/actor 隔离；把 class 默认思维带进以 struct 为主的模型 |

### 一句话

- Kotlin：**工程务实 + 生态互操作**，风险多用架构与边界收敛。  
- Swift：**类型与编译器前置安全**，风险更多在编译期暴露。  
两者无高下，只是**安全落点不同**。

---

## 36. Compose vs SwiftUI 运行时语义

### 相同

- UI ≈ 状态的函数；
- 小组件组合、状态提升；
- 渲染路径不做无控制副作用；
- 单向数据流是健康默认。

### 不同

| | Compose | SwiftUI |
|---|---|---|
| 宿主 | Activity/Fragment + Android 资源 | Scene / ViewController 生态 |
| 屏幕状态常见 | ViewModel + StateFlow | Observable / @StateObject 等 |
| 生命周期 | Lifecycle 显式收集 | `.task` / scene phase 等 |
| 并发 | 协程库 | async/await + MainActor |
| 重建压力 | 配置变更 + 进程死亡更日常 | 也有，但叙事不同 |

### 口述收束

> 我会复用 SwiftUI 的声明式与 UDF 经验，但重新学习 Android 的收集、权限、进程恢复与组件边界；不会做 API 名一一翻译。

---

---

# 第五部分：Kotlin 语言深潜与经典补充

> 37–43 是 Kotlin / 协程语言层高频面试点（中厂与 FAANG 风格常考），也是 iOS 工程师最容易“用 Swift 直觉误套”的地方；44–45 是经典 Android 运行时话题。建议在过完第一~三部分后专项刷。

## 37. data class 的相等性：equals / hashCode / copy `[经典]` `[Kotlin]`

**题目：** data class 自动生成了什么？两个实例何时相等？是深比较吗？对照 Swift。

### 参考回答

`data class` 基于**主构造器属性**自动生成 `equals()`、`hashCode()`、`toString()`、`componentN()`（解构）和 `copy()`。相等判定是：**主构造器里所有属性各自 `equals` 都为真**。要点与坑：

- **只有主构造器属性参与** `equals`/`hashCode`/`toString`；写在类体内的属性不参与——这是高频坑。
- `equals` 递归调用各属性自己的 `equals`。属性是 `List` 有结构相等；但属性是 `Array` 时 `equals` 是**引用比较**，要手写或改用 `List`。
- `copy()` 是**浅拷贝**：只复制第一层引用，内部可变对象仍共享。
- 作为 `HashMap` / `Set` 的 key 时，属性必须不可变，否则修改后 `hashCode` 变化会破坏哈希契约、导致查不到。
- `==` 调用 `equals`（结构相等），`===` 比较引用是否同一对象。

### Swift / iOS 对照

- Swift `struct` 若成员都 `Equatable`/`Hashable`，声明遵循协议后编译器可**合成**，概念相近；同样只包含存储属性。
- 关键差：Swift `struct` 的相等常伴随**值语义拷贝**（赋值即独立副本）；Kotlin `data class` 是**引用类型**，`copy()` 是显式浅拷贝。Swift 值类型没有 `===`（只有 class 有）。
- 记忆点：**相等 ≠ 独立副本**。Kotlin 里两个 `equals` 为真的对象可能共享同一个内部可变列表。

### 设计理念

Kotlin 在 JVM 引用类型上，用编译器合成把“数据载体”的样板消掉，但**不改变引用语义**；相等性是一份显式的值比较契约。Swift 则把值语义直接烙进类型系统。iOS 工程师迁移时必须把“`data class` 的相等”和“`struct` 的值拷贝”解耦理解。

---

## 38. inline / reified / crossinline / noinline `[经典]` `[Kotlin]`

**题目：** inline 函数解决什么？reified 为何必须配 inline？crossinline / noinline 何时用？Swift 有对应吗？

### 参考回答

- **inline**：把高阶函数体与其 lambda 在**调用处展开**，消除“为每个 lambda 创建 Function 对象 + 虚调用”的开销，也使 lambda 里的 `return` 能**非局部返回**到外层函数。适合小而热的高阶函数（集合操作、`synchronized`、`measureTime` 包装）。对大函数 inline 会导致字节码膨胀。
- **reified**：JVM 泛型**类型擦除**，运行时拿不到 `T`。inline 函数因为在编译期展开、`T` 被替换为具体类型，才能 `reified T`，从而 `T::class`、`is T`、`as T`。典型：`inline fun <reified T> Gson.fromJson(json): T`。**非 inline 函数无法 reified。**
- **crossinline**：lambda 仍被内联，但**禁止非局部 return**（因为它会在另一个执行上下文里被调用，比如塞进一个 Runnable）。
- **noinline**：让某个 lambda 参数**不**内联（比如要把它当对象存起来、传给别处）。

### Swift / iOS 对照

- Swift 有 `@inline(__always)`，但很少手动用，优化器自动内联；**Swift 泛型不擦除**（走单态化/见证表），运行时保留类型信息，可直接 `T.self`、`is T`、`as? T`。
- 所以 **Swift 根本不需要 `reified`**——这是最大差异：正是 JVM 擦除逼出了 `inline + reified` 这套机制。
- Swift 没有 `crossinline`/`noinline` 关键字，因为内联模型与非局部返回规则不同。

### 设计理念

Kotlin 运行在 JVM，受 Java 泛型擦除约束，`inline + reified` 是“在擦除世界里找回类型信息”的**务实补丁**，顺带优化高阶函数性能。Swift 从语言层保留类型信息，无此负担。迁移时别去找“reified 的 Swift 对应”，而要理解它是 **JVM 约束的产物**。

---

## 39. 属性委托：by lazy / lateinit / Delegates `[经典]` `[Kotlin]`

**题目：** `by lazy`、`lateinit`、`by Delegates.observable`、框架委托（`by viewModels()` / `by remember`）各解决什么？与 Swift property wrapper 的关系？

### 参考回答

属性委托 `val x by delegate` 把 get/set 委托给另一个对象的 `getValue`/`setValue`（operator 约定）。常见：

- **`by lazy { }`**：首次访问时计算并缓存，默认 `SYNCHRONIZED` 线程安全。用于昂贵、只读、一次性初始化——别把可失败 I/O 藏进去。
- **`lateinit var`**：不是委托，是延迟初始化的**非空 var**，用于 DI / 框架注入 / setUp 中赋值；提前读抛 `UninitializedPropertyAccessException`。不能用于原生类型、不能是 `val`、不能可空。
- **`by Delegates.observable / vetoable`**：赋值时回调，做通知或校验。
- **框架委托**：`by viewModels()`、`by activityViewModels()`、Compose 的 `by remember { mutableStateOf(...) }`、`by savedStateHandle`。

### Swift / iOS 对照

- Swift 的 property wrapper（`@State`、`@Published`、`@AppStorage`）与 `lazy var` 意图高度相似：把属性访问逻辑抽出复用。`lazy var` ≈ `by lazy`（但 Swift `lazy` 非线程安全）。
- Compose 的 `by remember { mutableStateOf() }` ≈ SwiftUI `@State` 的角色。
- 差异：Kotlin 委托是**通用 operator 约定**，任何对象都能当委托；Swift property wrapper 是**专门语言特性**，用 `wrappedValue` / 投影值 `$`。

### 设计理念

两种语言都识别到“属性行为可复用”。Kotlin 用**最小语言特性（operator 约定）+ 标准库委托**实现，强调正交与库化；Swift 用**一等的 property wrapper + 投影值**，与其声明式、编译期取向一致。

---

## 40. sealed class vs enum vs abstract `[经典]` `[Kotlin]`

**题目：** 三者何时用？sealed 相比 enum 强在哪？对照 Swift enum。

### 参考回答

- **enum class**：固定一组**单例常量**，可带属性/方法，但每个常量是**同一套字段**，不能各带不同结构数据。适合状态标签、模式。
- **sealed class / sealed interface**：编译期已知的**有限子类型集合**，每个子类型可以是 `data class` 携带各自不同的数据、可有多个实例。配合 `when` 可**穷尽检查**，作为表达式漏分支编译报错。适合结果/状态建模（`ConnectDeviceResult`、`UiState`）。
- **abstract class**：开放继承，子类型不封闭，编译器无法穷尽。适合可扩展层次。

### Swift / iOS 对照

- **最重要的对应**：Swift 的 `enum` with associated values **同时具备** Kotlin `enum`（简单 case）和 sealed class（带数据变体）两种能力。所以：
  - **Swift enum（带 associated values）≈ Kotlin sealed 层次**；
  - **Kotlin `enum` ≈ Swift 无 associated value 的简单 enum**。
- Swift enum 是值类型、更轻；Kotlin sealed 子类是普通类（可 `data class`）。

### 设计理念

Swift 把“代数数据类型（sum type）”做进了统一的 `enum` 特性；Kotlin 受 JVM 类模型影响，用 **sealed + data class 组合**达到同样表达力，更啰嗦但贴合类层次。两者共同目标：**状态空间可穷尽，编译器帮你检查漏分支**。

---

## 41. 泛型：型变 in / out、类型擦除、reified `[经典]` `[Kotlin]`

**题目：** `out` / `in` 是什么？类型擦除有何影响、如何绕过？对照 Swift 泛型。

### 参考回答

- **声明处型变**：`out T`（协变，生产者，只出不入，如 `List<out E>`）、`in T`（逆变，消费者，只入不出，如 `Comparable<in T>`）。**使用处型变**：`List<out Number>`（类似 Java `? extends`）。目的：在类型安全下允许 `List<Cat>` 当 `List<Animal>` 用。
- **类型擦除**：泛型参数运行时被擦除，`List<String>` 与 `List<Int>` 运行时同类型，不能 `is List<String>`。要保留类型需 `inline + reified` 或传 `Class`/`KClass` token。
- **星投影 `List<*>`**：不知道具体类型参数时的安全上界读取。

### Swift / iOS 对照

- Swift 泛型**不擦除**（单态化/见证表），运行时保留类型，可 `T.self`、`is`、`as?`；因此**无需 reified**。
- Swift 没有 Kotlin/Java 那样的声明处 `in`/`out` 关键字；集合协变通过具体类型与 `some` / `any`（不透明/存在类型）、协议关联类型处理，**模型不同**。
- 迁移坑：看到 `out`/`in` 别硬套 Swift；它是 JVM 泛型协变/逆变的显式标注。

### 设计理念

Kotlin 在 Java 擦除泛型之上，用**声明处型变**把 Java 里啰嗦易错的通配符（`? extends` / `? super`）前移到定义处，更安全清晰；擦除带来的运行时类型缺失用 `reified` 补。Swift 从语言层保留类型信息，走的是完全不同的路线。

---

## 42. 协程异常处理：SupervisorJob、异常处理器、async 时机 `[经典]` `[Kotlin]`

**题目：** `coroutineScope` 里子任务失败会怎样？`SupervisorJob` 有何不同？`async` 与 `launch` 的异常何时抛？`CancellationException` 特殊在哪？

### 参考回答

- 普通 `coroutineScope` / `Job`：任一子任务抛出未捕获异常 → **取消所有兄弟并向上传播**，取消父。
- `supervisorScope` / `SupervisorJob`：子任务失败**不**自动取消兄弟，失败被隔离（如多个独立请求，一个失败其余继续）。
- **`launch`**：异常发生时**立即**向上传播。**`async`**：异常被封装进 `Deferred`，**在 `await()` 时才抛**；不 `await` 可能被吞。
- **`CoroutineExceptionHandler`**：只对 `launch` 顶层生效（不处理 `async` 的 `await` 结果），是最后的日志/崩溃兜底，**不是**常规业务错误处理。
- **`CancellationException` 是正常取消信号**：别在 `catch (e: Exception)` 里吞，必须 rethrow；`runCatching` 会捕获它，协程里慎用。

### Swift / iOS 对照

- Swift `async throws` + `try await` 在调用点显式处理；`TaskGroup` 子任务抛错的传播 ≈ `coroutineScope`；`Task` 取消协作式，`Task.checkCancellation()` / `CancellationError` ≈ `ensureActive()` / `CancellationException`。
- 差异：Swift 用 `throws` **编译期强制**每个 await 点处理错误；Kotlin 不强制 checked exception，靠 **sealed result + 纪律**。`async` 异常延迟到 `await` 抛的“时机坑”，Swift group 迭代取值时也有类似现象。

### 设计理念

Kotlin 把异常传播绑定到 **Job 树（结构化并发）**，用 supervisor 提供“隔离失败”的逃生口；错误处理策略交给库与开发者纪律。Swift 把并发错误处理与 `throws` **类型系统整合**，编译期约束更强。两者都强调：**取消是一等公民，必须协作传播**。

---

## 43. Flow 操作符与背压 `[经典]` `[Kotlin]`

**题目：** 常用操作符？`flatMapLatest` 何时用？`buffer` / `conflate` / `collectLatest` 如何处理生产快于消费（背压）？

### 参考回答

- **变换**：`map`、`filter`、`mapNotNull`、`transform`；**组合**：`combine`（各流最新值组合）、`zip`（配对）、`merge`。
- **展平**：`flatMapConcat`（顺序）、`flatMapMerge`（并发）、`flatMapLatest`（新值到来**取消上一个内层流**——适合“选了新设备 / 重新扫描时取消旧扫描或旧连接尝试”“搜索框输入”）。
- **背压**：默认 Flow 顺序挂起，收集慢会自然反压生产者；可用 `buffer(n)` 加缓冲并发化、`conflate()` 只保留最新丢弃中间值、`collectLatest {}` 每来新值取消上一个未完成的处理。眼镜场景：传感器/连接状态高频更新、UI 只关心最新 → `conflate` / `collectLatest`。
- **冷热与共享**：`stateIn` / `shareIn` 决定 scope、初值、`SharingStarted`（`Eagerly` / `Lazily` / `WhileSubscribed`）。

### Swift / iOS 对照

- Combine / AsyncSequence 有近似物：`flatMapLatest` ≈ Combine `switchToLatest`；`combine` ≈ `combineLatest`；`conflate`/`collectLatest` ≈ 只取最新策略。AsyncSequence 也是拉式、可取消。
- 差异：Kotlin **cold Flow 每个 collector 独立启动生产**；Combine 是**推式、可多播**（需 `share`/`multicast`），语义不同，别直接套。

### 设计理念

Kotlin Flow 以**挂起**为基础做“拉式、结构化、可取消”的异步流，**背压由挂起天然表达**，再用操作符按需放宽，与协程一脉相承。Combine 是响应式推式管道。理解“**谁驱动、谁反压**”比记操作符名更重要。

---

## 44. View 渲染流水线：measure / layout / draw `[经典]`

**题目：** 传统 View 如何测量、布局、绘制？`invalidate` 与 `requestLayout` 区别？Compose 还要懂吗？

### 参考回答

- 传统 View 三步：**measure**（据 `MeasureSpec` 定尺寸，`onMeasure`）→ **layout**（定位置，`onLayout`）→ **draw**（画到 Canvas，`onDraw`）。自定义 View 重写这三个。
- **`invalidate()`**：内容变、尺寸没变 → 只重绘（`onDraw`）。**`requestLayout()`**：尺寸/布局可能变 → 触发重新 measure + layout（**更贵**）。频繁误用 `requestLayout` 会掉帧。
- 过深布局层级会放大 measure/layout 成本（历史上 `RelativeLayout` 二次测量、`ConstraintLayout` 扁平化改善）。
- **Compose**：不直接暴露三段式，但底层同样有 measure/layout/draw 阶段（`Layout` / `Modifier`），并多了 composition/recomposition。懂传统流水线有助于读遗留代码、`AndroidView` 互操作、相机 `SurfaceView`/`TextureView`。

### Swift / iOS 对照

- UIKit 有 `layoutSubviews` / `setNeedsLayout` / `setNeedsDisplay` / `draw(_:)`，对应良好：**`setNeedsLayout` ≈ `requestLayout`，`setNeedsDisplay` ≈ `invalidate`**。
- Auto Layout 约束求解 ≈ `ConstraintLayout`；SwiftUI 像 Compose，隐藏了命令式布局。

### 设计理念

两大平台的命令式 UI 都**区分“重排（layout）”与“重绘（draw）”**以省成本；声明式框架（Compose/SwiftUI）把这套隐藏进框架，但性能问题仍源于同一物理：**布局越深、失效越频，越慢**。

---

## 45. Bitmap 与图像内存管理 `[经典]` `[眼镜相关]`

**题目：** 为何 Bitmap 易 OOM？如何管理？图片加载库做了什么？眼镜相机流如何避免内存爆炸？

### 参考回答

- 一张 Bitmap 内存 ≈ **宽 × 高 × 每像素字节**（`ARGB_8888` = 4B）。高分辨率相机帧极占内存；历史上（Android 8 前）像素在 Java 堆，易 OOM，现代放 native/ashmem，但仍需管理。
- 关键手段：按目标视图尺寸**降采样**（`inSampleSize` / 请求合适分辨率）；选合适**像素格式**（不需 alpha 用 `RGB_565` 省一半）；及时 `recycle` / 复用；不在内存里囤多帧。
- **图片加载库（Glide / Coil）**：负责内存+磁盘缓存、按 View 尺寸解码、生命周期感知取消、复用池。Coil 基于协程，更贴合 Kotlin。
- **眼镜相机分析流**：背压只留最新帧、`ImageProxy.close()` 及时释放、少做全分辨率拷贝、分析用低分辨率流、预览与分析分离。

### Swift / iOS 对照

- `UIImage` / `CGImage` / Core Graphics；大图同样要 downsample（`CGImageSourceCreateThumbnailAtIndex` / ImageIO）。SDWebImage / Kingfisher ≈ Glide / Coil。
- 差异：Android 设备内存与分辨率**碎片更大**、OOM 更常见，降采样与像素格式选择更关键；iOS 内存告警机制不同但原则一致。

### 设计理念

图像是移动端最大内存与功耗来源之一。两平台都要求“**按需分辨率、及时释放、库化缓存**”。Android 因硬件碎片化与更严的进程回收，把这变成必须**显式设计**的架构约束——对同时跑相机的眼镜尤甚。

---

## 给 iOS 工程师的迁移速记表

| iOS / Swift | Android / Kotlin | 关键提醒 |
|---|---|---|
| SwiftUI `View` | `@Composable` | 勿承载长期 I/O |
| 屏幕 Observable 模型 | `ViewModel` + `StateFlow` | 跨配置变更 ≠ 跨进程 |
| `async` / `Task` | `suspend` / `viewModelScope` | suspend 不换线程 |
| Combine / AsyncSequence | Flow / StateFlow | cold/hot + 收集生命周期 |
| CoreBluetooth | BLE GATT | 状态机 + 权限矩阵 |
| AVFoundation | CameraX / Camera2 | 先 X 后 2 |
| UserDefaults | DataStore | 非实时连接真相 |
| Core Data / SwiftData | Room | 结构化本地数据 |
| AppDelegate 初始化 | Application | 勿塞屏幕业务 |
| 主线程卡顿 | 卡顿 + **ANR** | 主线程纪律更“系统化” |
| ARC + weak | GC + 生命周期取消 | GC 不修 Context 泄漏 |
| Info.plist / entitlement | Manifest + 权限 | 组件契约更中心 |
| BGTask / background modes | FGS / WorkManager | 策略完全重学 |

---

## 一段适合面试的总收束

> 我会把 Compose 当状态渲染层，用 ViewModel 暴露可重复渲染的 `UiState`，用 coroutine/Flow 管理可取消异步工作。BLE、相机、持久化放在 repository 后，权限、蓝牙关闭、重连、进程恢复都是显式状态。眼镜场景优先量化延迟、功耗、热与隐私，而不是用常驻任务掩盖生命周期。作为 iOS 工程师，我复用 SwiftUI 与 async/await 的经验，但按 Android 的组件化、权限与可恢复进程模型重新设计边界，而不是做机械翻译。

---

## 建议练习日程

| 场次 | 题目 | 时长 |
|---|---|---:|
| 基础 60 分钟 | 1–6, 10, 14, 26 或 33 | 60m |
| 经典系统 45 分钟 | 14–18, 22–24 + 30 | 45m |
| Kotlin 深潜 45 分钟 | 37–43（每题配一条 Swift 对照） | 45m |
| 完整模拟 90 分钟 | 1–13 → 14/16/17 → 26+33 → 34 | 90m |
| 理念精读 | 34–36 + 速记表 | 40m |

与 10 天计划对齐：Day 8 后做 5/6/9/10；Day 9 后做 12/26；Day 10 做 14/33/34。语言层可穿插：阶段一后刷 **37/38/40/42**，Day 7 后看 **43（Flow）**，相机/内存相关在 Day 9 后补 **44/45**。

---

## 官方延伸阅读

- [Android architecture](https://developer.android.com/topic/architecture)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Lifecycle-aware coroutines](https://developer.android.com/topic/libraries/architecture/coroutines)
- [Permissions](https://developer.android.com/guide/topics/permissions/overview)
- [Background work](https://developer.android.com/develop/background-work/background-tasks)
- [ANR](https://developer.android.com/topic/performance/vitals/anr)
- [App startup](https://developer.android.com/topic/performance/vitals/launch-time)
- [BLE](https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview)
- [CameraX](https://developer.android.com/media/camera/camerax)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Room](https://developer.android.com/training/data-storage/room)
- [Kotlin coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Swift concurrency](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/)
