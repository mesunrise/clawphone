package com.clawp.android.utils

import com.clawp.android.channel.Channel
import com.clawp.android.channel.ChannelManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 调试日志收集器
 * 收集关键日志并支持发送到飞书机器人
 */
object DebugLogCollector {
    private const val TAG = "DebugLogCollector"
    private const val MAX_LOGS = 200 // 最多保留 200 条日志

    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    data class LogEntry(
        val timestamp: String,
        val tag: String,
        val level: String,
        val message: String
    )

    /**
     * 添加日志
     */
    fun log(tag: String, level: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(timestamp, tag, level, message)
        logs.offer(entry)

        // 限制日志数量
        while (logs.size > MAX_LOGS) {
            logs.poll()
        }
    }

    /**
     * 获取所有日志
     */
    fun getAllLogs(): List<LogEntry> {
        return logs.toList()
    }

    /**
     * 获取最近 N 条日志
     */
    fun getRecentLogs(count: Int): List<LogEntry> {
        val allLogs = logs.toList()
        return if (allLogs.size <= count) {
            allLogs
        } else {
            allLogs.takeLast(count)
        }
    }

    /**
     * 过滤日志（按 TAG）
     */
    fun filterByTag(tag: String): List<LogEntry> {
        return logs.filter { it.tag.contains(tag, ignoreCase = true) }
    }

    /**
     * 过滤日志（按关键词）
     */
    fun filterByKeyword(keyword: String): List<LogEntry> {
        return logs.filter {
            it.message.contains(keyword, ignoreCase = true) ||
            it.tag.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 清空日志
     */
    fun clear() {
        logs.clear()
    }

    /**
     * 格式化日志为文本
     */
    fun formatLogs(entries: List<LogEntry>): String {
        return buildString {
            appendLine("=== 调试日志 (共 ${entries.size} 条) ===")
            appendLine()
            entries.forEach { entry ->
                appendLine("[${entry.timestamp}] [${entry.level}] ${entry.tag}")
                appendLine("  ${entry.message}")
                appendLine()
            }
        }
    }

    /**
     * 发送日志到飞书机器人
     */
    fun sendToFeishu(messageId: String? = null, filter: String? = null, count: Int = 50) {
        try {
            val entries = when {
                filter != null -> filterByKeyword(filter)
                else -> getRecentLogs(count)
            }

            if (entries.isEmpty()) {
                val msg = if (filter != null) {
                    "未找到包含 '$filter' 的日志"
                } else {
                    "暂无日志"
                }
                ChannelManager.sendMessage(Channel.FEISHU, msg, messageId ?: "")
                return
            }

            val logText = formatLogs(entries)

            // 如果日志太长，分段发送
            if (logText.length > 3000) {
                val chunks = logText.chunked(3000)
                chunks.forEachIndexed { index, chunk ->
                    val header = if (index == 0) "" else "（续 ${index + 1}/${chunks.size}）\n"
                    ChannelManager.sendMessage(Channel.FEISHU, header + chunk, messageId ?: "")
                    Thread.sleep(500) // 避免发送过快
                }
            } else {
                ChannelManager.sendMessage(Channel.FEISHU, logText, messageId ?: "")
            }

            XLog.i(TAG, "已发送 ${entries.size} 条日志到飞书")
        } catch (e: Exception) {
            XLog.e(TAG, "发送日志到飞书失败", e)
        }
    }
}
