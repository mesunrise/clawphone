package com.clawp.android.script.model

/**
 * A rule is a pair of conditions (AND-ed together) and a sequence of actions.
 * When **all** conditions match the current screen, the actions are executed
 * in order.
 * An empty [conditions] list means "always match" (fallback rule).
 */
data class Rule(
    val name: String? = null,
    val conditions: List<Condition> = emptyList(),
    val actions: List<Action> = emptyList()
)