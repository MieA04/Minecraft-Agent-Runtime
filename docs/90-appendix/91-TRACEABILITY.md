# V0 Baseline Traceability

| V0 Section | Detailed Spec |
|---|---|
| §1 Project Positioning | README, INVARIANTS |
| §2 Core Idea | INVARIANTS, EXPLORATION |
| §3 Architecture | REPOSITORY MAP, RUNTIME ARCHITECTURE, SKILL CONTRACT |
| §4 Boundary | INVARIANTS, AGENT RULES |
| §5 Skill vs Runtime | SKILL CONTRACT |
| §6 Installation | INSTALLATION |
| §7 Groovy Runtime | RUNTIME ARCHITECTURE |
| §8 Persistent Session | SESSION-RESULT-HANDLE |
| §9-12 RPC/eval | RPC PROTOCOL, THREAD EXECUTION |
| §13-15 Result/Handle/Output | SESSION-RESULT-HANDLE, RPC |
| §16 Bootstrap | INVARIANTS, RUNTIME ARCHITECTURE |
| §17 Project Directory | PROJECT WORKSPACE |
| §18 Project Tools | TOOL SYSTEM |
| §19 Project Knowledge | PROJECT WORKSPACE, RECORD FORMATS |
| §20 Public Skill | SKILL CONTRACT |
| §21 Promotion | KNOWLEDGE PROMOTION |
| §22 Exploration | EXPLORATION |
| §23 Degradation | EXPLORATION |
| §24-25 Skill Duties | SKILL CONTRACT, AGENT RULES |
| §26 Runtime API | RPC PROTOCOL |
| §27 Binding | SESSION-RESULT-HANDLE |
| §28 Hot Reload | TOOL SYSTEM |
| §29 Experience Record | RECORD FORMATS |
| §30 Project/Public Boundary | KNOWLEDGE PROMOTION |
| §31 Lifecycle | SKILL CONTRACT, IMPLEMENTATION PLAN |
| §32 Acceptance | TEST AND ACCEPTANCE |
| §33 Non-goals | INVARIANTS |
| §34 Principles | INVARIANTS |
| §35 Final Architecture | REPOSITORY MAP |
| §36 One-line Definition | README |

## 本详细设计新增、但基线未直接固定的内容

全部集中记录在 `02-FROZEN-DECISIONS.md`：TCP+NDJSON、dynamic port/state discovery、named Session、strong Handle、simple List/Map 精确定义、Tool path namespace、Runtime sourceSet 存放方式、thread-local output capture、第二环境 promotion 硬条件。

这些是为了避免实现 Agent 自由发挥，不伪装成基线原文已有结论。
