package com.clawp.android.script.action

import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.service.ClawAccessibilityService

/**
 * Executes flow-control actions:
 *  - restart_round → RESTART_ROUND
 *  - end_round → END_ROUND
 *  - exit_task → EXIT_TASK
 */
class FlowControlExecutor : ActionExecutor {

    override suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult {
        return when (action.type) {
            "restart_round" -> ActionResult.RESTART_ROUND
            "end_round" -> ActionResult.END_ROUND
            "exit_task" -> ActionResult.EXIT_TASK
            else -> ActionResult.FAILURE
        }
    }
}