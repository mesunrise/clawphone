package com.clawp.android

import android.app.Application
import android.os.Build
import com.clawp.android.agent.AgentConfig
import com.clawp.android.agent.DefaultAgentService
import com.clawp.android.channel.ChannelManager
import com.clawp.android.service.ForegroundService
import com.clawp.android.task.VideoPublishCoordinator
import com.clawp.android.tool.ToolRegistry
import com.clawp.android.utils.KVUtils
import com.clawp.android.utils.LogUploader
import com.clawp.android.utils.XLog

/**
 * Application 入口
 */
class ClawApplication : Application() {

    companion object {
        private const val TAG = "ClawApplication"
        lateinit var instance: ClawApplication
            private set
    }

    lateinit var taskOrchestrator: TaskOrchestrator
        private set
    private lateinit var videoPublishCoordinator: VideoPublishCoordinator

    override fun onCreate() {
        super.onCreate()
        instance = this
        XLog.setDEBUG(BuildConfig.DEBUG)

        KVUtils.init(this)
        ToolRegistry.getInstance().registerAllTools(ToolRegistry.DeviceType.MOBILE)
        XLog.i(TAG, "ClawApplication initialized, tools registered: ${ToolRegistry.getInstance().getAllTools().size}")

        // 网络日志输出到文件（调试时设为 true）
        DefaultAgentService.FILE_LOGGING_ENABLED = BuildConfig.DEBUG
        DefaultAgentService.FILE_LOGGING_CACHE_DIR = cacheDir

        // 创建任务编排器（Agent 消息路由核心）
        taskOrchestrator = TaskOrchestrator(
            agentConfigProvider = { buildAgentConfig() },
            onTaskFinished = { }
        )

        // 初始化视频发布协调器
        videoPublishCoordinator = VideoPublishCoordinator(this, taskOrchestrator)

        // 启动前台服务
        if (!ForegroundService.isRunning()) {
            val started = ForegroundService.start(this)
            if (!started) {
                XLog.e(TAG, "ForegroundService start failed: notification permission not granted")
            }
        }

        // 异步初始化 Agent 和通道
        Thread({
            if (KVUtils.hasLlmConfig()) {
                taskOrchestrator.initAgent()
                initChannels()
            }

            // 启动日志自动上报（如果已启用）
            if (KVUtils.isLogUploadEnabled()) {
                val serverUrl = KVUtils.getLogServerUrl()
                val deviceId = "${Build.BRAND}_${Build.MODEL}_${Build.SERIAL}".replace(" ", "_")
                LogUploader.start(serverUrl, deviceId)
                XLog.i(TAG, "日志自动上报已启动")
            }
        }, "app-async-init").start()
    }

    private fun initChannels() {
        ChannelManager.init(
            context = this,
            feishuAppId = KVUtils.getFeishuAppId(),
            feishuAppSecret = KVUtils.getFeishuAppSecret()
        )

        // 初始化视频发布协调器
        videoPublishCoordinator.init()

        // 设置文件下载器
        ChannelManager.getFeiShuFileDownloader()?.let { downloader ->
            videoPublishCoordinator.setFileDownloader(downloader)
        }
    }

    /**
     * 从 KV 存储构建 AgentConfig，用于 agentConfigProvider 回调
     */
    private fun buildAgentConfig(): AgentConfig {
        return AgentConfig.Builder()
            .apiKey(KVUtils.getLlmApiKey())
            .baseUrl(KVUtils.getLlmBaseUrl())
            .modelName(KVUtils.getLlmModelName())
            .maxIterations(30)
            .temperature(0.1)
            .streaming(false)
            .build()
    }
}
