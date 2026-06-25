package com.clawp.android.script.action

import com.clawp.android.script.HumanizeUtils
import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.service.ClawAccessibilityService

/**
 * Executes [swipe] actions with optional humanization
 * (start/end offset, path jitter, duration variation).
 */
class SwipeExecutor : ActionExecutor {

    override suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult {
        val from = action.from ?: return ActionResult.FAILURE
        val to = action.to ?: return ActionResult.FAILURE

        val (startX, startY) = extractCoordinate(from, "from") ?: return ActionResult.FAILURE
        val (endX, endY) = extractCoordinate(to, "to") ?: return ActionResult.FAILURE

        val h = action.humanize

        // Apply offsets
        var sx = startX
        var sy = startY
        var ex = endX
        var ey = endY
        if (h != null) {
            val (osx, osy) = HumanizeUtils.offset(sx, sy, h.fromOffsetPx)
            val (oex, oey) = HumanizeUtils.offset(ex, ey, h.toOffsetPx)
            sx = osx; sy = osy; ex = oex; ey = oey
        }

        val durationMs = if (h != null) {
            HumanizeUtils.varDuration(h.durationMs, h.durationVarMs)
        } else {
            500L
        }

        // For now, use a simple 2-point swipe gesture.
        // Path jitter (jitterPx) would require a multi-segment gesture;
        // we add a midpoint jitter as a simple approximation.
        val jitterPx = h?.jitterPx ?: 0
        val ok = if (jitterPx > 0) {
            // Approximate jitter by adding a displaced midpoint
            val midX = (sx + ex) / 2 + HumanizeUtils.offset(0, 0, jitterPx).first
            val midY = (sy + ey) / 2 + HumanizeUtils.offset(0, 0, jitterPx).second
            service.performSwipe(sx, sy, midX, midY, durationMs / 2) &&
                    service.performSwipe(midX, midY, ex, ey, durationMs / 2)
        } else {
            service.performSwipe(sx, sy, ex, ey, durationMs)
        }

        return if (ok) ActionResult.SUCCESS else ActionResult.FAILURE
    }

    private fun extractCoordinate(target: com.clawp.android.script.model.UiTarget, label: String): Pair<Int, Int>? {
        if (target.by == "coordinate") {
            val x = target.x ?: return null
            val y = target.y ?: return null
            return x to y
        }
        return null // Non-coordinate from/to not supported for swipe; return failure
    }
}