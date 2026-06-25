package com.clawp.android.script.model

/**
 * Result of executing a single action inside the script engine loop.
 */
enum class ActionResult {
    /** Action succeeded; continue with the next action in the same rule. */
    SUCCESS,

    /** Action failed; abort the current round entirely. */
    FAILURE,

    /** Immediately restart the current round (does NOT consume loopCount). */
    RESTART_ROUND,

    /** End the current round and proceed to the next iteration. */
    END_ROUND,

    /** Terminate the whole script task. */
    EXIT_TASK
}
