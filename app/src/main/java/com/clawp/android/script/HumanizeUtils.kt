package com.clawp.android.script

import kotlin.random.Random

/**
 * Utility functions for adding human-like randomness to touch actions.
 */
object HumanizeUtils {

    private val rng = Random.Default

    /**
     * Apply a random offset to coordinates within [-offsetPx, +offsetPx].
     */
    fun offset(baseX: Int, baseY: Int, offsetPx: Int): Pair<Int, Int> {
        if (offsetPx <= 0) return baseX to baseY
        val dx = rng.nextInt(-offsetPx, offsetPx + 1)
        val dy = rng.nextInt(-offsetPx, offsetPx + 1)
        return (baseX + dx) to (baseY + dy)
    }

    /**
     * Apply a random variation to a duration.
     * Actual duration = baseMs ± rng(0, varMs).
     */
    fun varDuration(baseMs: Long, varMs: Int): Long {
        if (varMs <= 0) return baseMs.coerceAtLeast(0)
        val delta = rng.nextInt(-varMs, varMs + 1)
        return (baseMs + delta).coerceAtLeast(0)
    }

    /**
     * Generate a random delay in milliseconds within [minSec, maxSec].
     */
    fun randomDelay(minSec: Double, maxSec: Double): Long {
        val range = maxSec - minSec
        val ms = if (range <= 0) minSec else minSec + rng.nextDouble() * range
        return (ms * 1000).toLong()
    }
}