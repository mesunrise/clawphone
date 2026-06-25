package com.clawp.android.script.condition

import com.clawp.android.script.model.Condition
import com.clawp.android.service.ClawAccessibilityService

/**
 * Evaluates a single [Condition] against the current screen state.
 */
interface ConditionEvaluator {
    /**
     * @return true if the condition is satisfied.
     */
    fun evaluate(
        condition: Condition,
        screenTreeText: String?,
        service: ClawAccessibilityService?
    ): Boolean
}