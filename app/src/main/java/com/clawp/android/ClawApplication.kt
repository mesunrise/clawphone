package com.clawp.android

import android.app.Application
import com.clawp.android.agent.DefaultAgentService
import com.clawp.android.channel.ChannelManager
import com.clawp.android.service.ForegroundService
import com.clawp.android.tool.ToolRegistry
import com.clawp.android.utils.KVUtils
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
                initChannels()
            }
        }, "app-async-init").start()
    }

    private fun initChannels() {
        ChannelManager.init(
            feishuAppId = KVUtils.getFeishuAppId(),
            feishuAppSecret = KVUtils.getFeishuAppSecret()
        )
    }
}
