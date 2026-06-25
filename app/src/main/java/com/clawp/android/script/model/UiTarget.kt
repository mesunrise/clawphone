package com.clawp.android.script.model

/**
 * Describes how to locate a UI element on screen.
 *
 * Supported "by" values:
 *  - "text"       — match by node text
 *  - "desc"       — match by content-description
 *  - "id"         — match by resource-id
 *  - "coordinate" — absolute pixel position (x, y required)
 */
data class UiTarget(
    val by: String,
    val value: String? = null,
    val match: String? = null,   // "contains" (default), "exact", "startsWith", "endsWith"
    val index: Int = 0,
    val x: Int? = null,          // only when by == "coordinate"
    val y: Int? = null           // only when by == "coordinate"
) {
    init {
        require(by in setOf("text", "desc", "id", "coordinate")) {
            "UiTarget.by must be one of [text, desc, id, coordinate], got '$by'"
        }
        if (by == "coordinate") {
            require(x != null && y != null) {
                "UiTarget.by='coordinate' requires x and y"
            }
        } else {
            require(!value.isNullOrBlank()) {
                "UiTarget.by='$by' requires a non-blank value"
            }
        }
    }
}
