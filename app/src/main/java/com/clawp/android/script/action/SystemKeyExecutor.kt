package com.clawp.android.script.action

import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.service.ClawAccessibilityService

/**
 * Executes [press_back] and [press_home] actions.
 */
class SystemKeyExecutor : ActionExecutor {

    override suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult {
        val ok = when (action.type) {
            "press_back" -> service.performGlobalBack()
            "press_home" -> service.performGlobalHome()
            else -> false
        }
        return if (ok) ActionResult.SUCCESS else ActionResult.FAILURE
    }
}