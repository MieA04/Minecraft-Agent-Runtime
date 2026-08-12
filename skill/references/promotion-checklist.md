# Public Promotion Checklist

Require every item before promotion:

- [ ] The source discovery is verified and project-local.
- [ ] The capability was used repeatedly with stable parameters and results.
- [ ] No project-private package, class, field, Mod ID, UI, data, or business dependency remains.
- [ ] Minecraft and Loader applicability ranges are explicit.
- [ ] Thread requirements are explicit.
- [ ] A genuinely independent second environment passed.
- [ ] The second verification ran in a separate process, not merely a repeated Session.
- [ ] Both verification environments and evidence are recorded.
- [ ] Known failures and fallback to raw API/Reflection/JVM are documented.
- [ ] Public source is under `skill/tools/minecraft/` or `skill/tools/loader/` only.
- [ ] Public source contains no private project knowledge path or private identifier.
- [ ] Public knowledge and executable Tool remain separate files.

If any item is false, keep the artifact project-local.
