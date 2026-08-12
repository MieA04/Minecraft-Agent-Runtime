# Minecraft Agent Runtime V0 — Development Specification Pack

本目录是 **Minecraft Agent Runtime（MAR）V0 的冻结开发规范**，用于直接指导 Agent 实现 MAR，而不是用于描述愿景。

## 权威顺序

发生冲突时按以下顺序处理：

1. `docs/00-governance/00-V0-BASELINE.md`：用户确认的 V0 架构基线。
2. `docs/00-governance/01-INVARIANTS.md`：从基线提取出的不可变约束。
3. `docs/00-governance/02-FROZEN-DECISIONS.md`：为消除实现歧义而冻结的 V0 实现决策。
4. 各模块详细规范。
5. `skill/` 中的 Agent-facing 操作文档与模板。
6. 实际实现代码。

低层文档或代码不得反向修改高层约束。若实现困难，只能记录冲突并提出变更，不得“先做一个更方便的版本”。

## 三个存储域必须分开

### A. MAR 源仓库

存放 V0 基线、开发规范、公共 Skill、Runtime 权威模板、公共 Tool、测试与 fixture。

### B. 目标项目 `.minecraft-agent-runtime/`

属于当前 Minecraft 项目，只存该项目安装的 Runtime 副本、项目 Tool、项目 Knowledge、实验记录和运行状态。

### C. 公共 Skill

位于本仓库 `skill/`，只存 Agent 操作流程、Runtime API 参考、安装规则、探索方法、已完成晋升的公共知识和公共 Tool。

项目私有类名、字段、UI、实验不得进入公共 Skill。

## 开发阅读顺序

1. `01-INVARIANTS.md`
2. `02-FROZEN-DECISIONS.md`
3. `03-REPOSITORY-AND-DOCUMENT-MAP.md`
4. `10-runtime/*`
5. `20-skill/*`
6. `30-project-workspace/*`
7. `40-development/40-IMPLEMENTATION-PLAN.md`
8. `41-TEST-AND-ACCEPTANCE.md`
9. `42-AGENT-DEVELOPMENT-RULES.md`

## Runtime 新能力判断

任何新功能加入 Runtime 前必须回答：

> 如果删掉这个功能，Agent 是否仍能通过 Groovy + JVM 原始能力重新探索得到它？

如果答案是“能”，默认不应加入 Runtime；应作为 Tool 或 Knowledge 演化出来。
