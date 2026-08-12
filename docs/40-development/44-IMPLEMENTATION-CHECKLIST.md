# MAR V0 Implementation Checklist

## Governance
- [ ] baseline 未修改
- [ ] invariants 未违反
- [ ] frozen decisions 未绕过
- [ ] 新行为有唯一权威文档
- [ ] 没有重复 source of truth

## Runtime
- [ ] Bootstrap 极薄
- [ ] 无 Minecraft 高层 API
- [ ] Groovy parent ClassLoader 正确
- [ ] Session 持续且隔离
- [ ] 同 Session eval 串行
- [ ] Handle session-scoped strong reference
- [ ] Result Bridge 不展开复杂对象
- [ ] stdout/stderr 对应正确 request
- [ ] client/server 不 fallback raw
- [ ] eval 失败不杀 Runtime

## RPC
- [ ] loopback TCP
- [ ] NDJSON
- [ ] 只有三个 eval method
- [ ] id 原样回传
- [ ] invalid request 有结构化错误
- [ ] state port 与实际一致

## Tool
- [ ] project tool 独立目录
- [ ] path -> namespace 唯一
- [ ] broken reload 保留旧版本
- [ ] experimental 不 stable-load
- [ ] Tool 逻辑未塞 Java Runtime

## Knowledge
- [ ] 原始探索写 discovery
- [ ] project/minecraft/loader 分开
- [ ] project.md 没变日志
- [ ] promotion 有第二环境
- [ ] public tool 无私有依赖

## Installer
- [ ] Runtime source 独立在 MAR workspace
- [ ] 未覆盖 project knowledge/tools
- [ ] start hook 仅一次
- [ ] build 修改最小
- [ ] smoke RPC 真正成功

## Tests
- [ ] unit
- [ ] integration
- [ ] architecture boundary
- [ ] failure path
- [ ] 未通过非目标能力绕过测试
