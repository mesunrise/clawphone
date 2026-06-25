package com.clawp.android.script

import com.clawp.android.script.condition.*

/**
 * Factory that creates the appropriate [ConditionEvaluator] for a given
 * condition type string.
 */
object ConditionEvaluatorFactory {

    private val evaluators = mapOf(
        "text_exists" to TextExistsEvaluator(exists = true),
        "text_not_exists" to TextExistsEvaluator(exists = false),
        "desc_exists" to DescExistsEvaluator(),
        "current_app_is" to CurrentAppEvaluator(isCheck = true),
        "current_app_not" to CurrentAppEvaluator(isCheck = false),
        "node_count" to NodeCountEvaluator()
    )

    fun create(type: String): ConditionEvaluator {
        return evaluators[type]
            ?: throw IllegalArgumentException("Unknown condition type: $type")
    }
}