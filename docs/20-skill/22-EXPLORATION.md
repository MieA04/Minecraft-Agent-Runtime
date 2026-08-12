# Agent Exploration Protocol

## 1. 原则

未知能力不通过猜 API 解决，而通过当前 JVM 的真实对象逐步探索。异常、失败调用、线程错误都属于探索数据。

## 2. 探索前固定检查

1. `knowledge/project.md`
2. `knowledge/minecraft.md`
3. `knowledge/loader.md`
4. `knowledge/discoveries/` 中相关结论
5. 项目 `tools/`
6. Public Skill 中适用的 Tool / Knowledge

已有能力先验证，失效再降级。

## 3. 未知对象固定顺序

```text
object
→ class name
→ superclass chain
→ interfaces
→ declared fields
→ declared methods
→ public methods
→ related objects
→ thread requirement
→ small no/low-side-effect call
→ necessary side-effect call
→ verify result/state
→ discovery
```

## 4. Thread Target 选择

- Reflection/Class 查询：优先 `eval.raw`
- Screen/GUI/client state：`eval.client`
- integrated server world/server state：`eval.server`

raw 出现 thread affinity error 时，应换正确 target 后再验证，不能直接断言 API 无效。Discovery 必须记录 thread requirement。

## 5. 一次验证一个假设

禁止一条超长 script 同时查字段、改状态、点击、验证、生成 Tool。探索拆成短 eval，使每一步可观测。

## 6. 保存中间对象

跨调用对象优先 `vars.target=...`；复杂返回可用 Handle。不要无意义重复定位同一对象，除非要验证对象变化。

## 7. 有价值失败

应记录：字段/签名变化、UI child 里不存在目标、线程错误、反射失败、返回结构与预期不同、public Tool 在当前版本失效。

纯语法错误通常无需永久 discovery。

## 8. 成功判定

“没异常”不等于成功。必须证明目标状态/返回与假设一致。

例如拿到 `menu.slots` 只证明存在槽列表，不自动证明某 index 就是结果槽；需要进一步验证 slot identity/behavior/state。

## 9. Discovery 时机

满足任一：解决未知结构、找到可重复操作、发现旧 Tool/API 失效原因、发现线程要求、发现版本差异、发现高概率重复踩坑的错误路径。

## 10. Tool 提取时机

只有操作重复使用、参数/输出清楚、依赖边界已理解、不是一次性片段时才提取 project Tool。

## 11. 能力降级

```text
known tool
→ current raw Minecraft/Loader API
→ Reflection
→ JVM primitives
```

Tool 不支持不能被解释为 MAR 做不到。

## 12. 禁止捷径

- 猜测直接写 Knowledge；
- 旧版本类名直接当当前版本事实；
- 为绕过未知字段修改 Runtime Java；
- 只看 source code 未运行验证却写“verified”；
- 一次失败就把公共 Tool 宣判永久失效。
