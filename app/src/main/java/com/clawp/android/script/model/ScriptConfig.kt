package com.clawp.android.script.model

/**
 * Global script configuration.
 *
 * If both [loopCount] and [loopDurationSec] are set, the engine
 * stops when **either** limit is reached (whichever comes first).
 */
data class ScriptConfig(
    val targetPackage: String,
    val loopCount: Int = 1000,
    val loopDurationSec: Int? = null,
    val roundDelay: RoundDelay = RoundDelay()
) {
    init {
        require(targetPackage.isNotBlank()) { "config.targetPackage is required" }
        require(loopCount > 0) { "config.loopCount must be > 0, got $loopCount" }
        if (loopDurationSec != null) {
            require(loopDurationSec > 0) { "config.loopDurationSec must be > 0, got $loopDurationSec" }
        }
    }
}
