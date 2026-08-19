package com.inferno.gallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


@Composable
fun MagicSearchIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.tertiary) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Base Search Icon
        Icon(ImageVector.vectorResource(R.drawable.ic_ms_search), contentDescription = null, tint = tint)
        // The Sparkle (Material 3 AutoAwesome) positioned at the top right
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-2).dp)
        )
    }
}
