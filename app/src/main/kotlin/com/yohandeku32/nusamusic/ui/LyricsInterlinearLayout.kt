package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyllableLayout
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.measureSyllablesAndDetermineAnimation
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import com.yohandeku32.nusamusic.data.LyricsPronunciation

internal data class LyricsReadingGroup(val source: IntRange, val reading: IntRange? = null)

/** Common authored interval boundaries define a character or a multi-kanji reading. */
internal fun interlinearReadingGroups(source: KaraokeLine, reading: LyricsPronunciation): List<LyricsReadingGroup>? {
    val cjk = source.syllables.any { unit -> unit.content.codePoints().anyMatch {
        when (Character.UnicodeScript.of(it)) {
            Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> true
            else -> false
        }
    } }
    if (!cjk || pronunciationKaraokeLine(reading) == null ||
        source.syllables.any { it.content.isBlank() || it.content.isRtl() } || reading.text.isRtl()) return null
    val units = source.syllables
    val words = reading.words
    if (units.zipWithNext().any { (a, b) -> a.end > b.start } ||
        words.zipWithNext().any { (a, b) -> a.end > b.start }) return null
    val result = ArrayList<LyricsReadingGroup>()
    var s = 0
    var r = 0
    while (s < units.size) {
        if (r == words.size || words[r].start > units[s].start) {
            result += LyricsReadingGroup(s..s)
            s++
            continue
        }
        if (words[r].start != units[s].start) return null
        val firstSource = s
        val firstReading = r
        while (units[s].end != words[r].end) {
            if (units[s].end < words[r].end) {
                if (s + 1 >= units.size || units[s + 1].start >= words[r].end) return null
                s++
            } else {
                if (r + 1 >= words.size || words[r + 1].start >= units[s].end) return null
                r++
            }
        }
        result += LyricsReadingGroup(firstSource..s, firstReading..r)
        s++
        r++
    }
    return result.takeIf { r == words.size && it.any { group -> group.reading != null } }
}

internal data class LyricsInterlinearCell(
    val source: IntRange, val reading: IntRange?, val row: Int,
    val left: Float, val width: Float
)

internal data class LyricsInterlinearLayout(
    val sourceRows: List<List<SyllableLayout>>,
    val readingRows: List<List<SyllableLayout>>,
    val readingLine: KaraokeLine,
    val cells: List<LyricsInterlinearCell>,
    val heightPx: Float,
    val readingScale: Float
)

internal fun measureInterlinearLyrics(
    source: KaraokeLine, pronunciation: LyricsPronunciation?, sourceUnits: List<SyllableLayout>,
    sourceStyle: TextStyle, readingStyle: TextStyle, measurer: TextMeasurer,
    availableWidth: Float, density: Float, alignEnd: Boolean
): LyricsInterlinearLayout? {
    if (pronunciation == null) return null
    val groups = interlinearReadingGroups(source, pronunciation) ?: return null
    if (sourceUnits.size != source.syllables.size || availableWidth <= 0f) return null
    val readingLine = pronunciationKaraokeLine(pronunciation) ?: return null
    val scale = readingStyle.fontSize.value / sourceStyle.fontSize.value.coerceAtLeast(1f)
    val virtualStyle = readingStyle.copy(fontSize = sourceStyle.fontSize,
        lineHeight = sourceStyle.lineHeight)
    val readingUnits = measureSyllablesAndDetermineAnimation(readingLine.syllables, measurer,
        virtualStyle, true, measurer.measure(" ", virtualStyle).size.width.toFloat())
    if (readingUnits.size != readingLine.syllables.size) return null
    val sourceHeight = measurer.measure("M", sourceStyle).size.height.toFloat()
    val readingHeight = maxOf(measurer.measure("M", virtualStyle).size.height * scale,
        measurer.measure("M", readingStyle).size.height.toFloat())
    val stride = sourceHeight + 2f * density + readingHeight + 10f * density
    val widths = groups.map { group ->
        val main = group.source.sumOf { sourceUnits[it].width.toDouble() }.toFloat()
        val trailing = sourceUnits[group.source.last].let { unit ->
            (unit.width - measurer.measure(unit.syllable.content.trimEnd(), sourceStyle).size.width).coerceAtLeast(0f)
        }
        val phonetic = group.reading?.sumOf { readingUnits[it].width.toDouble() }?.toFloat()?.times(scale) ?: 0f
        val readingTrailing = group.reading?.let { indices -> readingUnits[indices.last].let { unit ->
            (unit.width - measurer.measure(unit.syllable.content.trimEnd(), virtualStyle).size.width).coerceAtLeast(0f) * scale
        } } ?: 0f
        Triple(main - trailing, phonetic - readingTrailing, trailing)
    }
    // A reading without internal association is kept intact rather than split by guessed characters.
    if (widths.any { maxOf(it.first, it.second) > availableWidth }) return null
    val rowGroups = ArrayList<MutableList<Int>>()
    var rowWidth = 0f
    for (index in groups.indices) {
        val width = maxOf(widths[index].first, widths[index].second)
        val gap = if (rowGroups.isEmpty() || rowGroups.last().isEmpty()) 0f else
            maxOf(2f * density, widths[rowGroups.last().last()].third)
        if (rowGroups.isEmpty() || rowWidth + gap + width > availableWidth) {
            rowGroups += mutableListOf<Int>()
            rowWidth = 0f
        }
        if (rowGroups.last().isNotEmpty()) rowWidth += gap
        rowGroups.last() += index
        rowWidth += width
    }
    val mainRows = ArrayList<List<SyllableLayout>>()
    val phoneticRows = ArrayList<List<SyllableLayout>>()
    val cells = ArrayList<LyricsInterlinearCell>()
    for ((row, indices) in rowGroups.withIndex()) {
        val total = indices.sumOf { maxOf(widths[it].first, widths[it].second).toDouble() }.toFloat() +
            indices.dropLast(1).sumOf { maxOf(2f * density, widths[it].third).toDouble() }.toFloat()
        var x = if (alignEnd) availableWidth - total else 0f
        val main = ArrayList<SyllableLayout>()
        val phonetic = ArrayList<SyllableLayout>()
        val sourceBaseline = indices.maxOf { group -> groups[group].source.maxOf { sourceUnits[it].firstBaseline } }
        val readingBaseline = indices.mapNotNull { group -> groups[group].reading?.maxOf { readingUnits[it].firstBaseline } }.maxOrNull() ?: 0f
        for (index in indices) {
            val group = groups[index]
            val (sourceWidth, readingWidth, _) = widths[index]
            val width = maxOf(sourceWidth, readingWidth)
            val center = x + width * 0.5f
            var mainX = center - sourceWidth * 0.5f
            for (unitIndex in group.source) {
                val unit = sourceUnits[unitIndex]
                main += unit.copy(position = Offset(mainX, row * stride + sourceBaseline - unit.firstBaseline))
                mainX += unit.width
            }
            var readingX = (center - readingWidth * 0.5f) / scale
            group.reading?.forEach { unitIndex ->
                val unit = readingUnits[unitIndex]
                phonetic += unit.copy(position = Offset(readingX,
                    (row * stride + sourceHeight + 2f * density) / scale + readingBaseline - unit.firstBaseline))
                readingX += unit.width
            }
            cells += LyricsInterlinearCell(group.source, group.reading, row, x, width)
            x += width + maxOf(2f * density, widths[index].third)
        }
        mainRows += main
        phoneticRows += phonetic
    }
    return LyricsInterlinearLayout(mainRows, phoneticRows, readingLine, cells,
        rowGroups.size * stride - 2f * density, scale)
}
