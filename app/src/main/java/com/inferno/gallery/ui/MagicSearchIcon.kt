package com.inferno.gallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MagicSearchIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Base Search Icon
        Icon(Icons.Default.Search, contentDescription = null, tint = tint)
        // The Sparkle (Material 3 AutoAwesome) positioned at the top right
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-2).dp)
        )
    }
}
