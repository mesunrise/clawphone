package com.clawp.android.script.action

import android.graphics.Rect
import com.clawp.android.script.HumanizeUtils
import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.service.ClawAccessibilityService

/**
 * Executes [long_press] actions.
 */
class LongPressExecutor : ActionExecutor {

    override suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult {
        val target = action.target ?: return ActionResult.FAILURE
        val h = action.humanize

        val (x, y) = if (target.by == "coordinate") {
            val tx = target.x ?: return ActionResult.FAILURE
            val ty = target.y ?: return ActionResult.FAILURE
            if (h != null) HumanizeUtils.offset(tx, ty, h.offsetPx) else (tx to ty)
        } else {
            val node = ClickExecutor.locateNode(target, service) ?: return ActionResult.FAILURE
            try {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val cx = rect.centerX()
                val cy = rect.centerY()
                if (h != null) HumanizeUtils.offset(cx, cy, h.offsetPx) else (cx to cy)
            } finally {
                try { node.recycle() } catch (_: Exception) {}
            }
        }

        val durationMs = if (h != null) {
            HumanizeUtils.varDuration(h.durationMs, h.durationVarMs)
        } else {
            1000L
        }

        val ok = service.performLongPress(x, y, durationMs)
        return if (ok) ActionResult.SUCCESS else ActionResult.FAILURE
    }
}