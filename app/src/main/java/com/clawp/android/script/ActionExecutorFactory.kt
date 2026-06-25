package com.clawp.android.script

import com.clawp.android.script.action.*

/**
 * Factory that creates the appropriate [ActionExecutor] for a given
 * action type string.
 */
object ActionExecutorFactory {

    private val clickExecutor = ClickExecutor()
    private val longPressExecutor = LongPressExecutor()
    private val swipeExecutor = SwipeExecutor()
    private val openAppExecutor = OpenAppExecutor()
    private val waitExecutor = WaitExecutor()
    private val systemKeyExecutor = SystemKeyExecutor()
    private val flowControlExecutor = FlowControlExecutor()

    fun create(type: String): ActionExecutor = when (type) {
        "open_app" -> openAppExecutor
        "click" -> clickExecutor
        "long_press" -> longPressExecutor
        "swipe" -> swipeExecutor
        "wait" -> waitExecutor
        "press_back", "press_home" -> systemKeyExecutor
        "restart_round", "end_round", "exit_task" -> flowControlExecutor
        else -> throw IllegalArgumentException("Unknown action type: $type")
    }
}