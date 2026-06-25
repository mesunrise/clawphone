package com.clawp.android.script.model

/**
 * Delay range between rounds, in seconds. A random value in [min, max] is
 * chosen for each inter-round sleep.
 */
data class RoundDelay(
    val min: Double = 3.0,
    val max: Double = 5.0
) {
    init {
        require(min >= 0) { "roundDelay.min must be >= 0, got $min" }
        require(max >= min) { "roundDelay.max ($max) must be >= min ($min)" }
    }
}
