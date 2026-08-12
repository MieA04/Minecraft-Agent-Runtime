# MAR V0 不可变约束

本文件将 V0 基线中的原则转写为实现期 MUST / MUST NOT。除非用户显式修改 V0 架构，否则任何 Agent、实现者、重构都不得违反。

## 1. 项目定位

MAR V0 MUST：

- 面向 Minecraft 开发环境；
- 通过 Groovy 进入当前 Minecraft JVM；
- 通过 RPC 连续执行代码并获得结果；
- 能观察 Minecraft、Loader、当前 Mod、第三方 Mod 与 JVM 对象；
- 能把探索成果分别沉淀为项目 Tool、项目 Knowledge 与公共 Skill。

MAR V0 MUST NOT 被实现为：

- 完整 Minecraft 自动化框架；
- UI 自动化框架；
- Minecraft 高层语义 API；
- Mod Adapter 平台；
- 自动测试 DSL；
- 生产环境远程调试系统。

## 2. Runtime 能力边界

Runtime MUST 只提供探索基础设施：

- Groovy 动态执行；
- Persistent Session；
- RPC；
- `eval.raw` / `eval.client` / `eval.server`；
- Result Bridge；
- Object Handle；
- stdout / stderr / exception 捕获；
- 最小 Binding；
- Project Tool 热加载；
- Runtime Config / State。

Runtime MUST NOT 预置：

- Inventory API；
- Slot API；
- Crafting API；
- UI Tree；
- Button finder / click API；
- 玩家移动；
- 路径规划；
- 攻击；
- 挖掘；
- World Query；
- Mod UI Adapter；
- Game Test DSL；
- DOM-like abstraction；
- “统一 Minecraft API”。

需要这些能力时，Agent MUST 先通过 `eval.*` 探索，再落到项目 Tool / Knowledge。

## 3. Bootstrap 极薄

`MinecraftAgentRuntime.start()` 只能：

1. 定位 Project Root；
2. 读取 Runtime Config；
3. 创建 RuntimeHost；
4. 初始化 Groovy；
5. 初始化 Session；
6. 初始化 RPC；
7. 安装基础 Binding；
8. 写 Runtime State；
9. 注册关闭清理。

Bootstrap MUST NOT：

- 查询玩家/Screen/Inventory；
- 注册项目业务事件；
- 扫描 UI；
- 维护 Minecraft 版本业务适配表；
- 直接实现 Tool；
- 写 Project Knowledge。

## 4. Runtime 只保证能力下限

Agent MUST 可以通过 Groovy 使用当前 ClassLoader 能访问的 JVM 能力，包括 Reflection、对象引用、JDK/Minecraft/Loader/Mod API。

文档不得写出“只能调用 Runtime 暴露 API”一类能力限制。

## 5. 项目优先沉淀

任何首次发现 MUST 先属于项目工作区。

第一次成功调用 MUST NOT：

- 直接进入 public Tool；
- 直接成为公共稳定 Knowledge；
- 被描述为跨版本保证。

必须先生成 discovery，再根据复用与验证情况提取 project Tool，最后才允许 public promotion。

## 6. Tool 可以失效

Tool 失效时 Agent MUST：

1. 记录失败环境；
2. 降级到原始 API / Reflection / JVM；
3. 重新探索；
4. 修复 project Tool；
5. 若 public Tool 也应修订，再走公共修订流程。

Runtime MUST NOT 为 Tool 兼容承担大量 Minecraft 版本分支。

## 7. Skill 可成长但有边界

Skill MUST 可以增加公共 Knowledge / Tool / Loader 与 Minecraft 兼容经验，但项目私有实现永远不能进入公共区域。

## 8. Minecraft 线程语义

- `eval.client` MUST 在 Client Thread 执行实际 Groovy；
- `eval.server` MUST 在可用 Server Thread 执行实际 Groovy；
- 两者 MUST 等执行完成再返回；
- Target 不可用时 MUST 返回结构化错误；
- MUST NOT fallback 到 `eval.raw`。

## 9. Dedicated Server 边界

V0 第一阶段不要求跨 JVM 双 Runtime，但结构 MUST 不阻止未来出现独立 client runtime / server runtime。

不得为此提前实现复杂多目标路由、集群或跨进程管理层。

## 10. 非目标保护

若开发阶段顺手探索出高层能力，可作为 `tools/experimental/` 或项目 Tool 保存，但不得反向成为 Runtime 验收依赖。
