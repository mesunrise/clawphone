# 项目目录结构规划

## 目录设计原则

1. **研究与开发分离**：参考项目放在 `research/` 目录，自研项目放在 `src/` 目录
2. **文档集中管理**：所有文档统一放在 `docs/` 目录
3. **清晰的项目边界**：每个参考项目独立目录，便于对比学习
4. **便于后续选择**：可以基于某个参考项目 fork 或从零开始

## 推荐目录结构

```
clawp/
├── README.md                          # 项目总览
├── docs/                              # 文档目录
│   ├── planning/                      # 规划文档
│   │   ├── research-and-planning.md   # 已有的调研文档
│   │   ├── directory-structure.md     # 本文档
│   │   ├── vision-vs-accessibility.md # 技术方案对比分析
│   │   └── remote-control-design.md   # 远程控制方案设计
│   ├── architecture/                  # 架构设计
│   │   ├── system-overview.md
│   │   ├── component-design.md
│   │   └── api-design.md
│   └── development/                   # 开发文档
│       ├── setup-guide.md
│       ├── coding-standards.md
│       └── testing-guide.md
│
├── research/                          # 参考项目研究目录
│   ├── ApkClaw/                       # ApkClaw 项目
│   │   ├── source/                    # 源代码
│   │   ├── analysis.md                # 架构分析文档
│   │   └── key-features.md            # 核心功能提取
│   ├── mobile-use/                    # mobile-use 项目
│   │   ├── source/
│   │   └── analysis.md
│   ├── AppAgent/                      # AppAgent 项目
│   │   ├── source/
│   │   └── analysis.md
│   ├── AutoGLM/                       # AutoGLM 项目
│   │   ├── source/
│   │   └── analysis.md
│   └── comparison.md                  # 项目对比总结
│
├── src/                               # 自研项目源代码（后续开发）
│   ├── android/                       # Android 应用
│   │   ├── app/
│   │   └── build.gradle
│   ├── server/                        # 服务端（如需要）
│   │   ├── api/
│   │   └── requirements.txt
│   └── common/                        # 共享代码
│
├── experiments/                       # 实验性代码
│   ├── vision-approach/               # 纯视觉方案实验
│   ├── accessibility-approach/        # Accessibility 方案实验
│   └── hybrid-approach/               # 混合方案实验
│
├── benchmarks/                        # 性能测试
│   ├── token-cost/                    # Token 消耗测试
│   ├── accuracy/                      # 准确度测试
│   └── results/                       # 测试结果
│
└── tools/                             # 工具脚本
    ├── setup.sh
    ├── analyze-tokens.py
    └── compare-approaches.py
```

## 当前任务：研究 ApkClaw

### 第一步：获取 ApkClaw 源码

由于直接 git clone 网络不稳定，可以尝试：
1. 使用 GitHub 镜像站
2. 下载 zip 包
3. 使用代理

### 第二步：分析重点

1. **消息通道集成**
   - 飞书 Bot 如何接收服务端消息
   - 消息格式和协议
   - 如何触发手机端执行

2. **Accessibility Service 实现**
   - UI 树获取方式
   - 元素定位机制
   - 操作执行流程

3. **LLM 集成**
   - Prompt 设计
   - Token 优化策略
   - 工具调用机制

4. **Agent 循环**
   - 观察 → 思考 → 行动 → 验证
   - 死循环检测
   - 错误恢复

### 第三步：对比分析

基于 ApkClaw 和其他项目，回答你的两个核心问题：
1. 纯视觉方案 vs Accessibility 方案的准确度和 Token 消耗
2. 服务端到手机的远程控制方案设计

## 下一步行动

1. ✅ 创建目录结构
2. ⏳ 获取 ApkClaw 源码
3. ⏳ 深入分析 ApkClaw 架构
4. ⏳ 编写技术方案对比文档
5. ⏳ 设计远程控制方案
6. ⏳ 选择技术路线并开始开发
