package com.inferno.gallery.ui.editor.markup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.utils.pressScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkupTextEditorDialog(
    initialText: MarkupText?,
    onDismissRequest: () -> Unit,
    onConfirm: (MarkupText) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var textValue by remember { mutableStateOf(initialText?.text ?: "") }
    var selectedColor by remember { mutableStateOf(initialText?.color ?: Color.White) }
    var hasBackgroundBox by remember { mutableStateOf(initialText?.hasBackgroundBox ?: true) }
    val focusRequester = remember { FocusRequester() }
    val isEditing = initialText != null

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Top Bar: Cancel, Style toggle, Delete, Done
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Background box toggle pill
                    Surface(
                        shape = ShapeFull,
                        color = if (hasBackgroundBox) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.18f),
                        contentColor = if (hasBackgroundBox) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                        modifier = Modifier
                            .pressScale()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasBackgroundBox = !hasBackgroundBox
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_border_all),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (hasBackgroundBox) "Box: Fill" else "Box: None",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (isEditing && onDelete != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDelete(initialText.id)
                                onDismissRequest()
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Done Button
                    FilledTonalButton(
                        onClick = {
                            val trimmed = textValue.trim()
                            if (trimmed.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val textItem = initialText?.copy(
                                    text = trimmed,
                                    color = selectedColor,
                                    hasBackgroundBox = hasBackgroundBox
                                ) ?: MarkupText(
                                    text = trimmed,
                                    position = Offset(0.5f, 0.45f),
                                    color = selectedColor,
                                    scale = 1.0f,
                                    rotation = 0f,
                                    hasBackgroundBox = hasBackgroundBox
                                )
                                onConfirm(textItem)
                            } else if (initialText != null && onDelete != null) {
                                onDelete(initialText.id)
                                onDismissRequest()
                            } else {
                                onDismissRequest()
                            }
                        },
                        shape = ShapeFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // 2. Center Area: Unobstructed, live styled text field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isDark = (0.299 * selectedColor.red + 0.587 * selectedColor.green + 0.114 * selectedColor.blue) < 0.5
                    val boxBg = if (isDark) Color.White.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.85f)

                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .then(
                                if (hasBackgroundBox) {
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(boxBg)
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                } else {
                                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            textStyle = TextStyle(
                                color = selectedColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(selectedColor),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (textValue.isEmpty()) {
                                        Text(
                                            text = "Type text here…",
                                            style = TextStyle(
                                                color = if (hasBackgroundBox) selectedColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.55f),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // 3. Bottom Bar: ONLY the horizontal color palette directly above the keyboard
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(MarkupPalette.COLORS) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .pressScale()
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedColor = color
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                val checkTint = if (color == Color.White || color == Color(0xFFFFD600)) Color.Black else Color.White
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                    contentDescription = null,
                                    tint = checkTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
