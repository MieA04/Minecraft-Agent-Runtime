# MAR V0 Implementation Plan

开发按 Phase 推进。前一阶段未验收，不进入下一阶段；禁止同时顺手开发高层自动化。

## Phase 0 — Repository & Governance

Deliverables：完整目录、baseline、invariants、frozen decisions、docs map、Skill skeleton、Runtime source skeleton、workspace template、test harness skeleton。

Exit：三存储域和文档 source of truth 无冲突。

禁止写 Inventory/UI API 或公共 MC Tool。

## Phase 1 — Bootstrap + Root + Config

实现：start/idempotence、RuntimeHost lifecycle、ProjectRootResolver、RuntimeConfig、RuntimeStateWriter 基础、sourceSet 可编译。

测试：property root、parent search、root missing fail、config default/invalid schema、state atomic write。

## Phase 2 — Groovy + Session

实现：GroovyClassLoader parent、SessionManager/RuntimeSession、Binding、vars、runtime facade、optional mc resolver。

测试：1+2、vars 跨调用、Session 隔离、可加载 harness class、unknown session auto create。

## Phase 3 — Result + Handle

实现：simple values、List/Map recursion、cycle detection、HandleRegistry、ref、descriptor、exception stack。

测试：primitives/nested simple/non-string map key/complex list/cycle/same identity/equal-not-identical/cross-session/restart invalidation。

## Phase 4 — Output Capture

实现：Groovy print/println、thread-local System.out/err route、context cleanup。

测试：stdout/stderr、异常前输出、并发 Session 不串、unrelated thread 不进入 response。

## Phase 5 — RPC

实现：ServerSocket、dynamic port、NDJSON、validation、eval.raw、response/error、state ready。

测试：persistent connection、reconnect same Session、invalid JSON/request/method、multiline code、1 request 1 response。

## Phase 6 — Client/Server Targets

实现：ClientExecutionTarget、IntegratedServerExecutionTarget、current-thread detection、unavailable error、eval.client/server。

Minecraft 测试：client thread identity、server thread identity、no world -> unavailable、client-only class 不在 server-like 环境 eager load。

## Phase 7 — Tool Loader

实现：vars.tools、path namespace、reloadPath、reloadAllStable、atomic replace、delete cleanup、experimental exclusion。

测试：v1/v2 hot reload、broken v2 retains v1、delete removes namespace、experimental ignored、deterministic mapping。

## Phase 8 — Installer Skill

实现：detect project、dependency、sourceSet、copy Runtime、workspace、start hook、launch/read state/smoke eval。

Exit：从未安装项目自动做到 `eval.raw("1+2")` 且 current Mod class visible。

## Phase 9 — Knowledge Workflow

实现 project templates、discovery workflow、Tool header、promotion checklist、Skill references。

这里实现的是工作流，不是自动知识推理引擎。

## Phase 10 — Full Acceptance

跑全部 P0 acceptance + architecture boundary tests。不得通过新增高层业务 API 绕过测试。

## V0 Done

只有 Phase 0-10 全部通过才完成。能点击背包/自动挖矿从来不是完成标志。
