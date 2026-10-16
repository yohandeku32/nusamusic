package com.yohandeku32.nusamusic.model

enum class LibraryContentState {
    LOADING, CONTENT, EMPTY, PERMISSION_REQUIRED, SCAN_FAILED;

    companion object {
        fun resolve(
            hasSongs: Boolean,
            hasAccess: Boolean,
            cacheLoaded: Boolean,
            scanning: Boolean,
            scanFailed: Boolean
        ): LibraryContentState = when {
            !hasAccess -> PERMISSION_REQUIRED
            hasSongs -> CONTENT
            !cacheLoaded || scanning -> LOADING
            scanFailed -> SCAN_FAILED
            else -> EMPTY
        }
    }
}
