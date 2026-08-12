# Runtime State / Config / Operational Files

## 1. Config

`.minecraft-agent-runtime/runtime/config/runtime.json`

```json
{
  "schema": 1,
  "rpc": {"host":"127.0.0.1","port":0}
}
```

不存在则使用默认；不支持 schema 则启动失败。

Runtime config 只存基础设施配置，禁止加入 inventory/UI/movement/attack 等 Tool 业务配置。

## 2. Runtime State

`.minecraft-agent-runtime/state/runtime.json`

```json
{
  "schema":1,
  "status":"ready",
  "runtimeVersion":"0.1.0",
  "pid":12345,
  "host":"127.0.0.1",
  "port":49152,
  "startedAt":"2026-08-11T12:00:00Z",
  "projectRoot":"D:/code/example-mod",
  "processRole":"client"
}
```

`status`: starting|ready|stopping|stopped|failed；`processRole`: client|server|unknown。

## 3. Atomic Write

state 必须写临时文件、flush/close、atomic/replace 到 `runtime.json`，防止 Agent 读半截 JSON。

## 4. Stale State

异常退出可留下 ready。Skill 必须读 state 后实际连接 host/port；连接失败视为 stale，不得仅凭文件断言 Runtime 在线。

## 5. Shutdown

正常关闭：停止接收新 RPC、清基础资源、更新 stopped、关闭 socket；不得删除 Tool/Knowledge。

## 6. Session Logs

`logs/sessions/` 只存 Runtime 操作日志：start/stop/session create/connection/protocol/tool reload/runtime infra error。

V0 默认不要求永久记录每条 Groovy code/result。Session log 不是 Knowledge。

## 7. Experiments

`logs/experiments/` 可存 Reflection dump、大列表、批量测试输出等临时材料。长期结论必须提炼到 `knowledge/discoveries/`。
