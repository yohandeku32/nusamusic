package com.yohandeku32.nusamusic.model

enum class PlayerPresentationMode(val preferenceValue: String) {
    VINYL("vinyl"),
    CD("cd"),
    IMMERSIVE_ARTWORK("immersive_artwork");

    companion object {
        // Temporary presentation policy: CD is the only available player.
        // Switch this flag off to restore Vinyl/Immersive selection later;
        // leave the enum entries and their saved preference values intact.
        const val CD_ONLY_MODE = true

        fun fromPreference(value: String?): PlayerPresentationMode {
            if (CD_ONLY_MODE) return CD
            return entries.firstOrNull { it.preferenceValue == value } ?: VINYL
        }
    }
}
