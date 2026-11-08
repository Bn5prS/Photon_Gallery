package com.inferno.gallery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.ShapeExtraLarge

/**
 * Sleek, minimal tonal dialog following Material 3 Expressive guidelines.
 * Uses dynamic wallpaper accent color tokens throughout (zero hardcoded red/green/blue).
 * Cancel and Confirm buttons are clustered tightly together on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotonDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String = stringResource(R.string.action_cancel),
    onDismiss: () -> Unit = onDismissRequest,
    isConfirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp),
            shape = ShapeExtraLarge, // 24.dp rounded corners
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Asymmetric Left-Aligned Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 44dp Squircle Icon Badge in Wallpaper Primary Container
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Title & Optional Subtitle
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Optional Body / Input Content Slot
                if (content != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }

                // Clustered Action Buttons Row (Cancel & Confirm close to each other on the right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button: Tonal Surface Pill
                    Button(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = dismissText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Confirm Button: Wallpaper Accent Filled Pill
                    Button(
                        onClick = onConfirm,
                        enabled = isConfirmEnabled,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

