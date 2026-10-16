package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.R
import com.yohandeku32.nusamusic.data.AudioCodecLoader
import com.yohandeku32.nusamusic.model.Song

/** A fixed cover mark, based on measured bit depth rather than file extension. */
@Composable
internal fun CdHiResBadge(song: Song?, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val uri = song?.uri
    var info by remember(uri) { mutableStateOf(uri?.let(AudioCodecLoader::cached)) }
    LaunchedEffect(uri) {
        info = uri?.let { AudioCodecLoader.load(context, it) }
    }
    if (info?.isHiRes == true) {
        Image(
            painter = painterResource(R.drawable.hi_res_audio_logo),
            contentDescription = "Hi-Res Audio",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(28.dp)
        )
    }
}
