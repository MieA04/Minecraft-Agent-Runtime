# Knowledge Workflow

## Create a discovery

Copy `assets/project-workspace/knowledge/discoveries/TEMPLATE.md` to a date/sequence/topic filename. Record one hypothesis, exact environment, target thread, observations, failed calls worth retaining, successful calls, verification evidence, conclusion, and next action.

Mark status `verified` only when evidence proves the conclusion. Source inspection alone is not runtime verification.

## Update stable project knowledge

Link the discovery from exactly one primary stable knowledge domain:

- project-private behavior → `project.md`
- Vanilla/client/server behavior in this environment → `minecraft.md`
- Loader lifecycle/events/executor behavior → `loader.md`

Write a concise fact, version scope, verification date, and discovery path. Keep raw output in logs or the discovery.

## Extract a project Tool

Create one `.groovy` file per capability under `tools/minecraft`, `tools/loader`, or `tools/mod`. Begin with this header:

```groovy
/*
MAR Tool
Path: <category/path.groovy>
Status: project-verified
Minecraft: <version/range>
Loader: <loader/range or n/a>
Depends-On-Project-Code: <true|false>
Evidence:
- knowledge/discoveries/<id>.md
*/
```

Return one non-null object. Keep load-time code declarative; perform side effects only when an exported closure/method is invoked. Test v1, hot reload, and a broken replacement that must retain v1.

## Promote

Run the independent second-environment check in a different process or project. Copy the project Tool into public scope only after the checklist passes. Write public scope, environments, evidence, thread requirements, failures, and fallback in a separate reference; do not embed project-private evidence paths into public code.
