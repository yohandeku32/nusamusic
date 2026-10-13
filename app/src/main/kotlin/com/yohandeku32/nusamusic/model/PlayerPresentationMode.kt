package com.yohandeku32.nusamusic.model

enum class PlayerPresentationMode(val preferenceValue: String) {
    VINYL("vinyl"),
    IMMERSIVE_ARTWORK("immersive_artwork");

    companion object {
        fun fromPreference(value: String?): PlayerPresentationMode =
            if (value == IMMERSIVE_ARTWORK.preferenceValue) {
                IMMERSIVE_ARTWORK
            } else {
                VINYL
            }
    }
}
