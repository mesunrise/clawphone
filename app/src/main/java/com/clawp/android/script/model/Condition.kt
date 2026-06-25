package com.clawp.android.script.model

/**
 * A single condition in a rule.
 *
 * Supported types:
 *  - text_exists     — screen contains the given text
 *  - text_not_exists — screen does NOT contain the given text
 *  - desc_exists     — screen contains the given content-description
 *  - current_app_is  — foreground app package matches
 *  - current_app_not — foreground app package does NOT match
 *  - node_count      — count of matching nodes compared via operator
 */
data class Condition(
    val type: String,
    val text: String? = null,
    val desc: String? = null,
    val `package`: String? = null,
    val match: String? = null,
    val by: String? = null,       // node_count: "text", "desc", "id"
    val value: String? = null,    // node_count: the search value
    val operator: String? = null, // node_count: "eq", "gt", "gte", "lt", "lte"
    val count: Int? = null        // node_count: comparison threshold
) {
    companion object {
        val VALID_TYPES = setOf(
            "text_exists", "text_not_exists", "desc_exists",
            "current_app_is", "current_app_not", "node_count"
        )
    }

    init {
        require(type in VALID_TYPES) {
            "Condition.type must be one of $VALID_TYPES, got '$type'"
        }
    }
}