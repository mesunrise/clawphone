package com.clawp.android.script.action

import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.service.ClawAccessibilityService

/**
 * Executes [open_app] actions.
 */
class OpenAppExecutor : ActionExecutor {

    override suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult {
        val pkg = action.`package` ?: return ActionResult.FAILURE
        val ok = service.openApp(pkg)
        return if (ok) ActionResult.SUCCESS else ActionResult.FAILURE
    }
}