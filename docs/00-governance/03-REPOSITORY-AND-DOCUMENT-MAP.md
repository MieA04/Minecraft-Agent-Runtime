# MAR 仓库与文档归属地图

## 1. 完整结构

```text
minecraft-agent-runtime/
├── README.md
├── VERSION
├── docs/
│   ├── USAGE.md
│   ├── 00-governance/
│   │   ├── 00-V0-BASELINE.md
│   │   ├── 01-INVARIANTS.md
│   │   ├── 02-FROZEN-DECISIONS.md
│   │   └── 03-REPOSITORY-AND-DOCUMENT-MAP.md
│   ├── 10-runtime/
│   │   ├── 10-RUNTIME-ARCHITECTURE.md
│   │   ├── 11-RPC-PROTOCOL.md
│   │   ├── 12-SESSION-RESULT-HANDLE.md
│   │   ├── 13-THREAD-EXECUTION.md
│   │   ├── 14-TOOL-SYSTEM.md
│   │   └── 15-RUNTIME-STATE-AND-CONFIG.md
│   ├── 20-skill/
│   │   ├── 20-SKILL-CONTRACT.md
│   │   ├── 21-INSTALLATION.md
│   │   ├── 22-EXPLORATION.md
│   │   └── 23-KNOWLEDGE-PROMOTION.md
│   ├── 30-project-workspace/
│   │   ├── 30-PROJECT-WORKSPACE.md
│   │   └── 31-RECORD-FORMATS.md
│   ├── 40-development/
│   │   ├── 40-IMPLEMENTATION-PLAN.md
│   │   ├── 41-TEST-AND-ACCEPTANCE.md
│   │   ├── 42-AGENT-DEVELOPMENT-RULES.md
│   │   ├── 43-CHANGE-CONTROL.md
│   │   └── 44-IMPLEMENTATION-CHECKLIST.md
│   └── 90-appendix/
│       ├── 90-ERROR-CODES.md
│       └── 91-TRACEABILITY.md
├── skill/
│   ├── SKILL.md
│   ├── references/
│   ├── assets/
│   │   ├── runtime-template/
│   │   └── project-workspace/
│   └── tools/{minecraft,loader}/
├── test-harness/
└── fixtures/
```

## 2. 唯一职责

### `docs/USAGE.md`

面向 MAR 使用者的安装、连接、执行、Tool、Knowledge、安全边界和故障排查入口。它可以引用各权威规范，但不得改变或覆盖 V0 基线与冻结决策。

### `docs/00-governance/`

只存基线、不变量、冻结技术决策和文档地图。禁止存项目 discovery、Tool 代码或 UI/Inventory 经验。

### `docs/10-runtime/`

只描述 Java Runtime 基础设施：lifecycle、RPC、Session、Handle、Result、Thread、Tool loader、Config/State。禁止高层 Minecraft 业务能力。

### `docs/20-skill/`

只描述 Skill 行为契约：安装、启动、探索、降级、沉淀、晋升。禁止复制完整 Java 内部设计或项目私有知识。

### `docs/30-project-workspace/`

只定义真实项目 `.minecraft-agent-runtime/` 应该长什么样，不存真实项目数据。

### `docs/40-development/`

只服务 MAR 自身开发：阶段、测试、验收、Agent 编码规则、变更控制。

### `skill/SKILL.md`

只做 Agent 执行入口、流程分支和 reference 路由。不得内嵌全部 API/Knowledge/Tool/source code/log。

### `skill/references/`

每个主题独立文件。禁止重新合并为 `reference.md`。

### `skill/assets/runtime-template/`

公共 Runtime 唯一权威源码模板。不得在 `docs/` 或其他目录维护第二份可执行源码副本。

### `skill/tools/`

只存已完成公共晋升的 Groovy Tool；项目 Mod Tool 永远不能直接放这里。

## 3. 内容唯一落点

| 内容 | 唯一位置 |
|---|---|
| 用户使用说明 | `docs/USAGE.md` |
| V0 原始目标 | `00-V0-BASELINE.md` |
| Runtime 不变量 | `01-INVARIANTS.md` |
| 冻结技术选择 | `02-FROZEN-DECISIONS.md` |
| RPC wire format | `11-RPC-PROTOCOL.md` |
| Session/Handle/Result | `12-SESSION-RESULT-HANDLE.md` |
| Thread 语义 | `13-THREAD-EXECUTION.md` |
| Tool 加载协议 | `14-TOOL-SYSTEM.md` |
| Skill 安装 | `21-INSTALLATION.md` |
| 探索方法 | `22-EXPLORATION.md` |
| 公共晋升 | `23-KNOWLEDGE-PROMOTION.md` |
| 项目私有稳定事实 | 目标项目 `knowledge/project.md` |
| 单项探索证据 | 目标项目 `knowledge/discoveries/*.md` |
| 项目 Groovy Tool | 目标项目 `tools/` |
| Public Groovy Tool | `skill/tools/` |
| 运行状态 | 目标项目 `state/` |
| 临时大输出 | 目标项目 `logs/experiments/` |

## 4. 明确禁止的便利性合并

- 把 RPC + Session + Tool + Knowledge 合成一个 `architecture.md`；
- 把项目 `minecraft.md + loader.md + project.md + discoveries` 合成 `knowledge.md`；
- 把 public Tool 源码粘进 `SKILL.md` 作为唯一副本；
- 把 session log 当 Knowledge；
- 把 Runtime Java 直接写进项目 Mod 主类；
- 把所有 Tool 合成 `tools.groovy`；
- 把所有 discovery 永久追加在 `project.md`。

文件分离本身就是架构要求，不是排版偏好。
