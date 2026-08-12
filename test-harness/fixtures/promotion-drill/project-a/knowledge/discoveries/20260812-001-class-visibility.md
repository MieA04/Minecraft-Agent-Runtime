# Discovery: Active ClassLoader class visibility

- Status: verified
- Date: 2026-08-12
- Project/Mod: clean MAR fixture A
- Minecraft: none (clean fixture)
- Loader: context ClassLoader fixture
- Runtime version: 0.1.0-SNAPSHOT
- Eval target/thread: raw / fixture process main thread

## Hypothesis

The active context ClassLoader can resolve a named class without initializing it and can report a missing class without changing Runtime state.

## Verification Evidence

The Phase 10 promotion drill launches this project Tool in its own JVM, resolves `java.lang.String`, reports a deliberately missing class as absent, and exits successfully.

## Conclusion

The capability is independent of project business code and is suitable for a Loader-scoped project Tool pending a separate-process second environment.

## Knowledge / Tool Follow-up

Project Tool: `tools/loader/class-visibility.groovy`.
