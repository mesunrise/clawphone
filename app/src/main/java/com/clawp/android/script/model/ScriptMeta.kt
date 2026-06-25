package com.clawp.android.script.model

/**
 * Script metadata — displayed in UI for script selection.
 */
data class ScriptMeta(
    val name: String,
    val version: String? = null,
    val description: String? = null
)
