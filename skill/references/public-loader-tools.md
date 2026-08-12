# Public Loader Tools

## `loader/class-visibility.groovy`

Resolve a class without initialization through the current thread context ClassLoader, or test whether it is present. Use it for live classpath discovery before deeper Loader/Mod reflection.

- Scope: MAR-supported Java development runtimes; no project-private dependency.
- Thread: `eval.raw` is sufficient for class visibility. Switch targets before invoking thread-affine resolved APIs.
- Verification environment A: independent clean runtime fixture process, Java 21, Groovy 4.0.24.
- Verification environment B: independent clean runtime fixture process, Java 21, Groovy 4.0.24.
- Known failure: a class can be present but fail linkage because one of its dependencies is unavailable; `isPresent` returns false for `LinkageError`.
- Fallback: call `Class.forName` directly and inspect the exception or use JVM ClassLoader resources.
