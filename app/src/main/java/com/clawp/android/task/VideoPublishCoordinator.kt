package com.clawp.android.task

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.clawp.android.R
import com.clawp.android.channel.Channel
import com.clawp.android.channel.ChannelManager
import com.clawp.android.channel.feishu.FeiShuFileDownloader
import com.clawp.android.channel.feishu.MessageBatch
import com.clawp.android.channel.feishu.MessageBatchCollector
import com.clawp.android.utils.XLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 视频发布协调器
 * 负责协调文件下载、批次收集、任务解析和发布编排
 */
class VideoPublishCoordinator(
    private val context: Context
) : ChannelManager.OnFileMessageReceivedListener {

    companion object {
        private const val TAG = "VideoPublishCoordinator"
        private const val NOTIFICATION_CHANNEL_ID = "clawp_message_channel"
        private const val NOTIFICATION_ID_TEXT = 2001
        private const val NOTIFICATION_ID_FILE = 2002
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fileDownloader: FeiShuFileDownloader? = null
    private val batchCollector = MessageBatchCollector(scope) { batch ->
        handleBatchComplete(batch)
    }
    private val taskParser = VideoPublishTaskParser()

    /**
     * 初始化（在 ChannelManager 初始化后调用）
     */
    fun init() {
        createNotificationChannel()
        ChannelManager.setOnFileMessageReceivedListener(this)

        // 同时监听文本消息用于批次收集
        ChannelManager.setOnMessageReceivedListener(object : ChannelManager.OnMessageReceivedListener {
            override fun onMessageReceived(channel: Channel, message: String, messageID: String, chatId: String) {
                XLog.i(TAG, "========================================")
                XLog.i(TAG, "收到飞书文本消息!")
                XLog.i(TAG, "  - Channel: ${channel.displayName}")
                XLog.i(TAG, "  - MessageID: $messageID")
                XLog.i(TAG, "  - ChatID: $chatId")
                XLog.i(TAG, "  - Content: $message")
                XLog.i(TAG, "========================================")

                // 显示通知
                showNotification(
                    NOTIFICATION_ID_TEXT,
                    "收到飞书文本消息",
                    message.take(50)
                )

                // 回复确认消息（验证阶段）
                ChannelManager.sendMessage(
                    channel,
                    "✅ 收到消息: $message",
                    messageID
                )

                // 简化测试：直接响应"打开抖音"命令
                if (message.trim() == "打开抖音") {
                    XLog.i(TAG, "检测到测试命令：打开抖音")
                    scope.launch {
                        try {
                            val service = com.clawp.android.service.ClawAccessibilityService.getInstance()
                            if (service == null) {
                                XLog.e(TAG, "Accessibility Service 未运行")
                                ChannelManager.sendMessage(channel, "❌ Accessibility Service 未运行，请在设置中启用", messageID)
                                return@launch
                            }

                            XLog.i(TAG, "开始打开抖音极速版...")
                            val result = service.openApp("抖音极速版")
                            if (result) {
                                XLog.i(TAG, "✅ 抖音极速版已打开")
                                ChannelManager.sendMessage(channel, "✅ 抖音极速版已打开", messageID)
                            } else {
                                XLog.e(TAG, "❌ 打开抖音极速版失败")
                                ChannelManager.sendMessage(channel, "❌ 打开抖音极速版失败，请检查应用是否已安装", messageID)
                            }
                        } catch (e: Exception) {
                            XLog.e(TAG, "打开抖音极速版异常", e)
                            ChannelManager.sendMessage(channel, "❌ 打开抖音极速版异常: ${e.message}", messageID)
                        }
                    }
                    return
                }

                // 调试命令：列出已安装的应用
                if (message.trim() == "列出应用") {
                    XLog.i(TAG, "检测到调试命令：列出应用")
                    scope.launch {
                        try {
                            val service = com.clawp.android.service.ClawAccessibilityService.getInstance()
                            if (service == null) {
                                ChannelManager.sendMessage(channel, "❌ Accessibility Service 未运行", messageID)
                                return@launch
                            }

                            val apps = service.getInstalledApps()
                            val douyinApps = apps.filter { it.contains("抖音", ignoreCase = true) }

                            if (douyinApps.isEmpty()) {
                                ChannelManager.sendMessage(channel, "未找到包含'抖音'的应用\n\n所有应用数量: ${apps.size}", messageID)
                            } else {
                                val appList = douyinApps.joinToString("\n")
                                ChannelManager.sendMessage(channel, "找到 ${douyinApps.size} 个抖音相关应用:\n$appList", messageID)
                            }
                        } catch (e: Exception) {
                            XLog.e(TAG, "列出应用异常", e)
                            ChannelManager.sendMessage(channel, "❌ 列出应用异常: ${e.message}", messageID)
                        }
                    }
                    return
                }

                // 使用真实的 chatId 进行批次收集
                batchCollector.addTextMessage(chatId, message, messageID)
            }
        })

        XLog.i(TAG, "VideoPublishCoordinator initialized")
    }

    /**
     * 设置文件下载器（需要 Feishu API Client）
     */
    fun setFileDownloader(downloader: FeiShuFileDownloader) {
        this.fileDownloader = downloader
    }

    override fun onFileMessageReceived(
        channel: Channel,
        fileKey: String,
        fileName: String,
        messageID: String,
        chatId: String
    ) {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "收到飞书文件消息!")
        XLog.i(TAG, "  - Channel: ${channel.displayName}")
        XLog.i(TAG, "  - FileKey: $fileKey")
        XLog.i(TAG, "  - FileName: $fileName")
        XLog.i(TAG, "  - MessageID: $messageID")
        XLog.i(TAG, "  - ChatID: $chatId")
        XLog.i(TAG, "========================================")

        // 显示通知
        showNotification(
            NOTIFICATION_ID_FILE,
            "收到飞书文件消息",
            "文件名: $fileName"
        )

        val downloader = fileDownloader
        if (downloader == null) {
            XLog.w(TAG, "文件下载器未初始化，忽略文件消息")
            return
        }

        // 异步下载文件
        scope.launch {
            try {
                val localPath = downloader.downloadFile(fileKey, fileName, messageID)
                if (localPath != null) {
                    XLog.i(TAG, "文件下载成功: $localPath")
                    // 添加到批次收集器
                    batchCollector.addVideoMessage(chatId, localPath, fileName)
                } else {
                    XLog.e(TAG, "文件下载失败: $fileName")
                    // TODO: 通知用户下载失败
                }
            } catch (e: Exception) {
                XLog.e(TAG, "文件下载异常: $fileName", e)
            }
        }
    }

    /**
     * 批次完成处理
     */
    private fun handleBatchComplete(batch: MessageBatch) {
        XLog.i(TAG, "批次完成: chatId=${batch.chatId}, videos=${batch.videos.size}, texts=${batch.textMessages.size}")

        // 解析任务
        val taskRequest = taskParser.parse(batch)
        if (taskRequest == null) {
            XLog.w(TAG, "任务解析失败或无发布意图，忽略批次")
            return
        }

        XLog.i(TAG, "任务解析成功:")
        XLog.i(TAG, "  - 视频数量: ${taskRequest.videos.size}")
        XLog.i(TAG, "  - 话题: ${taskRequest.topics}")
        XLog.i(TAG, "  - 描述: ${taskRequest.description}")

        // 创建编排器并执行任务
        val orchestrator = VideoPublishOrchestrator(
            channel = Channel.FEISHU,
            chatId = batch.chatId
        )
        orchestrator.execute(taskRequest)
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "飞书消息通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "接收飞书消息时的通知"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            XLog.i(TAG, "通知渠道已创建: $NOTIFICATION_CHANNEL_ID")
        }
    }

    /**
     * 显示通知
     */
    private fun showNotification(notificationId: Int, title: String, content: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Android 13+ 需要检查通知权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    XLog.w(TAG, "通知权限未授予，无法显示通知")
                    return
                }
            }

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(notificationId, notification)
            XLog.i(TAG, "通知已显示: $title - $content")
        } catch (e: Exception) {
            XLog.e(TAG, "显示通知失败", e)
        }
    }
}
