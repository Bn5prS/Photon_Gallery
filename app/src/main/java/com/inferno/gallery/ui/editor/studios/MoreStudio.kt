package com.inferno.gallery.ui.editor.studios

import androidx.compose.ui.res.stringResource

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.inferno.gallery.R

/**
 * Representation of an external photo editor or sharing application installed on the device.
 */
data class ExternalEditorApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isEditAction: Boolean
)

/**
 * "More" Studio:
 * Queries and launches 3rd-party photo editing and sharing applications installed on the device.
 *
 * Horizontal squircle app carousel with live app icons and names,
 * plus a persistent fallback System Share / Open With tile.
 */
@Composable
fun MoreStudio(
    imageUri: Uri,
    modifier: Modifier = Modifier,
    onAppLaunched: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val apps = remember(context, imageUri) {
        queryEditorApps(context, imageUri)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Studio header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.editor_more_other_apps),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${apps.size} available",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (apps.isEmpty()) {
            // Empty state when no 3rd-party apps are installed (e.g. pure clean emulator)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.editor_more_no_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = { launchSystemChooser(context, imageUri) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ms_share),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.detail_share))
                }
            }
        } else {
            // Horizontal carousel of squircle app tiles
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                items(
                    items = apps,
                    key = { "${it.packageName}/${it.activityName}" }
                ) { app ->
                    AppSquircleTile(
                        app = app,
                        onClick = {
                            launchExternalApp(context, imageUri, app)
                            onAppLaunched?.invoke(app.packageName)
                        }
                    )
                }

                // Persistent "System Share" action tile at the end
                item(key = "system_share_tile") {
                    SystemShareTile(
                        onClick = { launchSystemChooser(context, imageUri) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSquircleTile(
    app: ExternalEditorApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(10.dp)
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ms_edit),
                        contentDescription = app.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SystemShareTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 2.dp,
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_share),
                    contentDescription = "System Share",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Share",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Queries package manager for activities supporting image editing or sending.
 */
internal fun queryEditorApps(context: Context, imageUri: Uri): List<ExternalEditorApp> {
    return try {
        val pm = context.packageManager
        val editIntent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(imageUri, "image/*")
        }

        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val editResolutions = pm.queryIntentActivities(editIntent, flags)

        val myPackage = context.packageName
        val appList = mutableListOf<ExternalEditorApp>()
        val seenPackages = mutableSetOf<String>()
        seenPackages.add(myPackage) // Exclude self

        for (resolve in editResolutions) {
            val pkg = resolve.activityInfo.packageName
            if (seenPackages.add(pkg)) {
                val label = resolve.loadLabel(pm).toString()
                val icon = try {
                    drawableToImageBitmap(resolve.loadIcon(pm))
                } catch (e: Exception) {
                    null
                }
                appList.add(
                    ExternalEditorApp(
                        packageName = pkg,
                        activityName = resolve.activityInfo.name,
                        label = label,
                        icon = icon,
                        isEditAction = true
                    )
                )
            }
        }

        appList.sortedBy { it.label.lowercase() }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
    return try {
        val bitmap = drawable.toBitmap(
            width = 96,
            height = 96,
            config = Bitmap.Config.ARGB_8888
        )
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

private fun launchExternalApp(context: Context, imageUri: Uri, app: ExternalEditorApp) {
    try {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(imageUri, "image/*")
            setClassName(app.packageName, app.activityName)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch ${app.label}: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun launchSystemChooser(context: Context, imageUri: Uri) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(sendIntent, "Edit or share photo with...")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch chooser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
