package com.clawp.android.task

import android.content.Context
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
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fileDownloader: FeiShuFileDownloader? = null
    private val batchCollector = MessageBatchCollector(scope) { batch ->
        handleBatchComplete(batch)
    }

    /**
     * 初始化（在 ChannelManager 初始化后调用）
     */
    fun init() {
        ChannelManager.setOnFileMessageReceivedListener(this)

        // 同时监听文本消息用于批次收集
        ChannelManager.setOnMessageReceivedListener(object : ChannelManager.OnMessageReceivedListener {
            override fun onMessageReceived(channel: Channel, message: String, messageID: String) {
                // 提取 chatId（从 messageID 或其他方式）
                // 这里简化处理，使用 messageID 作为 chatId
                batchCollector.addTextMessage(messageID, message, messageID)
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
        XLog.i(TAG, "收到文件消息: fileKey=$fileKey, fileName=$fileName, chatId=$chatId")

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

        // TODO: Phase 2 - 解析自然语言指令
        // TODO: Phase 3 - 创建发布任务并执行

        // 临时：仅记录日志
        XLog.i(TAG, "视频列表:")
        batch.videos.forEach { video ->
            XLog.i(TAG, "  - ${video.fileName}: ${video.localPath}")
        }
        XLog.i(TAG, "文本指令:")
        batch.textMessages.forEach { text ->
            XLog.i(TAG, "  - $text")
        }
    }
}
