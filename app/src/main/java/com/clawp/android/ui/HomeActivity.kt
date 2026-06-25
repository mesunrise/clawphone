package com.clawp.android.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clawp.android.ClawApplication
import com.clawp.android.R
import com.clawp.android.script.ScriptEngine
import com.clawp.android.script.ScriptLoader
import com.clawp.android.service.ClawAccessibilityService
import com.clawp.android.utils.KVUtils

/**
 * 主页 - 显示服务状态、脚本列表和快捷操作。
 *
 * 两种模式：
 *  - Agent 模式：原有的 LLM Agent 自动化
 *  - 脚本模式：基于 JSON 脚本的确定性自动化
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvLlmStatus: TextView
    private lateinit var tvFeishuStatus: TextView
    private lateinit var btnOpenAccessibility: Button
    private lateinit var btnSettings: Button

    // ── 脚本模式 ──────────────────────────────────────────────────────
    private lateinit var tabAgent: Button
    private lateinit var tabScript: Button
    private lateinit var panelAgent: LinearLayout
    private lateinit var panelScript: LinearLayout
    private lateinit var scriptListContainer: LinearLayout
    private lateinit var tvScriptSelected: TextView
    private lateinit var tvScriptState: TextView
    private lateinit var tvScriptRound: TextView
    private lateinit var btnStartScript: Button
    private lateinit var btnStopScript: Button
    private lateinit var tvScriptLog: TextView
    private lateinit var scrollScriptLog: ScrollView

    private val scriptEngine: ScriptEngine get() = ClawApplication.instance.scriptEngine
    private val scriptLoader: ScriptLoader get() = ClawApplication.instance.scriptLoader
    private var selectedScriptMeta: com.clawp.android.script.model.ScriptMeta? = null
    private var selectedScript: com.clawp.android.script.model.Script? = null
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        tvLlmStatus = findViewById(R.id.tv_llm_status)
        tvFeishuStatus = findViewById(R.id.tv_feishu_status)
        btnOpenAccessibility = findViewById(R.id.btn_open_accessibility)
        btnSettings = findViewById(R.id.btn_settings)

        // Script mode views
        tabAgent = findViewById(R.id.tab_agent)
        tabScript = findViewById(R.id.tab_script)
        panelAgent = findViewById(R.id.panel_agent)
        panelScript = findViewById(R.id.panel_script)
        scriptListContainer = findViewById(R.id.script_list_container)
        tvScriptSelected = findViewById(R.id.tv_script_selected)
        tvScriptState = findViewById(R.id.tv_script_state)
        tvScriptRound = findViewById(R.id.tv_script_round)
        btnStartScript = findViewById(R.id.btn_start_script)
        btnStopScript = findViewById(R.id.btn_stop_script)
        tvScriptLog = findViewById(R.id.tv_script_log)
        scrollScriptLog = findViewById(R.id.scroll_script_log)

        btnOpenAccessibility.setOnClickListener { openAccessibilitySettings() }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        // Tab switching
        tabAgent.setOnClickListener { switchToTab(TAB_AGENT) }
        tabScript.setOnClickListener { switchToTab(TAB_SCRIPT) }

        // Script buttons
        btnStartScript.setOnClickListener { startScript() }
        btnStopScript.setOnClickListener { stopScript() }

        // Default: show agent tab
        switchToTab(TAB_AGENT)
        setupScriptList()
        setupEngineCallbacks()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateScriptState()
        refreshScriptList()
    }

    // ── Tabs ──────────────────────────────────────────────────────────

    private fun switchToTab(tab: Int) {
        val isAgent = tab == TAB_AGENT
        tabAgent.isSelected = isAgent
        tabScript.isSelected = !isAgent
        panelAgent.visibility = if (isAgent) View.VISIBLE else View.GONE
        panelScript.visibility = if (isAgent) View.GONE else View.VISIBLE
    }

    // ── Status ────────────────────────────────────────────────────────

    private fun updateStatus() {
        val isA11y = ClawAccessibilityService.isRunning()
        tvAccessibilityStatus.text = if (isA11y) {
            getString(R.string.home_accessibility_enabled)
        } else {
            getString(R.string.home_accessibility_disabled)
        }

        val hasLlm = KVUtils.hasLlmConfig()
        tvLlmStatus.text = if (hasLlm) {
            getString(R.string.home_llm_configured)
        } else {
            getString(R.string.home_llm_not_configured)
        }

        val hasFeishu = KVUtils.getFeishuAppId().isNotEmpty() &&
                KVUtils.getFeishuAppSecret().isNotEmpty()
        tvFeishuStatus.text = if (hasFeishu) {
            getString(R.string.home_feishu_connected)
        } else {
            getString(R.string.home_feishu_disconnected)
        }
    }

    // ── Script List ───────────────────────────────────────────────────

    private fun setupScriptList() {
        refreshScriptList()
    }

    private fun refreshScriptList() {
        scriptListContainer.removeAllViews()
        val scripts = try {
            scriptLoader.listScripts()
        } catch (_: Exception) {
            emptyList()
        }

        if (scripts.isEmpty()) {
            val tv = TextView(this).apply {
                text = getString(R.string.home_script_none)
                setTextColor(0xFF888888.toInt())
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            scriptListContainer.addView(tv)
            return
        }

        for (meta in scripts) {
            val item = layoutInflater.inflate(
                R.layout.item_script, scriptListContainer, false
            )
            val tvName = item.findViewById<TextView>(R.id.tv_script_name)
            val tvDesc = item.findViewById<TextView>(R.id.tv_script_desc)

            tvName.text = meta.name
            tvDesc.text = meta.description ?: ""

            item.setOnClickListener {
                selectScript(meta)
            }

            // Highlight if selected
            if (selectedScriptMeta?.name == meta.name) {
                item.setBackgroundColor(0x2200AAFF.toInt())
            }

            scriptListContainer.addView(item)
        }
    }

    private fun selectScript(meta: com.clawp.android.script.model.ScriptMeta) {
        selectedScriptMeta = meta
        try {
            selectedScript = scriptLoader.loadScript(meta.name)
            tvScriptSelected.text = getString(
                R.string.home_script_selected,
                meta.name,
                selectedScript?.config?.loopCount ?: 0
            )
            appendLog("✓ 已选择脚本: ${meta.name}")
        } catch (e: Exception) {
            tvScriptSelected.text = "脚本加载失败: ${e.message}"
            selectedScript = null
        }
        refreshScriptList()
    }

    // ── Engine Callbacks ──────────────────────────────────────────────

    private fun setupEngineCallbacks() {
        scriptEngine.onStateChanged = { state ->
            runOnUiThread { updateScriptState() }
        }
        scriptEngine.onRoundUpdated = { round, ruleName ->
            runOnUiThread {
                tvScriptRound.text = getString(
                    R.string.home_script_round,
                    round,
                    selectedScript?.config?.loopCount ?: 0
                )
            }
        }
        scriptEngine.onLog = { msg ->
            runOnUiThread { appendLog(msg) }
        }
    }

    private fun updateScriptState() {
        val state = scriptEngine.state
        tvScriptState.text = getString(R.string.home_script_state, state.name)

        when (state) {
            ScriptEngine.State.IDLE -> {
                btnStartScript.isEnabled = selectedScript != null
                btnStopScript.isEnabled = false
            }
            ScriptEngine.State.RUNNING -> {
                btnStartScript.isEnabled = false
                btnStopScript.isEnabled = true
            }
            ScriptEngine.State.STOPPING -> {
                btnStartScript.isEnabled = false
                btnStopScript.isEnabled = false
            }
        }
    }

    // ── Script Control ────────────────────────────────────────────────

    private fun startScript() {
        val script = selectedScript
        if (script == null) {
            Toast.makeText(this, R.string.home_script_none, Toast.LENGTH_SHORT).show()
            return
        }

        if (!ClawAccessibilityService.isRunning()) {
            Toast.makeText(this, R.string.home_script_a11y_required, Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }

        logBuilder.clear()
        tvScriptLog.text = ""
        val success = scriptEngine.start(script)
        if (!success) {
            Toast.makeText(this, R.string.home_script_start_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopScript() {
        scriptEngine.stop()
    }

    private fun appendLog(msg: String) {
        logBuilder.append(msg).append("\n")
        // Keep last 200 lines
        val lines = logBuilder.lines()
        if (lines.size > 200) {
            logBuilder.clear()
            logBuilder.append(lines.takeLast(200).joinToString("\n"))
        }
        tvScriptLog.text = logBuilder.toString()
        scrollScriptLog.post {
            scrollScriptLog.fullScroll(View.FOCUS_DOWN)
        }
    }

    // ── Accessibility ─────────────────────────────────────────────────

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAB_AGENT = 0
        private const val TAB_SCRIPT = 1
    }
}
