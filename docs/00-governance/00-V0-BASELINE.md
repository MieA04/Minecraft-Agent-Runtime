# Minecraft Agent Runtime V0
## 架构与设计文档

**文档版本：V0.1**
**项目名称：Minecraft Agent Runtime，简称 MAR**

---

# 1. 项目定位

Minecraft Agent Runtime 是一个面向 Minecraft 模组开发环境的 **Agent 动态调试运行时与知识演化体系**。

它的目标不是预先提供完整的 Minecraft 自动化 API，也不是构建传统意义上的调试 Mod。

V0 的核心目标是：

> 让 Agent 能够将 Groovy 动态运行时注入正在开发的 Minecraft 项目，在游戏持续运行期间通过 RPC 执行任意 JVM 代码、获取结果、观察 Minecraft 和模组内部状态，并将探索过程中形成的有效经验持续沉淀为项目专属能力与公共 Skill 能力。

整体工作模式：

```text
Agent
  ↓
Minecraft Agent Runtime Skill
  ↓
向指定项目安装 Runtime
  ↓
启动 Minecraft 开发实例
  ↓
RPC + Groovy
  ↓
实时探索 JVM / Minecraft / Mod
  ↓
验证操作方式
  ↓
沉淀工具与知识
  ↓
项目专属能力 / 公共 Skill
```

---

# 2. V0 核心思想

V0 不追求：

```text
预先设计完整 API
→ 实现所有 Minecraft 控制能力
→ 再交给 Agent 使用
```

而采用：

```text
提供最低限度动态运行环境
→ Agent 自行探索
→ 发现稳定操作方式
→ 将操作方式封装
→ 形成工具
→ 积累知识
→ 继续探索
```

因此：

> Runtime 负责提供能力下限，Agent 负责发现能力上限。

---

# 3. 总体架构

Minecraft Agent Runtime 分为三个层级。

```text
┌──────────────────────────────────┐
│              Agent               │
│                                  │
│   Minecraft Agent Runtime Skill  │
└─────────────────┬────────────────┘
                  │
                  │ 安装 / 调用 / 探索 / 总结
                  ▼
┌──────────────────────────────────┐
│       Project Agent Runtime      │
│                                  │
│  Bootstrap                       │
│  Groovy Runtime                  │
│  RPC                             │
│  Session                         │
│  Result Bridge                   │
│                                  │
│  Project Knowledge               │
│  Project Tools                   │
└─────────────────┬────────────────┘
                  │
                  ▼
┌──────────────────────────────────┐
│           Minecraft JVM          │
│                                  │
│ Minecraft                        │
│ Mod Loader                       │
│ Current Mod                      │
│ Third-party Mods                 │
│ JVM APIs                         │
└──────────────────────────────────┘
```

三个层级分别承担：

### 公共 Skill

负责：

- Runtime 安装方法；
- Groovy 调用规范；
- Agent 探索策略；
- Runtime API 文档；
- 已验证的 Minecraft 通用经验；
- Loader / Minecraft 版本兼容经验；
- 公共 Groovy 工具模板；
- 项目经验晋升规则。

### 项目 Runtime

负责：

- 启动 Groovy；
- 接收 RPC；
- 执行代码；
- 保存 Session；
- 返回执行结果；
- 保存项目工具与知识。

### Minecraft JVM

是实际被探索与控制的运行环境。

Groovy 与 Minecraft 位于同一个 JVM，因此 Groovy 代码可以直接访问当前 JVM 中存在的 Java 对象与 API。

---

# 4. V0 功能边界

V0 只负责解决：

```text
Agent 如何进入 JVM
Agent 如何执行代码
Agent 如何获得执行结果
Agent 如何连续探索
Agent 如何保存探索成果
```

V0 不主动提供：

- UI 自动识别；
- 背包 API；
- 合成 API；
- 玩家移动 API；
- 攻击 API；
- 挖掘 API；
- 世界查询 API；
- Mod UI Adapter；
- Minecraft 高层语义 API；
- 自动测试 DSL；
- 通用自动化流程。

这些能力应该通过 Agent 后续探索逐渐形成。

---

# 5. Skill 与 Runtime 的关系

Minecraft Agent Runtime 不单独把 Skill 或 Runtime 定义为整个项目的唯一“本体”。

它们组成完整系统。

```text
Skill
=
安装器
+
操作手册
+
公共知识库
+
探索方法论
+
公共能力定义
```

```text
Project Runtime
=
JVM 内动态执行入口
+
项目实验环境
+
项目专属工具库
+
项目知识库
```

可以理解为：

> Skill 负责“知道怎么做”。

> Runtime 负责“实际能够做”。

---

# 6. 安装流程

当 Agent 第一次接触一个 Minecraft 项目时：

```text
读取 Minecraft Agent Runtime Skill
        ↓
识别项目结构
        ↓
识别 Loader / Minecraft / Java
        ↓
添加 Groovy 依赖
        ↓
复制 Bootstrap Runtime
        ↓
加入 Runtime 启动入口
        ↓
创建项目 Runtime 工作目录
        ↓
启动 runClient / runServer
        ↓
连接 RPC
        ↓
开始探索
```

V0 不要求 Minecraft Agent Runtime 本身作为独立 Mod。

最简单的方式是由 Agent 将 Bootstrap 临时加入正在开发的 Mod。

例如：

```java
@Mod("example")
public final class ExampleMod {

    public ExampleMod() {
        MinecraftAgentRuntime.start();

        // 原有初始化
    }
}
```

第一次完成 Runtime 注入后，绝大多数探索过程不再需要重新启动 Minecraft。

---

# 7. Groovy Runtime

Groovy 是 V0 的主要动态执行环境。

Runtime 至少维护：

```text
GroovyClassLoader
Binding
GroovyShell
Persistent Session
```

概念结构：

```java
class MinecraftAgentRuntime {

    GroovyClassLoader classLoader;
    Binding binding;
    GroovyShell shell;

}
```

Groovy ClassLoader 应以 Minecraft / Mod 当前有效 ClassLoader 作为父加载器，使动态脚本可以访问：

```text
Minecraft Classes
Loader Classes
Current Mod Classes
Third-party Mod Classes
JDK Classes
```

Groovy Runtime 不建立 Minecraft API 白名单。

Agent可以通过 Groovy 使用 JVM 当前能够访问的 API。

---

# 8. Persistent Session

V0 必须支持持续 Session。

例如 Agent 第一次执行：

```groovy
screen = mc.screen
```

随后可以继续：

```groovy
screen.class.name
```

再：

```groovy
screen.children()
```

而不需要每次重新获取对象。

因此 RPC Connection 或 Runtime Session 必须维护：

```text
变量
对象引用
辅助函数
临时工具
探索状态
```

推荐 Runtime 显式提供一个长期存在的：

```groovy
vars
```

例如：

```groovy
vars.screen = mc.screen
vars.menu = vars.screen.menu
```

后续：

```groovy
vars.menu.slots
```

以避免 Groovy 单次 Script 局部变量与 Binding 生命周期差异造成混乱。

---

# 9. RPC

V0 RPC 目标是：

> 让 Agent 能够低成本、连续、结构化地执行 Groovy。

V0 不需要复杂协议。

最低 API 可以只有：

```text
eval
```

但建议从一开始保留执行上下文：

```text
eval.raw
eval.client
eval.server
```

---

# 10. eval.raw

直接在 Runtime 当前处理线程执行 Groovy。

示例：

```json
{
  "method": "eval.raw",
  "code": "1 + 2"
}
```

返回：

```json
{
  "ok": true,
  "result": 3
}
```

适合：

- Reflection；
- JVM 查询；
- Class 查询；
- Runtime 自身操作；
- 不依赖 Minecraft 主线程的操作。

---

# 11. eval.client

将 Groovy Closure 或代码调度到 Minecraft Client Thread 执行。

例如：

```json
{
  "method": "eval.client",
  "code": "mc.screen"
}
```

逻辑：

```text
RPC Thread
    ↓
Minecraft Client Executor
    ↓
执行 Groovy
    ↓
等待结果
    ↓
RPC 返回
```

它存在的目的不是限制能力，而是提供符合 Minecraft 客户端线程语义的执行环境。

---

# 12. eval.server

在存在 Integrated Server 或可访问 Server Runtime 的情况下，将代码调度至 Server Thread。

例如：

```json
{
  "method": "eval.server",
  "code": "server.playerList.players"
}
```

Dedicated Server 的跨 JVM Runtime 支持不要求在 V0 第一阶段完成，但协议设计必须允许未来存在：

```text
client runtime
server runtime
```

两个独立执行目标。

---

# 13. Result Bridge

RPC 结果不能简单递归 JSON 序列化所有 Java Object。

Java 对象可能：

- 非序列化类型；
- 存在循环引用；
- 引用整个 Minecraft 对象图；
- 包含巨量数据。

因此结果分为两类。

## 13.1 基础值

直接 JSON 化：

```text
null
boolean
number
string
简单 list
简单 map
```

例如：

```groovy
mc.player.health
```

可以直接返回：

```json
{
  "ok": true,
  "result": 20.0
}
```

---

# 14. Object Handle

复杂 JVM Object 应转换为 Runtime Handle。

例如：

```groovy
mc.screen
```

返回：

```json
{
  "ok": true,
  "result": {
    "handle": "@42",
    "type": "net.minecraft.client.gui.screens.inventory.InventoryScreen",
    "string": "InventoryScreen@..."
  }
}
```

Runtime 内部：

```text
@42
↓
InventoryScreen Object
```

Agent 后续可再次引用：

```groovy
ref("@42").class.name
```

Object Handle 是 V0 建议直接实现的基础设施。

否则复杂对象探索体验会迅速恶化。

---

# 15. 输出捕获

每次 eval 应收集：

```text
return value
stdout
stderr
exception
```

统一结果：

```json
{
  "ok": true,
  "result": "...",
  "stdout": "...",
  "stderr": ""
}
```

失败：

```json
{
  "ok": false,
  "result": null,
  "stdout": "...",
  "stderr": "",
  "error": {
    "type": "...",
    "message": "...",
    "stack": "..."
  }
}
```

异常属于 Agent 探索过程的重要输入，不应只返回简单错误代码。

---

# 16. Bootstrap

Bootstrap 应保持极薄。

职责只包括：

```text
初始化 Groovy
初始化 Session
初始化 RPC
暴露基础 Runtime 变量
```

Bootstrap 不负责 Minecraft 业务能力。

公共 Skill 保存 Bootstrap 的权威模板。

项目安装时复制一个具体版本进入项目目录。

因此：

```text
公共 Bootstrap
        ↓
项目 Bootstrap 副本
        ↓
允许项目自行实验修改
        ↓
发现通用改进
        ↓
反向晋升公共 Bootstrap
```

---

# 17. 项目目录

建议 Agent 在项目根目录建立：

```text
.minecraft-agent-runtime/
│
├── runtime/
│   ├── bootstrap/
│   └── config/
│
├── tools/
│   ├── minecraft/
│   ├── loader/
│   ├── mod/
│   └── experimental/
│
├── knowledge/
│   ├── minecraft.md
│   ├── loader.md
│   ├── project.md
│   └── discoveries/
│
├── logs/
│   ├── sessions/
│   └── experiments/
│
└── state/
```

该目录属于当前项目。

---

# 18. 项目专属工具库

Agent 在探索过程中发现可复用操作后，应优先生成 Groovy 工具。

例如最初探索：

```groovy
mc.player.inventory.items
    .withIndex()
    .findAll { stack, index ->
        stack.item == target
    }
```

经过重复使用后可沉淀为：

```groovy
inventory.find(target)
```

保存：

```text
.minecraft-agent-runtime/tools/minecraft/inventory.groovy
```

工具生成后应支持重新加载，而无需重启 Minecraft。

---

# 19. 项目专属知识库

不是所有发现都应该立即进入公共 Skill。

例如：

```text
某个项目自己的 RuneScreen
某个 Mod 特有字段
某个临时 UI
某个私有数据结构
```

应该首先写入：

```text
knowledge/project.md
```

或者：

```text
knowledge/discoveries/
```

项目知识可以包含：

```text
类名
字段
方法
UI结构
有效调用
失败调用
线程要求
Minecraft版本
Loader版本
Mod版本
验证方法
```

---

# 20. 公共 Skill

公共 Minecraft Agent Runtime Skill 保存跨项目可复用知识。

例如：

```text
如何识别当前 Screen
如何查询 Screen.children()
AbstractContainerMenu 的通用结构
如何通过 Client Thread 调用 GUI
如何检查玩家 Inventory
NeoForge 某版本的结构变化
Groovy 与 Minecraft ClassLoader 的处理方式
```

公共 Skill 不保存单个项目私有实现细节。

---

# 21. 知识晋升机制

知识从探索到公共能力应经历：

```text
临时实验
    ↓
成功验证
    ↓
项目经验
    ↓
重复使用
    ↓
项目工具
    ↓
判断通用性
    ↓
跨项目验证
    ↓
公共工具 / 公共 Skill
```

禁止将第一次偶然成功的调用直接认定为公共 API。

---

# 22. Agent 探索模式

当 Agent 遇到未知对象时，推荐流程：

```text
获取对象
↓
读取 class
↓
查看继承关系
↓
查看字段
↓
查看方法
↓
获取相关对象
↓
尝试调用
↓
观察结果
↓
调整
↓
成功
↓
总结
```

例如未知 Screen：

```groovy
screen = mc.screen
```

然后：

```groovy
screen.class.name
```

继续：

```groovy
screen.class.declaredFields*.name
```

继续：

```groovy
screen.class.declaredMethods*.name
```

再根据实际结构继续探索。

---

# 23. 能力降级原则

未来高层 API 出现后，Agent 应优先：

```text
公共工具
↓
项目工具
↓
Minecraft原始API
↓
Reflection
↓
JVM原始能力
```

例如：

```text
ui.click("确定")
```

失败以后，不应该停止。

而应该自动下降：

```text
检查Screen
↓
检查children
↓
检查字段
↓
检查方法
↓
检查mouseClicked
↓
坐标尝试
```

探索成功后再反向封装。

---

# 24. Skill 的核心职责

未来创建 Minecraft Agent Runtime Skill 时，Skill 应指导 Agent 完成以下工作。

## 安装

- 检测项目类型；
- 检测 Minecraft；
- 检测 Loader；
- 检测 Java；
- 添加 Groovy；
- 安装 Bootstrap；
- 创建项目 Runtime 目录。

## 启动

- 启动目标开发实例；
- 找到 Runtime；
- 建立 RPC Session。

## 探索

- 优先读取已有项目知识；
- 优先使用已有工具；
- 工具失败后降级探索；
- 通过 Groovy 查询 JVM；
- 验证发现。

## 沉淀

- 保存实验；
- 更新项目知识；
- 提取项目工具；
- 判断通用性；
- 更新公共 Skill。

---

# 25. Skill 不应做什么

Skill 不应该假设：

```text
Minecraft UI 一定是什么结构
某字段一定存在
某 Loader API 永远稳定
某个工具永远正确
```

Skill 的核心理念应该是：

> 已知能力优先使用，未知能力实时探索。

因此 Minecraft Agent Runtime 的价值不是维护一个永远正确的 Minecraft API 数据库，而是：

> 即使已有知识失效，Agent 仍然拥有重新发现正确操作方式的能力。

---

# 26. V0 推荐最小 Runtime API

V0 最低建议只提供：

```text
eval.raw(code)

eval.client(code)

eval.server(code)

ref(handle)
```

其中：

```text
eval.*
```

属于 RPC API。

`ref()` 可以作为 Groovy Binding 内置函数。

除此之外的所有能力尽可能通过 Groovy 动态实现。

---

# 27. 初始 Binding

Runtime 可以预置少量无法避免的对象。

例如：

```text
vars
ref
runtime
```

Minecraft 对象是否预置：

```text
mc
player
screen
server
```

可以保持谨慎。

V0 可以仅提供 `mc`，甚至完全允许 Agent 自己寻找 Minecraft Singleton。

原则是：

> Bootstrap 只提供必要便利，不将便利 API 变成 Runtime 能力边界。

---

# 28. Runtime 热更新

项目 Groovy Tool Library 应支持运行时加载或重新加载。

流程：

```text
Agent探索
↓
生成Groovy Tool
↓
保存tools/
↓
reload
↓
立即调用
```

Minecraft 不需要因为添加一个新工具函数而重新启动。

这是 Minecraft Agent Runtime 能力演化效率的重要基础。

---

# 29. 经验记录

每次探索不需要永久保存所有 RPC 调用。

但 Agent 应保留有价值的实验结果。

例如：

```text
目标：
找到工作台结果槽

环境：
Minecraft 1.21.1
NeoForge 21.1.x

发现：
当前 Screen 继承 AbstractContainerScreen

有效：
screen.menu.slots[...]

无效：
screen.children() 中不存在 Slot Widget

结论：
容器槽应从 Menu 获取，而不是 Screen.children()
```

这类经验未来可以直接晋升公共 Skill。

---

# 30. 项目能力与公共能力边界

判断标准：

### 项目专属

如果能力依赖：

```text
当前项目类
当前项目字段
当前项目 Mod ID
项目特定逻辑
```

则保存项目内。

### 公共

如果能力适用于：

```text
Minecraft Vanilla
NeoForge
Fabric
通用 Minecraft UI Framework
多个独立项目
```

则可以晋升公共 Skill。

---

# 31. V0 生命周期

完整生命周期：

```text
Agent
↓
安装 Skill
↓
打开 Minecraft Mod 项目
↓
Skill 检查 Runtime
↓
没有 Runtime
↓
添加 Groovy 依赖
↓
复制 Bootstrap
↓
注入启动
↓
创建 .minecraft-agent-runtime
↓
启动游戏
↓
建立 RPC
↓
Groovy 探索
↓
产生项目工具
↓
产生项目知识
↓
持续调试
↓
发现通用经验
↓
晋升公共 Skill
↓
下一个项目直接复用
```

---

# 32. V0 验收标准

Minecraft Agent Runtime V0 完成的标准不是：

```text
可以自动点击背包
```

也不是：

```text
可以自动挖矿
```

而是以下能力全部成立。

### 运行时

Agent 可以向目标 Minecraft 开发项目安装 Groovy Runtime。

### 连接

Minecraft 启动后 Agent 可以建立 RPC Session。

### 动态执行

Agent 可以在 Minecraft 不重启的情况下重复执行 Groovy。

### JVM访问

Groovy 可以访问 Minecraft、Loader 和当前 Mod Class。

### 状态持续

多次调用可以共享 Session / Object Handle。

### 结果反馈

可以返回：

```text
基础值
Object Handle
stdout
stderr
异常
stack trace
```

### Minecraft线程

可以在 Client Thread 执行代码。

存在 Integrated Server 时可以在 Server Thread 执行代码。

### 工具成长

Agent 可以创建新的 Groovy 工具并在游戏不重启的情况下加载使用。

### 知识成长

Agent 可以将探索结果保存到项目知识库。

### 公共晋升

Agent 能够识别通用经验并更新 Minecraft Agent Runtime Skill。

满足以上条件即可认为 V0 达成。

---

# 33. V0 非目标

以下明确不属于 V0 必须完成内容：

```text
完整 UI Tree
自动视觉识别
完整 Inventory API
自动合成
自动攻击
自动挖掘
路径规划
Mod Adapter 系统
自动化测试 DSL
浏览器式 DOM
完整事件总线
复杂远程调试协议
生产环境支持
```

如果 Agent 在 V0 探索阶段自行实现其中部分能力，可以保存为工具，但不得反向要求 V0 Runtime 预先实现。

---

# 34. 核心设计原则

Minecraft Agent Runtime V0 最重要的设计原则：

### Runtime 极薄

只负责让 Agent 获得 JVM 动态执行能力。

### 不提前猜 API

高层能力从真实探索中自然产生。

### 项目优先沉淀

首次发现默认属于项目知识。

### 通用经验晋升

经过验证的跨项目经验进入公共 Skill。

### 工具可以失效

Minecraft、Loader 和 Mod 都可能变化。

工具失效时 Agent 应重新探索，而不是要求 Runtime 保证所有版本兼容。

### Skill 可以成长

Skill 不是静态说明书，而是整个系统持续积累的公共经验核心。

### Runtime 保证能力下限

即使全部高层工具失效，只要 Groovy Runtime 能够工作，Agent 就应该仍然能够从 JVM 原始能力重新开始探索。

---

# 35. 最终架构定义

Minecraft Agent Runtime 最终形成：

```text
                    Agent
                      │
                      ▼
          Minecraft Agent Runtime Skill
                      │
          ┌───────────┴───────────┐
          │                       │
     公共知识                 公共工具
          │                       │
          └───────────┬───────────┘
                      │
                      ▼
               Project Runtime
                      │
          ┌───────────┴───────────┐
          │                       │
     项目知识                 项目工具
          │                       │
          └───────────┬───────────┘
                      │
                      ▼
                Groovy Runtime
                      │
                      ▼
                 Minecraft JVM
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   Minecraft      Mod Loader      Mods
```

系统形成持续循环：

```text
探索
↓
成功
↓
总结
↓
封装
↓
复用
↓
遇到新问题
↓
继续探索
```

---

# 36. V0 一句话定义

> Minecraft Agent Runtime V0 是一个由公共 Skill 驱动安装、嵌入 Minecraft 开发 JVM 的极简 Groovy 动态执行 Runtime。它通过 RPC 为 Agent 提供持续代码执行和结果反馈能力，并允许 Agent 将实时探索形成的经验分别沉淀为项目专属知识、项目工具和可跨项目复用的公共 Skill 能力。

V0 的目标不是“实现 Minecraft 自动化”。

V0 的目标是：

> **让 Agent 获得自行发现 Minecraft 自动化能力的基础。**
