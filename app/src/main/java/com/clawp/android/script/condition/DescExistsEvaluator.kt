package com.clawp.android.script.condition

import com.clawp.android.script.model.Condition
import com.clawp.android.service.ClawAccessibilityService

/**
 * Evaluates [desc_exists] conditions by searching the screen-tree text
 * for `desc="...value..."` patterns.
 */
class DescExistsEvaluator : ConditionEvaluator {

    override fun evaluate(
        condition: Condition,
        screenTreeText: String?,
        service: ClawAccessibilityService?
    ): Boolean {
        val desc = condition.desc ?: return false
        if (screenTreeText.isNullOrBlank()) return false

        val pattern = Regex("desc=\"([^\"]*)\"")
        for (m in pattern.findAll(screenTreeText)) {
            val nodeDesc = m.groupValues[1]
            if (TextExistsEvaluator.matches(nodeDesc, desc, condition.match ?: "contains")) {
                return true
            }
        }
        return false
    }
}