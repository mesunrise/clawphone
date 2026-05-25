package com.clawp.android.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clawp.android.R
import com.clawp.android.channel.ChannelManager
import com.clawp.android.service.ClawAccessibilityService
import com.clawp.android.utils.KVUtils

/**
 * 主页 - 显示服务状态和快捷操作
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvLlmStatus: TextView
    private lateinit var tvFeishuStatus: TextView
    private lateinit var btnOpenAccessibility: Button
    private lateinit var btnSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        tvLlmStatus = findViewById(R.id.tv_llm_status)
        tvFeishuStatus = findViewById(R.id.tv_feishu_status)
        btnOpenAccessibility = findViewById(R.id.btn_open_accessibility)
        btnSettings = findViewById(R.id.btn_settings)

        btnOpenAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        // 无障碍服务状态
        val isAccessibilityEnabled = ClawAccessibilityService.isRunning()
        tvAccessibilityStatus.text = if (isAccessibilityEnabled) {
            getString(R.string.home_accessibility_enabled)
        } else {
            getString(R.string.home_accessibility_disabled)
        }

        // LLM 配置状态
        val hasLlmConfig = KVUtils.hasLlmConfig()
        tvLlmStatus.text = if (hasLlmConfig) {
            getString(R.string.home_llm_configured)
        } else {
            getString(R.string.home_llm_not_configured)
        }

        // 飞书通道状态（简化判断：有配置即视为可用）
        val hasFeishuConfig = KVUtils.getFeishuAppId().isNotEmpty() &&
                              KVUtils.getFeishuAppSecret().isNotEmpty()
        tvFeishuStatus.text = if (hasFeishuConfig) {
            getString(R.string.home_feishu_connected)
        } else {
            getString(R.string.home_feishu_disconnected)
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
