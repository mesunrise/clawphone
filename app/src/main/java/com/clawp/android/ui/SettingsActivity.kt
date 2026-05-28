package com.clawp.android.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.clawp.android.R
import com.clawp.android.agent.AgentConfig
import com.clawp.android.agent.AgentCallback
import com.clawp.android.agent.DefaultAgentService
import com.clawp.android.agent.llm.LlmClientFactory
import com.clawp.android.channel.Channel
import com.clawp.android.channel.ChannelManager
import com.clawp.android.tool.ToolResult
import com.clawp.android.utils.KVUtils
import com.clawp.android.utils.XLog
import dev.langchain4j.data.message.UserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置页 - LLM 和飞书配置
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    private lateinit var etApiKey: EditText
    private lateinit var etBaseUrl: EditText
    private lateinit var etModelName: EditText
    private lateinit var etFeishuAppId: EditText
    private lateinit var etFeishuAppSecret: EditText
    private lateinit var etLogServerUrl: EditText
    private lateinit var cbLogUploadEnabled: CheckBox
    private lateinit var btnSave: Button
    private lateinit var btnTestLlm: Button
    private lateinit var btnTestAgent: Button
    private lateinit var btnTestFeishu: Button
    private lateinit var btnSendTestMessage: Button
    private lateinit var btnCheckNotificationPermission: Button
    private lateinit var tvVersion: TextView
    private lateinit var tvMessageLog: TextView
    private lateinit var svMessageLog: ScrollView

    private var testJob: Job? = null
    private val messageLog = mutableListOf<String>()

    private val messageListener = object : ChannelManager.OnMessageReceivedListener {
        override fun onMessageReceived(channel: Channel, message: String, messageID: String, chatId: String) {
            runOnUiThread {
                addMessageToLog("收到消息", message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etApiKey = findViewById(R.id.et_api_key)
        etBaseUrl = findViewById(R.id.et_base_url)
        etModelName = findViewById(R.id.et_model_name)
        etFeishuAppId = findViewById(R.id.et_feishu_app_id)
        etFeishuAppSecret = findViewById(R.id.et_feishu_app_secret)
        etLogServerUrl = findViewById(R.id.et_log_server_url)
        cbLogUploadEnabled = findViewById(R.id.cb_log_upload_enabled)
        btnSave = findViewById(R.id.btn_save)
        btnTestLlm = findViewById(R.id.btn_test_llm)
        btnTestAgent = findViewById(R.id.btn_test_agent)
        btnTestFeishu = findViewById(R.id.btn_test_feishu)
        btnSendTestMessage = findViewById(R.id.btn_send_test_message)
        btnCheckNotificationPermission = findViewById(R.id.btn_check_notification_permission)
        tvVersion = findViewById(R.id.tv_version)
        tvMessageLog = findViewById(R.id.tv_message_log)
        svMessageLog = findViewById(R.id.sv_message_log)

        // 显示版本号
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
            tvVersion.text = "版本: $versionName (build $versionCode)"
        } catch (e: Exception) {
            tvVersion.text = "版本: 未知"
        }

        loadConfig()
        setupMessageListener()

        btnSave.setOnClickListener {
            saveConfig()
        }

        btnTestLlm.setOnClickListener {
            testLlmConnection()
        }

        btnTestAgent.setOnClickListener {
            testAgent()
        }

        btnTestFeishu.setOnClickListener {
            testFeishuConnection()
        }

        btnSendTestMessage.setOnClickListener {
            sendTestMessage()
        }

        btnCheckNotificationPermission.setOnClickListener {
            checkNotificationPermission()
        }
    }

    private fun loadConfig() {
        etApiKey.setText(KVUtils.getLlmApiKey())
        etBaseUrl.setText(KVUtils.getLlmBaseUrl())
        etModelName.setText(KVUtils.getLlmModelName())
        etFeishuAppId.setText(KVUtils.getFeishuAppId())
        etFeishuAppSecret.setText(KVUtils.getFeishuAppSecret())
        etLogServerUrl.setText(KVUtils.getLogServerUrl())
        cbLogUploadEnabled.isChecked = KVUtils.isLogUploadEnabled()
    }

    private fun saveConfig() {
        val apiKey = etApiKey.text.toString().trim()
        val baseUrl = etBaseUrl.text.toString().trim()
        val modelName = etModelName.text.toString().trim()
        val feishuAppId = etFeishuAppId.text.toString().trim()
        val feishuAppSecret = etFeishuAppSecret.text.toString().trim()
        val logServerUrl = etLogServerUrl.text.toString().trim()
        val logUploadEnabled = cbLogUploadEnabled.isChecked

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API Key 不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        KVUtils.setLlmApiKey(apiKey)
        KVUtils.setLlmBaseUrl(baseUrl)
        KVUtils.setLlmModelName(modelName)
        KVUtils.setFeishuAppId(feishuAppId)
        KVUtils.setFeishuAppSecret(feishuAppSecret)
        KVUtils.setLogServerUrl(logServerUrl)
        KVUtils.setLogUploadEnabled(logUploadEnabled)

        // 启动或停止日志上报
        if (logUploadEnabled && logServerUrl.isNotEmpty()) {
            val deviceId = "${Build.BRAND}_${Build.MODEL}_${Build.SERIAL}".replace(" ", "_")
            com.clawp.android.utils.LogUploader.start(logServerUrl, deviceId)
            addMessageToLog("系统", "日志自动上报已启动")
        } else {
            com.clawp.android.utils.LogUploader.stop()
            addMessageToLog("系统", "日志自动上报已停止")
        }

        // 重新初始化飞书通道
        if (feishuAppId.isNotEmpty() && feishuAppSecret.isNotEmpty()) {
            ChannelManager.reinitFeiShuFromStorage()
        }

        Toast.makeText(this, getString(R.string.settings_save_success), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun testLlmConnection() {
        // 防止重复点击
        if (testJob?.isActive == true) {
            return
        }

        val apiKey = etApiKey.text.toString().trim()
        val baseUrl = etBaseUrl.text.toString().trim()
        val modelName = etModelName.text.toString().trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.settings_test_llm_no_config), Toast.LENGTH_SHORT).show()
            return
        }

        // 禁用按钮，显示测试中
        btnTestLlm.isEnabled = false
        Toast.makeText(this, getString(R.string.settings_test_llm_testing), Toast.LENGTH_SHORT).show()

        // 使用 Activity 的生命周期感知协程作用域
        testJob = lifecycleScope.launch {
            var success = false
            var errorMsg = ""

            try {
                // 在 IO 线程执行网络请求
                withContext(Dispatchers.IO) {
                    // 构建测试配置
                    val config = AgentConfig(
                        apiKey = apiKey,
                        baseUrl = baseUrl.ifEmpty { "https://api.gpugeek.com/v1" },
                        modelName = modelName.ifEmpty { "Vendor3/DeepSeek-V4-Flash" },
                        temperature = 0.7,
                        provider = com.clawp.android.agent.LlmProvider.ANTHROPIC,
                        streaming = false
                    )

                    // 创建 LLM 客户端
                    val llmClient = LlmClientFactory.create(config)

                    // 发送简单的测试消息
                    val testMessages = listOf(UserMessage.from("Hello"))
                    val response = llmClient.chat(testMessages, emptyList())

                    // 检查响应
                    if (response.text != null || response.toolExecutionRequests.isNotEmpty()) {
                        success = true
                    } else {
                        errorMsg = "No response from LLM"
                    }
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "Unknown error"
                e.printStackTrace()
            }

            // 已经在主线程（lifecycleScope 默认在主线程）
            btnTestLlm.isEnabled = true
            if (success) {
                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.settings_test_llm_success),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.settings_test_llm_failed, errorMsg),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun testAgent() {
        if (testJob?.isActive == true) {
            return
        }

        val apiKey = etApiKey.text.toString().trim()
        val baseUrl = etBaseUrl.text.toString().trim()
        val modelName = etModelName.text.toString().trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查无障碍服务
        val accessibilityEnabled = com.clawp.android.service.ClawAccessibilityService.getInstance() != null
        if (!accessibilityEnabled) {
            addMessageToLog("Agent 测试", "❌ 无障碍服务未启用，请先开启")
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            return
        }

        btnTestAgent.isEnabled = false
        addMessageToLog("Agent 测试", "开始测试 Agent（含工具调用）...")

        val agentService = DefaultAgentService()
        val config = AgentConfig(
            apiKey = apiKey,
            baseUrl = baseUrl.ifEmpty { "https://api.gpugeek.com/v1" },
            modelName = modelName.ifEmpty { "Vendor3/DeepSeek-V4-Flash" },
            temperature = 0.7,
            provider = com.clawp.android.agent.LlmProvider.ANTHROPIC,
            streaming = true,
            maxIterations = 5
        )

        agentService.initialize(config)

        val startTime = System.currentTimeMillis()
        agentService.executeTask("请获取当前屏幕信息（调用 get_screen_info），然后告诉我当前屏幕上显示了什么内容。调用 finish 报告结果。", object : AgentCallback {
            override fun onLoopStart(round: Int) {
                val elapsed = System.currentTimeMillis() - startTime
                runOnUiThread {
                    addMessageToLog("Agent 测试", "第 ${round} 轮开始 (${elapsed}ms)")
                }
            }

            override fun onContent(round: Int, content: String) {
                runOnUiThread {
                    addMessageToLog("Agent 思考", content.take(200))
                }
            }

            override fun onToolCall(round: Int, toolId: String, toolName: String, parameters: String) {
                runOnUiThread {
                    addMessageToLog("Agent 工具", "调用: $toolName($parameters)")
                }
            }

            override fun onToolResult(round: Int, toolId: String, toolName: String, parameters: String, result: ToolResult) {
                val resultPreview = if (result.isSuccess) "✅ 成功" else "❌ 失败: ${result.error}"
                runOnUiThread {
                    addMessageToLog("Agent 工具", "结果: $toolName → $resultPreview")
                }
            }

            override fun onComplete(round: Int, finalAnswer: String, totalTokens: Int) {
                val elapsed = System.currentTimeMillis() - startTime
                runOnUiThread {
                    addMessageToLog("Agent 测试", "✅ 完成! (${elapsed}ms, ${round}轮, ${totalTokens}tokens)")
                    addMessageToLog("Agent 结果", finalAnswer.take(300))
                    btnTestAgent.isEnabled = true
                }
                agentService.shutdown()
            }

            override fun onError(round: Int, error: Exception, totalTokens: Int) {
                val elapsed = System.currentTimeMillis() - startTime
                runOnUiThread {
                    addMessageToLog("Agent 测试", "❌ 失败 (${elapsed}ms, ${round}轮): ${error.message}")
                    btnTestAgent.isEnabled = true
                }
                agentService.shutdown()
            }

            override fun onSystemDialogBlocked(round: Int, totalTokens: Int) {
                runOnUiThread {
                    addMessageToLog("Agent 测试", "⚠️ 系统弹窗拦截")
                }
            }
        })
    }

    private fun testFeishuConnection() {
        val feishuAppId = etFeishuAppId.text.toString().trim()
        val feishuAppSecret = etFeishuAppSecret.text.toString().trim()

        if (feishuAppId.isEmpty() || feishuAppSecret.isEmpty()) {
            Toast.makeText(this, "请先配置飞书 App ID 和 Secret", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查飞书连接状态
        val isConnected = ChannelManager.isFeiShuConnected()
        val hasListener = ChannelManager.hasMessageListener()

        val statusMsg = buildString {
            appendLine("飞书连接诊断:")
            appendLine("- App ID: ${feishuAppId.take(10)}...")
            appendLine("- WebSocket 连接: ${if (isConnected) "✅ 已连接" else "❌ 未连接"}")
            appendLine("- 消息监听器: ${if (hasListener) "✅ 已注册" else "❌ 未注册"}")
            appendLine()
            appendLine("请在飞书中向机器人发送消息测试")
        }

        XLog.i(TAG, statusMsg)
        Toast.makeText(this, statusMsg, Toast.LENGTH_LONG).show()
    }

    private fun setupMessageListener() {
        // 注册消息监听器，在设置页显示收到的消息
        ChannelManager.setOnMessageReceivedListener(messageListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除监听器，避免内存泄漏
        ChannelManager.removeOnMessageReceivedListener(messageListener)
    }

    private fun addMessageToLog(prefix: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $prefix: $message"
        messageLog.add(0, logEntry)

        // 只保留最近 20 条
        if (messageLog.size > 20) {
            messageLog.removeAt(messageLog.size - 1)
        }

        tvMessageLog.text = messageLog.joinToString("\n\n")

        // 滚动到顶部
        svMessageLog.post {
            svMessageLog.fullScroll(ScrollView.FOCUS_UP)
        }
    }

    private fun sendTestMessage() {
        val feishuAppId = etFeishuAppId.text.toString().trim()
        val feishuAppSecret = etFeishuAppSecret.text.toString().trim()

        if (feishuAppId.isEmpty() || feishuAppSecret.isEmpty()) {
            Toast.makeText(this, "请先配置并保存飞书 App ID 和 Secret", Toast.LENGTH_SHORT).show()
            return
        }

        if (!ChannelManager.isFeiShuConnected()) {
            Toast.makeText(this, "飞书未连接，请先保存配置", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取最后一次收到消息的 messageID
        val lastMessageId = ChannelManager.getLastSenderId(Channel.FEISHU)
        if (lastMessageId.isNullOrEmpty()) {
            Toast.makeText(this, "请先在飞书中向机器人发送一条消息，然后再测试", Toast.LENGTH_LONG).show()
            return
        }

        val testMessage = "测试回复 - ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"

        // 使用最后一次的 messageID 回复消息
        ChannelManager.sendMessage(Channel.FEISHU, testMessage, lastMessageId)

        addMessageToLog("发送消息", testMessage)
        Toast.makeText(this, "已发送测试消息（回复到最后一条消息）", Toast.LENGTH_SHORT).show()
    }

    private fun checkNotificationPermission() {
        val statusMsg = buildString {
            appendLine("通知权限诊断:")
            appendLine()

            // 1. 检查运行时权限（Android 13+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@SettingsActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                appendLine("1. 运行时权限: ${if (hasPermission) "✅ 已授予" else "❌ 未授予"}")

                if (!hasPermission) {
                    appendLine("   → 需要请求权限")
                }
            } else {
                appendLine("1. 运行时权限: ✅ 不需要（Android < 13）")
            }
            appendLine()

            // 2. 检查通知是否启用
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else {
                true
            }
            appendLine("2. 通知总开关: ${if (notificationsEnabled) "✅ 已启用" else "❌ 已禁用"}")

            if (!notificationsEnabled) {
                appendLine("   → 需要在系统设置中开启")
            }
            appendLine()

            // 3. 检查通知渠道状态
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel("clawp_message_channel")
                if (channel != null) {
                    val importance = channel.importance
                    val importanceText = when (importance) {
                        NotificationManager.IMPORTANCE_NONE -> "❌ 已禁用"
                        NotificationManager.IMPORTANCE_MIN -> "⚠️ 最低"
                        NotificationManager.IMPORTANCE_LOW -> "⚠️ 低"
                        NotificationManager.IMPORTANCE_DEFAULT -> "✅ 默认"
                        NotificationManager.IMPORTANCE_HIGH -> "✅ 高"
                        else -> "未知"
                    }
                    appendLine("3. 通知渠道: $importanceText")

                    if (importance == NotificationManager.IMPORTANCE_NONE) {
                        appendLine("   → 渠道已被禁用，需要在系统设置中开启")
                    }
                } else {
                    appendLine("3. 通知渠道: ❌ 未创建")
                }
            } else {
                appendLine("3. 通知渠道: ✅ 不需要（Android < 8）")
            }
            appendLine()

            // 4. 检查勿扰模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val filter = notificationManager.currentInterruptionFilter
                val dndEnabled = filter != NotificationManager.INTERRUPTION_FILTER_ALL
                appendLine("4. 勿扰模式: ${if (dndEnabled) "⚠️ 已开启" else "✅ 未开启"}")
            }
        }

        XLog.i(TAG, statusMsg)

        // 显示诊断结果
        AlertDialog.Builder(this)
            .setTitle("通知权限诊断")
            .setMessage(statusMsg)
            .setPositiveButton("请求权限") { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton("打开设置") { _, _ ->
                openNotificationSettings()
            }
            .setNeutralButton("关闭", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "当前系统版本不需要请求权限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", packageName, null)
                }
            }
        }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
                addMessageToLog("系统", "通知权限已授予")
            } else {
                Toast.makeText(this, "通知权限被拒绝", Toast.LENGTH_SHORT).show()
                addMessageToLog("系统", "通知权限被拒绝")
            }
        }
    }
}
