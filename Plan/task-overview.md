# V0.2.1 开发任务总览

## 任务依赖关系

```
Task 1: 数据模型 (POJOs + Parser)
  └─→ Task 2: 条件求值器
  └─→ Task 3: 动作执行器
  └─→ Task 4: ScriptLoader
        └─→ Task 5: ScriptEngine 核心循环
              └─→ Task 6: UI 集成 (HomeActivity)
                    └─→ Task 7: 版本号升级
```

## 任务清单

| # | 任务 | 文档 | 预估工作量 | 依赖 |
|---|------|------|-----------|------|
| 1 | 数据模型 POJOs + ScriptParser | [Plan/task-1-data-model.md](Plan/task-1-data-model.md) | 小 (~10 个文件) | 无 |
| 2 | 条件求值器 | [Plan/task-2-condition-evaluators.md](Plan/task-2-condition-evaluators.md) | 小 (~5 个文件) | Task 1 |
| 3 | 动作执行器 + 人类化工具 | [Plan/task-3-action-executors.md](Plan/task-3-action-executors.md) | 中 (~9 个文件) | Task 1 |
| 4 | ScriptLoader + 示例脚本 | [Plan/task-4-script-loader.md](Plan/task-4-script-loader.md) | 小 (~2 个文件) | Task 1 |
| 5 | ScriptEngine 核心循环 | [Plan/task-5-script-engine-core.md](Plan/task-5-script-engine-core.md) | 中 (~1 个文件) | Task 1-4 |
| 6 | UI 集成 (HomeActivity) | [Plan/task-6-ui-integration.md](Plan/task-6-ui-integration.md) | 中 (~3 个文件修改) | Task 1-5 |
| 7 | 版本号升级 v0.1 → v0.2 | [Plan/task-7-version-bump.md](Plan/task-7-version-bump.md) | 极小 (~3 行修改) | 无 |

## 建议执行顺序

按依赖关系顺序执行：**1 → 2 → 3 → 4 → 5 → 6 → 7**

Task 2 和 Task 3 可并行开发（互不依赖），但考虑到工作量和代码审查效率，建议顺序执行。

## 关于脚本编写

脚本引擎完成后，具体任务脚本（如惊喜红包）的编写属于 **配置工作**，只需按 [Plan/script-format-design.md](Plan/script-format-design.md) 格式编写 JSON 文件放入 `assets/scripts/` 即可，无需额外代码开发。

建议先完成引擎开发（Task 1-7），验证基本功能可用后，再根据具体业务需求编写脚本 JSON。
