package com.inferno.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.theme.ShapeSmall

/**
 * Unified section header composable.
 *
 * Replaces 3 different ad-hoc section header treatments:
 *   - Albums: accent bar via shapes.small
 *   - Search: RoundedCornerShape(2.dp) + labelLarge
 *   - Settings: uppercased labelMedium
 *
 * Design: left accent bar (2dp wide, ShapeSmall, primary color) +
 * titleSmall weight text + consistent horizontal padding.
 *
 * @param title   Section title text.
 * @param modifier Modifier for the outer Row.
 * @param action  Optional trailing slot (e.g. a TextButton "See all").
 */
@Composable
fun PhotonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = ShapeSmall
                )
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (action != null) {
            action()
        }
    }
}
