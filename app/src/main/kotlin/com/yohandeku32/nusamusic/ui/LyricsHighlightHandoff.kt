package com.yohandeku32.nusamusic.ui

/** A shared fill cursor around adjacent vocal boundaries; held-note geometry stays independent. */
internal class LyricsHighlightHandoff(units: List<LyricsHeldWordUnit>) {
    private class Slot(val unit: LyricsHeldWordUnit) {
        val feather = minOf(12f, (unit.pathEndDp - unit.pathStartDp) * 0.4f).coerceAtLeast(0.5f)
        var previous: Boundary? = null
        var next: Boundary? = null

        fun velocity(): Float = (endEdge() - startEdge()) / (unit.endMs.toLong() - unit.startMs)
        private fun startEdge() = unit.pathStartDp - if (previous == null) feather else 0f
        private fun endEdge() = unit.pathEndDp + if (next == null) feather else 0f
        fun edge(now: Int): Float = startEdge() + velocity() * (now.toLong() - unit.startMs)
    }

    private class Boundary(val outgoing: Slot, val incoming: Slot, window: Int) {
        val start = outgoing.unit.endMs - window
        val end = incoming.unit.startMs + window

        fun edge(now: Int): Float {
            val duration = (end.toLong() - start).toFloat()
            val t = (now.toLong() - start) / duration
            val left = outgoing.edge(start)
            val right = incoming.edge(end)
            val distance = right - left
            val first = outgoing.velocity() * duration
            val last = incoming.velocity() * duration
            // Hermite endpoints match the authored sweep's position and velocity.
            return left + first * t + (3f * distance - 2f * first - last) * t * t +
                (-2f * distance + first + last) * t * t * t
        }
    }

    private val slots = units.map { unit ->
        if (unit.reliableHold && unit.endMs > unit.startMs &&
            unit.pathEndDp > unit.pathStartDp && unit.content.isNotBlank()) Slot(unit) else null
    }

    init {
        for (index in 0 until slots.lastIndex) {
            val outgoing = slots[index] ?: continue
            val incoming = slots[index + 1] ?: continue
            val before = outgoing.unit
            val after = incoming.unit
            val gap = after.startMs.toLong() - before.endMs
            if (gap !in 0L..45L) continue
            val window = minOf(90L - gap, (before.endMs.toLong() - before.startMs) / 5,
                (after.endMs.toLong() - after.startMs) / 5).toInt()
            val pathGap = after.pathStartDp - before.pathEndDp
            if (window < 1 || gap !in 0L..window.toLong() || pathGap !in 0f..48f ||
                before.endMs.toLong() - window < Int.MIN_VALUE ||
                after.startMs.toLong() + window > Int.MAX_VALUE) continue
            val boundary = Boundary(outgoing, incoming, window)
            outgoing.next = boundary
            incoming.previous = boundary
        }
    }

    fun featherDp(index: Int): Float = requireNotNull(slots[index]).feather

    /** Null retains the existing isolated/ambiguous-time fill. Values are local to the glyph. */
    fun edgeDp(index: Int, now: Int): Float? {
        val slot = slots.getOrNull(index) ?: return null
        if (slot.previous == null && slot.next == null) return null
        val boundary = slot.previous?.takeIf { now in it.start..it.end }
            ?: slot.next?.takeIf { now in it.start..it.end }
        val width = slot.unit.pathEndDp - slot.unit.pathStartDp
        if (boundary == null) {
            return when {
                now < slot.unit.startMs -> -slot.feather
                now > slot.unit.endMs -> width + slot.feather
                else -> slot.edge(now) - slot.unit.pathStartDp
            }
        }
        val edge = (boundary.edge(now) - slot.unit.pathStartDp).coerceIn(-slot.feather, width + slot.feather)
        return when {
            now < slot.unit.startMs -> {
                val arrival = heldWordEase((now.toLong() - boundary.start) /
                    (slot.unit.startMs.toLong() - boundary.start).toFloat())
                -slot.feather + (edge + slot.feather) * arrival
            }
            now > slot.unit.endMs -> {
                val release = heldWordEase((now.toLong() - slot.unit.endMs) /
                    (boundary.end.toLong() - slot.unit.endMs).toFloat())
                edge + (width + slot.feather - edge) * release
            }
            else -> edge
        }
    }
}
