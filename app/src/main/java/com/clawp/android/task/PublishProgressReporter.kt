package com.clawp.android.task

import com.clawp.android.channel.Channel
import com.clawp.android.channel.ChannelManager
import com.clawp.android.utils.XLog

/**
 * 发布进度汇报器
 * 通过飞书实时汇报发布进度和结果
 */
class PublishProgressReporter(
    private val channel: Channel,
    private val chatId: String
) {
    companion object {
        private const val TAG = "PublishProgressReporter"
    }

    private var lastMessageId: String = chatId

    /**
     * 汇报任务开始
     */
    fun reportStart(totalVideos: Int) {
        val message = "📹 开始发布任务\n共 $totalVideos 个视频待发布"
        sendMessage(message)
    }

    /**
     * 汇报单个视频进度
     */
    fun reportProgress(current: Int, total: Int, fileName: String) {
        val message = "⏳ 正在发布视频 $current/$total\n文件: $fileName"
        sendMessage(message)
    }

    /**
     * 汇报单个视频成功
     */
    fun reportVideoSuccess(current: Int, total: Int, fileName: String) {
        val message = "✅ 视频 $current/$total 发布成功\n文件: $fileName"
        sendMessage(message)
    }

    /**
     * 汇报单个视频失败
     */
    fun reportVideoFailed(current: Int, total: Int, fileName: String, error: String) {
        val message = "❌ 视频 $current/$total 发布失败\n文件: $fileName\n错误: $error"
        sendMessage(message)
    }

    /**
     * 汇报任务完成
     */
    fun reportComplete(successCount: Int, failedCount: Int, videos: List<VideoItem>) {
        val totalCount = successCount + failedCount

        val summary = buildString {
            appendLine("📊 发布任务完成")
            appendLine()
            appendLine("总计: $totalCount 个视频")
            appendLine("✅ 成功: $successCount")
            appendLine("❌ 失败: $failedCount")

            if (failedCount > 0) {
                appendLine()
                appendLine("失败列表:")
                videos.filter { it.status == PublishStatus.FAILED }.forEach { video ->
                    appendLine("  - ${video.fileName}: ${video.errorMessage ?: "未知错误"}")
                }
            }
        }

        sendMessage(summary)
    }

    /**
     * 发送消息到飞书
     */
    private fun sendMessage(message: String) {
        try {
            XLog.i(TAG, "汇报进度: $message")
            ChannelManager.sendMessage(channel, message, lastMessageId)
        } catch (e: Exception) {
            XLog.e(TAG, "发送进度消息失败", e)
        }
    }
}
