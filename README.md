# Minecraft Agent Runtime（MAR）

Minecraft Agent Runtime（MAR）是面向 Minecraft Mod 开发环境的本地动态执行基础设施。它把一个轻量 Runtime 安装到目标 Mod 项目中，让 Agent 能在正在运行的 Minecraft JVM 内执行 Groovy，检查真实对象与线程状态，并把验证过的操作逐步沉淀为项目 Tool 和 Knowledge。

MAR V0 只提供三种通用执行入口：`eval.raw`、`eval.client` 和 `eval.server`。背包、UI、移动、战斗或特定 Mod 行为不会被固化成 Runtime API，而是由 Agent 在项目中探索、验证并维护。这样既能适应 Minecraft、Loader 和 Mod API 的变化，也能避免把项目私有知识写入公共 Runtime。

完整的安装、连接、执行、Tool 热加载、知识沉淀和故障排查方法见 **[MAR 使用说明](docs/USAGE.md)**。

## 当前支持范围

MAR V0 安装器当前支持满足以下条件的项目：

- 可修改源码的 Minecraft Mod 开发项目；
- NeoForge ModDevGradle；
- Groovy DSL，即项目使用 `build.gradle`；
- `gradle.properties` 中能够识别 `minecraft_version` 和 `neo_version`；
- 能够定位唯一的 `@Mod` 入口类；
- 项目 Java 版本不低于 Runtime 清单声明的最低版本。

Gradle Kotlin DSL、其他 Loader 或无法唯一识别入口类的项目不会被安装器猜测性修改。

## 安装

### 1. 获取 MAR

```powershell
git clone https://github.com/MieA04/Minecraft-Agent-Runtime.git
cd Minecraft-Agent-Runtime
```

需要 Python 3 执行安装与 RPC 客户端。目标 Mod 项目仍使用自己的 Gradle Wrapper、JDK 和 Minecraft 开发运行配置。

### 2. 先检测项目

```powershell
python skill/scripts/install_mar.py `
  --project-root "D:\path\to\your-mod" `
  --dry-run
```

确认输出中的项目根目录、Minecraft/NeoForge/Java 版本、入口类和 `runClient` 任务正确后再安装。若项目存在多个 `@Mod` 类，可在检查候选文件后显式指定：

```powershell
python skill/scripts/install_mar.py `
  --project-root "D:\path\to\your-mod" `
  --entrypoint "src/main/java/com/example/ExampleMod.java"
```

### 3. 安装 Runtime

```powershell
python skill/scripts/install_mar.py --project-root "D:\path\to\your-mod"
```

安装器会：

- 创建目标项目的 `.minecraft-agent-runtime/` 工作区；
- 安装受 MAR 管理的 Runtime 源码和固定版本依赖；
- 将 Runtime source directory 与运行时 classpath 接入 `build.gradle`；
- 设置 `mar.projectRoot`；
- 在 Mod 构造器中加入一次 `MinecraftAgentRuntime.start()`。

安装器可重复运行。更新受管 Runtime 文件时，不会覆盖项目已有的 Tool、Knowledge、日志、实验记录或 Runtime 配置值。

### 4. 构建并启动 Minecraft

在目标 Mod 项目中执行项目自己的构建和开发运行任务，例如：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Minecraft 启动后，MAR 会把动态 loopback 端口写入：

```text
<项目根>/.minecraft-agent-runtime/state/runtime.json
```

### 5. 验证安装

将 `<入口类完整限定名>` 替换为安装器输出的入口类：

```powershell
python skill/scripts/mar_rpc.py `
  --state "D:\path\to\your-mod\.minecraft-agent-runtime\state\runtime.json" `
  --wait 90 `
  --smoke-mod-class "com.example.ExampleMod"
```

只有动态连接成功、`1 + 2` 返回 `3`，并且正在运行的 JVM 能解析目标 Mod 类，才算安装成功；仅编译通过不代表 Runtime 已可用。

## 最小使用示例

Minecraft 保持运行时，可以从 MAR 仓库根目录执行：

```powershell
python skill/scripts/mar_rpc.py `
  --state "D:\path\to\your-mod\.minecraft-agent-runtime\state\runtime.json" `
  --method eval.client `
  --code "[thread: Thread.currentThread().name, screen: mc.screen?.class?.name]"
```

进入单人世界后，可在 Integrated Server Thread 上执行：

```powershell
python skill/scripts/mar_rpc.py `
  --state "D:\path\to\your-mod\.minecraft-agent-runtime\state\runtime.json" `
  --method eval.server `
  --code "[thread: Thread.currentThread().name, server: mc.singleplayerServer?.class?.name]"
```

更多示例与安全边界见 [MAR 使用说明](docs/USAGE.md)。

## 仓库结构

```text
docs/          V0 架构、协议、治理、使用说明与验收规范
fixtures/      测试与演练 fixture
skill/         Agent Skill、安装/RPC 脚本、Runtime 与工作区模板
test-harness/  Java 与 Python 自动化测试
```

目标 Mod 项目安装后会得到独立的 `.minecraft-agent-runtime/`。公共 MAR 仓库、目标项目工作区和公共 Skill 是三个不同的存储域，项目私有发现不得回写到公共 Skill。

## 开发与验证

在 Windows 上运行：

```powershell
mvn.cmd -f test-harness/pom.xml clean package
python -m unittest discover -s test-harness/python
```

架构与协议的权威约束从以下文档开始：

- [V0 架构基线](docs/00-governance/00-V0-BASELINE.md)
- [不可变约束](docs/00-governance/01-INVARIANTS.md)
- [冻结决策](docs/00-governance/02-FROZEN-DECISIONS.md)
- [测试与验收](docs/40-development/41-TEST-AND-ACCEPTANCE.md)

## 安全提示

MAR 能在 Minecraft 开发 JVM 中执行任意 Groovy/JVM 操作。V0 仅监听 `127.0.0.1` 且不提供身份验证，因此只应在可信的本地开发环境使用；不要把端口转发或暴露给不可信网络，也不要执行未经审查的脚本。
