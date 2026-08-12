# Knowledge Governance Reference

Use this lifecycle without skipping the project stage:

```text
temporary experiment → verified discovery → project knowledge → repeated use
→ project tool → universality review → independent second environment
→ public knowledge or public tool
```

Keep any artifact project-local when it references the current project's package, class, field, method, Mod ID, custom UI, data structure, or business behavior. Put detailed evidence in discoveries, concise stable facts in the correct knowledge file, and reusable project code in `tools/mod/`.

Public code may exist only under `skill/tools/minecraft/` or `skill/tools/loader/`. Parameterizing a private class does not make a Tool universal. Scope every version-sensitive statement and record known failure/fallback behavior.

Before promotion, execute every check in [promotion-checklist.md](promotion-checklist.md).
