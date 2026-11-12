package com.inferno.gallery.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.ShapeMedium

@Composable
fun RenameAlbumDialog(
    currentName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(currentName) { mutableStateOf(currentName) }
    val trimmed = text.trim()
    val isChanged = trimmed.isNotEmpty() && trimmed != currentName

    PhotonDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.dialog_rename_album_title),
        icon = ImageVector.vectorResource(R.drawable.ic_ms_edit),
        confirmText = stringResource(R.string.action_save),
        isConfirmEnabled = isChanged,
        onConfirm = {
            if (isChanged) {
                onConfirm(trimmed)
            }
            onDismissRequest()
        },
        dismissText = stringResource(R.string.action_cancel),
        onDismiss = onDismissRequest,
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.dialog_rename_album_label)) },
                singleLine = true,
                shape = ShapeMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    )
}

