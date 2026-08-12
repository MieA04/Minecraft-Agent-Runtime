# Public MAR Skill Contract

## 1. Skill 职责

Skill 负责“知道怎么做”：识别项目、安装 Runtime、构建修改、启动连接、调用 `eval.*`、读取项目 Knowledge/Tool、Tool 失败后降级、保存 discovery、提取 project Tool、做 public promotion。

Skill 不替代 Runtime 执行 JVM 代码。

## 2. SKILL.md 内容限制

只能包含：定位、入口判定、流程路由、核心边界、reference/assets/public tool 链接。

禁止：完整 RPC 细节、全部 Java 内部设计、所有兼容知识、项目知识、长版本矩阵、Tool 源码、实验日志。

## 3. Reference 单一主题

- runtime-api.md：Agent 可调用 surface
- installation.md：安装/集成
- exploration.md：未知对象探索/降级
- project-workspace.md：产物落盘位置
- knowledge-governance.md：project/public 边界和 promotion

## 4. Skill 固定决策流程

```text
1 确认项目
2 查 .minecraft-agent-runtime
3 未安装 -> 安装
4 已安装 -> 读 state
5 未运行 -> 启动 dev instance
6 连接 RPC
7 读取 project knowledge
8 读取 project tools
9 已有工具 -> 使用并验证
10 失败/未知 -> Groovy 降级探索
11 取得有效操作
12 写 discovery
13 重复能力 -> project tool
14 满足 promotion -> public skill/tool
```

不得在已有有效 Knowledge/Tool 时无理由从零探索。

## 5. 已知能力不是盲信

使用 Tool 前检查当前 MC/Loader/适用范围/项目依赖/验证状态。失败后进入降级，不要反复撞同一个已失败调用。

## 6. Runtime 扩张门槛

频繁操作默认提取 Groovy Tool，不是新增 Java Runtime method。只有所有探索都不可避免的基础设施能力才允许提议 Runtime change，并走 Change Control。
