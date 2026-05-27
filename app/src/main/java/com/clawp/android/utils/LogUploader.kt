package com.clawp.android.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 日志自动上报器
 * 定时将 DebugLogCollector 中的日志上传到服务器
 */
object LogUploader {
    private const val TAG = "LogUploader"

    // 上传间隔（默认 5 分钟）
    private const val UPLOAD_INTERVAL_MS = 5 * 60 * 1000L

    // 服务器地址（需要在设置中配置）
    private var serverUrl: String = ""

    // 设备标识（用于区分不同设备的日志）
    private var deviceId: String = ""

    private val scope = CoroutineScope(Dispatchers.IO)
    private var uploadJob: Job? = null
    private var isEnabled = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 启动自动上报
     */
    fun start(serverUrl: String, deviceId: String) {
        if (serverUrl.isEmpty()) {
            XLog.w(TAG, "服务器地址为空，无法启动日志上报")
            return
        }

        this.serverUrl = serverUrl
        this.deviceId = deviceId
        this.isEnabled = true

        XLog.i(TAG, "启动日志自动上报: serverUrl=$serverUrl, deviceId=$deviceId, interval=${UPLOAD_INTERVAL_MS}ms")

        uploadJob?.cancel()
        uploadJob = scope.launch {
            while (isEnabled) {
                try {
                    uploadLogs()
                } catch (e: Exception) {
                    XLog.e(TAG, "日志上报失败", e)
                }
                delay(UPLOAD_INTERVAL_MS)
            }
        }
    }

    /**
     * 停止自动上报
     */
    fun stop() {
        isEnabled = false
        uploadJob?.cancel()
        uploadJob = null
        XLog.i(TAG, "停止日志自动上报")
    }

    /**
     * 立即上传一次日志
     */
    suspend fun uploadNow() {
        if (serverUrl.isEmpty()) {
            XLog.w(TAG, "服务器地址为空，无法上传日志")
            return
        }
        uploadLogs()
    }

    /**
     * 上传日志到服务器
     */
    private suspend fun uploadLogs() {
        val logs = DebugLogCollector.getAllLogs()
        if (logs.isEmpty()) {
            XLog.d(TAG, "没有日志需要上传")
            return
        }

        val timestamp = dateFormat.format(Date())
        val fileName = "clawp_${deviceId}_${timestamp}.log"

        // 格式化日志内容
        val logContent = DebugLogCollector.formatLogs(logs)

        XLog.i(TAG, "开始上传日志: fileName=$fileName, size=${logs.size} 条")

        try {
            // 构造请求
            val requestBody = logContent.toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url("$serverUrl/$fileName")
                .put(requestBody)
                .build()

            // 发送请求
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                XLog.i(TAG, "日志上传成功: $fileName")
                // 上传成功后清空日志（可选）
                // DebugLogCollector.clear()
            } else {
                XLog.e(TAG, "日志上传失败: ${response.code} ${response.message}")
            }

            response.close()
        } catch (e: Exception) {
            XLog.e(TAG, "日志上传异常: ${e.message}", e)
            throw e
        }
    }

    /**
     * 检查是否正在运行
     */
    fun isRunning(): Boolean = isEnabled
}
