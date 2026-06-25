package com.clawp.android.script.condition

import com.clawp.android.script.model.Condition
import com.clawp.android.service.ClawAccessibilityService

/**
 * Evaluates [node_count] conditions: counts matching nodes and compares
 * against a threshold using the given operator.
 */
class NodeCountEvaluator : ConditionEvaluator {

    override fun evaluate(
        condition: Condition,
        screenTreeText: String?,
        service: ClawAccessibilityService?
    ): Boolean {
        val by = condition.by ?: return false
        val value = condition.value ?: return false
        val operator = condition.operator ?: return false
        val count = condition.count ?: return false
        if (service == null) return false

        val nodes = when (by) {
            "text" -> service.findNodesByText(value)
            "desc" -> {
                // findNodesByText also matches contentDescription in Android
                service.findNodesByText(value)
            }
            "id" -> service.findNodesById(value)
            else -> return false
        }

        val actualCount = nodes.size
        // Recycle nodes after counting
        for (node in nodes) {
            try { node.recycle() } catch (_: Exception) {}
        }

        return compare(actualCount, operator, count)
    }

    private fun compare(actual: Int, operator: String, threshold: Int): Boolean {
        return when (operator) {
            "eq" -> actual == threshold
            "gt" -> actual > threshold
            "gte" -> actual >= threshold
            "lt" -> actual < threshold
            "lte" -> actual <= threshold
            else -> false
        }
    }
}