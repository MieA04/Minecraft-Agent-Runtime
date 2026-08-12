# MAR V0 RPC Protocol

## 1. Transport

- TCP loopback
- host `127.0.0.1`
- port dynamic by default
- UTF-8
- NDJSON: one complete JSON object per line
- request/response 不跨行
- JSON 内部换行必须 escape

Session 与 TCP connection 生命周期解耦。

## 2. Request

```json
{"id":"req-1","session":"default","method":"eval.raw","code":"1 + 2"}
```

### id

MUST String；客户端生成；server 原样回传；只用于 correlation。

### session

OPTIONAL String；默认 `default`；空字符串非法；未知 name 自动创建。

### method

只能：`eval.raw`, `eval.client`, `eval.server`。其他返回 `METHOD_NOT_FOUND`。

### code

MUST String；不做业务过滤；按 Groovy Script 执行。

## 3. Success Response

```json
{"id":"req-1","ok":true,"result":3,"stdout":"","stderr":""}
```

`result` 即使为 null 也必须显式返回。

## 4. Handle Result

```json
{
  "id":"req-2",
  "ok":true,
  "result":{"kind":"handle","handle":"@42","type":"net.minecraft.client.gui.screens.Screen","string":"InventoryScreen@..."},
  "stdout":"",
  "stderr":""
}
```

`kind=handle` 避免普通 Map 与 descriptor 混淆。

`string` 来自安全 `String.valueOf()`；若 `toString` 抛异常，返回 `<toString failed: Type>`，不得让 Result Bridge 整体失败。

## 5. Error Response

```json
{
  "id":"req-3",
  "ok":false,
  "result":null,
  "stdout":"...",
  "stderr":"",
  "error":{"code":"EVAL_EXCEPTION","type":"java.lang.IllegalStateException","message":"...","stack":"..."}
}
```

异常前输出必须保留，stack 必须完整，单次 error 不关闭 Session/RpcServer。

## 6. Protocol Error

- invalid JSON -> `INVALID_JSON`, id null
- missing/invalid fields -> `INVALID_REQUEST`
- unknown method -> `METHOD_NOT_FOUND`
- unavailable client/server -> `TARGET_UNAVAILABLE`
- script/ref/compile/runtime exception -> `EVAL_EXCEPTION`
- unexpected Runtime infra failure -> `RUNTIME_INTERNAL_ERROR`

## 7. eval.raw

在当前 Runtime worker thread 执行，不自动调度。适合 Reflection、Class 查询、Runtime 内省等。

## 8. eval.client

流程：

1. obtain Session mutex
2. create eval task
3. submit Client Thread
4. target thread installs output capture
5. execute Groovy
6. bridge result
7. complete future
8. worker waits
9. send response

Client Target 不存在 -> `TARGET_UNAVAILABLE`。

## 9. eval.server

V0 第一阶段保证 Integrated Server：解析 integrated server、提交其 executor、等待完成。不存在则 `TARGET_UNAVAILABLE`，不得 fallback raw/client。

## 10. 并发

- connection 可长期保持；
- 不同 connection 可并发；
- 同 Session 串行；
- 不同 Session 可并发请求；
- client/server 最终仍由 Minecraft executor 排队。

V0 不要求 pipelined out-of-order response，最简单实现可以每连接同步 request-response。

## 11. 明确不属于 V0

不加 auth、permission、event subscription、binary frame、file upload、remote object proxy、RPC reflection API、transaction、cancel、timeout negotiation、cluster routing。
