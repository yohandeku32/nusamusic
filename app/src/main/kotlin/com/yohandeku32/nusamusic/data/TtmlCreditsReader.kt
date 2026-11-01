package com.yohandeku32.nusamusic.data

import java.util.Locale
import org.w3c.dom.Element

/** Explicit head credits only: voice agents and lyric-file editors do not identify song authors. */
internal fun readTtmlCredits(root: Element): SongCredits {
    val composers = linkedMapOf<String, String>()
    val lyricists = linkedMapOf<String, String>()
    val songwriters = linkedMapOf<String, String>()
    val whitespace = Regex("\\s+")
    for (h in 0 until root.childNodes.length) {
        val head = root.childNodes.item(h) as? Element ?: continue
        if (head.localName != "head") continue
        for (m in 0 until head.childNodes.length) {
            val metadata = head.childNodes.item(m) as? Element ?: continue
            if (metadata.localName != "metadata") continue
            val lists = metadata.getElementsByTagName("*")
            for (i in 0 until lists.length) {
                val list = lists.item(i) as Element
                val (entryName, names) = when (list.localName) {
                    "composers" -> "composer" to composers
                    "lyricists" -> "lyricist" to lyricists
                    "songwriters" -> "songwriter" to songwriters
                    else -> continue
                }
                for (j in 0 until list.childNodes.length) {
                    val entry = list.childNodes.item(j) as? Element ?: continue
                    if (entry.localName != entryName || names.size >= 32) continue
                    val raw = entry.textContent.orEmpty()
                    if (raw.length > 2048) continue
                    val name = raw.replace(whitespace, " ").filterNot(Char::isISOControl).trim()
                    val key = name.lowercase(Locale.ROOT)
                    if (key in setOf("", "unknown", "unknown composer", "unknown songwriter", "n/a", "-")) continue
                    names.putIfAbsent(key, name)
                }
            }
        }
    }
    return SongCredits(composers.values.toList(), lyricists.values.toList(), songwriters.values.toList())
}
