package com.inferno.gallery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * M3 Expressive stitched tonal group inside the Trash confirmation card.
 * Allows users to suppress confirmation dialogs for the current session,
 * and/or bypass Trash to delete permanently.
 */
@Composable
fun TrashSessionOptions(
    skipConfirm: Boolean,
    onSkipConfirmChange: (Boolean) -> Unit,
    skipRecycleBin: Boolean,
    onSkipRecycleBinChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TrashOptionRow(
            checked = skipConfirm,
            onCheckedChange = onSkipConfirmChange,
            title = "Quick delete this session",
            subtitle = "Skip this prompt until app restarts",
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            isDanger = false
        )

        TrashOptionRow(
            checked = skipRecycleBin,
            onCheckedChange = onSkipRecycleBinChange,
            title = "Skip Trash (permanent)",
            subtitle = "Bypass Trash and free storage now",
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            isDanger = true
        )
    }
}

@Composable
private fun TrashOptionRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String,
    shape: RoundedCornerShape,
    isDanger: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor = when {
        checked && isDanger -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        checked -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    }

    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = (-0.1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = if (isDanger) {
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onError,
                        checkedTrackColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    SwitchDefaults.colors()
                },
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}
