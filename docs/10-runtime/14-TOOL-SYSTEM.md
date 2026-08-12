# Project Groovy Tool System

## 1. 定位

Tool 是 Agent 探索后形成的可复用 Groovy 能力，不属于 Bootstrap API。

项目 Tool：

```text
.minecraft-agent-runtime/tools/
├── minecraft/
├── loader/
├── mod/
└── experimental/
```

## 2. 分类

### minecraft

依赖 Minecraft 原版结构、已在当前项目验证。即使看起来通用，首次形成仍先留项目侧。

### loader

依赖当前 Loader。

### mod

依赖当前项目 class/package/Mod ID/自定义 UI/数据结构/业务逻辑。

### experimental

未稳定、只验证一次或仍在探索，不参与 stable reload-all。

## 3. 文件粒度

一个 Tool 文件对应一个清晰能力域：

```text
minecraft/inventory.groovy
minecraft/screen.groovy
loader/neoforge-events.groovy
mod/rune-screen.groovy
```

禁止 `all.groovy`, `misc.groovy`, 超大 `common.groovy` 将无关能力混在一起。

## 4. Tool 文件协议

Tool script 在独立临时加载上下文编译/执行，MUST `return` 一个对象。推荐 Map/Closure 集合。

```groovy
return [
  find: { inventory, predicate ->
    inventory.items.withIndex().findAll { stack, index -> predicate(stack) }
  }
]
```

路径：

`tools/minecraft/inventory.groovy`

namespace：

`vars.tools.minecraft.inventory`

调用：

```groovy
vars.tools.minecraft.inventory.find(mc.player.inventory) { !it.isEmpty() }
```

## 5. 加载阶段禁止业务副作用

Tool 顶层用于定义能力，不应在 load 时直接点击 UI、丢物品、修改世界等。load = define；invoke = act。

## 6. stable reload

`runtime.tools.reloadAllStable()` 只扫描：minecraft、loader、mod；忽略 experimental。路径字典序处理，保证可预测。

## 7. reloadPath

`runtime.tools.reloadPath("minecraft/inventory.groovy")` 固定步骤：

1. 校验路径位于 tool root；
2. read；
3. compile；
4. execute / get return；
5. 全部成功后 atomic replace namespace；
6. 失败保留旧 namespace；
7. 返回 load result；
8. 无需重启 Minecraft。

## 8. 删除 Tool

stable reload 发现文件已删除，必须移除 namespace，不能保留 ghost tool。

## 9. Namespace 冲突

路径是唯一 namespace。一个路径只对应一个 namespace。禁止运行时让多个文件 merge 到同一全局对象。

## 10. Public Tool

权威位置：`skill/tools/minecraft/` 与 `skill/tools/loader/`。

Runtime 不直接从 Skill 目录加载。Skill 在项目使用时复制到项目 `tools/`，当前项目副本可因环境适配而修改。

Public Tool 失败：先在项目 discovery 中记录和修复，再走公共修订流程。

## 11. Tool load error

必须包含 path、phase(read/compile/init/replace)、exception type/message/stack。失败不得停止 Runtime。

## 12. 禁止演化

Tool System 不得变成 Java plugin framework、DI container、Mod Adapter registry、RPC method registry、版本兼容引擎、bot scheduler。
