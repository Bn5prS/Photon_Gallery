package com.inferno.gallery.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R

/**
 * Floating quick action toolbar positioned above/around the photo canvas.
 *
 * Provides instant access to:
 * - Straighten icon shortcut
 * - Aspect ratio selector pill with current ratio label and dropdown chevron
 * - Hold-to-compare button `[|]` with gesture detection to reveal the original image
 * - Rotate 90° shortcut
 */
@Composable
fun EditorQuickActionBar(
    onStraighten: () -> Unit,
    currentAspect: String = "Freeform",
    onAspectClick: () -> Unit,
    onCompareChange: ((Boolean) -> Unit)? = null,
    onCompareDown: (() -> Unit)? = null,
    onCompareUp: (() -> Unit)? = null,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isComparing by remember { mutableStateOf(false) }

    val updateComparing: (Boolean) -> Unit = { comparing ->
        if (isComparing != comparing) {
            isComparing = comparing
            onCompareChange?.invoke(comparing)
            if (comparing) {
                onCompareDown?.invoke()
            } else {
                onCompareUp?.invoke()
            }
        }
    }

    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = CircleShape
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Straighten button
            IconButton(
                onClick = onStraighten,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_straighten),
                    contentDescription = "Straighten",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Aspect ratio pill dropdown
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAspectClick)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_aspect_ratio),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentAspect,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_keyboard_arrow_down),
                    contentDescription = "Select aspect ratio",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Hold-to-compare button [|]
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isComparing) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            updateComparing(true)
                            waitForUpOrCancellation()
                            updateComparing(false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_compare),
                    contentDescription = "Hold to compare with original",
                    tint = if (isComparing) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Rotate 90 degrees button
            IconButton(
                onClick = onRotate,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_rotate_90_degrees_ccw),
                    contentDescription = "Rotate 90 degrees",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
