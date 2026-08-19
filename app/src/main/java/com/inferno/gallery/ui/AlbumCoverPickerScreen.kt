package com.inferno.gallery.ui

import androidx.compose.material3.FilledTonalIconButton
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.sqlite.db.SimpleSQLiteQuery
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.video.videoFrameMillis
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.utils.tick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumCoverPickerScreen(
    bucketName: String,
    onCoverPicked: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    var isLoading by remember { mutableStateOf(true) }
    var uriList by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // For Favorites we observe the live ViewModel state
    val favoriteItems by viewModel.favoriteMedia.collectAsState()

    LaunchedEffect(bucketName, favoriteItems) {
        withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)

            val uriStrings: List<String> = when {
                bucketName == "Favorites" -> {
                    favoriteItems.filter { !it.isVideo }.map { it.uri.toString() }
                }
                bucketName == "All" -> {
                    val sql = "SELECT uriString FROM core_media WHERE bucketName != 'Trash' AND isVideo = 0 ORDER BY dateAdded DESC LIMIT 300"
                    db.mediaDao().getUrisRaw(SimpleSQLiteQuery(sql))
                }
                bucketName == "Videos" -> {
                    val sql = "SELECT uriString FROM core_media WHERE isVideo = 1 AND bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 300"
                    db.mediaDao().getUrisRaw(SimpleSQLiteQuery(sql))
                }
                bucketName.equals("Camera", ignoreCase = true) -> {
                    val sql = "SELECT uriString FROM core_media WHERE bucketName LIKE '%Camera%' AND bucketName != 'Trash' AND isVideo = 0 ORDER BY dateAdded DESC LIMIT 300"
                    db.mediaDao().getUrisRaw(SimpleSQLiteQuery(sql))
                }
                bucketName.contains("Screenshot", ignoreCase = true) -> {
                    val sql = "SELECT uriString FROM core_media WHERE bucketName LIKE '%Screenshot%' AND bucketName != 'Trash' AND isVideo = 0 ORDER BY dateAdded DESC LIMIT 300"
                    db.mediaDao().getUrisRaw(SimpleSQLiteQuery(sql))
                }
                bucketName.contains("Screenrecord", ignoreCase = true) || bucketName.contains("Screen record", ignoreCase = true) -> {
                    val sql = "SELECT uriString FROM core_media WHERE (bucketName LIKE '%Screenrecord%' OR bucketName LIKE '%Screen record%') AND bucketName != 'Trash' AND isVideo = 0 ORDER BY dateAdded DESC LIMIT 300"
                    db.mediaDao().getUrisRaw(SimpleSQLiteQuery(sql))
                }
                else -> {
                    val safeName = bucketName.replace("'", "''")
                    val sql = "SELECT uriString FROM core_media WHERE bucketName = '$safeName' AND bucketName != 'Trash' AND isVideo = 0 ORDER BY dateAdded DESC LIMIT 300"
                    db.mediaDao().getUrisRaw(SimpleSQLiteQuery(sql))
                }
            }

            withContext(Dispatchers.Main) {
                uriList = uriStrings.map { Uri.parse(it) }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Change Cover",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.ContainedLoadingIndicator(modifier = Modifier.size(40.dp))
                }
            }
            uriList.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No photos in this album",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uriList, key = { _, uri -> uri.toString() }) { _, uri ->
                        var isPressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.93f else 1f,
                            animationSpec = if (isPressed) MotionTokens.snappySpring() else MotionTokens.bouncySpring(),
                            label = "coverPickerScale"
                        )
                        val request = remember(uri) {
                            ImageRequest.Builder(context)
                                .data(uri)
                                .size(300, 300)
                                .precision(Precision.INEXACT)
                                .crossfade(MotionTokens.CrossfadeDuration)
                                .build()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .scale(scale)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .pointerInput(uri) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            tryAwaitRelease()
                                            isPressed = false
                                        },
                                        onTap = {
                                            view.tick()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onCoverPicked(uri.toString())
                                        }
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
