# MAR 使用说明

本文说明如何把 Minecraft Agent Runtime（MAR）V0 安装到受支持的 Minecraft Mod 项目，连接正在运行的 Runtime，并使用 Groovy 探索 Minecraft、维护项目 Tool 和沉淀可靠 Knowledge。

## 1. MAR 的工作方式

MAR 由三部分组成：

1. **Runtime**：随目标 Mod 一起编译并运行，在本机动态端口提供 NDJSON RPC。
2. **项目工作区**：目标项目中的 `.minecraft-agent-runtime/`，保存 Runtime、Tool、Knowledge、日志和运行状态。
3. **公共 Skill**：本仓库的 `skill/`，为 Agent 提供安装、探索、验证和知识治理流程。

Runtime 刻意保持很薄。它只负责执行 Groovy、切换到正确的 Minecraft 线程、保存 Session/Handle 和热加载 Tool，不内置背包、UI、移动或战斗等业务 API。

```text
Agent / mar_rpc.py
        │ loopback NDJSON
        ▼
MAR Runtime ── eval.raw    → RPC worker thread
            ├─ eval.client → Minecraft Client Thread
            └─ eval.server → Integrated Server Thread
```

## 2. 前置条件与支持范围

安装前需要：

- Python 3；
- Git；
- 能正常构建和启动的目标 Minecraft Mod 项目；
- 目标项目所需的 JDK 和 Gradle Wrapper；
- 对目标项目源码及 `build.gradle` 的写权限。

V0 自带安装器仅支持 NeoForge ModDevGradle 的 Groovy DSL 项目。它必须能从 `gradle.properties` 确认 `minecraft_version`、`neo_version`，从 `build.gradle` 确认 Java toolchain，并找到唯一的 `@Mod` 入口类。

以下情况会停止而不是猜测：

- 使用 `build.gradle.kts`；
- 不是 NeoForge ModDevGradle；
- 项目版本信息不完整；
- 没有入口类或存在多个候选入口类；
- Java 版本低于 Runtime 清单要求。

## 3. 安装 MAR

以下命令均从 MAR 仓库根目录运行。

### 3.1 克隆仓库

```powershell
git clone https://github.com/MieA04/Minecraft-Agent-Runtime.git
cd Minecraft-Agent-Runtime
```

### 3.2 执行只读检测

安装前先运行 `--dry-run`：

```powershell
python skill/scripts/install_mar.py `
  --project-root "D:\path\to\your-mod" `
  --dry-run
```

检查 JSON 输出中的这些字段：

- `projectRoot`
- `buildSystem`
- `minecraftVersion`
- `loaderVersion`
- `javaVersion`
- `entrypoint`
- `entrypointClass`
- `runTask`

如果安装器报告多个 `@Mod` 入口，不要随意选择。检查源码后传入项目根目录下的相对路径：

```powershell
python skill/scripts/install_mar.py `
  --project-root "D:\path\to\your-mod" `
  --entrypoint "src/main/java/com/example/ExampleMod.java" `
  --dry-run
```

### 3.3 写入项目

检测正确后去掉 `--dry-run`：

```powershell
python skill/scripts/install_mar.py --project-root "D:\path\to\your-mod"
```

安装器会修改目标项目的 `build.gradle` 和 Mod 入口类，并创建 `.minecraft-agent-runtime/`。`build.gradle` 中的受管内容位于以下标记之间：

```text
// MAR-BEGIN: managed runtime integration
// MAR-END: managed runtime integration
```

入口类只会加入一次 `MinecraftAgentRuntime.start()`。重复运行安装器是幂等的；升级时会更新受管 Runtime 文件，但不会覆盖已有 Tool、Knowledge、日志、实验记录或用户配置值。

## 4. 构建、启动与验证

### 4.1 使用目标项目自己的 Gradle Wrapper

```powershell
cd D:\path\to\your-mod
.\gradlew.bat build
.\gradlew.bat runClient
```

安装器当前报告的开发运行任务为 `runClient`。不要只以构建成功判断 MAR 可用，Runtime 必须在真实 Minecraft JVM 中启动。

### 4.2 查看 Runtime 状态

Runtime 启动后写入：

```text
.minecraft-agent-runtime/state/runtime.json
```

典型内容如下：

```json
{
  "schema": 1,
  "status": "ready",
  "runtimeVersion": "0.1.0",
  "pid": 12345,
  "host": "127.0.0.1",
  "port": 49152,
  "startedAt": "2026-08-11T12:00:00Z",
  "projectRoot": "D:/path/to/your-mod",
  "processRole": "client"
}
```

端口默认由系统动态分配。即使文件显示 `ready`，也必须实际连接验证；Minecraft 异常退出后可能留下过期 state。

### 4.3 执行安装 smoke test

回到 MAR 仓库根目录：

```powershell
python skill/scripts/mar_rpc.py `
  --state "D:\path\to\your-mod\.minecraft-agent-runtime\state\runtime.json" `
  --wait 90 `
  --smoke-mod-class "com.example.ExampleMod"
```

该检查必须同时证明：

1. 能连接 state 中的 loopback 端口；
2. `eval.raw` 执行 `1 + 2` 返回 `3`；
3. 正在运行的 ClassLoader 能解析目标 Mod 入口类。

## 5. 执行 Groovy

通用命令格式：

```powershell
python skill/scripts/mar_rpc.py `
  --state "<项目根>\.minecraft-agent-runtime\state\runtime.json" `
  --session default `
  --method eval.raw `
  --code "1 + 2"
```

响应是 JSON，成功响应包含 `ok`、`result`、`stdout` 和 `stderr`。脚本异常会返回结构化 `error`，单次失败不会关闭 Runtime 或 Session。

### 5.1 `eval.raw`

在 RPC worker thread 执行，适合 JVM 内省、类查询、Reflection 和 Runtime 管理。它不会自动切换到 Minecraft Client/Server Thread。

```powershell
python skill/scripts/mar_rpc.py `
  --state "<项目根>\.minecraft-agent-runtime\state\runtime.json" `
  --method eval.raw `
  --code "[value: 1 + 2, thread: Thread.currentThread().name]"
```

不要用 `eval.raw` 读写要求 Minecraft 线程亲和性的状态。

### 5.2 `eval.client`

在 Minecraft Client Thread 执行，适合客户端、当前 Screen、玩家渲染侧状态和 UI 操作。

```powershell
python skill/scripts/mar_rpc.py `
  --state "<项目根>\.minecraft-agent-runtime\state\runtime.json" `
  --method eval.client `
  --code "[thread: Thread.currentThread().name, screen: mc.screen?.class?.name]"
```

`mc` 会在 Runtime 安全解析到 Minecraft Client 后按需加入 Binding。不要假设存在 `player`、`screen`、`world`、`server` 或 `inventory` 等额外变量；应从真实对象继续探索。

### 5.3 `eval.server`

在 Integrated Server Thread 执行。只有进入单人世界并存在 integrated server 时才可用：

```powershell
python skill/scripts/mar_rpc.py `
  --state "<项目根>\.minecraft-agent-runtime\state\runtime.json" `
  --method eval.server `
  --code "[thread: Thread.currentThread().name, server: mc.singleplayerServer?.class?.name]"
```

在主菜单或多人客户端中没有 Integrated Server 时会返回 `TARGET_UNAVAILABLE`。MAR 不会把该请求降级到 raw/client 线程。

## 6. Session、变量与 Handle

Session 名称通过 `--session` 指定。相同 Session 会在多次连接之间保留 `vars`、Handle 和已加载 Tool namespace，直到 Runtime 重启。

保存和读取中间值：

```powershell
python skill/scripts/mar_rpc.py --state "<state>" --session inspect --method eval.client --code "vars.currentScreen = mc.screen; vars.currentScreen?.class?.name"
python skill/scripts/mar_rpc.py --state "<state>" --session inspect --method eval.client --code "vars.currentScreen"
```

复杂且不能安全转换为 JSON 的对象会返回 Session 内的 Handle descriptor，例如 `@1`。在同一 Session 中使用 `ref()` 恢复对象身份：

```powershell
python skill/scripts/mar_rpc.py --state "<state>" --session inspect --method eval.client --code "ref('@1').class.name"
```

Handle 不能跨 Session 使用，Runtime 重启后全部失效。

## 7. 项目 Tool

项目 Tool 位于：

```text
.minecraft-agent-runtime/tools/
├── minecraft/    Vanilla/client/server 能力
├── loader/       NeoForge/Loader 能力
├── mod/          当前 Mod 私有能力
└── experimental/ 尚未稳定的实验能力
```

每个能力域使用独立的 `.groovy` 文件。稳定 Tool 的加载期代码应保持声明式，把副作用放在导出的 closure 或方法调用中。

重新加载所有稳定目录：

```powershell
python skill/scripts/mar_rpc.py `
  --state "<state>" `
  --method eval.raw `
  --code "runtime.tools.reloadAllStable()"
```

重新加载单个 Tool：

```powershell
python skill/scripts/mar_rpc.py `
  --state "<state>" `
  --method eval.raw `
  --code "runtime.tools.reloadPath('minecraft/inventory.groovy')"
```

稳定批量加载只扫描 `minecraft`、`loader` 和 `mod`；`experimental` 必须显式加载。Tool 加载后按路径进入 `vars.tools` namespace，例如 `minecraft/inventory.groovy` 对应 `vars.tools.minecraft.inventory`。

## 8. 推荐的探索流程

操作未知 Minecraft 对象前，依次阅读目标项目中的：

1. `knowledge/project.md`
2. `knowledge/minecraft.md`
3. `knowledge/loader.md`
4. 相关 `knowledge/discoveries/`
5. 项目 Tool
6. 适用的公共 Tool

探索时一次只验证一个短假设：

```text
对象 → class → 父类/接口 → 字段 → 方法 → 关联对象
    → 所需线程 → 低副作用调用 → 验证结果 → discovery
```

优先使用已知 Tool；Tool 失败时回到当前 API、Reflection 和 JVM 原始能力继续验证，不要立即把失败解释为 MAR 的能力上限，也不要为某个业务动作增加新的 Runtime RPC 方法。

## 9. Knowledge 与 discovery

详细实验记录放在：

```text
.minecraft-agent-runtime/knowledge/discoveries/
```

每个 discovery 应记录一个假设、环境版本、目标线程、有效与关键无效调用、验证证据、结论和下一步。只有运行时证据证明结论后才能标记为 `verified`。

稳定结论再按领域提炼到：

- `project.md`：项目私有结构和行为；
- `minecraft.md`：当前环境验证过的 Vanilla/client/server 事实；
- `loader.md`：Loader 生命周期、事件和执行器事实。

原始大输出放入 `logs/experiments/`，不要直接把日志当成 Knowledge。项目私有类名、字段、UI 和实验不得写入公共 Skill。

## 10. 完整验收

Minecraft 和 MAR 正在运行时，可以执行可重复的 V0 验收：

```powershell
python skill/scripts/mar_acceptance.py `
  --project-root "D:\path\to\your-mod" `
  --state "D:\path\to\your-mod\.minecraft-agent-runtime\state\runtime.json" `
  --mod-class "com.example.ExampleMod" `
  --server available
```

- 已进入单人世界时使用 `--server available`；
- 当前没有 Integrated Server 时使用 `--server unavailable`；
- 如需额外验证第三方依赖可见性，传入 `--third-party-class <完整类名>`；
- 只有明确不检查 Tool reload 时才使用 `--skip-tool-reload`。

查看所有参数：

```powershell
python skill/scripts/mar_acceptance.py --help
```

## 11. 目标项目工作区

```text
.minecraft-agent-runtime/
├── runtime/
│   ├── bootstrap/       受安装器管理的 Runtime 副本
│   └── config/runtime.json
├── tools/               项目 Groovy Tool
├── knowledge/           稳定知识与 discoveries
├── logs/                Session 与实验记录
└── state/runtime.json   当前进程连接信息
```

MAR 不强制这些文件是否进入目标项目的 Git。若决定提交，通常保留 Runtime、配置、Tool 和 Knowledge，并忽略动态 state、临时 Session 日志和大型实验输出。

## 12. 配置

Runtime 配置文件：

```text
.minecraft-agent-runtime/runtime/config/runtime.json
```

V0 默认配置：

```json
{
  "schema": 1,
  "rpc": {
    "host": "127.0.0.1",
    "port": 0
  }
}
```

`port: 0` 表示动态端口。V0 的 Runtime 配置仅用于基础设施，不应放入背包、UI、移动、攻击等 Tool 业务配置。

## 13. 常见问题

### 找不到或无法连接 state

确认 Minecraft 开发实例仍在运行，检查 `runtime.json` 的 `status`、`pid`、`host` 和 `port`。异常退出可能留下显示 `ready` 的旧文件；以实际 TCP 连接结果为准。

### `TARGET_UNAVAILABLE`

- `eval.client`：Runtime 当前无法解析客户端实例或 executor；
- `eval.server`：通常表示尚未进入单人世界或 integrated server 已关闭。

请求不会自动转移到其他线程。先确认当前游戏状态，再选择正确的执行目标。

### `EVAL_EXCEPTION`

检查响应中的异常类型、message、完整 stack、`stdout` 和 `stderr`。区分 Groovy 语法错误、类/成员名称变化、Handle 失效和 Minecraft 线程要求，然后用更小的只读表达式逐步验证。

### 安装器拒绝入口类

检查 `src/main/java` 下的 `@Mod` 类。如果确实有多个候选，人工确认正确入口后使用 `--entrypoint`；不要为了通过检测而删除或改写其他 Mod 入口。

### Minecraft 重启后 Handle 失效

这是预期行为。Session 与 Handle 只属于当前 Runtime 进程，重启后重新获取对象。

## 14. 安全与操作边界

MAR 可执行任意 Groovy/JVM 操作，其能力等同于目标 Minecraft 开发进程。使用时必须遵守：

- 只在可信的本地开发环境运行；
- 保持 RPC 监听地址为 `127.0.0.1`，不要转发或公开端口；
- 执行修改世界、文件或项目状态的代码前先确认影响范围；
- 探索优先使用只读、低副作用表达式；
- 不执行来源不可信的 Groovy 或 Tool；
- 不把 MAR 接入生产服务器或含敏感数据的环境。

V0 没有认证、授权、远程文件上传或网络级隔离。loopback 限制是开发期安全边界的一部分，不能把它当作可公开部署的服务。

## 15. 延伸阅读

- [Runtime 架构](10-runtime/10-RUNTIME-ARCHITECTURE.md)
- [RPC 协议](10-runtime/11-RPC-PROTOCOL.md)
- [Session、Result 与 Handle](10-runtime/12-SESSION-RESULT-HANDLE.md)
- [线程执行语义](10-runtime/13-THREAD-EXECUTION.md)
- [Tool 系统](10-runtime/14-TOOL-SYSTEM.md)
- [项目工作区规范](30-project-workspace/30-PROJECT-WORKSPACE.md)
- [测试与验收](40-development/41-TEST-AND-ACCEPTANCE.md)
- [错误码](90-appendix/90-ERROR-CODES.md)
