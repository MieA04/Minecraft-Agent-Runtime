# Runtime Architecture

## 1. 目标

Project Runtime 是嵌入目标 Minecraft 开发 JVM 的最小动态执行基础设施。它必须保证：Agent 可连接、Groovy 可连续执行、JVM 对象可持续引用、结果可反馈、Minecraft 线程可选择、项目 Tool 可热更新。

## 2. Java 包结构

```text
mar.runtime
├── bootstrap/MinecraftAgentRuntime
├── host/RuntimeHost
├── config/{RuntimeConfig,ProjectRootResolver}
├── groovy/GroovyRuntime
├── session/{RuntimeSession,SessionManager}
├── rpc/{RpcServer,RpcConnection,RpcRequest,RpcResponse}
├── result/{ResultBridge,HandleRegistry,HandleDescriptor}
├── thread/{RawExecutionTarget,ClientExecutionTarget,ServerExecutionTarget}
├── tool/{ToolManager,ToolLoadResult}
├── io/{EvalOutputCapture,ThreadLocalPrintRouter}
└── state/RuntimeStateWriter
```

这是职责边界。实际类数量可微调，但不得把业务 Tool 混入这些 package。

## 3. Bootstrap

`MinecraftAgentRuntime` 允许：

- `start()`；
- 防重复启动；
- 调用 ProjectRootResolver；
- 创建 RuntimeHost；
- shutdown hook。

禁止：RPC parsing、Groovy 业务脚本、Handle 逻辑、Tool 内容、Minecraft 业务查询、Knowledge 写入。

## 4. RuntimeHost

RuntimeHost 是基础设施组合根，持有 Config、GroovyRuntime、SessionManager、RpcServer、ToolManager、Execution Targets、StateWriter。

固定启动顺序：

1. resolve root
2. read config
3. init output router
4. init Groovy classloader
5. init ToolManager
6. init SessionManager
7. create default Session
8. init execution targets
9. start RPC
10. write state `ready`

关闭按反向顺序。

## 5. GroovyRuntime

负责：

- 以当前 Minecraft/Mod 有效 ClassLoader 为 parent 创建 `GroovyClassLoader`；
- 创建 Session 的 Binding/GroovyShell；
- compile/execute script。

Parent ClassLoader 顺序：

1. Thread context ClassLoader；
2. `MinecraftAgentRuntime.class.getClassLoader()`；
3. 都不可用则启动失败。

禁止创建沙箱 ClassLoader 或 API 白名单。

## 6. SessionManager / RuntimeSession

SessionManager 只维护 name -> RuntimeSession。

RuntimeSession 独立拥有：Binding、`vars`、`vars.tools`、Shell、HandleRegistry、eval mutex、metadata。

同 Session 的 `eval.*` 必须从执行开始到 Result Bridge/output capture 完成全程持锁。

## 7. RpcServer

只负责：bind loopback、accept、NDJSON framing、JSON decode/encode、dispatch、response write。

不得理解 Inventory/UI/World 等业务。

## 8. ResultBridge

只负责 simple value 或 HandleDescriptor。不得为了方便 Agent 反射展开对象。

## 9. Execution Targets

- raw：RPC worker；
- client：Minecraft Client Thread；
- server：Integrated Server Thread（V0）。

它们只决定执行线程，不做权限过滤或业务适配。

## 10. ToolManager

只读取项目 `.groovy` Tool、建立 path namespace、reload/replace `vars.tools`。

不负责判断 public promotion。

## 11. Output Capture

当前 eval 的 stdout/stderr 必须与 Minecraft 其他线程日志区分。不得把整个 JVM 同期日志全部归入当前 response。

## 12. 启动幂等

同 JVM 重复 `start()`：

- ready -> 返回现有 host；
- starting -> 复用同一启动；
- failed -> 清理半初始化资源后才允许显式重试。

不得开启多个 RpcServer。

## 13. 失败原则

基础设施启动失败必须明确失败，不写假 `ready`。单次 eval 失败不得停止 Runtime。

## 14. 依赖边界

生产 Runtime 允许：JDK、Groovy、必要 JSON module、当前 Minecraft/Loader 编译环境中 thread target 必需部分。

禁止引入 Web framework、DI、DB、MQ、browser automation、UI automation、大型 RPC framework。
