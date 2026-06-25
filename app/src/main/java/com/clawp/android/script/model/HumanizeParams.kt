package com.clawp.android.script.model

/**
 * Humanization parameters for touch actions.
 * All values are in pixels or milliseconds and are applied as
 * uniformly-randomised offsets around the canonical value.
 */
data class HumanizeParams(
    val offsetPx: Int = 0,          // position random offset (± pixels) for tap / long-press
    val tapDurationMs: Long = 100,  // press duration for click (ms)
    val durationMs: Long = 1000,    // base duration for long-press / swipe
    val durationVarMs: Int = 0,     // random variation (±) on durationMs
    val fromOffsetPx: Int = 0,      // swipe start-point random offset
    val toOffsetPx: Int = 0,        // swipe end-point random offset
    val jitterPx: Int = 0           // path jitter amplitude for swipe
)
