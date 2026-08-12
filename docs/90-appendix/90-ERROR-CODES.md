# MAR V0 Error Codes

| Code | Meaning |
|---|---|
| `INVALID_JSON` | NDJSON line is not valid JSON |
| `INVALID_REQUEST` | JSON is valid but required fields/types invalid |
| `METHOD_NOT_FOUND` | method is not one of three eval methods |
| `TARGET_UNAVAILABLE` | client/server execution target unavailable |
| `EVAL_EXCEPTION` | Groovy compile/runtime/ref exception |
| `RUNTIME_INTERNAL_ERROR` | unexpected Runtime infrastructure failure |

规则：不为每种 Minecraft exception 发明 RPC code；真实 exception type/message/stack 放 detail。错误码属于 Runtime transport 语义，不属于 Minecraft 业务语义。
