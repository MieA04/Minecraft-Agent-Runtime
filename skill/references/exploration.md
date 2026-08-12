# Exploration Reference

Before exploring, read project knowledge in this order: `project.md`, `minecraft.md`, `loader.md`, relevant discoveries, project Tools, then applicable public Tools.

Explore an unknown object incrementally:

```text
object → class → superclass/interfaces → fields → methods → related objects
→ thread requirement → low-side-effect call → verify result → discovery
```

Use `eval.raw` for JVM/Reflection, `eval.client` for client state, and `eval.server` for integrated-server state. When raw encounters thread affinity, retry on the correct target before concluding the API is invalid.

Test one hypothesis per short eval. Save intermediate objects in `vars` or handles. Treat signature changes, thread errors, unexpected structure, and failed public Tools as useful evidence; ordinary syntax mistakes do not need permanent records.

Do not call an exploration successful merely because it did not throw. Verify the returned value or changed state. Create a discovery when the evidence resolves an unknown structure, version difference, recurring failure, thread requirement, or reusable operation.

Extract a project Tool only after repeated use, clear parameters/results, and understood dependencies. On Tool failure, descend through current API, Reflection, and JVM primitives rather than changing Runtime Java.
