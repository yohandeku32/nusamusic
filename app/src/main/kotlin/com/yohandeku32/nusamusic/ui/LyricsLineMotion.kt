package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.exp

/** Visual followers of consumed list scroll; authored lyric times stay untouched. */
internal class LyricsLineMotion(onsets: IntArray) {
    private class Row {
        var offset by mutableFloatStateOf(0f)
        var velocity = 0f
    }

    private val rows = arrayOfNulls<Row>(onsets.size)
    private val ranks = IntArray(onsets.size).also { result ->
        for (index in 1 until onsets.size) {
            result[index] = result[index - 1] + if (onsets[index] == onsets[index - 1]) 0 else 1
        }
    }
    private var first = 0
    private var last = -1

    fun rank(index: Int): Float = ranks.getOrElse(index) { 0 }.toFloat()
    fun offsetDp(index: Int): Float {
        if (index !in rows.indices) return 0f
        // The first draw must observe the same state that later frames update.
        val row = rows[index] ?: Row().also { rows[index] = it }
        return row.offset
    }

    fun reset() {
        for (index in first..last) {
            rows[index]?.let { it.offset = 0f; it.velocity = 0f }
        }
        first = 0
        last = -1
    }

    fun advance(
        scrollDp: Float, deltaMs: Long, firstVisible: Int, lastVisible: Int,
        activeRank: Float, rowHeightDp: Float, durationScale: Float = 1f
    ) {
        if (!scrollDp.isFinite() || scrollDp < 0f || !durationScale.isFinite() || durationScale <= 0f ||
            !activeRank.isFinite() || !rowHeightDp.isFinite() || rowHeightDp <= 0f ||
            deltaMs < 0 || deltaMs > 250 || firstVisible !in rows.indices || lastVisible < firstVisible
        ) {
            reset()
            return
        }
        if (deltaMs <= 0) return
        val end = lastVisible.coerceAtMost(rows.lastIndex)
        for (index in first..last) if (index !in firstVisible..end) {
            rows[index]?.let { it.offset = 0f; it.velocity = 0f }
        }
        first = firstVisible
        last = end
        val limit = (rowHeightDp * 0.35f).coerceIn(8f, 32f)
        val seconds = deltaMs / 1000f
        var preceding = 0f
        for (index in first..last) {
            val row = rows[index] ?: Row().also { fresh ->
                // A second simultaneous singer inherits the already visible phase.
                val neighbor = when {
                    index > 0 && ranks[index - 1] == ranks[index] -> rows[index - 1]
                    index < rows.lastIndex && ranks[index + 1] == ranks[index] -> rows[index + 1]
                    else -> null
                }
                if (neighbor != null) {
                    fresh.offset = neighbor.offset
                    fresh.velocity = neighbor.velocity
                }
                rows[index] = fresh
            }
            val distance = (ranks[index] - activeRank).coerceIn(-1f, 3f)
            val frequency = (if (distance < 0f) 30f - 6f * distance else 30f / (1f + 0.45f * distance)) /
                durationScale.coerceIn(0.2f, 3f)
            val offset = (row.offset + scrollDp).coerceAtMost(limit)
            val joint = row.velocity + frequency * offset
            val decay = exp(-frequency * seconds)
            var next = ((offset + joint * seconds) * decay).coerceIn(0f, limit)
            var velocity = (row.velocity - frequency * joint * seconds) * decay
            if (next < 0.01f && abs(velocity) < 0.1f) { next = 0f; velocity = 0f }
            // Preserve row order while the active cue changes during a transition.
            if (next < preceding) { next = preceding; velocity = 0f }
            if (index > first && ranks[index - 1] == ranks[index]) {
                next = rows[index - 1]!!.offset
                velocity = rows[index - 1]!!.velocity
            }
            row.offset = next
            row.velocity = if (next == 0f) 0f else velocity
            preceding = next
        }
    }
}
