package com.clawp.android.script.condition

import android.view.accessibility.AccessibilityNodeInfo
import com.clawp.android.script.model.Condition
import com.clawp.android.service.ClawAccessibilityService

/**
 * Evaluates [current_app_is] and [current_app_not] conditions.
 * Reads the foreground app package from the accessibility root node.
 */
class CurrentAppEvaluator(private val isCheck: Boolean) : ConditionEvaluator {

    override fun evaluate(
        condition: Condition,
        screenTreeText: String?,
        service: ClawAccessibilityService?
    ): Boolean {
        val pkg = condition.`package` ?: return false
        if (service == null) return false

        val root: AccessibilityNodeInfo? = try {
            service.rootInActiveWindow
        } catch (_: Exception) {
            null
        }

        val currentPkg = root?.packageName?.toString()
        root?.recycle()

        val matches = currentPkg != null && currentPkg == pkg
        return if (isCheck) matches else !matches
    }
}