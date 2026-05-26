package com.clawp.android.channel.feishu

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.clawp.android.utils.XLog
import com.lark.oapi.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 飞书文件下载器
 * 负责下载飞书消息中的视频/文件到本地存储，并注册到 MediaStore
 */
class FeiShuFileDownloader(
    private val context: Context,
    private val apiClient: Client
) {
    companion object {
        private const val TAG = "FeiShuFileDownloader"
        private const val DOWNLOAD_DIR = "clawp"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * 下载文件并返回本地路径
     * @param fileKey 飞书文件 key
     * @param fileName 文件名
     * @param messageId 消息 ID（用于日志）
     * @return 本地文件路径，失败返回 null
     */
    suspend fun downloadFile(
        fileKey: String,
        fileName: String,
        messageId: String
    ): String? = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                XLog.i(TAG, "开始下载文件: fileKey=$fileKey, fileName=$fileName, attempt=${attempt + 1}")

                // 调用飞书 API 获取文件内容
                val resp = apiClient.im().messageResource().get(
                    com.lark.oapi.service.im.v1.model.GetMessageResourceReq.newBuilder()
                        .messageId(messageId)
                        .fileKey(fileKey)
                        .type("file")
                        .build()
                )

                if (!resp.success()) {
                    throw Exception("飞书 API 返回错误: code=${resp.code}, msg=${resp.msg}")
                }

                // 飞书 SDK 返回的是文件对象，需要写入临时文件再读取
                val tempFile = File(context.cacheDir, "feishu_temp_${System.currentTimeMillis()}_$fileName")
                resp.writeFile(tempFile.absolutePath)

                val fileBytes = tempFile.readBytes()
                tempFile.delete()

                if (fileBytes.isEmpty()) {
                    throw Exception("文件内容为空")
                }

                XLog.i(TAG, "文件下载成功: size=${fileBytes.size} bytes")

                // 保存到本地并注册到 MediaStore
                val localPath = saveToLocalStorage(fileBytes, fileName)
                XLog.i(TAG, "文件已保存: $localPath")

                return@withContext localPath

            } catch (e: Exception) {
                lastException = e
                XLog.e(TAG, "下载文件失败 (attempt ${attempt + 1}/$MAX_RETRIES)", e)

                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1)) // 指数退避
                }
            }
        }

        XLog.e(TAG, "文件下载最终失败: fileKey=$fileKey", lastException)
        return@withContext null
    }

    /**
     * 保存文件到本地存储并注册到 MediaStore
     */
    private fun saveToLocalStorage(fileBytes: ByteArray, fileName: String): String {
        // 确定文件类型和目标目录
        val isVideo = isVideoFile(fileName)
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        // 创建 ContentValues
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH,
                if (isVideo) "${Environment.DIRECTORY_MOVIES}/$DOWNLOAD_DIR"
                else "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_DIR"
            )
        }

        // 插入到 MediaStore
        val uri = context.contentResolver.insert(collection, contentValues)
            ?: throw Exception("无法创建 MediaStore 条目")

        // 写入文件内容
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(fileBytes)
            outputStream.flush()
        } ?: throw Exception("无法打开输出流")

        // 获取实际文件路径
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                return cursor.getString(columnIndex)
            }
        }

        throw Exception("无法获取文件路径")
    }

    /**
     * 判断是否为视频文件
     */
    private fun isVideoFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in setOf("mp4", "mov", "avi", "mkv", "flv", "wmv", "3gp", "webm")
    }

    /**
     * 获取 MIME 类型
     */
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "flv" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            "3gp" -> "video/3gpp"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
    }
}
