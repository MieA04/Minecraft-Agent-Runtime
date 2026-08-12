# Project Record Formats

## 1. Discovery 模板

```markdown
# <ID> - <Title>

## Status
verified | partial | invalidated

## Goal

## Environment
- Minecraft:
- Loader:
- Loader Version:
- Java:
- Mod:
- Runtime Version:

## Execution Target
raw | client | server

## Starting Object

## Hypothesis

## Observations

## Valid Calls

## Invalid Calls

## Result

## Verification

## Dependencies

## Tool Candidate
none | <path>

## Promotion Candidate
no | maybe | yes-after-cross-project-validation

## Related
- knowledge:
- tools:
- discoveries:
```

规则：只有 verified 才能被稳定 Knowledge 当事实引用；partial 用不确定语气；invalidated 保留用于防止重复踩坑；Handle ID 不能作为永久对象 identity。

## 2. project.md 模板

```markdown
# Project Knowledge
## Identity
## Runtime Integration
## Stable Project Structures
## Project Tools
## Known Version-Sensitive Areas
## Open Questions
```

稳定结构每条应链接 discovery，不复制 Tool source。

## 3. minecraft.md 模板

```markdown
# Minecraft Knowledge — Project Local
## Environment
## Client
## Screen / Menu
## Inventory
## Server
## Version Notes
## Evidence
```

这里可以有 Inventory 章节，因为它是探索产生的项目侧知识；这不意味着 Runtime 提供 Inventory API。

## 4. loader.md 模板

```markdown
# Loader Knowledge — Project Local
## Environment
## Lifecycle
## Events
## Thread / Executor Notes
## Version Notes
## Evidence
```

## 5. Project Tool Header

```groovy
/*
MAR Tool
Path: minecraft/inventory.groovy
Status: project-verified
Minecraft: <version/range>
Loader: <if relevant>
Depends-On-Project-Code: false
Evidence:
- knowledge/discoveries/<id>.md
*/
```

`mod/` Tool 必须 `Depends-On-Project-Code: true`。

## 6. Public Tool Header

```groovy
/*
MAR Public Tool
Scope: minecraft | loader
Status: public-verified
Minecraft: <range>
Loader: <range if applicable>
Cross-Project-Verification:
- <environment A>
- <environment B>
Source Discoveries:
- <references>
*/
```

Public Tool 不得省略验证范围。

## 7. Experiment

自由格式，但顶部至少写 date、goal、session、target、related discovery（若有）。Experiment 不是稳定事实来源。
