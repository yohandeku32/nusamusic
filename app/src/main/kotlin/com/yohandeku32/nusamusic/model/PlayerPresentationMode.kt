package com.yohandeku32.nusamusic.model

enum class PlayerPresentationMode(val preferenceValue: String) {
    VINYL("vinyl"),
    CD("cd"),
    IMMERSIVE_ARTWORK("immersive_artwork");

    // Keep the legacy enum value so previously stored preferences can migrate safely.
    // Vinyl is no longer a selectable or restorable player mode.
    val isAvailable: Boolean
        get() = this != VINYL

    companion object {
        fun fromPreference(value: String?): PlayerPresentationMode {
            return entries.firstOrNull { it.preferenceValue == value && it.isAvailable } ?: IMMERSIVE_ARTWORK
        }
    }
}
