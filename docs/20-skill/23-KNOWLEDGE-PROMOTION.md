# Knowledge / Tool Promotion Governance

## 1. 固定生命周期

```text
temporary experiment
→ verified discovery
→ project knowledge
→ repeated use
→ project tool
→ universality review
→ cross-project verification
→ public knowledge / public tool
```

不得跳过 Project 阶段。

## 2. Project-only 判定

满足任一即默认项目专属：引用当前项目 package/class/field/method/Mod ID、自定义 UI/数据结构/业务逻辑、只在一个项目验证。

对应位置：稳定摘要 -> `knowledge/project.md`；详细证据 -> `knowledge/discoveries/`；复用代码 -> `tools/mod/`。

## 3. 项目侧 Minecraft / Loader Knowledge

`knowledge/minecraft.md` 只表示当前项目实际 MC 环境中验证的 Vanilla/client/server 事实，必须带 Minecraft version、验证时间、discovery 证据。

`knowledge/loader.md` 同理，只记录当前 Loader 环境事实，带 Loader/Minecraft version 和证据。

它们都不是公共 Skill。

## 4. Public Promotion 硬条件

全部满足：

1. 无项目私有 package/class/field/Mod ID dependency；
2. 明确适用范围；
3. 来源是 verified project discovery；
4. 不是第一次偶然成功；
5. 至少有一个独立第二环境验证；
6. 第二验证不是同一进程重复执行；
7. 记录验证环境；
8. Tool 有已知失败/降级说明；
9. 版本敏感事实写明确 version scope。

第二环境可以是另一个独立项目或不含原项目业务代码的 clean fixture。

## 5. Public Tool 分类

只能：

- `skill/tools/minecraft/`
- `skill/tools/loader/`

不得出现 `skill/tools/mod/` 或 `project/`。

## 6. Public Knowledge 与 Public Tool 分离

Knowledge 文档写能力、适用范围、线程、验证环境、失败边界、Tool path。Groovy 源码单独放 `skill/tools/...`。禁止大段源码内嵌文档作为唯一副本。

## 7. Public Tool 修订

新项目发现 public Tool 失效：项目 discovery -> 项目侧修复 -> 验证 -> 第二环境验证 -> 修改 public Tool -> 更新范围。禁止第一次失败就直接为了当前项目改 public source of truth。

## 8. 废弃与版本

旧知识可保留并标记 version range。新版本新增对应记录/Tool variant。不得把庞大版本兼容逻辑下沉 Runtime。

## 9. 通用性不是“参数化”

把项目 class 变成函数参数并不会自动让 Tool 公共；判断依据是实际适用范围与验证，而不是代码看起来抽象。
