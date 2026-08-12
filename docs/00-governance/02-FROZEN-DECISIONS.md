# MAR V0 实现冻结决策

以下是 V0 基线未完全规定、但为了消除 Agent 自由发挥而冻结的实现选择。变更必须走 Change Control。

## FD-001 RPC Transport

- TCP；
- loopback；
- host 默认 `127.0.0.1`；
- port 默认 `0`，由 OS 分配；
- UTF-8；
- NDJSON framing；
- 一 request 一 response；
- 不做 HTTP/WebSocket/JSON-RPC 2.0/batch/notification。

## FD-002 RPC Method Surface

V0 对 Agent 暴露的执行方法只能是：

- `eval.raw`
- `eval.client`
- `eval.server`

不得新增 `inventory.*`、`ui.*`、`player.*`、`world.*` 等业务 RPC。

## FD-003 Session

- 命名 Persistent Session；
- request 可带 `session`，默认 `default`；
- 未知 session 首次引用时自动创建；
- RPC connection 断开不销毁 Session；
- Session 生命周期默认等同 Runtime 进程；
- Session 内独立 Binding、`vars`、HandleRegistry、GroovyShell、eval mutex；
- 同 Session eval 串行；不同 Session 可并发发起。

## FD-004 Object Handle

- Handle 格式 `@<decimal>`，从 `@1` 开始；
- Session scoped；
- strong reference；
- identity-based，不使用 `equals()`；
- 同一对象 identity 重复返回同一 handle；
- Runtime restart 后全部失效；
- 不跨进程序列化 Object。

## FD-005 Result Bridge

直接 JSON 化仅：

- null
- boolean
- number
- string
- 递归 simple List
- String key + simple value 的 Map

其他对象一律 Handle。

禁止自动展开 Bean/getter/fields/Minecraft Object，也禁止截断后伪装成完整结果。

## FD-006 Runtime 安装位置

权威模板：`skill/assets/runtime-template/`

项目副本：`.minecraft-agent-runtime/runtime/bootstrap/`

Runtime Java 源码不得散落到项目业务 package。构建必须把 bootstrap source 目录作为额外 Java source directory 编译。

## FD-007 Project Root Resolution

唯一顺序：

1. `-Dmar.projectRoot=...`
2. 从 `user.dir` 向父目录查找 `.minecraft-agent-runtime/`
3. 找不到则启动失败

不得在不确定目录自动创建新工作区。

## FD-008 Base Binding

每 Session MUST 有：

- `vars`
- `ref`
- `runtime`

`mc` 仅在 Client singleton 可安全解析时提供。

MUST NOT 预置：`player`, `screen`, `server`, `inventory`, `world`, `ui`。

## FD-009 Tool Namespace

只从项目 `.minecraft-agent-runtime/tools/` 加载。

例如：

`tools/minecraft/inventory.groovy` -> `vars.tools.minecraft.inventory`

每个 Tool script MUST return 一个对象。reload 成功后原子替换；失败保留旧版本。

## FD-010 Experimental Tool

`tools/experimental/` 不参与 stable reload-all，只能显式加载或直接 eval。

## FD-011 Runtime Discovery

Runtime ready 后原子写：`.minecraft-agent-runtime/state/runtime.json`。

Agent 通过 state file 找端口，不把固定端口扫描作为主机制。

## FD-012 Eval Timeout

V0 不强制默认执行超时，也不承诺安全取消卡死脚本。不得自行增加 5s/30s 等默认 timeout 改变任意 JVM 执行语义。

## FD-013 Output Capture

必须捕获：

- Groovy `print/println`；
- 当前 eval 执行线程的 `System.out/err`。

不承诺捕获脚本新建后台线程或其他 Minecraft 线程日志。实现应使用 thread-local routing，而非每次 eval 粗暴全局替换输出流。

## FD-014 Public vs Project Tool

Public Tool 权威位置：

- `skill/tools/minecraft/`
- `skill/tools/loader/`

Runtime 不直接加载 Skill 目录。公共 Tool 使用时由 Skill 复制/适配到当前项目 `tools/` 后再 reload。

## FD-015 文档不可合并

以下必须独立：Runtime Architecture、RPC、Session/Handle/Result、Thread、Tool、Skill Contract、Installation、Exploration、Knowledge Promotion、Project Workspace、Test/Acceptance、Agent Rules、Change Control。

`SKILL.md` 只做入口和导航，禁止膨胀为单一大文档。
