# Rules for the Agent Implementing MAR

本文件直接约束编码 Agent。违反 MUST NOT 即实现偏离。

## 1. 编码前

MUST 阅读 baseline -> invariants -> frozen decisions -> 当前模块 spec -> 检查已有实现 -> 只修改当前 Phase 所需文件。

不得先写代码再用文档解释既成事实。

## 2. 不得替换冻结方案

禁止自行：TCP->HTTP/WebSocket、NDJSON->JSON-RPC、named Session->connection-only、strong Handle->weak、独立 Runtime 目录->业务 package、path namespace->全局污染、三个 eval->业务 RPC、无 timeout->默认 timeout、state discovery->固定端口扫描。

认为有问题只能写 Change Proposal。

## 3. 新生产类必须可归类

只能属于 bootstrap/host/config/groovy/session/rpc/result/thread/tool/io/state。

`InventoryController`, `ScreenScanner`, `PlayerMover` 等不能进入 Runtime。无法归类的类先判定是否根本不应属于 Runtime。

## 4. Demo 不得反向升级 Runtime

Demo 可用 Groovy 查背包/Screen/改对象，但脚本只能在 fixture/project tool/experiment。不得为 Demo 在 Java Runtime 实现高层 API。

## 5. 文档更新

行为变化先找唯一权威 spec；影响 Frozen Decision 必须 Change Control；Skill reference 只同步 Agent-facing surface；禁止多个文档各自维护同一完整事实。

## 6. Project Knowledge 落盘

- 原始过程 -> experiment/discovery
- 项目事实 -> project.md
- Minecraft 事实 -> minecraft.md
- Loader 事实 -> loader.md
- 可复用代码 -> tools/

不得混在同一文档。

## 7. Public Skill 写入

没有第二环境验证，即使“显然通用”也不得 promotion。

## 8. 失败处理

Tool/API 失败：不改 Runtime -> 查环境/target -> raw API -> Reflection -> JVM -> discovery -> 修 Tool。只有失败明确属于基础设施才改 Runtime。

## 9. Installer 最小修改

允许：build dependency、sourceSet、run JVM arg、一个 start hook、`.minecraft-agent-runtime/`。

禁止：重构项目业务、改 package/UI/gameplay、引入无关 framework。

## 10. 不提前做兼容抽象

不得为“以后很多 Loader”提前建多层 adapter factory/plugin SPI/service discovery/version negotiation。只抽象当前确实需要替换的 thread/access 点。

## 11. 不生产化扩张

不得主动加 TLS/auth/permission/sandbox/remote/cluster/DB/telemetry/web dashboard。

## 12. 每 Phase 报告

必须报告：修改文件、实现 spec、tests、未完成项、是否新增非 spec 行为。非 spec 行为应回滚或走 Change Control。

## 13. 架构漂移 Stop Signals

出现以下任一必须停止扩张：新增第 4 个业务 RPC；Bootstrap 出现 Screen/Inventory/World；project.md 变日志；SKILL.md 开始内嵌所有细节；Runtime 出现大量 Minecraft 版本兼容业务分支；同一事实在多个文档维护不同版本。
