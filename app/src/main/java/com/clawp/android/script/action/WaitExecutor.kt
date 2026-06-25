package com.clawp.android.script.action

import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.service.ClawAccessibilityService
import kotlinx.coroutines.delay

/**
 * Executes [wait] actions.
 */
class WaitExecutor : ActionExecutor {

    override suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult {
        val ms = action.durationMs ?: 1000L
        delay(ms)
        return ActionResult.SUCCESS
    }
}