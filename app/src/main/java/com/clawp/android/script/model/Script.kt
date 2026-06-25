package com.clawp.android.script.model

/**
 * Top-level parsed script object.
 */
data class Script(
    val meta: ScriptMeta,
    val config: ScriptConfig,
    val setup: List<Action> = emptyList(),
    val rules: List<Rule> = emptyList()
) {
    init {
        require(rules.isNotEmpty()) { "Script must have at least one rule" }
    }
}