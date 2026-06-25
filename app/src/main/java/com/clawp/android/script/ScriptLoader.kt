package com.clawp.android.script

import com.clawp.android.script.model.Script
import com.clawp.android.script.model.ScriptMeta

/**
 * Abstraction for loading script definitions from a source.
 *
 * Future implementations could load scripts from remote servers,
 * local storage, or other sources.
 */
interface ScriptLoader {
    /** List all available scripts (metadata only, lightweight). */
    fun listScripts(): List<ScriptMeta>

    /** Load the full script by name. */
    fun loadScript(name: String): Script
}