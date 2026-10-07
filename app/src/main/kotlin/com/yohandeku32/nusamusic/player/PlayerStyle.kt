package com.yohandeku32.nusamusic.player

enum class PlayerStyle(
    val key: String
) {
    VINYL("vinyl"),
    CD_CASE("cd_case");

    companion object {
        fun fromKey(value: String?): PlayerStyle =
            entries.firstOrNull { it.key == value } ?: VINYL
    }
}
