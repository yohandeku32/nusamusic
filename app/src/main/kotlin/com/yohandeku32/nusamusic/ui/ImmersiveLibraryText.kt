package com.yohandeku32.nusamusic.ui

import java.util.Locale

internal fun libraryText(id: String, en: String): String =
    if (Locale.getDefault().language.equals("id", ignoreCase = true)) id else en

internal fun librarySongCount(count: Int): String =
    libraryText("$count lagu", if (count == 1) "1 song" else "$count songs")
