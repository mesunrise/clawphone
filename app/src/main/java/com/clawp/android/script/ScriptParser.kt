package com.clawp.android.script

import com.clawp.android.script.model.Action
import com.clawp.android.script.model.Condition
import com.clawp.android.script.model.HumanizeParams
import com.clawp.android.script.model.Rule
import com.clawp.android.script.model.Script
import com.clawp.android.script.model.ScriptConfig
import com.clawp.android.script.model.ScriptMeta
import com.clawp.android.script.model.RoundDelay
import com.clawp.android.script.model.UiTarget
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses a script JSON string into a [Script] domain object.
 *
 * Supports variable substitution: `{{config.targetPackage}}` is replaced
 * with the actual value of `config.targetPackage` after parsing.
 */
object ScriptParser {

    private val VARIABLE_PATTERN = Regex("\\{\\{\\s*config\\.targetPackage\\s*\\}\\}")

    /**
     * Parse a JSON string into a [Script].
     *
     * @throws IllegalArgumentException if the JSON is malformed or required
     *         fields are missing.
     */
    fun parse(jsonString: String): Script {
        val raw = JSONObject(jsonString)

        // --- meta (required) ---
        val metaJson = raw.optJSONObject("meta")
            ?: throw IllegalArgumentException("Missing required field: meta")
        val meta = parseMeta(metaJson)

        // --- config (required) ---
        val configJson = raw.optJSONObject("config")
            ?: throw IllegalArgumentException("Missing required field: config")
        val config = parseConfig(configJson)

        // --- Variable substitution ---
        val substituted = applyVariables(raw, config)

        // --- setup (optional) ---
        val setup = if (substituted.has("setup")) {
            parseActions(substituted.getJSONArray("setup"))
        } else {
            emptyList()
        }

        // --- rules (required) ---
        val rulesJson = substituted.optJSONArray("rules")
            ?: throw IllegalArgumentException("Missing required field: rules")
        if (rulesJson.length() == 0) {
            throw IllegalArgumentException("rules array must not be empty")
        }
        val rules = parseRules(rulesJson)

        return Script(meta, config, setup, rules)
    }

    // ── meta ──────────────────────────────────────────────────────────

    private fun parseMeta(json: JSONObject): ScriptMeta {
        return ScriptMeta(
            name = json.optString("name", null) ?: throw IllegalArgumentException("meta.name is required"),
            version = json.optString("version", "1.0"),
            description = json.optString("description", null)
        )
    }

    // ── config ────────────────────────────────────────────────────────

    private fun parseConfig(json: JSONObject): ScriptConfig {
        val targetPackage = json.optString("targetPackage", null)
            ?: throw IllegalArgumentException("config.targetPackage is required")
        
        val loopCount = json.optInt("loopCount", 1000)
        val loopDurationSec = if (json.has("loopDurationSec")) json.optInt("loopDurationSec") else null

        val roundDelay = if (json.has("roundDelay")) {
            parseRoundDelay(json.getJSONObject("roundDelay"))
        } else {
            RoundDelay()
        }

        return ScriptConfig(
            targetPackage = targetPackage,
            loopCount = loopCount,
            loopDurationSec = loopDurationSec,
            roundDelay = roundDelay
        )
    }

    private fun parseRoundDelay(json: JSONObject): com.clawp.android.script.model.RoundDelay {
        return com.clawp.android.script.model.RoundDelay(
            min = json.optDouble("min", 3.0),
            max = json.optDouble("max", 5.0)
        )
    }

    // ── rules ─────────────────────────────────────────────────────────

    private fun parseRules(arr: JSONArray): List<Rule> {
        val list = mutableListOf<Rule>()
        for (i in 0 until arr.length()) {
            list.add(parseRule(arr.getJSONObject(i)))
        }
        return list
    }

    private fun parseRule(json: JSONObject): Rule {
        val name = json.optString("name", null)?.takeIf { it.isNotEmpty() }
        val conditionsJson = json.optJSONArray("conditions")
        val conditions = if (conditionsJson != null && conditionsJson.length() > 0) {
            parseConditions(conditionsJson)
        } else {
            emptyList()
        }
        val actionsJson = json.optJSONArray("actions")
            ?: throw IllegalArgumentException("Rule actions array is required")
        val actions = parseActions(actionsJson)
        
        return Rule(name, conditions, actions)
    }

    // ── conditions ────────────────────────────────────────────────────

    private fun parseConditions(arr: JSONArray): List<Condition> {
        val list = mutableListOf<Condition>()
        for (i in 0 until arr.length()) {
            list.add(parseCondition(arr.getJSONObject(i)))
        }
        return list
    }

    private fun parseCondition(json: JSONObject): Condition {
        val type = json.optString("type", null)
            ?: throw IllegalArgumentException("Condition type is required")
        
        return when (type) {
            "text_exists", "text_not_exists" -> {
                val text = json.optString("text", null)
                    ?: throw IllegalArgumentException("Condition '$type' requires 'text'")
                Condition(
                    type = type,
                    text = text,
                    match = json.optString("match", "contains")
                )
            }
            "desc_exists" -> {
                val desc = json.optString("desc", null)
                    ?: throw IllegalArgumentException("Condition 'desc_exists' requires 'desc'")
                Condition(
                    type = type,
                    desc = desc,
                    match = json.optString("match", "contains")
                )
            }
            "current_app_is", "current_app_not" -> {
                val packageVal = json.optString("package", null)
                    ?: throw IllegalArgumentException("Condition '$type' requires 'package'")
                Condition(
                    type = type,
                    `package` = packageVal
                )
            }
            "node_count" -> {
                val by = json.optString("by", null)
                    ?: throw IllegalArgumentException("Condition 'node_count' requires 'by'")
                val count = json.optInt("count", 0)
                val operator = json.optString("operator", "eq")
                Condition(
                    type = type,
                    by = by,
                    value = json.optString("value", null),
                    operator = operator,
                    count = count
                )
            }
            else -> throw IllegalArgumentException("Unknown condition type: $type")
        }
    }

    // ── actions ───────────────────────────────────────────────────────

    private fun parseActions(arr: JSONArray): List<Action> {
        val list = mutableListOf<Action>()
        for (i in 0 until arr.length()) {
            list.add(parseAction(arr.getJSONObject(i)))
        }
        return list
    }

    private fun parseAction(json: JSONObject): Action {
        val type = json.optString("type", null)
            ?: throw IllegalArgumentException("Action type is required")
        return Action(
            type = type,
            `package` = json.optString("package", null)?.takeIf { it.isNotEmpty() },
            target = if (json.has("target") && !json.isNull("target")) {
                parseUiTarget(json.getJSONObject("target"))
            } else null,
            from = if (json.has("from") && !json.isNull("from")) {
                parseUiTarget(json.getJSONObject("from"))
            } else null,
            to = if (json.has("to") && !json.isNull("to")) {
                parseUiTarget(json.getJSONObject("to"))
            } else null,
            humanize = if (json.has("humanize") && !json.isNull("humanize")) {
                parseHumanize(json.getJSONObject("humanize"))
            } else null,
            durationMs = if (json.has("durationMs")) json.optLong("durationMs") else null
        )
    }

    // ── ui target ─────────────────────────────────────────────────────

    private fun parseUiTarget(json: JSONObject): UiTarget {
        val by = json.optString("by", null)
            ?: throw IllegalArgumentException("UiTarget.by is required")
        return UiTarget(
            by = by,
            value = json.optString("value", null)?.takeIf { it.isNotEmpty() },
            match = json.optString("match", null)?.takeIf { it.isNotEmpty() },
            index = json.optInt("index", 0),
            x = if (json.has("x")) json.optInt("x") else null,
            y = if (json.has("y")) json.optInt("y") else null
        )
    }

    // ── humanize ──────────────────────────────────────────────────────

    private fun parseHumanize(json: JSONObject): HumanizeParams {
        return HumanizeParams(
            offsetPx = json.optInt("offsetPx", 0),
            tapDurationMs = json.optLong("tapDurationMs", 100),
            durationMs = json.optLong("durationMs", 1000),
            durationVarMs = json.optInt("durationVarMs", 0),
            fromOffsetPx = json.optInt("fromOffsetPx", 0),
            toOffsetPx = json.optInt("toOffsetPx", 0),
            jitterPx = json.optInt("jitterPx", 0)
        )
    }

    // ── variable substitution ─────────────────────────────────────────

    /**
     * Replace `{{config.targetPackage}}` in the raw JSON string with the
     * actual [ScriptConfig.targetPackage] value, then re-parse.
     * This handles the substitution across all string fields in the JSON.
     */
    private fun applyVariables(raw: JSONObject, config: ScriptConfig): JSONObject {
        val substituted = raw.toString()
        val replaced = VARIABLE_PATTERN.replace(substituted, config.targetPackage)
        return JSONObject(replaced)
    }
}
