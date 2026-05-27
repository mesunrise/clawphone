我计划在claudecode中使用replicant-mcp进行apk的构建优化，解决目前构建依赖git并且无法自动发现构建失败及原因的问题。
1、replicant-mcp工具完整使用指南
这个工具是为 Android 开发者设计的 MCP 服务器，专门解决 AI 助手在 vibe coding 中跳过构建验证的问题。以下是详细的使用教程。
2、核心功能：

✅ 构建 APK 并验证输出

✅ 验证 APK 签名是否正确

✅ 运行 Android 测试并返回结构化结果

✅ 自动配置签名（双密钥策略）

✅ 验证 ProGuard/R8 混淆映射文件
3、已经在本机完成了安装
Node.js：版本 16 或更高

Java JDK：OpenJDK 11（用于 Gradle 构建）

Android SDK：包含 build-tools（用于 apksigner）
4、已经在.claude/mcp.json中配置了replicant-mcp信息。请使用mcp构建apk