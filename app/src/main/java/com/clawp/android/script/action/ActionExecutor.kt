package com.clawp.android.script.action

import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.service.ClawAccessibilityService

/**
 * Executes a single [Action] and returns the [ActionResult].
 */
interface ActionExecutor {
    suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult
}