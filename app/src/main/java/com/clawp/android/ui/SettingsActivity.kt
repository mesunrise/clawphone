package com.clawp.android.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clawp.android.R
import com.clawp.android.agent.AgentConfig
import com.clawp.android.agent.llm.LlmClientFactory
import com.clawp.android.channel.ChannelManager
import com.clawp.android.utils.KVUtils
import dev.langchain4j.data.message.UserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置页 - LLM 和飞书配置
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var etApiKey: EditText
    private lateinit var etBaseUrl: EditText
    private lateinit var etModelName: EditText
    private lateinit var etFeishuAppId: EditText
    private lateinit var etFeishuAppSecret: EditText
    private lateinit var btnSave: Button
    private lateinit var btnTestLlm: Button

    private var testJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etApiKey = findViewById(R.id.et_api_key)
        etBaseUrl = findViewById(R.id.et_base_url)
        etModelName = findViewById(R.id.et_model_name)
        etFeishuAppId = findViewById(R.id.et_feishu_app_id)
        etFeishuAppSecret = findViewById(R.id.et_feishu_app_secret)
        btnSave = findViewById(R.id.btn_save)
        btnTestLlm = findViewById(R.id.btn_test_llm)

        loadConfig()

        btnSave.setOnClickListener {
            saveConfig()
        }

        btnTestLlm.setOnClickListener {
            testLlmConnection()
        }
    }

    private fun loadConfig() {
        etApiKey.setText(KVUtils.getLlmApiKey())
        etBaseUrl.setText(KVUtils.getLlmBaseUrl())
        etModelName.setText(KVUtils.getLlmModelName())
        etFeishuAppId.setText(KVUtils.getFeishuAppId())
        etFeishuAppSecret.setText(KVUtils.getFeishuAppSecret())
    }

    private fun saveConfig() {
        val apiKey = etApiKey.text.toString().trim()
        val baseUrl = etBaseUrl.text.toString().trim()
        val modelName = etModelName.text.toString().trim()
        val feishuAppId = etFeishuAppId.text.toString().trim()
        val feishuAppSecret = etFeishuAppSecret.text.toString().trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API Key 不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        KVUtils.setLlmApiKey(apiKey)
        KVUtils.setLlmBaseUrl(baseUrl)
        KVUtils.setLlmModelName(modelName)
        KVUtils.setFeishuAppId(feishuAppId)
        KVUtils.setFeishuAppSecret(feishuAppSecret)

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
}
