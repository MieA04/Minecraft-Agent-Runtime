# Installation Reference

## Supported V0 adapter

Use the bundled installer for editable NeoForge ModDevGradle projects using Groovy `build.gradle`. Detect project root, Minecraft version, NeoForge version, Java toolchain, main source set, run task, and exactly one `@Mod` entrypoint before writing.

If detection reports an unsupported build system or ambiguous entrypoint, stop. Do not invent build syntax. Supply `--entrypoint <project-relative-path>` only after inspecting the candidates.

## Install

```text
python scripts/install_mar.py --project-root <project-root>
```

Use `--dry-run` to inspect detection without writing. The JSON result reports the entrypoint class and dev run task.

The installer:

1. creates missing project-workspace files without overwriting existing files;
2. updates managed Runtime bootstrap files from `assets/runtime-template/`;
3. adds manifest-pinned Runtime dependencies and the Runtime Java source directory;
4. adds per-run additional runtime classpath and `mar.projectRoot` configuration;
5. inserts exactly one `MinecraftAgentRuntime.start()` call in the Mod constructor.

Re-running is idempotent. Upgrade may replace managed Runtime files but must preserve project Tools, Knowledge, Logs, config values, and business source beyond the marked start hook.

## Build and launch

Run the detected project-native build first. Then start the reported development run task. Do not treat compilation alone as successful installation.

## Required smoke verification

```text
python scripts/mar_rpc.py \
  --state <project-root>/.minecraft-agent-runtime/state/runtime.json \
  --wait 90 \
  --smoke-mod-class <entrypoint-fqcn>
```

Accept installation only when the script connects to the dynamic loopback port, returns `3` for `1+2`, and resolves the current Mod class through the live Runtime ClassLoader.
