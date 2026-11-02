package com.inferno.gallery.ui.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R

/**
 * Top navigation bar for the photo editor.
 *
 * Contains:
 * - Cancel 'X' icon button on the left
 * - Undo and Redo icon buttons (enabled/disabled states)
 * - Prominent "Save" pill button with "Save as copy" option (visible when changes exist)
 * - More options '⋮' icon button for overflow menu
 */
@Composable
fun EditorTopBar(
    onCancel: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    hasModifications: Boolean,
    onSave: () -> Unit = {},
    onSaveAsCopy: () -> Unit = {},
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Close / Cancel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_close),
                    contentDescription = stringResource(id = R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Undo button
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(44.dp)
                    .alpha(if (canUndo) 1.0f else 0.38f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_undo),
                    contentDescription = "Undo",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Redo button
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier
                    .size(44.dp)
                    .alpha(if (canRedo) 1.0f else 0.38f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_redo),
                    contentDescription = "Redo",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right side: "Save" split pill and More options menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedVisibility(
                visible = hasModifications,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSave,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        elevation = null
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSaveMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_unfold_more),
                                contentDescription = "Save options",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSaveMenu,
                            onDismissRequest = { showSaveMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save (replace original)") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ms_save),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showSaveMenu = false
                                    onSave()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save as copy") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ms_content_copy),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showSaveMenu = false
                                    onSaveAsCopy()
                                }
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onMoreOptions,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_more_vert),
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
