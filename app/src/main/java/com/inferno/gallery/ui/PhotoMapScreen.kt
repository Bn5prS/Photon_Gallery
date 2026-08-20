package com.inferno.gallery.ui

import androidx.compose.material3.FilledTonalIconButton
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.compose.AsyncImage
import coil3.request.*
import coil3.size.Precision
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.utils.haptickClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.*
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


// ── Data Classes ──

data class PhotoCluster(
    val center: GeoPoint,
    val items: List<GalleryItem>,
    val radius: Double // in degrees
)

// Helper to format date headers
private fun formatGroupHeader(dateAddedSeconds: Long): String {
    val timeMs = dateAddedSeconds * 1000L
    val itemDate = java.time.Instant.ofEpochMilli(timeMs)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    
    val today = java.time.LocalDate.now()
    val yesterday = today.minusDays(1)
    
    return when (itemDate) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> {
            if (itemDate.year == today.year) {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d", java.util.Locale.getDefault())
                itemDate.format(formatter)
            } else {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.getDefault())
                itemDate.format(formatter)
            }
        }
    }
}

// Helper to convert drawable to bitmap safely
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }
    
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

// ── Main Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoMapScreen(
    galleryViewModel: GalleryViewModel = viewModel(),
    onPhotoClick: (String, String?, String?) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gpsScanState by galleryViewModel.gpsScanState.collectAsState()
    val geotaggedMedia by galleryViewModel.geotaggedMedia.collectAsState()

    // Initialize osmdroid config
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = context.cacheDir
        Configuration.getInstance().osmdroidTileCache = java.io.File(context.cacheDir, "osmdroid")
        // Load existing geotagged media first, then scan for new GPS data
        galleryViewModel.loadGeotaggedMedia()
        galleryViewModel.scanGpsMetadata()
    }

    val isScanning = gpsScanState is GalleryViewModel.GpsScanState.Scanning
    val isDone = gpsScanState is GalleryViewModel.GpsScanState.Done || geotaggedMedia.isNotEmpty()

    // Cluster state
    val clusters = remember(geotaggedMedia) {
        clusterPhotos(geotaggedMedia, clusterRadiusDeg = 0.01) // ~1km radius
    }

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onTertiaryColor = MaterialTheme.colorScheme.onTertiary

    Box(modifier = Modifier.fillMaxSize()) {
        if (geotaggedMedia.isEmpty() && isScanning) {
            // Scanning state (imported from MapScanningState.kt)
            GpsScanningAnimation(
                scanState = gpsScanState,
                onBackClick = onBackClick
            )
        } else if (geotaggedMedia.isEmpty() && isDone) {
            // No geotagged photos (imported from MapScanningState.kt)
            NoGeotaggedPhotosState(onBackClick = onBackClick)
        } else {
            // Map with persistent image sheet
            var mapView by remember { mutableStateOf<MapView?>(null) }
            var focusedMarker by remember { mutableStateOf<Marker?>(null) }
            
            // Sheet is locked to 50% height
            val heightFraction = 0.50f

            // Group geotagged items by date
            val groupedMedia = remember(geotaggedMedia) {
                val list = mutableListOf<GalleryListItem>()
                var lastHeader: String? = null
                geotaggedMedia.forEach { item ->
                    val header = formatGroupHeader(item.dateAdded)
                    if (header != lastHeader) {
                        list.add(GalleryListItem.Header(header))
                        lastHeader = header
                    }
                    list.add(GalleryListItem.Item(item))
                }
                list
            }

            // Grid state to track scrolling
            val gridState = rememberLazyGridState()

            val clusterBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

            // Preload cluster cover thumbnails
            LaunchedEffect(clusters) {
                clusters.forEach { cluster ->
                    val firstItem = cluster.items.firstOrNull()
                    if (firstItem != null && !clusterBitmaps.containsKey(firstItem.id)) {
                        launch(Dispatchers.IO) {
                            try {
                                val loader = coil3.SingletonImageLoader.get(context)
                                val request = ImageRequest.Builder(context)
                                    .data(firstItem.resolvedUri)
                                    .size(160, 160)
                                    .precision(Precision.EXACT)
                                    .allowHardware(false)
                                    .build()
                                val result = loader.execute(request)
                                val image = (result as? coil3.request.SuccessResult)?.image
                                val bitmap = when (image) {
                                    is BitmapImage -> image.bitmap
                                    is DrawableImage -> drawableToBitmap(image.drawable)
                                    else -> null
                                }
                                if (bitmap != null) {
                                    clusterBitmaps[firstItem.id] = bitmap
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            // Track the first visible image of the scrolled viewport
            val firstVisibleImage by remember(groupedMedia) {
                derivedStateOf {
                    if (groupedMedia.isEmpty()) return@derivedStateOf null
                    val startIndex = gridState.firstVisibleItemIndex
                    if (startIndex in groupedMedia.indices) {
                        for (i in startIndex until groupedMedia.size) {
                            val item = groupedMedia[i]
                            if (item is GalleryListItem.Item) {
                                return@derivedStateOf item.galleryItem
                            }
                        }
                    }
                    null
                }
            }

            val haptic = LocalHapticFeedback.current
            var lastHapticId by remember { mutableStateOf<String?>(null) }

            // Snappy Tactile feedback on scrolling
            LaunchedEffect(firstVisibleImage) {
                val item = firstVisibleImage
                if (item != null && item.id != lastHapticId) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticId = item.id
                }
            }

            // Liquid breathing wave phase animation
            val infiniteTransition = rememberInfiniteTransition(label = "markerBreathe")
            val breatheAnimation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2 * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "breathe"
            )

            var loadedBitmapState by remember { mutableStateOf<Bitmap?>(null) }

            // Spring-pop scale Animatable for focused marker
            val markerScale = remember { Animatable(1f) }

            // Spring-pop animation: plays when focused image changes
            LaunchedEffect(firstVisibleImage) {
                markerScale.snapTo(0f)
                markerScale.animateTo(
                    targetValue = 1.1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
                markerScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }

            // 1. Asynchronously load the raw bitmap on change (prevents infinite reloading)
            LaunchedEffect(firstVisibleImage) {
                val item = firstVisibleImage
                if (item != null) {
                    val bitmap = withContext<Bitmap?>(Dispatchers.IO) {
                        try {
                            val loader = coil3.SingletonImageLoader.get(context)
                            val request = ImageRequest.Builder(context)
                                .data(item.resolvedUri)
                                .size(160, 160)
                                .precision(Precision.EXACT)
                                .allowHardware(false)
                                .build()
                            val result = loader.execute(request)
                            val image = (result as? coil3.request.SuccessResult)?.image
                            when (image) {
                                is BitmapImage -> image.bitmap
                                is DrawableImage -> drawableToBitmap(image.drawable)
                                else -> null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    loadedBitmapState = bitmap
                } else {
                    loadedBitmapState = null
                }
            }

            // 2. Debounced Glide Panning (80ms delay during rapid scroll to prevent jitter)
            LaunchedEffect(firstVisibleImage, mapView) {
                val item = firstVisibleImage
                val map = mapView
                if (item != null && map != null) {
                    val lat = item.latitude
                    val lon = item.longitude
                    if (lat != null && lon != null) {
                        delay(80) // Debounce delay
                        val geoPoint = GeoPoint(lat, lon)
                        map.controller.animateTo(geoPoint)
                    }
                }
            }

            // 3. Dynamic Marker Render Updating (includes spring-pop scale)
            val animPhase = breatheAnimation
            val currentMarkerScale = markerScale.value
            LaunchedEffect(loadedBitmapState, animPhase, currentMarkerScale, firstVisibleImage, mapView) {
                val item = firstVisibleImage
                val map = mapView
                if (item != null && map != null) {
                    val lat = item.latitude
                    val lon = item.longitude
                    if (lat != null && lon != null) {
                        val geoPoint = GeoPoint(lat, lon)
                        val bitmap = loadedBitmapState
                        val wavyDrawable = if (bitmap != null) {
                            createWavyDrawable(context, bitmap, animPhase, currentMarkerScale)
                        } else {
                            createDefaultWavyDrawable(context, animPhase, currentMarkerScale)
                        }

                        var marker = focusedMarker
                        if (marker == null) {
                            marker = Marker(map).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                            focusedMarker = marker
                        }

                        marker.position = geoPoint
                        marker.icon = wavyDrawable
                        marker.title = item.name

                        if (!map.overlays.contains(marker)) {
                            map.overlays.add(marker)
                        }
                        map.invalidate()
                    }
                } else {
                    focusedMarker?.let { marker ->
                        map?.overlays?.remove(marker)
                        focusedMarker = null
                        map?.invalidate()
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Map View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f - heightFraction)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                                mapView = this
                            }
                        },
                        update = { map ->
                            map.overlays.clear()

                            // Render clusters/other markers
                            clusters.forEach { cluster ->
                                val count = cluster.items.size
                                val marker = Marker(map)
                                marker.position = cluster.center
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                marker.title = "${count} photo${if (count > 1) "s" else ""}"

                                val sizeDp = (42 + minOf(count * 2, 20)).coerceAtMost(64)
                                val density = context.resources.displayMetrics.density
                                val sizePx = (sizeDp * density).toInt()

                                val firstItem = cluster.items.firstOrNull()
                                val bitmap = firstItem?.let { clusterBitmaps[it.id] }

                                val drawable = if (bitmap != null) {
                                    createWavyClusterDrawable(
                                        context = context,
                                        bitmap = bitmap,
                                        count = count,
                                        sizePx = sizePx,
                                        bgColor = tertiaryColor.toArgb(),
                                        textColor = onTertiaryColor.toArgb()
                                    )
                                } else {
                                    createPlaceholderWavyClusterDrawable(
                                        context = context,
                                        count = count,
                                        sizePx = sizePx,
                                        bgColor = tertiaryColor.toArgb(),
                                        textColor = onTertiaryColor.toArgb()
                                    )
                                }
                                marker.icon = drawable

                                marker.setOnMarkerClickListener { _, _ ->
                                    // Cluster-burst zoom: fit the map to the cluster's bounding box
                                    val clusterItems = cluster.items
                                    if (clusterItems.size <= 1) {
                                        // Single item — zoom in close
                                        map.controller.animateTo(cluster.center)
                                        map.controller.setZoom(17.0)
                                    } else {
                                        val lats = clusterItems.mapNotNull { it.latitude }
                                        val lons = clusterItems.mapNotNull { it.longitude }
                                        if (lats.isNotEmpty() && lons.isNotEmpty()) {
                                            val padding = 0.003
                                            val boundingBox = BoundingBox(
                                                lats.max() + padding,
                                                lons.max() + padding,
                                                lats.min() - padding,
                                                lons.min() - padding
                                            )
                                            map.post {
                                                map.zoomToBoundingBox(boundingBox, true, 60)
                                            }
                                        }
                                    }
                                    // Smoothly scroll list to the first image of this cluster
                                    val targetId = clusterItems.firstOrNull()?.id
                                    val index = groupedMedia.indexOfFirst {
                                        (it as? GalleryListItem.Item)?.galleryItem?.id == targetId
                                    }
                                    if (index >= 0) {
                                        coroutineScope.launch {
                                            gridState.animateScrollToItem(index)
                                        }
                                    }
                                    true
                                }
                                map.overlays.add(marker)
                            }

                            // Keep focusedMarker rendered on top
                            focusedMarker?.let {
                                map.overlays.add(it)
                            }

                            // Zoom out to bound initial clusters once when loaded
                            if (clusters.isNotEmpty() && focusedMarker == null) {
                                try {
                                    val points = clusters.map { it.center }
                                    val minLat = points.minOf { it.latitude }
                                    val maxLat = points.maxOf { it.latitude }
                                    val minLon = points.minOf { it.longitude }
                                    val maxLon = points.maxOf { it.longitude }
                                    val padding = 0.02
                                    map.post {
                                        map.zoomToBoundingBox(
                                            BoundingBox(
                                                maxLat + padding, maxLon + padding,
                                                minLat - padding, minLon - padding
                                            ),
                                            true, 80
                                        )
                                    }
                                } catch (_: Exception) {}
                            }

                            map.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Bar back overlay with transparent material styling
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = com.inferno.gallery.ui.theme.ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            tonalElevation = 0.dp
                        ) {
                            FilledTonalIconButton(
                                onClick = onBackClick,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = com.inferno.gallery.ui.theme.ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = "Photo Map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Re-center FAB
                    FloatingActionButton(
                        onClick = {
                            mapView?.let { map ->
                                if (clusters.isNotEmpty()) {
                                    val points = clusters.map { it.center }
                                    val minLat = points.minOf { it.latitude }
                                    val maxLat = points.maxOf { it.latitude }
                                    val minLon = points.minOf { it.longitude }
                                    val maxLon = points.maxOf { it.longitude }
                                    val padding = 0.02
                                    map.zoomToBoundingBox(
                                        BoundingBox(
                                            maxLat + padding, maxLon + padding,
                                            minLat - padding, minLon - padding
                                        ),
                                        true, 80
                                    )
                                }
                            }
                        },
                        shape = com.inferno.gallery.ui.theme.ShapeFull,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .size(52.dp)
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_my_location), contentDescription = "Re-center", modifier = Modifier.size(24.dp))
                    }
                }

                // Bottom Image Grid Sheet
                Surface(
                    shape = ShapeEdgeTop,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // Drag Handle / Header area
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(com.inferno.gallery.ui.theme.ShapeFull)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                            )
                        }

                        // Info title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Geotagged Photos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = com.inferno.gallery.ui.theme.ShapeFull,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${geotaggedMedia.size} items",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Scrollable grid
                        Box(modifier = Modifier.weight(1f)) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                state = gridState,
                                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    count = groupedMedia.size,
                                    key = { index ->
                                        val item = groupedMedia[index]
                                        when (item) {
                                            is GalleryListItem.Header -> "header_${item.title}_$index"
                                            is GalleryListItem.Item -> item.galleryItem.id
                                        }
                                    },
                                    span = { index ->
                                        val item = groupedMedia[index]
                                        if (item is GalleryListItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                                    }
                                ) { index ->
                                    val listItem = groupedMedia[index]
                                    when (listItem) {
                                        is GalleryListItem.Header -> {
                                            Text(
                                                text = listItem.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                                            )
                                        }
                                        is GalleryListItem.Item -> {
                                            val item = listItem.galleryItem
                                            Surface(
                                                shape = com.inferno.gallery.ui.theme.ShapeMedium,
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(com.inferno.gallery.ui.theme.ShapeMedium)
                                                    .haptickClickable {
                                                        galleryViewModel.setInitialDetailItem(item)
                                                        onPhotoClick(item.id, "geotagged", null)
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(item.resolvedUri)
                                                        .size(256)
                                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                                        .crossfade(true)
                                                        .build(),
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
                }
            }
        }
    }
}

// ── Custom Wavy Marker Drawing ──

private fun createWavyDrawable(
    context: android.content.Context,
    bitmap: Bitmap,
    phase: Float,
    springScale: Float = 1f
): Drawable {
    val sizePx = 160
    val breatheScale = 1.0f + 0.04f * sin(phase)
    val scale = breatheScale * springScale.coerceAtLeast(0.01f)
    val finalSize = (sizePx * scale).toInt()
    val output = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path()

    val centerX = finalSize / 2f
    val centerY = finalSize / 2f
    val r0 = (finalSize / 2f) - 12f
    val amplitude = r0 * 0.07f
    val waveCount = 8f
    val numPoints = 120
    val wavePhase = phase * 2f

    for (i in 0 until numPoints) {
        val angle = i * (2 * Math.PI / numPoints)
        val r = r0 + amplitude * sin(waveCount * angle + wavePhase)
        val x = (centerX + r * cos(angle)).toFloat()
        val y = (centerY + r * sin(angle)).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x2A000000
        style = Paint.Style.FILL
    }
    val shadowPath = Path(path).apply {
        offset(0f, 4f)
    }
    canvas.drawPath(shadowPath, shadowPaint)

    // Clip & Draw Image
    canvas.save()
    canvas.clipPath(path)
    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
    val destRect = Rect(0, 0, finalSize, finalSize)
    canvas.drawBitmap(bitmap, srcRect, destRect, paint)
    canvas.restore()

    // Border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawPath(path, borderPaint)

    return BitmapDrawable(context.resources, output)
}

private fun createDefaultWavyDrawable(
    context: android.content.Context,
    phase: Float,
    springScale: Float = 1f
): Drawable {
    val sizePx = 120
    val breatheScale = 1.0f + 0.04f * sin(phase)
    val scale = breatheScale * springScale.coerceAtLeast(0.01f)
    val finalSize = (sizePx * scale).toInt()
    val output = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val path = Path()
    val centerX = finalSize / 2f
    val centerY = finalSize / 2f
    val r0 = (finalSize / 2f) - 8f
    val amplitude = r0 * 0.08f
    val waveCount = 8f
    val numPoints = 120
    val wavePhase = phase * 2f

    for (i in 0 until numPoints) {
        val angle = i * (2 * Math.PI / numPoints)
        val r = r0 + amplitude * sin(waveCount * angle + wavePhase)
        val x = (centerX + r * cos(angle)).toFloat()
        val y = (centerY + r * sin(angle)).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.RED
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawPath(path, borderPaint)

    return BitmapDrawable(context.resources, output)
}

// ── Clustering ──

private fun clusterPhotos(
    photos: List<GalleryItem>,
    clusterRadiusDeg: Double
): List<PhotoCluster> {
    if (photos.isEmpty()) return emptyList()

    val geoPhotos = photos.filter { it.latitude != null && it.longitude != null }
    if (geoPhotos.isEmpty()) return emptyList()

    val assigned = BooleanArray(geoPhotos.size)
    val clusters = mutableListOf<PhotoCluster>()

    for (i in geoPhotos.indices) {
        if (assigned[i]) continue

        val centerLat = geoPhotos[i].latitude!!
        val centerLon = geoPhotos[i].longitude!!
        val clusterItems = mutableListOf(geoPhotos[i])
        assigned[i] = true

        for (j in i + 1 until geoPhotos.size) {
            if (assigned[j]) continue
            val lat = geoPhotos[j].latitude!!
            val lon = geoPhotos[j].longitude!!
            val dist = sqrt((centerLat - lat).pow(2) + (centerLon - lon).pow(2))
            if (dist <= clusterRadiusDeg) {
                clusterItems.add(geoPhotos[j])
                assigned[j] = true
            }
        }

        val avgLat = clusterItems.mapNotNull { it.latitude }.average()
        val avgLon = clusterItems.mapNotNull { it.longitude }.average()

        clusters.add(
            PhotoCluster(
                center = GeoPoint(avgLat, avgLon),
                items = clusterItems.sortedByDescending { it.dateAdded },
                radius = clusterRadiusDeg
            )
        )
    }

    return clusters.sortedByDescending { it.items.size }
}

// ── Custom Cluster Marker Drawable ──

private fun createWavyClusterDrawable(
    context: android.content.Context,
    bitmap: Bitmap,
    count: Int,
    sizePx: Int,
    bgColor: Int,
    textColor: Int
): Drawable {
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path()

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val r0 = sizePx / 2f - 8f
    val amplitude = r0 * 0.07f
    val waveCount = 8f
    val numPoints = 120

    for (i in 0 until numPoints) {
        val angle = i * (2 * Math.PI / numPoints)
        val r = r0 + amplitude * sin(waveCount * angle)
        val x = (centerX + r * cos(angle)).toFloat()
        val y = (centerY + r * sin(angle)).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    // Draw shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x2A000000
        style = Paint.Style.FILL
    }
    val shadowPath = Path(path).apply {
        offset(0f, 3f)
    }
    canvas.drawPath(shadowPath, shadowPaint)

    // Clip and draw image
    canvas.save()
    canvas.clipPath(path)
    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
    val destRect = Rect(0, 0, sizePx, sizePx)
    canvas.drawBitmap(bitmap, srcRect, destRect, paint)

    // Draw dark semi-transparent overlay to make text pop
    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, overlayPaint)
    canvas.restore()

    // Draw white border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    canvas.drawPath(path, borderPaint)

    // Draw count text centered
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = sizePx * 0.35f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, 0xFF000000.toInt())
    }
    val textY = sizePx / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(count.toString(), sizePx / 2f, textY, textPaint)

    return BitmapDrawable(context.resources, output)
}

private fun createPlaceholderWavyClusterDrawable(
    context: android.content.Context,
    count: Int,
    sizePx: Int,
    bgColor: Int,
    textColor: Int
): Drawable {
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val path = Path()
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val r0 = sizePx / 2f - 8f
    val amplitude = r0 * 0.07f
    val waveCount = 8f
    val numPoints = 120

    for (i in 0 until numPoints) {
        val angle = i * (2 * Math.PI / numPoints)
        val r = r0 + amplitude * sin(waveCount * angle)
        val x = (centerX + r * cos(angle)).toFloat()
        val y = (centerY + r * sin(angle)).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    // Draw fill with primary background color
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, fillPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawPath(path, borderPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = sizePx * 0.35f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val textY = sizePx / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(count.toString(), sizePx / 2f, textY, textPaint)

    return BitmapDrawable(context.resources, output)
}
