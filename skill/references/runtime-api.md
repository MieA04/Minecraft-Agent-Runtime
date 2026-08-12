# Runtime API Reference

## RPC

Use TCP loopback, UTF-8, and one JSON object per line.

```json
{"id":"1","session":"default","method":"eval.raw","code":"1+2"}
```

The only methods are `eval.raw`, `eval.client`, and `eval.server`.

```json
{"id":"1","ok":true,"result":3,"stdout":"","stderr":""}
```

Failures retain output and include structured type, message, and stack information. A failed request must not stop the Runtime.

## Sessions and Binding

Named Sessions retain `vars`, handles, and loaded Tool namespaces across connections. Runtime restart invalidates them.

Always available: `vars`, `ref`, `runtime`.

`mc` is added lazily when a Client singleton is safely resolved by `eval.client` or `eval.server`. Never assume `player`, `screen`, `server`, `inventory`, or `world` bindings.

## Handles

Complex objects return a Session-scoped descriptor. Restore the same identity only in that Session:

```groovy
ref("@1")
```

## Tool reload

```groovy
runtime.tools.reloadAllStable()
runtime.tools.reloadPath("minecraft/inventory.groovy")
```

`minecraft/inventory.groovy` maps to `vars.tools.minecraft.inventory`. Stable reload scans only `minecraft`, `loader`, and `mod`; load `experimental` paths explicitly.
