# MAR V0 Test & Acceptance

## 1. 测试层级

- L1 Unit：纯 Java/Groovy 基础设施
- L2 Runtime Integration：真实 Groovy + RPC
- L3 Minecraft Integration：真实 dev instance ClassLoader/Thread
- L4 Skill Installation：从未安装项目自动注入

## 2. P0 Acceptance

### A-001 Groovy Install
目标项目通过 MAR 安装 Groovy 并编译 Runtime。

### A-002 Runtime Start
Minecraft 启动后 state ready，host/port 可连接。

### A-003 Repeat Eval
同进程连续至少 100 次 `1+2` 不重启。

### A-004 ClassLoader
Groovy 可访问 Minecraft、Loader、current Mod、JDK；第三方 Mod class 在实际 classpath 存在时可访问。

### A-005 Session Persistence
`vars.value=41` 后下一 request `vars.value+1` == 42。

### A-006 Session Isolation
A/B Session vars 不污染。

### A-007 Handle
复杂 Object -> handle -> `ref` 恢复同 identity。

### A-008 Simple Result
null/bool/number/string/simple list/map 正确 JSON。

### A-009 Cycle Safety
循环对象不 stack overflow，转 Handle。

### A-010 Output
stdout/stderr 进入正确 response。

### A-011 Exception
ok=false + type/message/stack；Runtime 后续仍可 eval。

### A-012 Client Thread
真实证明 `eval.client` 在 Client Thread。

### A-013 Server Thread
单人世界中真实证明 `eval.server` 在 Integrated Server Thread。

### A-014 Server Unavailable
无 integrated server 时明确失败且不 fallback。

### A-015 Hot Reload
v1 load/use -> 改 v2 -> reload/use，全程不重启 Minecraft。

### A-016 Broken Tool Atomicity
v2 syntax error -> reload error -> v1 仍可用。

### A-017 Knowledge Workspace
安装目录与模板完整，无虚构知识。

### A-018 Promotion Drill
至少演练一次 discovery -> project tool -> second environment -> public tool/reference，public 产物不引用私有项目代码。

## 3. Architecture Boundary Tests

### B-001 RPC Surface
生产 dispatch 只允许三个 `eval.*`。

### B-002 No Runtime Business API
生产 Runtime 不得暴露 inventory/crafting/attack/mining/pathfinding/ui.click/screen.findButton 等业务入口。

### B-003 Thin Bootstrap
人工/静态审查确认只做 orchestration，无 Tool/业务查询。

### B-004 Workspace Segregation
Installer 不 overwrite knowledge、不 delete tools、不把 project knowledge copy 到 skill。

## 4. Failure Injection

覆盖：invalid Groovy、runtime exception、invalid JSON、unknown method、unknown handle、stale state、port bind failure、broken Tool、no client、no server、invalid config schema。

## 5. 性能边界

V0 无生产 SLA，但禁止明显浪费：每 eval 不重建 RuntimeHost/所有 Session；Tool 仅显式 reload 扫描；Result Bridge 不反射展开大对象。

## 6. 明确非验收项

自动背包/合成/挖矿/攻击/路径规划/视觉识别/完整 UI Tree/Mod Adapter/自动测试 DSL/公网调试都不是 V0 验收项。
