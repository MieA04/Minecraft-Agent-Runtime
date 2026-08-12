# Minecraft Thread Execution

## 1. 目的

`eval.client` / `eval.server` 是显式线程语义，不是权限系统。

## 2. Raw

`eval.raw` 在 RPC worker 执行：不检查业务线程安全、不自动切 Client Thread。

## 3. Client Target

### 可用性

只有 Runtime 能解析 Minecraft Client 实例和 executor 时可用。Production Runtime 不得在 server-only classpath 上无条件静态加载 client-only class 导致整体启动失败。

推荐：client access 独立封装，延迟加载/反射判断。

### 调度

- 当前已经是 Client Thread -> 直接执行，避免 self-deadlock；
- 否则 -> client executor submit，worker 等 Future。

禁止：target 失败 fallback raw；以“应该是 client thread”为由跳过真实检测；为 UI 业务增加专用 thread RPC。

## 4. Server Target

V0 第一阶段定义为 Integrated Server Target。

- 存在 integrated server -> 使用 server executor；
- 当前已经是该 Server Thread -> 直接执行；
- 不存在 -> `TARGET_UNAVAILABLE`。

## 5. Dedicated Server

未来可让 dedicated server JVM 自己安装 MAR，Agent 分别连接 client/server runtime。当前协议不得写死只能 client JVM，但 V0 不做多目标路由层。

## 6. Output Capture

真正执行 Groovy 的 target thread 安装本次 capture context：raw 在 worker、client 在 Client Thread、server 在 Server Thread。worker 等待期间的日志不得算 script stdout。

## 7. Exception

Target thread 中的脚本 Throwable 转回 RPC error，不能杀 Minecraft executor，也不能吞 stack。JVM fatal error 不属于 V0 恢复承诺。

## 8. Deadlock 最低保护

必须做 current-target-thread detection。同线程直接执行。V0 不做复杂 deadlock detector，也不得把同步返回语义改成 fire-and-forget。

## 9. 验收

不能只验证“没报错”。必须通过 Thread identity/name 或 Minecraft executor identity 证明代码实际运行在 Client Thread / Integrated Server Thread。
