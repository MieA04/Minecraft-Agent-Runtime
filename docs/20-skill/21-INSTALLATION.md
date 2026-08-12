# MAR Runtime Installation Specification

## 1. 前置识别

安装前 MUST 确认：项目根、构建系统、Minecraft version、Loader/type/version、Java、main source set、dev run task、Mod 初始化入口。

不得只凭文件名猜版本后写配置。

## 2. V0 支持边界

首选支持可修改源码、可添加 Java source directory、可加 Groovy dependency 的 Minecraft Mod 开发项目。遇到无法识别构建系统，不发明语法；记录 unsupported installer adapter，不改变 Runtime 架构。

## 3. 工作区

创建：

```text
.minecraft-agent-runtime/
├── runtime/{bootstrap,config}/
├── tools/{minecraft,loader,mod,experimental}/
├── knowledge/{minecraft.md,loader.md,project.md,discoveries/}
├── logs/{sessions,experiments}/
└── state/
```

已存在时禁止整目录覆盖。升级只更新受管 Runtime 文件，不能删除 Tool/Knowledge/Logs。

## 4. Runtime 模板

源：`skill/assets/runtime-template/`

目标：`.minecraft-agent-runtime/runtime/bootstrap/`

保持 package/relative path，记录 runtime version。不得粘进项目业务 package。

## 5. Groovy Dependency

版本必须来自 MAR release/version manifest，安装 Agent 不得每项目随意选择“最新版”。只加入 Runtime 真正需要的 Groovy modules；某 Tool 的第三方依赖不能升级为 Runtime 固定依赖。

## 6. SourceSet

把 `.minecraft-agent-runtime/runtime/bootstrap/src/main/java` 加入项目 Java main source set。禁止复制同一 Runtime 到第二编译目录；项目 `.groovy` Tool 不作为 Java source 编译。

## 7. Runtime start hook

项目初始化只加入一次：

```java
MinecraftAgentRuntime.start();
```

不得移动/重写原项目初始化，不把 Runtime 初始化细节展开进 Mod 主类，不写 Tool/Knowledge 逻辑。

## 8. Project Root

优先在 dev run 加 `-Dmar.projectRoot=<root>`。不便时 Runtime 可从 user.dir 向上查。禁止把绝对路径硬编码在 Java source。

## 9. Config 初始化

首次创建 schema=1 loopback/dynamic-port config；已有 config 不覆盖用户值，schema migration 除外。

## 10. Knowledge 初始化

只创建模板和环境 header，不得在尚未运行 JVM 前编造字段/UI/API 发现。

## 11. 启动验收

1. start dev instance
2. wait state ready
3. read host/port
4. connect
5. `eval.raw` -> `1+2`
6. verify current Mod class visible

只有两项 smoke test 真正通过才算安装成功；仅 Gradle build 成功不算。

## 12. 升级边界

可替换 Runtime bootstrap、必要 dependency version、config schema migration。

不可覆盖 tools/knowledge/logs/experiments 或项目业务源码（除必要 start hook 维护）。升级后重跑安装验收。
