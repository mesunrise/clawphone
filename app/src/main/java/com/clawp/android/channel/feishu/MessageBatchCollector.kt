package com.clawp.android.channel.feishu

import com.clawp.android.utils.XLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 消息批次收集器
 * 在指定时间窗口内聚合同一用户的多条消息（视频+文本），然后触发批次处理
 */
class MessageBatchCollector(
    private val scope: CoroutineScope,
    private val windowMillis: Long = 30_000L, // 30秒窗口
    private val onBatchComplete: (MessageBatch) -> Unit
) {
    companion object {
        private const val TAG = "MessageBatchCollector"
    }

    private val batches = ConcurrentHashMap<String, MessageBatch>()
    private val timers = ConcurrentHashMap<String, Job>()

    /**
     * 添加文本消息
     */
    fun addTextMessage(chatId: String, text: String, messageId: String) {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "MessageBatchCollector.addTextMessage() 被调用")
        XLog.i(TAG, "  - chatId: $chatId")
        XLog.i(TAG, "  - text: $text")
        XLog.i(TAG, "  - messageId: $messageId")

        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "addTextMessage: chatId=$chatId, text=$text")

        val batch = batches.getOrPut(chatId) {
            XLog.i(TAG, "  - 创建新批次: chatId=$chatId")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "创建新批次: chatId=$chatId")
            MessageBatch(chatId = chatId)
        }

        batch.textMessages.add(text)
        XLog.i(TAG, "  - 当前批次状态: videos=${batch.videos.size}, texts=${batch.textMessages.size}")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "批次状态: videos=${batch.videos.size}, texts=${batch.textMessages.size}")

        // 文本消息触发批次完成（或重置定时器）
        if (batch.videos.isNotEmpty()) {
            // 已有视频，立即触发
            XLog.i(TAG, "  - 已有视频，立即触发批次完成")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "已有视频，立即触发批次完成")
            completeBatch(chatId)
        } else {
            // 还没有视频，重置定时器等待
            XLog.i(TAG, "  - 还没有视频，重置定时器等待")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "还没有视频，重置定时器等待")
            resetTimer(chatId)
        }
        XLog.i(TAG, "========================================")
    }

    /**
     * 添加视频消息
     */
    fun addVideoMessage(chatId: String, videoPath: String, fileName: String) {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "MessageBatchCollector.addVideoMessage() 被调用")
        XLog.i(TAG, "  - chatId: $chatId")
        XLog.i(TAG, "  - videoPath: $videoPath")
        XLog.i(TAG, "  - fileName: $fileName")

        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "addVideoMessage: chatId=$chatId, fileName=$fileName")

        val batch = batches.getOrPut(chatId) {
            XLog.i(TAG, "  - 创建新批次: chatId=$chatId")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "创建新批次: chatId=$chatId")
            MessageBatch(chatId = chatId)
        }

        batch.videos.add(com.clawp.android.task.VideoItem(localPath = videoPath, fileName = fileName))
        XLog.i(TAG, "  - 当前批次状态: videos=${batch.videos.size}, texts=${batch.textMessages.size}")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "批次状态: videos=${batch.videos.size}, texts=${batch.textMessages.size}")

        // 视频消息不立即触发，等待文本指令或超时
        XLog.i(TAG, "  - 视频消息不立即触发，重置定时器等待文本指令")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "视频消息不立即触发，重置定时器等待文本指令")
        resetTimer(chatId)
        XLog.i(TAG, "========================================")
    }

    /**
     * 重置定时器
     */
    private fun resetTimer(chatId: String) {
        // 取消旧定时器
        timers[chatId]?.cancel()

        // 启动新定时器
        val job = scope.launch {
            delay(windowMillis)
            XLog.i(TAG, "批次窗口超时: chatId=$chatId")
            completeBatch(chatId)
        }

        timers[chatId] = job
    }

    /**
     * 完成批次并触发回调
     */
    private fun completeBatch(chatId: String) {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "completeBatch() 被调用: chatId=$chatId")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "completeBatch: chatId=$chatId")

        timers[chatId]?.cancel()
        timers.remove(chatId)

        val batch = batches.remove(chatId)
        if (batch == null) {
            XLog.w(TAG, "  - 批次不存在，已被移除")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "WARN", "批次不存在，已被移除")
            XLog.i(TAG, "========================================")
            return
        }

        XLog.i(TAG, "  - 批次内容: videos=${batch.videos.size}, texts=${batch.textMessages.size}")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "批次内容: videos=${batch.videos.size}, texts=${batch.textMessages.size}")

        if (batch.videos.isEmpty()) {
            XLog.i(TAG, "  - 批次无视频，忽略")
            com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "批次无视频，忽略")
            XLog.i(TAG, "========================================")
            return
        }

        XLog.i(TAG, "  - 批次完成，触发回调 onBatchComplete()")
        com.clawp.android.utils.DebugLogCollector.log(TAG, "INFO", "批次完成，触发回调 onBatchComplete()")
        XLog.i(TAG, "========================================")
        onBatchComplete(batch)
    }
}

/**
 * 消息批次
 */
data class MessageBatch(
    val chatId: String,
    val videos: MutableList<com.clawp.android.task.VideoItem> = mutableListOf(),
    val textMessages: MutableList<String> = mutableListOf()
)
