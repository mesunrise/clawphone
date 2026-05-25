package com.clawp.android.task

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
    private val chatId: String
) {
    companion object {
        private const val TAG = "VideoPublishOrchestrator"
        private const val MAX_ITERATIONS_PER_VIDEO = 40
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val progressReporter = PublishProgressReporter(channel, chatId)

    /**
     * 执行发布任务
     */
    fun execute(taskRequest: PublishTaskRequest) {
        scope.launch {
            mutex.withLock {
                executeInternal(taskRequest)
            }
        }
    }

    private suspend fun executeInternal(taskRequest: PublishTaskRequest) {
        XLog.i(TAG, "开始执行发布任务: ${taskRequest.videos.size} 个视频")

        progressReporter.reportStart(taskRequest.videos.size)

        var successCount = 0
        var failedCount = 0

        taskRequest.videos.forEachIndexed { index, video ->
            val videoNumber = index + 1
            val totalVideos = taskRequest.videos.size

            XLog.i(TAG, "开始发布视频 $videoNumber/$totalVideos: ${video.fileName}")

            video.status = PublishStatus.PUBLISHING
            progressReporter.reportProgress(videoNumber, totalVideos, video.fileName)

            try {
                val success = publishSingleVideo(video, taskRequest.topics, taskRequest.description)

                if (success) {
                    video.status = PublishStatus.SUCCESS
                    successCount++
                    progressReporter.reportVideoSuccess(videoNumber, totalVideos, video.fileName)
                    XLog.i(TAG, "视频发布成功: ${video.fileName}")
                } else {
                    video.status = PublishStatus.FAILED
                    video.errorMessage = "发布失败"
                    failedCount++
                    progressReporter.reportVideoFailed(videoNumber, totalVideos, video.fileName, "发布失败")
                    XLog.e(TAG, "视频发布失败: ${video.fileName}")
                }

            } catch (e: Exception) {
                video.status = PublishStatus.FAILED
                video.errorMessage = e.message
                failedCount++
                progressReporter.reportVideoFailed(videoNumber, totalVideos, video.fileName, e.message ?: "未知错误")
                XLog.e(TAG, "视频发布异常: ${video.fileName}", e)
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
        // 构造系统提示词
        val systemPrompt = DouyinPublishPrompts.buildPrompt(
            videoPath = video.localPath,
            topics = topics,
            description = description
        )

        XLog.d(TAG, "系统提示词:\n$systemPrompt")

        // 创建 Agent 配置
        val agentConfig = AgentConfig(
            apiKey = KVUtils.getLlmApiKey(),
            baseUrl = KVUtils.getLlmBaseUrl(),
            modelName = KVUtils.getLlmModelName(),
            temperature = 0.0,
            systemPrompt = systemPrompt,
            maxIterations = MAX_ITERATIONS_PER_VIDEO
        )

        // 执行 Agent 任务
        var taskCompleted = false
        var taskSuccess = false

        val agentService = DefaultAgentService()
        agentService.executeTask(
            config = agentConfig,
            initialMessage = "开始发布视频",
            callback = object : AgentCallback {
                override fun onStart() {
                    XLog.i(TAG, "Agent 开始执行")
                }

                override fun onThinking(content: String) {
                    // 思考过程，不输出
                }

                override fun onResponse(content: String) {
                    XLog.d(TAG, "Agent 响应: $content")
                }

                override fun onToolCall(toolName: String, arguments: String) {
                    XLog.d(TAG, "工具调用: $toolName($arguments)")
                }

                override fun onToolResult(toolName: String, result: String) {
                    XLog.d(TAG, "工具结果: $toolName -> ${result.take(200)}")
                }

                override fun onComplete(success: Boolean, finalMessage: String) {
                    XLog.i(TAG, "Agent 完成: success=$success, message=$finalMessage")
                    taskCompleted = true
                    taskSuccess = success
                }

                override fun onError(error: String) {
                    XLog.e(TAG, "Agent 错误: $error")
                    taskCompleted = true
                    taskSuccess = false
                }
            }
        )

        // 等待任务完成（最多等待 5 分钟）
        var waitTime = 0
        while (!taskCompleted && waitTime < 300_000) {
            kotlinx.coroutines.delay(1000)
            waitTime += 1000
        }

        if (!taskCompleted) {
            XLog.e(TAG, "Agent 任务超时")
            return false
        }

        return taskSuccess
    }
}
