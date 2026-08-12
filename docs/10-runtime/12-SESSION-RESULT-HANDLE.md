# Session / Result Bridge / Object Handle

## 1. Persistent Session

V0 连续探索必须由真正的 Session 状态实现，不允许每次 RPC 重建上下文。

每个 `RuntimeSession` 独立拥有：

```text
Binding
GroovyShell
vars
vars.tools
HandleRegistry
eval mutex
session metadata
```

Session name 是进程内标识，默认 `default`。

## 2. vars

`vars` 是 Agent 的显式长期状态容器。

要求：

- Session 创建时初始化；
- 多次 eval 间保持；
- Tool namespace 位于 `vars.tools`；
- Agent 需要跨调用保存对象时优先写 `vars`；
- 不依赖单个 Script 局部变量跨 eval 自动存活。

示例：

```groovy
vars.screen = mc.screen
vars.menu = vars.screen.menu
```

后续：

```groovy
vars.menu.slots
```

## 3. Base Binding

固定：

- `vars`
- `ref`
- `runtime`

可选：

- `mc`：Client singleton 可安全解析时提供。

禁止预置：`player`, `screen`, `server`, `inventory`, `world`, `ui`。

### ref

```groovy
ref("@42")
```

语义：只在当前 Session HandleRegistry 查询并直接返回原始 Java Object；不创建代理、不复制。不存在则抛明确异常。

### runtime

只暴露基础设施操作，例如 runtime info、tool reload、handle/session 基础管理。禁止出现 `runtime.inventory`, `runtime.ui`, `runtime.world` 等业务入口。

## 4. Result Bridge 判定算法

按顺序：

1. null -> JSON null
2. Boolean -> boolean
3. Number -> JSON number
4. String/GString/Character -> JSON string
5. List -> 尝试 simple list
6. Map -> 尝试 simple map
7. 其他 -> Handle

### Simple List

只有全部元素递归为 simple value 才直接 JSON 化。任意元素复杂时，**整个 List** 返回 Handle；禁止在未说明的情况下混合“部分 simple + 部分 handle”。Agent 若需要混合结构，应在 Groovy 中显式投影。

### Simple Map

只有 key 全 String 且 value 全 simple 才 JSON 化，否则整个 Map 返回 Handle。

### 循环

递归检查使用 identity visited set。遇到 cycle 时当前最外层对象改为 Handle，不能 stack overflow 或无限递归。

## 5. Number 特殊值

NaN / Infinity 不能产生非法 JSON。V0 固定表示为字符串：

- `"NaN"`
- `"Infinity"`
- `"-Infinity"`

其余 Number 保持 JSON 数值语义。

## 6. HandleRegistry

必须支持：

- handle -> object
- object identity -> handle

推荐 under Session lock 使用 `HashMap<String,Object>` + `IdentityHashMap<Object,String>`。

Handle：`@1`, `@2`, ...，只在 Session 内唯一。

必须强引用直到 Session clear/reset 或 Runtime shutdown；禁止 WeakReference 导致保存的 Handle 随 GC 随机失效。

## 7. Handle Descriptor

固定字段：

```json
{"kind":"handle","handle":"@1","type":"fully.qualified.ClassName","string":"..."}
```

`type` 来自实际运行时 class。`string` 只用于观察，不用于恢复对象。

## 8. Session Reset

不是 V0 必须 RPC。若实现 `runtime.session.resetCurrent()`，必须：清 vars/tools/handles，重装基础 Binding，不影响其他 Session，不重启 RPC。

## 9. 线程安全

同一 Session 的 raw/client/server eval 必须严格串行，mutex 覆盖执行、输出捕获、Result Bridge 完成。

## 10. Runtime Restart

重启后：Session/Handle/vars 全失效；项目 Tool/Knowledge 保留。Agent 必须重新连接和 reload，旧 Handle 不可继续使用。
