package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun VinylQualityBadge(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(label, fontSize = 10.sp, lineHeight = 12.sp,
            color = Color(0xFF242424), maxLines = 1,
            modifier = Modifier.testTag("vinyl-quality-face")
                .background(Color.Black.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                .padding(horizontal = 5.dp, vertical = 3.dp))
    }
}
