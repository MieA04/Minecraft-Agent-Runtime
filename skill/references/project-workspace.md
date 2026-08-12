# Project Workspace Reference

```text
.minecraft-agent-runtime/
├── runtime/{bootstrap,config}/
├── tools/{minecraft,loader,mod,experimental}/
├── knowledge/{minecraft.md,loader.md,project.md,discoveries/}
├── logs/{sessions,experiments}/
└── state/
```

Place artifacts by authority:

- project-specific stable fact: `knowledge/project.md`
- current Minecraft fact: `knowledge/minecraft.md`
- current Loader fact: `knowledge/loader.md`
- full verified exploration evidence: `knowledge/discoveries/<id>.md`
- reusable Groovy capability: `tools/`
- temporary or large raw output: `logs/experiments/`
- machine discovery state: `state/`

Keep each knowledge domain independent. Do not turn `project.md` into an append-only session log. Installation and upgrade must not overwrite existing Knowledge, Tools, or Logs.
