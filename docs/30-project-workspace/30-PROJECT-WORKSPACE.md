# Target Project `.minecraft-agent-runtime/` Specification

## 1. 完整结构

```text
.minecraft-agent-runtime/
├── runtime/
│   ├── bootstrap/
│   │   ├── src/main/java/mar/runtime/...
│   │   └── INSTALL-MANIFEST.json
│   └── config/runtime.json
├── tools/
│   ├── minecraft/
│   ├── loader/
│   ├── mod/
│   └── experimental/
├── knowledge/
│   ├── minecraft.md
│   ├── loader.md
│   ├── project.md
│   └── discoveries/
├── logs/
│   ├── sessions/
│   └── experiments/
└── state/runtime.json
```

## 2. runtime/

受 MAR 安装/升级管理。

`bootstrap/` 是项目安装的 Runtime 副本，可在项目内实验修改，但通用改进不能自动回写 public；必须走 Change Control/Promotion。

`config/` 只存 Runtime infrastructure config，不存 Tool 业务配置。

## 3. tools/

- `minecraft/`：当前项目验证过的 Vanilla Tool
- `loader/`：当前 Loader Tool
- `mod/`：项目私有 Tool
- `experimental/`：未稳定 Tool

每个能力域独立文件。

## 4. knowledge/project.md

只存稳定项目画像和稳定项目专属知识索引。

允许：Project/Mod ID/version/key package、确认的项目系统、discovery/tool 链接、版本敏感区域、open questions。

禁止：每次 RPC、长 stack、大量原始实验、未验证猜测、Minecraft/Loader 通用事实。

`project.md` 必须保持快速可读，绝不能成为 append-only 日志。

## 5. knowledge/minecraft.md

只存当前项目 MC 环境已验证 Vanilla/client/server 事实。禁止项目自定义 class、Loader 独有 API、原始实验。

## 6. knowledge/loader.md

只存当前 Loader 已验证事实。禁止项目 UI、Vanilla 通用知识和 session log。

## 7. knowledge/discoveries/

一个 discovery 一个 Markdown 文件，例如：

```text
20260811-001-container-slot-location.md
20260811-002-rune-screen-button-path.md
```

详细记录假设、观察、有效/无效调用、线程、证据与结论。稳定文档只引用其结论。

## 8. logs/

`sessions/` 是 Runtime 操作日志；`experiments/` 是临时大输出。两者都不是 Knowledge source of truth。重要结论必须提炼成 discovery。

## 9. state/

纯机器状态。禁止 Knowledge/Tool/业务数据。

## 10. Git 建议

MAR 不强制是否提交工作区。若提交，建议保留 runtime/config/tools/knowledge，通常忽略 state、transient session logs、大 experiment。

## 11. 绝对禁止混放

- project.md 粘 Runtime source；
- Tool 文件作为知识文档唯一来源；
- experiment 未提炼就声称已沉淀；
- runtime config 保存高层 Tool 配置；
- state 保存项目业务数据；
- minecraft.md 写 RuneScreen 等项目私有结构。
