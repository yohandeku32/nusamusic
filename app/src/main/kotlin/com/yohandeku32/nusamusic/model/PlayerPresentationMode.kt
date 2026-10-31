package com.yohandeku32.nusamusic.model

enum class PlayerPresentationMode(val preferenceValue: String) {
    VINYL("vinyl"),
    CD("cd"),
    IMMERSIVE_ARTWORK("immersive_artwork");

    val isAvailable: Boolean
        get() = true

    companion object {
        fun fromPreference(value: String?): PlayerPresentationMode {
            return entries.firstOrNull { it.preferenceValue == value && it.isAvailable } ?: IMMERSIVE_ARTWORK
        }
    }
}
