package com.clawp.android.script.model

/**
 * A single action to execute.
 *
 * Supported types:
 *  - open_app, click, long_press, swipe, wait,
 *  - press_back, press_home, restart_round, end_round, exit_task
 */
data class Action(
    val type: String,
    val `package`: String? = null,
    val target: UiTarget? = null,
    val from: UiTarget? = null,     // swipe start
    val to: UiTarget? = null,       // swipe end
    val humanize: HumanizeParams? = null,
    val durationMs: Long? = null  // wait duration
) {
    companion object {
        val VALID_TYPES = setOf(
            "open_app", "click", "long_press", "swipe", "wait",
            "press_back", "press_home", "restart_round", "end_round", "exit_task"
        )
    }

    init {
        require(type in VALID_TYPES) {
            "Action.type must be one of $VALID_TYPES, got '$type'"
        }
    }
}
