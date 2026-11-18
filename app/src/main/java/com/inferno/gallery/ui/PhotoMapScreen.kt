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
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
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
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.getDefault())
    
    return when (itemDate) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> itemDate.format(formatter)
    }
}

// Ensure a bitmap is software-renderable (hardware bitmaps crash on Canvas draw)
private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
    if (bitmap.config == Bitmap.Config.HARDWARE) {
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    return bitmap
}

// Helper to convert drawable to bitmap safely (always returns a software bitmap)
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        val bmp = drawable.bitmap
        if (bmp != null) {
            // Hardware bitmaps cannot be drawn onto a software Canvas;
            // copy to a software-config bitmap when needed.
            if (bmp.config == Bitmap.Config.HARDWARE) {
                return bmp.copy(Bitmap.Config.ARGB_8888, false)
                    ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
            return bmp
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
            // ── Map with BottomSheetScaffold ──
            var mapView by remember { mutableStateOf<MapView?>(null) }
            var focusedMarker by remember { mutableStateOf<Marker?>(null) }
            var focusedItemFromMarker by remember { mutableStateOf<GalleryItem?>(null) }

            // Place name lookups from geocoded cache
            val placesClusters by galleryViewModel.placesClusters.collectAsState()
            val placeNameByMediaId by galleryViewModel.placeNameByMediaId.collectAsState()

            // Place filter + search state
            var selectedPlace by remember { mutableStateOf<String?>(null) }
            var searchQuery by remember { mutableStateOf("") }
            var isSearchActive by remember { mutableStateOf(false) }

            // Map style state (0=default, 1=topo, 2=dark)
            var mapStyle by remember { mutableIntStateOf(0) }
            val darkTileSource = remember {
                org.osmdroid.tileprovider.tilesource.XYTileSource(
                    "CartoDB_Dark", 0, 19, 256, ".png",
                    arrayOf("https://basemaps.cartocdn.com/dark_all/")
                )
            }

            // Filter media by place and search
            val filteredMedia = remember(geotaggedMedia, selectedPlace, placeNameByMediaId, searchQuery) {
                var media = geotaggedMedia
                if (selectedPlace != null) {
                    media = media.filter { placeNameByMediaId[it.id.toLong()] == selectedPlace }
                }
                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.lowercase()
                    media = media.filter { item ->
                        item.name.lowercase().contains(q) ||
                        (placeNameByMediaId[item.id.toLong()]?.lowercase()?.contains(q) == true) ||
                        formatGroupHeader(item.dateAdded).lowercase().contains(q)
                    }
                }
                media
            }

            // Re-cluster based on filtered media
            val displayClusters = remember(filteredMedia) {
                clusterPhotos(filteredMedia, clusterRadiusDeg = 0.01)
            }

            // Group filtered items by date with place names
            val groupedMedia = remember(filteredMedia, placeNameByMediaId) {
                val list = mutableListOf<GalleryListItem>()
                var lastHeader: String? = null
                filteredMedia.forEach { item ->
                    val header = formatGroupHeader(item.dateAdded)
                    if (header != lastHeader) {
                        list.add(GalleryListItem.Header(title = header, id = item.id))
                        lastHeader = header
                    }
                    list.add(GalleryListItem.Item(item))
                }
                list
            }

            // Grid state
            val gridState = rememberLazyGridState()

            val clusterBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

            // Preload cluster cover thumbnails
            LaunchedEffect(displayClusters) {
                displayClusters.forEach { cluster ->
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
                                    is BitmapImage -> ensureSoftwareBitmap(image.bitmap)
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

            // Track the first visible image
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

            // Haptic feedback on scroll
            LaunchedEffect(firstVisibleImage) {
                val item = firstVisibleImage
                if (item != null && item.id != lastHapticId) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticId = item.id
                }
            }

            var loadedBitmapState by remember { mutableStateOf<Bitmap?>(null) }

            // Load focused bitmap
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
                                is BitmapImage -> ensureSoftwareBitmap(image.bitmap)
                                is DrawableImage -> drawableToBitmap(image.drawable)
                                else -> null
                            }
                        } catch (e: Exception) { null }
                    }
                    loadedBitmapState = bitmap
                } else {
                    loadedBitmapState = null
                }
            }

            // Debounced map panning on grid scroll
            LaunchedEffect(firstVisibleImage, mapView) {
                val item = firstVisibleImage
                val map = mapView
                if (item != null && map != null) {
                    val lat = item.latitude
                    val lon = item.longitude
                    if (lat != null && lon != null) {
                        delay(80)
                        val geoPoint = GeoPoint(lat, lon)
                        map.controller.animateTo(geoPoint)
                    }
                }
            }

            // Focused marker update (circular photo marker)
            val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
            LaunchedEffect(loadedBitmapState, firstVisibleImage, mapView) {
                val item = firstVisibleImage
                val map = mapView
                if (item != null && map != null) {
                    val lat = item.latitude
                    val lon = item.longitude
                    if (lat != null && lon != null) {
                        val geoPoint = GeoPoint(lat, lon)
                        val bitmap = loadedBitmapState
                        val density = context.resources.displayMetrics.density
                        val markerBitmap = if (bitmap != null) {
                            createCircularMarkerBitmap(
                                bitmap = bitmap,
                                sizePx = (64 * density).toInt()
                            )
                        } else {
                            createDefaultMarkerBitmap(
                                sizePx = (44 * density).toInt(),
                                fillColor = primaryContainerColor
                            )
                        }

                        var marker = focusedMarker
                        if (marker == null) {
                            marker = Marker(map).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                            focusedMarker = marker
                        }

                        marker.position = geoPoint
                        marker.icon = BitmapDrawable(context.resources, markerBitmap)
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

            // Map style toggle effect
            LaunchedEffect(mapStyle) {
                val source = when (mapStyle) {
                    1 -> TileSourceFactory.OpenTopo
                    2 -> try { darkTileSource } catch (_: Exception) { TileSourceFactory.MAPNIK }
                    else -> TileSourceFactory.MAPNIK
                }
                mapView?.setTileSource(source)
                mapView?.invalidate()
            }

            // Zoom to place when filter changes
            LaunchedEffect(selectedPlace, filteredMedia) {
                val map = mapView ?: return@LaunchedEffect
                if (selectedPlace != null && filteredMedia.isNotEmpty()) {
                    val lats = filteredMedia.mapNotNull { it.latitude }
                    val lons = filteredMedia.mapNotNull { it.longitude }
                    if (lats.isNotEmpty() && lons.isNotEmpty()) {
                        val padding = 0.005
                        map.post {
                            map.zoomToBoundingBox(
                                BoundingBox(lats.max() + padding, lons.max() + padding, lats.min() - padding, lons.min() - padding),
                                true, 80
                            )
                        }
                    }
                } else if (selectedPlace == null && displayClusters.isNotEmpty()) {
                    val points = displayClusters.map { it.center }
                    val padding = 0.02
                    map.post {
                        map.zoomToBoundingBox(
                            BoundingBox(
                                points.maxOf { it.latitude } + padding, points.maxOf { it.longitude } + padding,
                                points.minOf { it.latitude } - padding, points.minOf { it.longitude } - padding
                            ),
                            true, 80
                        )
                    }
                }
            }

            // Clear focused item from marker after 3s
            LaunchedEffect(focusedItemFromMarker) {
                if (focusedItemFromMarker != null) {
                    delay(3000)
                    focusedItemFromMarker = null
                }
            }

            // Search back handler
            androidx.activity.compose.BackHandler(enabled = isSearchActive) {
                searchQuery = ""
                isSearchActive = false
            }

            // ── BottomSheetScaffold Layout ──
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
            )

            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = 88.dp,
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                sheetContentColor = MaterialTheme.colorScheme.onSurface,
                sheetShape = ShapeEdgeTop,
                sheetDragHandle = {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(com.inferno.gallery.ui.theme.ShapeFull)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        )
                    }
                },
                sheetContent = {
                    // Sheet header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Places",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = com.inferno.gallery.ui.theme.ShapeFull,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            val placesCount = placesClusters.size
                            Text(
                                text = "${filteredMedia.size} photos" + if (placesCount > 0) " · $placesCount places" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Place filter chips
                    if (placesClusters.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            item {
                                androidx.compose.material3.FilterChip(
                                    selected = selectedPlace == null,
                                    onClick = { selectedPlace = null },
                                    label = { Text("All (${geotaggedMedia.size})", style = MaterialTheme.typography.labelMedium) },
                                    shape = com.inferno.gallery.ui.theme.ShapeFull
                                )
                            }
                            items(placesClusters.size) { index ->
                                val place = placesClusters[index]
                                // Skip coordinate-only place names
                                if (!place.bucketName.startsWith("Location (")) {
                                    androidx.compose.material3.FilterChip(
                                        selected = selectedPlace == place.bucketName,
                                        onClick = {
                                            selectedPlace = if (selectedPlace == place.bucketName) null else place.bucketName
                                        },
                                        label = { Text("${place.bucketName} (${place.itemCount})", style = MaterialTheme.typography.labelMedium) },
                                        shape = com.inferno.gallery.ui.theme.ShapeFull
                                    )
                                }
                            }
                        }
                    }

                    // Photo grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = gridState,
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    ) {
                        items(
                            count = groupedMedia.size,
                            key = { index ->
                                val item = groupedMedia[index]
                                when (item) {
                                    is GalleryListItem.Header -> "header_${item.id}"
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
                                    // Date + Place header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = listItem.title,
                                            style = com.inferno.gallery.ui.theme.TimelineDateHeaderStyle,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        // Find the place for the first item in this date group
                                        val nextItemIndex = groupedMedia.indexOf(listItem) + 1
                                        val firstItemInGroup = if (nextItemIndex in groupedMedia.indices) {
                                            (groupedMedia[nextItemIndex] as? GalleryListItem.Item)?.galleryItem
                                        } else null
                                        val placeName = firstItemInGroup?.let { placeNameByMediaId[it.id.toLong()] }
                                        if (placeName != null && !placeName.startsWith("Location (")) {
                                            Text(
                                                text = "📍 $placeName",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                is GalleryListItem.Item -> {
                                    val item = listItem.galleryItem
                                    val isHighlighted = item.id == focusedItemFromMarker?.id
                                    Surface(
                                        shape = com.inferno.gallery.ui.theme.ShapeMedium,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(com.inferno.gallery.ui.theme.ShapeMedium)
                                            .then(
                                                if (isHighlighted) Modifier.border(
                                                    2.dp, MaterialTheme.colorScheme.primaryContainer,
                                                    com.inferno.gallery.ui.theme.ShapeMedium
                                                ) else Modifier
                                            )
                                            .haptickClickable {
                                                galleryViewModel.setInitialDetailItem(item, "geotagged")
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
            ) { innerPadding ->
                // ── Map Content (behind the sheet) ──
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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

                            // Render cluster & single-photo markers with circular thumbnails
                            displayClusters.forEach { cluster ->
                                val count = cluster.items.size
                                val marker = Marker(map)
                                marker.position = cluster.center
                                marker.title = "${count} photo${if (count > 1) "s" else ""}"

                                val density = context.resources.displayMetrics.density
                                val firstItem = cluster.items.firstOrNull()
                                val bitmap = firstItem?.let { clusterBitmaps[it.id] }

                                if (count == 1) {
                                    val singleBitmap = if (bitmap != null) {
                                        createCircularMarkerBitmap(
                                            bitmap = bitmap,
                                            sizePx = (42 * density).toInt()
                                        )
                                    } else {
                                        createDefaultMarkerBitmap(
                                            sizePx = (36 * density).toInt(),
                                            fillColor = tertiaryColor.toArgb()
                                        )
                                    }
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    marker.icon = BitmapDrawable(context.resources, singleBitmap)
                                } else {
                                    val sizeDp = (46 + minOf(count * 2, 18)).coerceAtMost(64)
                                    val sizePx = (sizeDp * density).toInt()
                                    val placeName = firstItem?.let { placeNameByMediaId[it.id.toLong()] }
                                        ?.takeIf { !it.startsWith("Location (") }
                                    val (markerBitmap, anchorY) = createClusterMarkerBitmap(
                                        bitmap = bitmap,
                                        count = count,
                                        sizePx = sizePx,
                                        density = density,
                                        badgeColor = tertiaryColor.toArgb(),
                                        badgeTextColor = onTertiaryColor.toArgb(),
                                        labelText = placeName
                                    )
                                    marker.setAnchor(Marker.ANCHOR_CENTER, anchorY)
                                    marker.icon = BitmapDrawable(context.resources, markerBitmap)
                                }

                                marker.setOnMarkerClickListener { _, _ ->
                                    val clusterItems = cluster.items
                                    if (clusterItems.size <= 1) {
                                        map.controller.animateTo(cluster.center)
                                        map.controller.setZoom(17.0)
                                    } else {
                                        val lats = clusterItems.mapNotNull { it.latitude }
                                        val lons = clusterItems.mapNotNull { it.longitude }
                                        if (lats.isNotEmpty() && lons.isNotEmpty()) {
                                            val padding = 0.003
                                            val boundingBox = BoundingBox(
                                                lats.max() + padding, lons.max() + padding,
                                                lats.min() - padding, lons.min() - padding
                                            )
                                            map.post { map.zoomToBoundingBox(boundingBox, true, 60) }
                                        }
                                    }
                                    // Bi-directional sync: marker → grid scroll
                                    val targetItem = clusterItems.firstOrNull()
                                    focusedItemFromMarker = targetItem
                                    val targetId = targetItem?.id
                                    val idx = groupedMedia.indexOfFirst {
                                        (it as? GalleryListItem.Item)?.galleryItem?.id == targetId
                                    }
                                    if (idx >= 0) {
                                        coroutineScope.launch { gridState.animateScrollToItem(idx) }
                                    }
                                    true
                                }
                                map.overlays.add(marker)
                            }

                            // Keep focused marker on top
                            focusedMarker?.let { map.overlays.add(it) }

                            // Initial zoom to fit clusters
                            if (displayClusters.isNotEmpty() && focusedMarker == null) {
                                try {
                                    val points = displayClusters.map { it.center }
                                    val minLat = points.minOf { it.latitude }
                                    val maxLat = points.maxOf { it.latitude }
                                    val minLon = points.minOf { it.longitude }
                                    val maxLon = points.maxOf { it.longitude }
                                    val padding = 0.02
                                    map.post {
                                        map.zoomToBoundingBox(
                                            BoundingBox(maxLat + padding, maxLon + padding, minLat - padding, minLon - padding),
                                            true, 80
                                        )
                                    }
                                } catch (_: Exception) {}
                            }

                            map.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // ── Translucent Top Bar with Search ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                    )
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = onBackClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                        }

                        // Search bar
                        Surface(
                            shape = com.inferno.gallery.ui.theme.ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f).height(40.dp)
                                .clickable { isSearchActive = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.ic_ms_search),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                if (isSearchActive) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = ""; isSearchActive = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                ImageVector.vectorResource(R.drawable.ic_ms_close),
                                                contentDescription = "Clear",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        "Search places & photos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ── Preview Card (shown when focused item exists) ──
                    val previewItem = focusedItemFromMarker ?: firstVisibleImage
                    if (previewItem != null) {
                        val previewPlace = placeNameByMediaId[previewItem.id.toLong()]
                            ?.takeIf { !it.startsWith("Location (") }
                        Surface(
                            shape = com.inferno.gallery.ui.theme.ShapeMedium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, end = 72.dp, bottom = 12.dp)
                                .fillMaxWidth()
                                .clickable {
                                    galleryViewModel.setInitialDetailItem(previewItem, "geotagged")
                                    onPhotoClick(previewItem.id, "geotagged", null)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Surface(
                                    shape = com.inferno.gallery.ui.theme.ShapeMedium,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(previewItem.resolvedUri)
                                            .size(128)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = previewItem.name,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    val dateStr = formatGroupHeader(previewItem.dateAdded)
                                    Text(
                                        text = if (previewPlace != null) "📍 $previewPlace · $dateStr" else dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // ── FAB Column (map style + re-center) ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Map style toggle
                        SmallFloatingActionButton(
                            onClick = { mapStyle = (mapStyle + 1) % 3 },
                            shape = com.inferno.gallery.ui.theme.ShapeMedium,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(
                                ImageVector.vectorResource(
                                    when (mapStyle) {
                                        1 -> R.drawable.ic_ms_terrain
                                        2 -> R.drawable.ic_ms_dark_mode
                                        else -> R.drawable.ic_ms_map
                                    }
                                ),
                                contentDescription = "Map style",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Re-center FAB
                        FloatingActionButton(
                            onClick = {
                                mapView?.let { map ->
                                    if (displayClusters.isNotEmpty()) {
                                        val points = displayClusters.map { it.center }
                                        val padding = 0.02
                                        map.zoomToBoundingBox(
                                            BoundingBox(
                                                points.maxOf { it.latitude } + padding,
                                                points.maxOf { it.longitude } + padding,
                                                points.minOf { it.latitude } - padding,
                                                points.minOf { it.longitude } - padding
                                            ),
                                            true, 80
                                        )
                                    }
                                }
                            },
                            shape = com.inferno.gallery.ui.theme.ShapeFull,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_my_location), contentDescription = "Re-center", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Circular Marker Rendering (Clean, Outline-Free) ──

private fun drawCircularPhoto(canvas: Canvas, bitmap: Bitmap, cx: Float, cy: Float, radius: Float) {
    val scale = (radius * 2f) / minOf(bitmap.width, bitmap.height).toFloat()
    val matrix = android.graphics.Matrix().apply {
        setScale(scale, scale)
        postTranslate(cx - bitmap.width * scale / 2f, cy - bitmap.height * scale / 2f)
    }
    val shader = android.graphics.BitmapShader(
        bitmap,
        android.graphics.Shader.TileMode.CLAMP,
        android.graphics.Shader.TileMode.CLAMP
    ).apply { setLocalMatrix(matrix) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
    canvas.drawCircle(cx, cy, radius, paint)
}

private fun createCircularMarkerBitmap(
    bitmap: Bitmap,
    sizePx: Int
): Bitmap {
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val r = sizePx / 2f
    drawCircularPhoto(canvas, bitmap, r, r, r)
    return output
}

private fun createClusterMarkerBitmap(
    bitmap: Bitmap?,
    count: Int,
    sizePx: Int,
    density: Float,
    badgeColor: Int,
    badgeTextColor: Int,
    labelText: String? = null
): Pair<Bitmap, Float> {
    val badgeHeight = 20f * density
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = badgeTextColor
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val countStr = count.toString()
    val badgeWidth = maxOf(badgeHeight, badgeTextPaint.measureText(countStr) + 10f * density)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val labelPillHeight = if (labelText != null) 20f * density else 0f
    val labelGap = if (labelText != null) 4f * density else 0f
    val labelPillWidth = if (labelText != null) labelPaint.measureText(labelText) + 14f * density else 0f

    val topPad = ceil(badgeHeight * 0.35f + 1f).toInt()
    val sidePad = ceil(badgeWidth * 0.55f).toInt()
    val totalWidth = maxOf(sizePx + sidePad * 2, ceil(labelPillWidth + 4f).toInt())
    val bottomExtra = ceil(labelGap + labelPillHeight + 2f).toInt()
    val totalHeight = topPad + sizePx + bottomExtra

    val output = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val cx = totalWidth / 2f
    val cy = topPad + sizePx / 2f
    val radius = sizePx / 2f

    if (bitmap != null) {
        drawCircularPhoto(canvas, bitmap, cx, cy, radius)
        val angleRad = Math.toRadians(-45.0)
        val badgeCx = (cx + radius * cos(angleRad).toFloat()).coerceAtMost(totalWidth - badgeWidth / 2f - 1f)
        val badgeCy = (cy + radius * sin(angleRad).toFloat()).coerceAtLeast(badgeHeight / 2f + 1f)
        val badgeRect = android.graphics.RectF(
            badgeCx - badgeWidth / 2f,
            badgeCy - badgeHeight / 2f,
            badgeCx + badgeWidth / 2f,
            badgeCy + badgeHeight / 2f
        )
        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, badgeFill)
        val badgeTextY = badgeCy - (badgeTextPaint.ascent() + badgeTextPaint.descent()) / 2f
        canvas.drawText(countStr, badgeCx, badgeTextY, badgeTextPaint)
    } else {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor; style = Paint.Style.FILL }
        canvas.drawCircle(cx, cy, radius, fillPaint)
        val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeTextColor
            textSize = sizePx * 0.36f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val textY = cy - (centerTextPaint.ascent() + centerTextPaint.descent()) / 2f
        canvas.drawText(countStr, cx, textY, centerTextPaint)
    }

    if (labelText != null) {
        val pillTop = cy + radius + labelGap
        val pillRect = android.graphics.RectF(
            cx - labelPillWidth / 2f,
            pillTop,
            cx + labelPillWidth / 2f,
            pillTop + labelPillHeight
        )
        val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAA000000.toInt(); style = Paint.Style.FILL }
        canvas.drawRoundRect(pillRect, labelPillHeight / 2f, labelPillHeight / 2f, labelBg)
        val labelTextY = pillRect.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2f
        canvas.drawText(labelText, cx, labelTextY, labelPaint)
    }

    return output to (cy / totalHeight)
}

private fun createDefaultMarkerBitmap(
    sizePx: Int,
    fillColor: Int
): Bitmap {
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val r = sizePx / 2f
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor; style = Paint.Style.FILL }
    canvas.drawCircle(r, r, r, fillPaint)
    return output
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

// (Cluster marker rendering now uses createClusterMarkerBitmap above)
