package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.R

@Composable
internal fun VinylQualityBadge(
    label: String,
    showLosslessIcon: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier.testTag("vinyl-quality-face")
                .background(Color.Black, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLosslessIcon) {
                Icon(painterResource(R.drawable.apple_lossless_logo), null,
                    Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
            }
            Text(label, fontSize = 11.sp, lineHeight = 14.sp,
                color = Color.White, maxLines = 1)
        }
    }
}
