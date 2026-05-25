package com.clawp.android.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clawp.android.R
import com.clawp.android.channel.ChannelManager
import com.clawp.android.utils.KVUtils

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etApiKey = findViewById(R.id.et_api_key)
        etBaseUrl = findViewById(R.id.et_base_url)
        etModelName = findViewById(R.id.et_model_name)
        etFeishuAppId = findViewById(R.id.et_feishu_app_id)
        etFeishuAppSecret = findViewById(R.id.et_feishu_app_secret)
        btnSave = findViewById(R.id.btn_save)

        loadConfig()

        btnSave.setOnClickListener {
            saveConfig()
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
}
