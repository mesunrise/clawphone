# 脚本格式设计文档

## 概述

为实现无 Agent 决策的自动化脚本执行，设计一套基于 JSON 的 DSL（领域特定语言），脚本文件打包在 APK 的 `assets/scripts/` 目录下。

---

## 1. 设计目标

| 目标 | 说明 |
|------|------|
| 可读性 | 脚本结构清晰，字段名自解释 |
| 可解析性 | Android 原生 `JSONObject` 即可解析，无需第三方库 |
| 可扩展性 | 条件类型和动作类型可逐步扩展 |
| 人类操作模拟 | 支持随机偏移、随机速度、随机路径抖动 |

---

## 2. 脚本顶层结构

```json
{
  "meta": {
    "name": "惊喜红包自动任务",
    "version": "1.0",
    "description": "自动完成惊喜红包APP的观看视频任务"
  },
  "config": {
    "targetPackage": "com.example.hongbao",
    "loopCount": 1000,
    "roundDelay": { "min": 3, "max": 5 }
  },
  "setup": [
    { "type": "open_app", "package": "com.example.hongbao" },
    { "type": "wait", "durationMs": 2000 }
  ],
  "rules": [
    {
      "name": "不在目标APP时切换",
      "conditions": [
        { "type": "current_app_not", "package": "{{config.targetPackage}}" }
      ],
      "actions": [
        { "type": "open_app", "package": "{{config.targetPackage}}" },
        { "type": "wait", "durationMs": 2000 },
        { "type": "restart_round" }
      ]
    },
    {
      "name": "邀请有奖+看视频领金币 → 点击看视频",
      "conditions": [
        { "type": "text_exists", "text": "邀请有奖" },
        { "type": "text_exists", "text": "看视频" }
      ],
      "actions": [
        {
          "type": "click",
          "target": { "by": "text", "value": "看视频", "match": "contains" },
          "humanize": { "offsetPx": 5, "tapDurationMs": 150 }
        },
        { "type": "end_round" }
      ]
    },
    {
      "name": "看完视频+完成视频要求 → 点击关闭",
      "conditions": [
        { "type": "text_exists", "text": "看完视频" },
        { "type": "text_exists", "text": "完成视频要求" }
      ],
      "actions": [
        {
          "type": "click",
          "target": { "by": "text", "value": "关闭", "match": "contains" },
          "humanize": { "offsetPx": 5, "tapDurationMs": 150 }
        },
        { "type": "end_round" }
      ]
    },
    {
      "name": "兜底：无匹配条件时按返回键",
      "conditions": [],
      "actions": [
        { "type": "press_back" },
        { "type": "end_round" }
      ]
    }
  ]
}
```

### 2.1 `meta` 元信息（必填）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 任务名称，用于 UI 展示 |
| `version` | string | 否 | 脚本版本号 |
| `description` | string | 否 | 任务描述 |

### 2.2 `config` 全局配置（必填）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `targetPackage` | string | 是 | 目标 APP 的 Android package name，如 `com.example.hongbao` |
| `loopCount` | integer | 否 | 最大循环轮次，默认 1000。与 `loopDurationSec` 同时存在时取先到达者 |
| `loopDurationSec` | integer | 否 | 最大运行时长（秒），与 `loopCount` 同时存在时取先到达者 |
| `roundDelay` | object | 否 | 轮次间随机延迟，默认 `{"min": 3, "max": 5}` |
| `roundDelay.min` | number | 否 | 最小延迟秒数 |
| `roundDelay.max` | number | 否 | 最大延迟秒数 |

### 2.3 `setup` 前置步骤（可选）

在循环开始前执行一次的步骤序列。每个步骤是一个 action 对象。用于首次打开 APP 等一次性操作。

### 2.4 `rules` 规则列表（必填）

按顺序逐条判断的规则列表。每条规则包含 `conditions` 和 `actions`。执行逻辑：

```
for each rule in rules:
    if all conditions match:
        execute all actions in sequence
        break  // 不再执行后续规则
```

- `conditions: []` 表示无条件匹配（通常用于兜底规则）
- 条件之间是 **AND** 关系（全部满足才触发）
- 规则之间是 **OR** 关系（第一条匹配即执行）

---

## 3. 条件类型 (conditions)

### 3.1 `text_exists` — 屏幕中存在指定文字

```json
{ "type": "text_exists", "text": "邀请有奖", "match": "contains" }
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `text` | string | 是 | 要查找的文字 |
| `match` | string | 否 | 匹配模式：`"contains"`(默认)、`"exact"`、`"startsWith"`、`"endsWith"` |

### 3.2 `text_not_exists` — 屏幕中不存在指定文字

```json
{ "type": "text_not_exists", "text": "加载中" }
```

参数同 `text_exists`。

### 3.3 `desc_exists` — 存在指定 contentDescription

```json
{ "type": "desc_exists", "desc": "搜索", "match": "contains" }
```

### 3.4 `current_app_is` — 当前前景 APP 是指定包名

```json
{ "type": "current_app_is", "package": "com.example.hongbao" }
```

### 3.5 `current_app_not` — 当前前景 APP 不是指定包名

```json
{ "type": "current_app_not", "package": "com.example.hongbao" }
```

### 3.6 `node_count` — 匹配节点的数量判断

```json
{ "type": "node_count", "by": "text", "value": "视频", "operator": "gte", "count": 1 }
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `by` | string | 是 | 查找方式：`"text"`、`"desc"`、`"id"` |
| `value` | string | 是 | 查找值 |
| `operator` | string | 是 | `"eq"`, `"gt"`, `"gte"`, `"lt"`, `"lte"` |
| `count` | integer | 是 | 比较阈值 |

---

## 4. 动作类型 (actions)

### 4.1 `open_app` — 打开指定 APP

```json
{ "type": "open_app", "package": "com.example.hongbao" }
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `package` | string | 是 | 目标 APP package name |

### 4.2 `click` — 点击操作

```json
{
  "type": "click",
  "target": { "by": "text", "value": "看视频", "match": "contains", "index": 0 },
  "humanize": { "offsetPx": 5, "tapDurationMs": 150 }
}
```

**`target` 定位方式：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `by` | string | 是 | `"text"`、`"desc"`、`"id"`、`"coordinate"` |
| `value` | string | 是(除coordinate) | 查找值 |
| `match` | string | 否 | 匹配模式（仅 `text`/`desc`）：`"contains"`(默认)、`"exact"` |
| `index` | integer | 否 | 多个匹配时选第几个，默认 0 |
| `x` | integer | 仅coordinate | 坐标 X |
| `y` | integer | 仅coordinate | 坐标 Y |

**`humanize` 人类化参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `offsetPx` | integer | 否 | 0 | 点击位置随机偏移范围(±像素)，模拟手指抖动 |
| `tapDurationMs` | integer | 否 | 100 | 按压时长(ms)，真人点击通常 80-200ms |

### 4.3 `long_press` — 长按操作

```json
{
  "type": "long_press",
  "target": { "by": "text", "value": "视频区域" },
  "humanize": { "offsetPx": 3, "durationMs": 800, "durationVarMs": 200 }
}
```

**`humanize` 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `offsetPx` | integer | 否 | 0 | 位置随机偏移 |
| `durationMs` | integer | 否 | 1000 | 基准长按时长 |
| `durationVarMs` | integer | 否 | 0 | 时长随机变化范围(±)，实际时长 = duration ± random(0, durationVarMs) |

### 4.4 `swipe` — 滑动操作

```json
{
  "type": "swipe",
  "from": { "x": 540, "y": 1800 },
  "to": { "x": 540, "y": 400 },
  "humanize": {
    "fromOffsetPx": 30,
    "toOffsetPx": 50,
    "jitterPx": 10,
    "durationMs": 500,
    "durationVarMs": 200
  }
}
```

**`humanize` 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `fromOffsetPx` | integer | 否 | 0 | 起始点随机偏移 |
| `toOffsetPx` | integer | 否 | 0 | 终点随机偏移 |
| `jitterPx` | integer | 否 | 0 | 滑动过程中的随机路径抖动幅度 |
| `durationMs` | integer | 否 | 500 | 基准滑动时长 |
| `durationVarMs` | integer | 否 | 0 | 时长随机变化范围 |

### 4.5 `wait` — 等待

```json
{ "type": "wait", "durationMs": 5000 }
```

等待后继续执行下一步。

### 4.6 `press_back` / `press_home` — 系统按键

```json
{ "type": "press_back" }
{ "type": "press_home" }
```

### 4.7 `restart_round` — 重新开始当前轮次

```json
{ "type": "restart_round" }
```

立即终止当前轮次，重新从第一个规则开始判断。**不消耗 loopCount**。

### 4.8 `end_round` — 结束当前轮次

```json
{ "type": "end_round" }
```

立即结束当前轮次，等待 `roundDelay` 后进入下一轮。

### 4.9 `exit_task` — 终止整个任务

```json
{ "type": "exit_task" }
```

---

## 5. 变量引用

支持 `{{config.targetPackage}}` 引用 config 中的值，减少重复配置。

---

## 6. 执行生命周期

```
┌──────────────────────────────────────┐
│ 1. 验证脚本格式                        │
│ 2. 执行 setup 步骤（仅一次）            │
│ 3. roundIndex = 0                     │
│ 4. while roundIndex < loopCount:      │
│    a. 读取屏幕树（getScreenTree）       │
│    b. 遍历 rules，找到第一个全匹配的规则  │
│    c. 执行匹配规则的 actions            │
│    d. roundIndex++                     │
│    e. 等待 random(roundDelay.min,      │
│       roundDelay.max) 秒               │
│ 5. 任务完成                            │
└──────────────────────────────────────┘
```

---

## 7. 完整示例脚本

见 `app/src/main/assets/scripts/example_hongbao.json`（后续创建）。

---

## 8. 待确定问题

见 `Plan/open-questions.md`
