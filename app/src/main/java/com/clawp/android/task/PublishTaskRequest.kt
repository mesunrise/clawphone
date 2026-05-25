package com.clawp.android.task

/**
 * 发布任务请求
 * 包含视频列表、话题、描述等信息
 */
data class PublishTaskRequest(
    val videos: List<VideoItem>,
    val topics: List<String>,           // ["#美食", "#日常"]
    val description: String?,            // 视频描述文案
    val chatId: String,                  // 飞书 chat ID（用于回复）
    val rawInstruction: String           // 原始指令文本
)

/**
 * 视频项
 */
data class VideoItem(
    val localPath: String,
    val fileName: String,
    var status: PublishStatus = PublishStatus.PENDING,
    var errorMessage: String? = null
)

/**
 * 发布状态
 */
enum class PublishStatus {
    PENDING,      // 待发布
    PUBLISHING,   // 发布中
    SUCCESS,      // 成功
    FAILED        // 失败
}
