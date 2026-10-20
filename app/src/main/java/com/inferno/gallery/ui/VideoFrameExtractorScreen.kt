@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.inferno.gallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.workers.MediaSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun VideoFrameExtractorScreen(
    videoUri: Uri,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var currentFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingFrame by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val filmstripBitmaps = remember { mutableStateListOf<Bitmap>() }

    // Initialize retriever and read video metadata
    val retriever = remember { MediaMetadataRetriever() }

    DisposableEffect(videoUri) {
        try {
            retriever.setDataSource(context, videoUri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 1000L
        } catch (e: Throwable) {
            Toast.makeText(context, "Failed to load video", Toast.LENGTH_SHORT).show()
        }

        onDispose {
            try {
                retriever.release()
            } catch (_: Throwable) { }
            filmstripBitmaps.forEach {
                if (!it.isRecycled) it.recycle()
            }
            filmstripBitmaps.clear()
            currentFrameBitmap?.let {
                if (!it.isRecycled) it.recycle()
            }
        }
    }

    // Generate timeline filmstrip thumbnails (8 frames across duration)
    LaunchedEffect(durationMs) {
        if (durationMs <= 0L) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val count = 8
            val step = durationMs / count.coerceAtLeast(1)
            for (i in 0 until count) {
                val timeUs = (i * step) * 1000L
                val thumb = try {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        160,
                        160
                    )
                } catch (_: Throwable) { null }

                if (thumb != null) {
                    withContext(Dispatchers.Main) {
                        filmstripBitmaps.add(thumb)
                    }
                }
            }
        }
    }

    // Debounced high-precision frame extraction when currentPositionMs changes
    var extractJob by remember { mutableStateOf<Job?>(null) }
    fun requestFrame(positionMs: Long) {
        extractJob?.cancel()
        extractJob = coroutineScope.launch {
            isLoadingFrame = true
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    retriever.getFrameAtTime(
                        positionMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                } catch (_: Throwable) {
                    null
                }
            }
            if (bitmap != null) {
                currentFrameBitmap?.let { if (!it.isRecycled) it.recycle() }
                currentFrameBitmap = bitmap
            }
            isLoadingFrame = false
        }
    }

    // Load initial frame at 0ms
    LaunchedEffect(durationMs) {
        if (durationMs > 0L) {
            requestFrame(0L)
        }
    }

    // Save frame to MediaStore
    fun saveFrame() {
        if (isSaving) return
        isSaving = true
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    // Extract full uncompressed frame at exact microsecond
                    val fullBitmap = retriever.getFrameAtTime(
                        currentPositionMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    ) ?: currentFrameBitmap ?: return@withContext false

                    val fileName = "FRAME_${System.currentTimeMillis()}.jpg"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Photon Gallery")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
                        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    }

                    val insertUri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: return@withContext false

                    context.contentResolver.openOutputStream(insertUri)?.use { out ->
                        fullBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(insertUri, contentValues, null, null)

                    // Dispatch MediaSyncWorker to index newly saved frame into Room database
                    val syncWork = OneTimeWorkRequestBuilder<MediaSyncWorker>().build()
                    WorkManager.getInstance(context).enqueue(syncWork)
                    true
                } catch (e: Throwable) {
                    false
                }
            }

            isSaving = false
            if (success) {
                Toast.makeText(context, "Frame saved to Pictures/Photon Gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save frame", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Screen Layout ──────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "Frame Extractor",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ElevatedButton(
                    onClick = { saveFrame() },
                    enabled = !isSaving && currentFrameBitmap != null,
                    shape = CircleShape,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isSaving) {
                        ContainedLoadingIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_save),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Main Frame Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(ShapeLargeIncreased)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                val bmp = currentFrameBitmap
                if (bmp != null && !bmp.isRecycled) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Extracted video frame",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isLoadingFrame) {
                    ContainedLoadingIndicator(modifier = Modifier.size(48.dp))
                }
            }

            // Timeline & Controls Dock
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Timecode Readout
                    val formattedCurrent = formatTimeCode(currentPositionMs)
                    val formattedTotal = formatTimeCode(durationMs)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedCurrent,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "/ $formattedTotal",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filmstrip Track Preview
                    if (filmstripBitmaps.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(ShapeMedium)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            itemsIndexed(filmstripBitmaps) { _, thumb ->
                                if (!thumb.isRecycled) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Scrubber Slider
                    var sliderValue by remember(currentPositionMs) {
                        mutableFloatStateOf(currentPositionMs.toFloat())
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            sliderValue = newValue
                            currentPositionMs = newValue.toLong().coerceIn(0L, durationMs)
                        },
                        onValueChangeFinished = {
                            requestFrame(currentPositionMs)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Precision Nudge Controls (-1s, -33ms, +33ms, +1s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NudgeButton(label = "-1s", tooltip = "Rewind 1 second") {
                            val target = (currentPositionMs - 1000L).coerceAtLeast(0L)
                            currentPositionMs = target
                            requestFrame(target)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        NudgeButton(label = "-33ms", tooltip = "Previous frame (~33ms)") {
                            val target = (currentPositionMs - 33L).coerceAtLeast(0L)
                            currentPositionMs = target
                            requestFrame(target)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        NudgeButton(label = "+33ms", tooltip = "Next frame (~33ms)") {
                            val target = (currentPositionMs + 33L).coerceAtMost(durationMs)
                            currentPositionMs = target
                            requestFrame(target)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        NudgeButton(label = "+1s", tooltip = "Forward 1 second") {
                            val target = (currentPositionMs + 1000L).coerceAtMost(durationMs)
                            currentPositionMs = target
                            requestFrame(target)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(
    label: String,
    tooltip: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTimeCode(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
}
