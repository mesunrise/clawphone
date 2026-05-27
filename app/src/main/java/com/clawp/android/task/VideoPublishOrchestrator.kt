package com.clawp.android.task

import android.os.PowerManager
import android.content.Context
import com.clawp.android.agent.AgentCallback
import com.clawp.android.agent.AgentConfig
import com.clawp.android.agent.DefaultAgentService
import com.clawp.android.channel.Channel
import com.clawp.android.channel.ChannelManager
import com.clawp.android.utils.KVUtils
import com.clawp.android.utils.XLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 视频发布编排器
 * 负责逐个视频顺序执行发布任务，追踪进度，汇报结果
 */
class VideoPublishOrchestrator(
    private val channel: Channel,
    private val chatId: String,
    private val context: Context
) {
    companion object {
        private const val TAG = "VideoPublishOrchestrator"
        private const val MAX_ITERATIONS_PER_VIDEO = 40
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val progressReporter = PublishProgressReporter(channel, chatId)
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * 执行发布任务
     */
    fun execute(taskRequest: PublishTaskRequest) {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "VideoPublishOrchestrator.execute() 被调用")
        XLog.i(TAG, "  - 视频数量: ${taskRequest.videos.size}")
        XLog.i(TAG, "  - 准备启动协程...")
        XLog.i(TAG, "========================================")

        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "execute() 被调用，准备启动协程，videos=${taskRequest.videos.size}")

        scope.launch {
            XLog.i(TAG, "协程已启动，准备获取锁...")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "协程已启动，准备获取锁")

            try {
                mutex.withLock {
                    XLog.i(TAG, "已获取锁，准备调用 executeInternal()")
                    com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "已获取锁，准备调用 executeInternal()")

                    // 获取 WakeLock 保持屏幕常亮
                    acquireWakeLock()

                    try {
                        executeInternal(taskRequest)
                    } finally {
                        // 释放 WakeLock
                        releaseWakeLock()
                    }
                }
            } catch (e: Exception) {
                XLog.e(TAG, "execute() 协程异常", e)
                com.clawp.android.utils.DebugLogCollector.log(TAG, "ERROR", "execute() 协程异常: ${e.message}")
                releaseWakeLock()
            }
        }
    }

    /**
     * 获取 WakeLock 保持屏幕常亮
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Clawp:VideoPublish"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 最多保持 10 分钟
            XLog.i(TAG, "WakeLock 已获取，防止 CPU 休眠")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "WakeLock 已获取，防止 CPU 休眠")
        } catch (e: Exception) {
            XLog.e(TAG, "获取 WakeLock 失败", e)
            com.clawp.android.utils.DebugLogCollector.log(TAG, "ERROR", "获取 WakeLock 失败: ${e.message}")
        }
    }

    /**
     * 释放 WakeLock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    XLog.i(TAG, "WakeLock 已释放")
                    com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "WakeLock 已释放")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            XLog.e(TAG, "释放 WakeLock 失败", e)
        }
    }

    private suspend fun executeInternal(taskRequest: PublishTaskRequest) {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "VideoPublishOrchestrator.executeInternal() 被调用")
        XLog.i(TAG, "  - 视频数量: ${taskRequest.videos.size}")
        XLog.i(TAG, "  - 话题: ${taskRequest.topics}")
        XLog.i(TAG, "  - 描述: ${taskRequest.description}")
        XLog.i(TAG, "========================================")

        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "executeInternal: videos=${taskRequest.videos.size}, topics=${taskRequest.topics}")

        progressReporter.reportStart(taskRequest.videos.size)

        var successCount = 0
        var failedCount = 0

        taskRequest.videos.forEachIndexed { index, video ->
            val videoNumber = index + 1
            val totalVideos = taskRequest.videos.size

            XLog.i(TAG, "开始发布视频 $videoNumber/$totalVideos: ${video.fileName}")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "开始发布视频 $videoNumber/$totalVideos: ${video.fileName}")

            video.status = PublishStatus.PUBLISHING
            progressReporter.reportProgress(videoNumber, totalVideos, video.fileName)

            try {
                val success = publishSingleVideo(video, taskRequest.topics, taskRequest.description)

                if (success) {
                    video.status = PublishStatus.SUCCESS
                    successCount++
                    progressReporter.reportVideoSuccess(videoNumber, totalVideos, video.fileName)
                    XLog.i(TAG, "视频发布成功: ${video.fileName}")
                    com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "视频发布成功: ${video.fileName}")
                } else {
                    video.status = PublishStatus.FAILED
                    video.errorMessage = "发布失败"
                    failedCount++
                    progressReporter.reportVideoFailed(videoNumber, totalVideos, video.fileName, "发布失败")
                    XLog.e(TAG, "视频发布失败: ${video.fileName}")
                    com.clawp.android.utils.DebugLogCollector.log(TAG, "ERROR", "视频发布失败: ${video.fileName}")
                }

            } catch (e: Exception) {
                video.status = PublishStatus.FAILED
                video.errorMessage = e.message
                failedCount++
                progressReporter.reportVideoFailed(videoNumber, totalVideos, video.fileName, e.message ?: "未知错误")
                XLog.e(TAG, "视频发布异常: ${video.fileName}", e)
                com.clawp.android.utils.DebugLogCollector.log(TAG, "ERROR", "视频发布异常: ${video.fileName}, error=${e.message}")
            }
        }

        // 汇总报告
        progressReporter.reportComplete(successCount, failedCount, taskRequest.videos)
        XLog.i(TAG, "发布任务完成: 成功 $successCount, 失败 $failedCount")
    }

    /**
     * 发布单个视频
     */
    private suspend fun publishSingleVideo(
        video: VideoItem,
        topics: List<String>,
        description: String?
    ): Boolean {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "publishSingleVideo() 被调用")
        XLog.i(TAG, "  - videoPath: ${video.localPath}")
        XLog.i(TAG, "  - fileName: ${video.fileName}")
        XLog.i(TAG, "  - topics: $topics")
        XLog.i(TAG, "  - description: $description")

        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "publishSingleVideo: fileName=${video.fileName}, topics=$topics")

        // 构造系统提示词
        val systemPrompt = DouyinPublishPrompts.buildPrompt(
            videoPath = video.localPath,
            topics = topics,
            description = description
        )

        XLog.i(TAG, "  - 系统提示词已构建，长度: ${systemPrompt.length}")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "系统提示词已构建，长度: ${systemPrompt.length}")

        // 创建 Agent 配置
        val agentConfig = AgentConfig(
            apiKey = KVUtils.getLlmApiKey(),
            baseUrl = KVUtils.getLlmBaseUrl(),
            modelName = KVUtils.getLlmModelName(),
            temperature = 0.0,
            systemPrompt = systemPrompt,
            maxIterations = MAX_ITERATIONS_PER_VIDEO
        )

        XLog.i(TAG, "  - Agent 配置已创建")
        XLog.i(TAG, "  - 开始执行 Agent 任务...")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "Agent 配置已创建，开始执行 Agent 任务")

        // 执行 Agent 任务
        var taskCompleted = false
        var taskSuccess = false

        try {
            val agentService = DefaultAgentService()
            XLog.i(TAG, "  - DefaultAgentService 已创建")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "DefaultAgentService 已创建")

            agentService.initialize(agentConfig)
            XLog.i(TAG, "  - AgentService 已初始化")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "AgentService 已初始化")

            agentService.executeTask(
                userPrompt = "开始发布视频",
                callback = object : AgentCallback {
                    override fun onLoopStart(round: Int) {
                        XLog.i(TAG, "Agent 第 $round 轮开始")
                        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "Agent 第 $round 轮开始")
                    }

                    override fun onContent(round: Int, content: String) {
                        XLog.d(TAG, "Agent 响应: $content")
                    }

                    override fun onToolCall(round: Int, toolId: String, toolName: String, parameters: String) {
                        XLog.d(TAG, "工具调用: $toolName($parameters)")
                        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "工具调用: $toolName")
                    }

                    override fun onToolResult(round: Int, toolId: String, toolName: String, parameters: String, result: com.clawp.android.tool.ToolResult) {
                        XLog.d(TAG, "工具结果: $toolName -> ${result.toString().take(200)}")
                    }

                    override fun onComplete(round: Int, finalAnswer: String, totalTokens: Int) {
                        XLog.i(TAG, "Agent 完成: rounds=$round, tokens=$totalTokens, answer=$finalAnswer")
                        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "Agent 完成: rounds=$round, tokens=$totalTokens")
                        taskCompleted = true
                        taskSuccess = true
                    }

                    override fun onError(round: Int, error: Exception, totalTokens: Int) {
                        XLog.e(TAG, "Agent 错误: ${error.message}")
                        com.clawp.android.utils.DebugLogCollector.log(TAG, "ERROR", "Agent 错误: ${error.message}")
                        taskCompleted = true
                        taskSuccess = false
                    }

                    override fun onSystemDialogBlocked(round: Int, totalTokens: Int) {
                        XLog.w(TAG, "系统弹窗拦截")
                        com.clawp.android.utils.DebugLogCollector.log(TAG, "WARN", "系统弹窗拦截")
                        taskCompleted = true
                        taskSuccess = false
                    }
                }
            )

            XLog.i(TAG, "  - executeTask() 已调用，开始等待完成...")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "executeTask() 已调用，开始等待完成")

            // 等待任务完成（最多等待 5 分钟）
            var waitTime = 0
            while (!taskCompleted && waitTime < 300_000) {
                kotlinx.coroutines.delay(1000)
                waitTime += 1000
            }

            if (!taskCompleted) {
                XLog.e(TAG, "Agent 任务超时")
                com.clawp.android.utils.DebugLogCollector.log(TAG, "ERROR", "Agent 任务超时")
                XLog.i(TAG, "========================================")
                return false
            }

            XLog.i(TAG, "  - Agent 任务完成，结果: $taskSuccess")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "Agent 任务完成，结果: $taskSuccess")
            XLog.i(TAG, "========================================")
            return taskSuccess

        } catch (e: Exception) {
            XLog.e(TAG, "publishSingleVideo 异常", e)
            com.clawp.android.utils.DebugLogCollector.log(TAG, "ERROR", "publishSingleVideo 异常: ${e.message}")
            XLog.i(TAG, "========================================")
            return false
        }
    }
}
