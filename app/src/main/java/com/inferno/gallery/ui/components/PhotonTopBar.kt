package com.inferno.gallery.ui.components

import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


/**
 * Unified top bar composable.
 *
 * Replaces 5 different header patterns:
 *   - Collage / Stitch: hand-rolled 72dp Surface row
 *   - DuplicateCleaner / AlbumCover: standard TopAppBar
 *   - PhotoMap: floating pill surface
 *   - ImageEditor: floating row toolbar (use [TopBarStyle.Floating])
 *
 * @param title            Header title.
 * @param style            Visual style — [TopBarStyle.Attached], [TopBarStyle.Floating].
 * @param onNavigateUp     If non-null, shows a back arrow IconButton.
 * @param actions          Trailing icon slot (Row scope for multiple icons).
 */
enum class PhotonTopBarStyle {
    /** Attached to the top of the screen with status-bar padding. */
    Attached,
    /** Pill-shaped floating surface, overlaid on content. */
    Floating
}

@Composable
fun PhotonTopBar(
    title: String,
    modifier: Modifier = Modifier,
    style: PhotonTopBarStyle = PhotonTopBarStyle.Attached,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    when (style) {
        PhotonTopBarStyle.Attached -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(72.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onNavigateUp != null) {
                        FilledTonalIconButton(onClick = onNavigateUp) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(

                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = if (onNavigateUp != null) 4.dp else 16.dp)
                            .weight(1f)
                    )
                    actions()
                }
            }
        }

        PhotonTopBarStyle.Floating -> {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = ShapeFull,
                shadowElevation = 4.dp,
                modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onNavigateUp != null) {
                        FilledTonalIconButton(onClick = onNavigateUp) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(

                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = if (onNavigateUp != null) 4.dp else 12.dp)
                            .weight(1f)
                    )
                    actions()
                }
            }
        }
    }
}
