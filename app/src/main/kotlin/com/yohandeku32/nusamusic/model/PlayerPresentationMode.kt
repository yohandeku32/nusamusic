package com.yohandeku32.nusamusic.model

enum class PlayerPresentationMode(val preferenceValue: String) {
    VINYL("vinyl"),
    CD("cd"),
    IMMERSIVE_ARTWORK("immersive_artwork");

    companion object {
        fun fromPreference(value: String?): PlayerPresentationMode =
            entries.firstOrNull { it.preferenceValue == value } ?: VINYL
    }
}
