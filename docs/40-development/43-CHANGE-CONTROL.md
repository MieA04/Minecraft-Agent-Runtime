# MAR V0 Change Control

## 1. 必须 Proposal 的修改

V0 目标、Runtime/Skill/Project 边界、RPC transport/method set、Session、Handle、Result Bridge simple 定义、Tool namespace/reload、workspace layout、promotion hard conditions、Bootstrap 职责、acceptance criteria。

## 2. Proposal 模板

```markdown
# MAR Change Proposal <ID>
## Problem
## Current Rule
## Proposed Change
## Why Runtime/Skill/Tool Layer
## Compatibility Impact
## Migration
## Tests
## Rejected Alternatives
```

## 3. 未批准前

允许 prototype/experiment、写 proposal/test；禁止修改 frozen decision 后当“重构”继续、先合代码再补 proposal、用兼容层偷渡新协议。

## 4. 通常无需 Proposal

不改变 observable semantics 的 bug fix、private class rename、加测试、文档错字、保持语义的数据结构优化。

## 5. Runtime 新能力四问

1. 能否 Groovy 临时做？
2. 能否 Project Tool？
3. 是否应 Public Tool？
4. 是否只有 Java Runtime 才能提供？

只有第 4 明确为是才进入 Runtime proposal。

## 6. 批准后同步顺序

governance/frozen -> module spec -> Skill reference -> code -> tests -> traceability。禁止只改代码。
