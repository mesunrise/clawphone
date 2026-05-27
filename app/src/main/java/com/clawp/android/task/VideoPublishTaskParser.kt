package com.clawp.android.task

import com.clawp.android.channel.feishu.MessageBatch
import com.clawp.android.utils.XLog

/**
 * 视频发布任务解析器
 * 从自然语言指令中提取发布意图、话题列表、描述等信息
 */
class VideoPublishTaskParser {

    companion object {
        private const val TAG = "VideoPublishTaskParser"

        // 发布关键词
        private val PUBLISH_KEYWORDS = setOf(
            "发", "发布", "发到", "发抖音", "发到抖音",
            "上传", "传到", "传抖音",
            "post", "upload", "publish"
        )

        // 话题正则
        private val TOPIC_REGEX = Regex("""#([^\s#]+)""")
    }

    /**
     * 解析消息批次，生成发布任务请求
     */
    fun parse(batch: MessageBatch): PublishTaskRequest? {
        XLog.i(TAG, "========================================")
        XLog.i(TAG, "VideoPublishTaskParser.parse() 被调用")
        XLog.i(TAG, "  - chatId: ${batch.chatId}")
        XLog.i(TAG, "  - videos: ${batch.videos.size}")
        XLog.i(TAG, "  - textMessages: ${batch.textMessages.size}")

        if (batch.videos.isEmpty()) {
            XLog.w(TAG, "  - 批次无视频，无法生成任务")
            XLog.i(TAG, "========================================")
            return null
        }

        // 合并所有文本消息
        val fullText = batch.textMessages.joinToString(" ")
        XLog.i(TAG, "  - 合并后的文本: $fullText")

        // 检查是否包含发布意图
        val hasIntent = containsPublishIntent(fullText)
        XLog.i(TAG, "  - 是否包含发布意图: $hasIntent")

        if (!hasIntent) {
            XLog.w(TAG, "  - 未检测到发布意图，返回 null")
            XLog.i(TAG, "========================================")
            return null
        }

        // 提取话题
        val topics = extractTopics(fullText)
        XLog.i(TAG, "  - 提取到的话题: $topics")

        // 提取描述（去掉话题后的剩余文本）
        val description = extractDescription(fullText, topics)
        XLog.i(TAG, "  - 提取到的描述: $description")

        val request = PublishTaskRequest(
            videos = batch.videos,
            topics = topics,
            description = description,
            chatId = batch.chatId,
            rawInstruction = fullText
        )

        XLog.i(TAG, "  - 任务解析成功，返回 PublishTaskRequest")
        XLog.i(TAG, "========================================")
        return request
    }

    /**
     * 检查是否包含发布意图
     */
    private fun containsPublishIntent(text: String): Boolean {
        val lowerText = text.lowercase()
        return PUBLISH_KEYWORDS.any { keyword ->
            lowerText.contains(keyword)
        }
    }

    /**
     * 提取话题列表
     */
    private fun extractTopics(text: String): List<String> {
        val topics = mutableListOf<String>()

        TOPIC_REGEX.findAll(text).forEach { match ->
            val topic = match.groupValues[1].trim()
            if (topic.isNotEmpty()) {
                // 规范化话题：确保以 # 开头，去除尾部标点
                val normalized = normalizeTopicText(topic)
                if (normalized.isNotEmpty() && !topics.contains("#$normalized")) {
                    topics.add("#$normalized")
                }
            }
        }

        XLog.i(TAG, "提取到 ${topics.size} 个话题: $topics")
        return topics
    }

    /**
     * 规范化话题文本
     */
    private fun normalizeTopicText(topic: String): String {
        // 去除尾部标点符号
        var normalized = topic.trimEnd('。', '，', '、', '！', '？', '.', ',', '!', '?', ' ')

        // 限制长度（抖音话题通常不超过20字符）
        if (normalized.length > 20) {
            normalized = normalized.substring(0, 20)
        }

        return normalized
    }

    /**
     * 提取描述文案
     */
    private fun extractDescription(text: String, topics: List<String>): String? {
        // 移除话题标签
        var description = text
        topics.forEach { topic ->
            description = description.replace(topic, "")
        }

        // 移除发布关键词
        PUBLISH_KEYWORDS.forEach { keyword ->
            description = description.replace(keyword, "", ignoreCase = true)
        }

        // 移除常见的连接词
        description = description
            .replace("把这些视频", "", ignoreCase = true)
            .replace("这些视频", "", ignoreCase = true)
            .replace("把视频", "", ignoreCase = true)
            .replace("视频", "", ignoreCase = true)
            .replace("到抖音", "", ignoreCase = true)
            .replace("抖音", "", ignoreCase = true)
            .replace("话题加上", "", ignoreCase = true)
            .replace("加上", "", ignoreCase = true)
            .trim()

        // 如果描述为空或太短，返回 null
        return if (description.length < 3) null else description
    }
}
