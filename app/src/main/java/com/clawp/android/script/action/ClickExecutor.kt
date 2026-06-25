package com.clawp.android.script.action

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.clawp.android.script.HumanizeUtils
import com.clawp.android.script.model.Action
import com.clawp.android.script.model.ActionResult
import com.clawp.android.script.model.Target
import com.clawp.android.service.ClawAccessibilityService

/**
 * Executes [click] actions.
 *
 * Locates a target node or coordinate and taps it, with optional humanization.
 */
class ClickExecutor : ActionExecutor {

    override suspend fun execute(
        action: Action,
        service: ClawAccessibilityService
    ): ActionResult {
        val target = action.target ?: return ActionResult.FAILURE

        // Coordinate-based click
        if (target.by == "coordinate") {
            val x = target.x ?: return ActionResult.FAILURE
            val y = target.y ?: return ActionResult.FAILURE
            val h = action.humanize
            val (tx, ty) = if (h != null) HumanizeUtils.offset(x, y, h.offsetPx) else (x to y)
            val tapDuration = h?.tapDurationMs ?: 100
            val ok = service.performTap(tx, ty, tapDuration)
            return if (ok) ActionResult.SUCCESS else ActionResult.FAILURE
        }

        // Node-based click
        val node = locateNode(target, service) ?: return ActionResult.FAILURE
        try {
            // Get bounds for coordinate-based tap with humanization
            val h = action.humanize
            if (h != null && h.offsetPx > 0) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val cx = rect.centerX()
                val cy = rect.centerY()
                val (tx, ty) = HumanizeUtils.offset(cx, cy, h.offsetPx)
                val tapDuration = h.tapDurationMs
                val ok = service.performTap(tx, ty, tapDuration)
                return if (ok) ActionResult.SUCCESS else ActionResult.FAILURE
            }

            // Standard click via node
            val ok = service.performClick(node)
            return if (ok) ActionResult.SUCCESS else ActionResult.FAILURE
        } finally {
            try { node.recycle() } catch (_: Exception) {}
        }
    }

    companion object {
        /**
         * Locate a single node matching [target].
         * Returns null if no matching node found.
         */
        fun locateNode(target: Target, service: ClawAccessibilityService): AccessibilityNodeInfo? {
            val nodes = when (target.by) {
                "text" -> service.findNodesByText(target.value ?: return null)
                "desc" -> service.findNodesByText(target.value ?: return null) // contentDescription
                "id" -> service.findNodesById(target.value ?: return null)
                else -> return null
            }

            if (nodes.isEmpty()) return null

            // Filter by match mode
            val matchMode = target.match ?: "contains"
            val filtered = when (target.by) {
                "text" -> nodes.filter { node ->
                    val t = node.text?.toString() ?: ""
                    matchesFilter(t, target.value!!, matchMode)
                }
                "desc" -> nodes.filter { node ->
                    val d = node.contentDescription?.toString() ?: ""
                    matchesFilter(d, target.value!!, matchMode)
                }
                else -> nodes
            }

            if (filtered.isEmpty()) {
                // Recycle all unfiltered nodes
                nodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                return null
            }

            // Sort: prefer clickable nodes
            val sorted = filtered.sortedByDescending { node ->
                var score = 0
                if (node.isClickable) score += 10
                if (node.isEnabled) score += 5
                score
            }

            // Pick by index
            val idx = target.index.coerceIn(0, sorted.size - 1)
            val result = sorted[idx]

            // Recycle the rest
            sorted.forEachIndexed { i, node ->
                if (i != idx) try { node.recycle() } catch (_: Exception) {}
            }

            return result
        }

        fun matchesFilter(nodeText: String, searchText: String, matchMode: String): Boolean {
            return when (matchMode) {
                "exact" -> nodeText == searchText
                "startsWith" -> nodeText.startsWith(searchText)
                "endsWith" -> nodeText.endsWith(searchText)
                "contains" -> nodeText.contains(searchText)
                else -> nodeText.contains(searchText)
            }
        }
    }
}