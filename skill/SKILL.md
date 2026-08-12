---
name: minecraft-agent-runtime
description: Install, connect, operate, and evolve the Minecraft Agent Runtime (MAR) in editable Minecraft Mod development projects. Use when Codex needs to inject MAR into a supported NeoForge Gradle project, connect through state-discovered NDJSON RPC, evaluate Groovy on raw/client/integrated-server threads, hot-reload project Groovy tools, record verified project discoveries, or promote validated Minecraft/Loader knowledge and tools without leaking project-private code.
---

# Minecraft Agent Runtime

Use MAR as dynamic execution infrastructure. Keep inventory, UI, movement, combat, world queries, and other high-level Minecraft behavior in project Tools rather than Java Runtime APIs.

## Entry flow

1. Locate the target project and inspect `.minecraft-agent-runtime/`.
2. For an uninstalled project, read [installation.md](references/installation.md) and run the installer only after detection succeeds.
3. For an installed project, read `state/runtime.json` and connect; reject stale state that cannot accept a connection.
4. Read project Knowledge and Tools before exploring.
5. Prefer a known Tool, but verify it in the current environment.
6. When a Tool fails or the capability is unknown, read [exploration.md](references/exploration.md) and descend to raw API, Reflection, then JVM primitives.
7. Record verified evidence using [knowledge-workflow.md](references/knowledge-workflow.md).
8. Extract repeated, bounded capability into a project Tool.
9. Promote only after every item in [promotion-checklist.md](references/promotion-checklist.md) passes.

## Installation and connection

- Install: `python scripts/install_mar.py --project-root <project>`
- Wait and smoke-test: `python scripts/mar_rpc.py --state <project>/.minecraft-agent-runtime/state/runtime.json --smoke-mod-class <fqcn>`
- Run live V0 checks: `python scripts/mar_acceptance.py --help`
- Runtime/API details: [runtime-api.md](references/runtime-api.md)
- Workspace placement: [project-workspace.md](references/project-workspace.md)
- Public Loader Tool scopes: [public-loader-tools.md](references/public-loader-tools.md)

## Bundled resources

- Runtime authority template: `assets/runtime-template/`
- Project workspace template: `assets/project-workspace/`
- Public Minecraft Tools: `tools/minecraft/`
- Public Loader Tools: `tools/loader/`

## Hard boundaries

- Never write project-private discoveries into public Skill resources.
- Never overwrite project Tools, Knowledge, or Logs during install/upgrade.
- Never promote a first successful call directly to public scope.
- Never bypass client/server thread semantics.
- Never interpret a failed Tool as a limit of MAR itself.
- Never add business RPC methods beyond `eval.raw`, `eval.client`, and `eval.server`.
