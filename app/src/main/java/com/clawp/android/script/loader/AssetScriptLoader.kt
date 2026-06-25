package com.clawp.android.script.loader

import android.content.Context
import com.clawp.android.script.ScriptLoader
import com.clawp.android.script.ScriptParser
import com.clawp.android.script.model.Script
import com.clawp.android.script.model.ScriptMeta
import org.json.JSONObject
import java.io.IOException

/**
 * Loads scripts from the APK's `assets/scripts/` directory.
 *
 * @param context Android context for accessing assets.
 */
class AssetScriptLoader(private val context: Context) : ScriptLoader {

    companion object {
        private const val SCRIPTS_DIR = "scripts"
    }

    override fun listScripts(): List<ScriptMeta> {
        val metas = mutableListOf<ScriptMeta>()
        try {
            val files = context.assets.list(SCRIPTS_DIR) ?: emptyArray()
            for (fileName in files) {
                if (!fileName.endsWith(".json")) continue
                val meta = loadMetaOnly(fileName)
                if (meta != null) {
                    metas.add(meta)
                }
            }
        } catch (e: IOException) {
            // Directory doesn't exist or is inaccessible
        }
        return metas
    }

    override fun loadScript(name: String): Script {
        val fileName = if (name.endsWith(".json")) name else "$name.json"
        val content = readAsset("$SCRIPTS_DIR/$fileName")
        return ScriptParser.parse(content)
    }

    /**
     * Load only the meta section of a script file (lightweight, no full parse).
     */
    private fun loadMetaOnly(fileName: String): ScriptMeta? {
        return try {
            val content = readAsset("$SCRIPTS_DIR/$fileName")
            val json = JSONObject(content)
            val metaJson = json.optJSONObject("meta") ?: return null
            ScriptMeta(
                name = metaJson.optString("name", null)?.takeIf { it.isNotEmpty() }
                    ?: return null,
                version = metaJson.optString("version", null)?.takeIf { it.isNotEmpty() },
                description = metaJson.optString("description", null)?.takeIf { it.isNotEmpty() }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun readAsset(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}