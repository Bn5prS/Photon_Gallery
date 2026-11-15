package com.inferno.gallery.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.model.EditorTab

/**
 * Material 3 Expressive floating bottom dock for photo editing studios.
 *
 * Uses a floating tonal pill container with spring-morphed active tab corners
 * (`MaterialTheme.motionScheme`) paired with a detached circular Tool Search pill.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditorBottomTabs(
    selectedTab: EditorTab,
    onSelectTab: (EditorTab) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(EditorTab.entries) { tab ->
                        val isSelected = tab == selectedTab
                        val cornerRadius by animateDpAsState(
                            targetValue = if (isSelected) 22.dp else 12.dp,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                            label = "editorTabCorner"
                        )
                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            label = "editorTabBg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            label = "editorTabFg"
                        )

                        Surface(
                            onClick = { onSelectTab(tab) },
                            shape = RoundedCornerShape(cornerRadius),
                            color = containerColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                val tabLabel = when (tab) {
                                    EditorTab.AUTO -> stringResource(R.string.editor_tab_suggestions)
                                    EditorTab.CROP -> stringResource(R.string.editor_tab_crop)
                                    EditorTab.ADJUST -> stringResource(R.string.editor_tab_adjust)
                                    EditorTab.MARKUP -> stringResource(R.string.editor_tab_markup)
                                    EditorTab.FILTERS -> stringResource(R.string.editor_tab_filters)
                                    EditorTab.MORE -> stringResource(R.string.editor_tab_more)
                                }
                                Text(
                                    text = tabLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }

            // Detached circular Tool Search satellite pill
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_thin_search),
                    contentDescription = "Open tool search",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
