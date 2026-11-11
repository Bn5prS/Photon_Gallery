@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
package com.inferno.gallery.ui.components

import androidx.compose.ui.res.stringResource

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.Tag
import java.io.BufferedInputStream
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import com.inferno.gallery.data.model.ExifStripAction
import com.inferno.gallery.ui.utils.ExifStripperHelper
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.TechnicalParamLabelStyle
import com.inferno.gallery.ui.theme.TechnicalParamValueStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.graphics.Bitmap
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import coil3.request.crossfade
import androidx.compose.ui.layout.ContentScale
import com.inferno.gallery.data.db.DatabaseProvider
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R


// ── Detailed EXIF Data Model ──────────────────────────────────────────────────

data class DeepExifTag(
    val category: String,
    val tag: String,
    val value: String
)

data class DetailedExifData(
    // File & Format
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String?,
    val formatDescription: String,
    val dateCreated: Long?,
    val dateModified: Long?,
    val storagePath: String,
    val albumName: String,

    // Resolution & Geometry
    val width: Int,
    val height: Int,
    val resolutionString: String,
    val megapixelsString: String,
    val aspectRatioString: String,
    val colorSpace: String?,
    val orientation: String?,

    // Camera & Optics
    val cameraMaker: String?,
    val cameraModel: String?,
    val lensModel: String?,
    val software: String?,
    val aperture: String?,
    val iso: String?,
    val shutterSpeed: String?,
    val focalLength: String?,
    val focalLength35mm: String?,
    val flash: String?,
    val whiteBalance: String?,
    val exposureMode: String?,
    val meteringMode: String?,

    // GPS & Location
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val formattedCoordinates: String?,

    // Video Specific (if video)
    val isVideo: Boolean,
    val durationMs: Long?,
    val bitrate: Long?,

    // All extracted raw tags
    val allTags: List<DeepExifTag>
)

private fun mapDirectoryCategory(rawName: String): String {
    val lower = rawName.lowercase(Locale.ROOT)
    return when {
        lower.contains("subifd") -> "Exif SubIFD (Optics & Shot)"
        lower.contains("ifd0") -> "Exif IFD0 (Device & Hardware)"
        lower.contains("gps") -> "GPS & Location"
        lower.contains("makernote") -> {
            val brand = rawName.replace("Makernote", "", ignoreCase = true)
                .replace("Maker Note", "", ignoreCase = true).trim()
            if (brand.isNotBlank()) "MakerNote ($brand)" else "MakerNote"
        }
        lower.contains("xmp") -> "XMP & Computational Photo"
        lower.contains("icc") -> "Color & ICC Profile"
        lower.contains("iptc") -> "IPTC & Editorial"
        lower.contains("photoshop") -> "Photoshop & Adjustments"
        lower.contains("mp4") || lower.contains("quicktime") -> "Video & Audio Tracks"
        lower.contains("thumbnail") -> "Thumbnail Metadata"
        lower.contains("file type") || lower.contains("file") -> "File & Format"
        else -> rawName.trim()
    }
}

// ── Extraction Helper ─────────────────────────────────────────────────────────

suspend fun extractDetailedExif(context: Context, item: GalleryItem): DetailedExifData = withContext(Dispatchers.IO) {
    val uri = item.uri
    val allTagsList = mutableListOf<DeepExifTag>()
    val seenTagKeys = mutableSetOf<String>()

    fun addTag(category: String, tag: String, value: String) {
        val cleanCat = category.trim()
        val cleanTag = tag.trim()
        val cleanVal = value.trim()
        if (cleanVal.isBlank()) return
        val key = "${cleanCat.lowercase(Locale.ROOT)}_${cleanTag.lowercase(Locale.ROOT)}"
        if (seenTagKeys.add(key)) {
            allTagsList.add(DeepExifTag(cleanCat, cleanTag, cleanVal))
        }
    }

    var make: String? = null
    var model: String? = null
    var lensModel: String? = null
    var software: String? = null
    var aperture: String? = null
    var iso: String? = null
    var shutterSpeed: String? = null
    var focalLength: String? = null
    var focalLength35mm: String? = null
    var flash: String? = null
    var whiteBalance: String? = null
    var exposureMode: String? = null
    var meteringMode: String? = null
    var colorSpaceStr: String? = null
    var orientationStr: String? = null
    var lat: Double? = null
    var lng: Double? = null
    var alt: Double? = null
    var dateCreatedParsed: Long? = null
    var imgW = 0
    var imgH = 0

    // 1. Surgical deep metadata extraction via Drew Noakes metadata-extractor
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedInputStream(stream).use { buffered ->
                val metadata = ImageMetadataReader.readMetadata(buffered)
                for (directory in metadata.directories) {
                    val category = mapDirectoryCategory(directory.name)
                    for (tag in directory.tags) {
                        val tagName = tag.tagName?.trim() ?: "Tag 0x${Integer.toHexString(tag.tagType)}"
                        val desc = tag.description?.trim()
                        if (!desc.isNullOrBlank()) {
                            addTag(category, tagName, desc)

                            // Pick up high-value metadata values directly with rich decoded descriptions
                            when {
                                category.contains("SubIFD") && tagName.contains("Flash", ignoreCase = true) && flash == null -> flash = desc
                                category.contains("SubIFD") && tagName.contains("White Balance", ignoreCase = true) && whiteBalance == null -> whiteBalance = desc
                                category.contains("SubIFD") && tagName.contains("Exposure Mode", ignoreCase = true) && exposureMode == null -> exposureMode = desc
                                category.contains("SubIFD") && tagName.contains("Metering Mode", ignoreCase = true) && meteringMode == null -> meteringMode = desc
                                category.contains("SubIFD") && (tagName.contains("Lens Model", ignoreCase = true) || tagName.contains("Lens Specification", ignoreCase = true)) && lensModel == null -> lensModel = desc
                                category.contains("Color") && tagName.contains("Profile Description", ignoreCase = true) && colorSpaceStr == null -> colorSpaceStr = desc
                            }
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}

    // 2. Video track, codec & color standard extraction via MediaMetadataRetriever (if video)
    if (item.isVideo) {
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                val cat = "Video & Audio Tracks"

                fun addVideoTag(tag: String, keyCode: Int, transform: ((String) -> String)? = null) {
                    retriever.extractMetadata(keyCode)?.trim()?.takeIf { it.isNotBlank() }?.let { v ->
                        val display = transform?.invoke(v) ?: v
                        addTag(cat, tag, display)
                    }
                }

                addVideoTag("Duration", MediaMetadataRetriever.METADATA_KEY_DURATION) { ms ->
                    val sec = (ms.toLongOrNull() ?: 0L) / 1000
                    String.format(Locale.US, "%02d:%02d (%s ms)", sec / 60, sec % 60, ms)
                }
                addVideoTag("Video Resolution", MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) { w ->
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "?"
                    "$w × $h px"
                }
                addVideoTag("Video Bitrate", MediaMetadataRetriever.METADATA_KEY_BITRATE) { bps ->
                    val kbps = (bps.toLongOrNull() ?: 0L) / 1000
                    "$kbps kbps"
                }
                addVideoTag("Video Rotation", MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) { "${it}°" }
                addVideoTag("MIME Type", MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                addVideoTag("Capture Frame Rate", MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE) { "${it} fps" }
                addVideoTag("Creation Date", MediaMetadataRetriever.METADATA_KEY_DATE)
                addVideoTag("Location", MediaMetadataRetriever.METADATA_KEY_LOCATION)
                addVideoTag("Track Count", MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    addVideoTag("Color Standard", MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)
                    addVideoTag("Color Transfer", MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)
                    addVideoTag("Color Range", MediaMetadataRetriever.METADATA_KEY_COLOR_RANGE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    addVideoTag("Frame Count", MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                }
            }
        } catch (_: Exception) {}
    }

    // 3. AndroidX ExifInterface for standard fields & GPS coordinates
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)

            if (make == null) make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
            if (model == null) model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
            if (lensModel == null) lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()
            if (software == null) software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim()

            if (aperture == null) aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
            if (iso == null) iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }

            val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            if (exposureTime > 0.0 && shutterSpeed == null) {
                shutterSpeed = when {
                    exposureTime < 1.0 -> "1/${(1.0 / exposureTime).roundToInt()}s"
                    else -> String.format(Locale.US, "%.1fs", exposureTime)
                }
            }

            if (focalLength == null) focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it} mm" }
            if (focalLength35mm == null) focalLength35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let { "${it} mm" }

            val flashVal = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
            if (flashVal != -1 && flash == null) {
                flash = if (flashVal and 1 != 0) "Fired" else "Did not fire"
            }

            val wbVal = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
            if (wbVal != -1 && whiteBalance == null) {
                whiteBalance = if (wbVal == ExifInterface.WHITE_BALANCE_AUTO.toInt()) "Auto" else "Manual"
            }

            val expModeVal = exif.getAttributeInt(ExifInterface.TAG_EXPOSURE_MODE, -1)
            if (expModeVal != -1 && exposureMode == null) {
                exposureMode = when (expModeVal) {
                    ExifInterface.EXPOSURE_MODE_AUTO.toInt() -> "Auto Exposure"
                    ExifInterface.EXPOSURE_MODE_MANUAL.toInt() -> "Manual Exposure"
                    else -> "Auto Bracket"
                }
            }

            val meterVal = exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1)
            if (meterVal != -1 && meteringMode == null) {
                meteringMode = when (meterVal) {
                    1 -> "Average"
                    2 -> "Center-weighted Average"
                    3 -> "Spot"
                    4 -> "Multi-spot"
                    5 -> "Pattern / Multi-segment"
                    6 -> "Partial"
                    else -> "Other"
                }
            }

            val csVal = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)
            if (csVal != -1 && colorSpaceStr == null) {
                colorSpaceStr = if (csVal == 1) "sRGB" else "Adobe RGB / Wide Gamut"
            }

            val orientVal = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            orientationStr = when (orientVal) {
                ExifInterface.ORIENTATION_NORMAL -> "0° (Normal)"
                ExifInterface.ORIENTATION_ROTATE_90 -> "90° CW"
                ExifInterface.ORIENTATION_ROTATE_180 -> "180°"
                ExifInterface.ORIENTATION_ROTATE_270 -> "270° CW"
                else -> "Normal"
            }

            if (imgW <= 0) imgW = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            if (imgH <= 0) imgH = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

            val latLong = exif.latLong
            if (latLong != null && latLong.size >= 2) {
                lat = latLong[0]
                lng = latLong[1]
            }
            alt = exif.getAltitude(0.0).takeIf { it != 0.0 }

            val dateOrigStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (dateOrigStr != null) {
                try {
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    dateCreatedParsed = sdf.parse(dateOrigStr)?.time
                } catch (_: Exception) {}
            }
        }
    } catch (_: Exception) {}

    // Fallback image dimensions from BitmapFactory or MediaStore if EXIF was empty
    if (imgW <= 0 || imgH <= 0) {
        try {
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, opts)
            }
            imgW = opts.outWidth
            imgH = opts.outHeight
        } catch (_: Exception) {}
    }

    val resStr = if (imgW > 0 && imgH > 0) "$imgW × $imgH px" else "Unknown Resolution"
    val mpStr = if (imgW > 0 && imgH > 0) {
        val mp = (imgW.toLong() * imgH.toLong()) / 1_000_000.0
        String.format(Locale.US, "%.1f MP", mp)
    } else ""

    val aspectStr = if (imgW > 0 && imgH > 0) {
        val gcd = computeGcd(imgW, imgH)
        val wRatio = imgW / gcd
        val hRatio = imgH / gcd
        if (wRatio <= 20 && hRatio <= 20) "$wRatio:$hRatio" else String.format(Locale.US, "%.2f:1", imgW.toFloat() / imgH.toFloat())
    } else ""

    val extension = item.name.substringAfterLast(".", "").uppercase(Locale.US)
    val formatDesc = when {
        item.isVideo -> "Video ($extension)"
        extension in listOf("JPG", "JPEG") -> "JPEG Image"
        extension == "PNG" -> "PNG Image"
        extension == "WEBP" -> "WebP Image"
        extension == "HEIC" -> "HEIF / HEIC Image"
        extension in listOf("DNG", "RAW", "CR2", "NEF") -> "RAW Digital Negative"
        extension == "GIF" -> "Animated GIF"
        else -> "$extension File"
    }

    val album = item.path.substringBeforeLast("/", "").substringAfterLast("/", "Gallery")

    val finalLat = lat
    val finalLng = lng
    val formattedCoords = if (finalLat != null && finalLng != null) {
        val latDir = if (finalLat >= 0) "N" else "S"
        val lngDir = if (finalLng >= 0) "E" else "W"
        "${String.format(Locale.US, "%.5f", kotlin.math.abs(finalLat))}° $latDir, ${String.format(Locale.US, "%.5f", kotlin.math.abs(finalLng))}° $lngDir"
    } else null

    DetailedExifData(
        fileName = item.name,
        fileSizeBytes = item.size,
        mimeType = if (item.isVideo) "video/*" else "image/${extension.lowercase()}",
        formatDescription = formatDesc,
        dateCreated = dateCreatedParsed ?: (if (item.dateAdded > 0) item.dateAdded * 1000L else item.dateModified * 1000L),
        dateModified = if (item.dateModified > 0) item.dateModified * 1000L else item.dateAdded * 1000L,
        storagePath = item.path,
        albumName = album,
        width = imgW,
        height = imgH,
        resolutionString = resStr,
        megapixelsString = mpStr,
        aspectRatioString = aspectStr,
        colorSpace = colorSpaceStr,
        orientation = orientationStr,
        cameraMaker = make,
        cameraModel = model,
        lensModel = lensModel,
        software = software,
        aperture = aperture,
        iso = iso,
        shutterSpeed = shutterSpeed,
        focalLength = focalLength,
        focalLength35mm = focalLength35mm,
        flash = flash,
        whiteBalance = whiteBalance,
        exposureMode = exposureMode,
        meteringMode = meteringMode,
        latitude = lat,
        longitude = lng,
        altitude = alt,
        formattedCoordinates = formattedCoords,
        isVideo = item.isVideo,
        durationMs = null,
        bitrate = null,
        allTags = allTagsList
    )
}

private fun computeGcd(a: Int, b: Int): Int {
    var n1 = a
    var n2 = b
    while (n2 != 0) {
        val temp = n2
        n2 = n1 % n2
        n1 = temp
    }
    return if (n1 > 0) n1 else 1
}

// ── ExifDetailsSheet Composable ───────────────────────────────────────────────

@Composable
fun ExifDetailsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    galleryItem: GalleryItem?,
    exifData: DetailedExifData?,
    onPersonClick: ((Long) -> Unit)? = null,
    onStripComplete: ((Uri?, Boolean) -> Unit)? = null
) {
    if (!isOpen || galleryItem == null) return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeepMetadataSheet by remember { mutableStateOf(false) }
    var showStripConfirmDialog by remember { mutableStateOf(false) }
    var isStripping by remember { mutableStateOf(false) }
    var localExifData by remember(exifData) { mutableStateOf(exifData) }

    val settingsRepo = remember { com.inferno.gallery.data.SettingsRepository.getInstance(context) }
    val stripActionPreference by settingsRepo.exifStripActionFlow.collectAsState(initial = ExifStripAction.ASK_EVERY_TIME)

    val db = remember { DatabaseProvider.getDatabase(context) }
    val resolvedMediaId by produceState<Long?>(initialValue = galleryItem.id.toLongOrNull(), galleryItem) {
        val directId = galleryItem.id.toLongOrNull()
        if (directId != null && directId > 0) {
            value = directId
        } else {
            value = withContext(Dispatchers.IO) {
                db.mediaDao().getMediaByUri(galleryItem.uri.toString())?.id
                    ?: db.mediaDao().getMediaByPath(galleryItem.path)?.id
            }
        }
    }
    val peopleInPhoto by remember(resolvedMediaId) {
        if (resolvedMediaId != null) db.faceDao().observePeopleForMedia(resolvedMediaId!!)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    LaunchedEffect(resolvedMediaId, galleryItem.uri, peopleInPhoto.isEmpty()) {
        val mid = resolvedMediaId ?: return@LaunchedEffect
        if (galleryItem.isVideo) return@LaunchedEffect
        if (peopleInPhoto.isNotEmpty()) return@LaunchedEffect
        if (com.inferno.gallery.data.ai.FaceScanHelper.scannedSessionIds.contains(mid)) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val faceEnabled = settingsRepo.faceRecognitionEnabledFlow.first()
            val engine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(context)
            if (faceEnabled && engine.isModelDownloaded()) {
                com.inferno.gallery.data.ai.FaceScanHelper.scannedSessionIds.add(mid)
                val clusteringManager = com.inferno.gallery.data.ai.FaceClusteringManager.getInstance(context)
                com.inferno.gallery.data.ai.FaceScanHelper.scanSingleMedia(
                    context = context,
                    mediaId = mid,
                    filePath = galleryItem.path,
                    uri = galleryItem.uri,
                    database = db,
                    engine = engine,
                    clusteringManager = clusteringManager
                )
            }
        }
    }

    val onTriggerStrip = {
        when (stripActionPreference) {
            ExifStripAction.ASK_EVERY_TIME -> {
                showStripConfirmDialog = true
            }
            ExifStripAction.SAVE_AS_COPY -> {
                coroutineScope.launch {
                    isStripping = true
                    val result = withContext(Dispatchers.IO) {
                        ExifStripperHelper.stripSaveCopy(context, galleryItem)
                    }
                    isStripping = false
                    when (result) {
                        is ExifStripperHelper.StripResult.Success -> {
                            Toast.makeText(context, context.getString(R.string.exif_strip_success_copy), Toast.LENGTH_SHORT).show()
                            onStripComplete?.invoke(result.newUri, true)
                        }
                        is ExifStripperHelper.StripResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            ExifStripAction.OVERWRITE_ORIGINAL -> {
                coroutineScope.launch {
                    isStripping = true
                    val result = withContext(Dispatchers.IO) {
                        ExifStripperHelper.stripOverwrite(context, galleryItem)
                    }
                    isStripping = false
                    when (result) {
                        is ExifStripperHelper.StripResult.Success -> {
                            Toast.makeText(context, context.getString(R.string.exif_strip_success_overwrite), Toast.LENGTH_SHORT).show()
                            val refreshed = withContext(Dispatchers.IO) {
                                extractDetailedExif(context, galleryItem)
                            }
                            localExifData = refreshed
                            onStripComplete?.invoke(galleryItem.uri, false)
                        }
                        is ExifStripperHelper.StripResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    if (showStripConfirmDialog) {
        StripExifConfirmationDialog(
            onDismiss = { showStripConfirmDialog = false },
            onConfirm = { action, rememberChoice ->
                showStripConfirmDialog = false
                coroutineScope.launch {
                    if (rememberChoice) {
                        settingsRepo.setExifStripAction(action)
                    }
                    isStripping = true
                    val result = withContext(Dispatchers.IO) {
                        if (action == ExifStripAction.SAVE_AS_COPY) {
                            ExifStripperHelper.stripSaveCopy(context, galleryItem)
                        } else {
                            ExifStripperHelper.stripOverwrite(context, galleryItem)
                        }
                    }
                    isStripping = false
                    when (result) {
                        is ExifStripperHelper.StripResult.Success -> {
                            val msg = if (result.isCopy) context.getString(R.string.exif_strip_success_copy)
                                      else context.getString(R.string.exif_strip_success_overwrite)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (!result.isCopy) {
                                val refreshed = withContext(Dispatchers.IO) {
                                    extractDetailedExif(context, galleryItem)
                                }
                                localExifData = refreshed
                            }
                            onStripComplete?.invoke(result.newUri ?: galleryItem.uri, result.isCopy)
                        }
                        is ExifStripperHelper.StripResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = ShapeEdgeTop,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Info & Metadata",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = galleryItem.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_close), contentDescription = "Close")
                    }
                }
            }

            // ── People in this photo ──────────────────────────────────────────
            if (peopleInPhoto.isNotEmpty()) {
                item {
                    Surface(
                        shape = ShapeLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (peopleInPhoto.size == 1) stringResource(R.string.exif_people_in_photo) else stringResource(R.string.exif_people_in_photo_plural),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = "${peopleInPhoto.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(peopleInPhoto, key = { it.faceId }) { person ->
                                    val hasTarget = person.personId != null
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(72.dp)
                                            .clip(ShapeMedium)
                                            .clickable(enabled = hasTarget) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onDismiss()
                                                person.personId?.let { onPersonClick?.invoke(it) }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(56.dp),
                                            shape = ShapeFull,
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = if (hasTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                        else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            val cropPath = person.cropPath
                                            if (!cropPath.isNullOrBlank() && File(cropPath).exists()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(File(cropPath))
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = person.name ?: "Person",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = person.name ?: "Unnamed",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (person.name != null) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (person.name != null) MaterialTheme.colorScheme.onSurface
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        if (person.personId != null) {
                                            Text(
                                                text = "${person.faceCount} ${if (person.faceCount == 1) "photo" else "photos"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Group 1: General Specs Card ──────────────────────────────────
            item {
                Surface(
                    shape = ShapeLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_description),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.exif_general_specs),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Dimensions + MP + Aspect Ratio
                        val resStr = localExifData?.resolutionString
                        val mpStr = localExifData?.megapixelsString
                        val aspectStr = localExifData?.aspectRatioString
                        val dimValue = if (!resStr.isNullOrBlank()) {
                            val extra = listOfNotNull(
                                mpStr?.takeIf { it.isNotBlank() },
                                aspectStr?.takeIf { it.isNotBlank() }
                            ).joinToString(", ")
                            if (extra.isNotEmpty()) "$resStr ($extra)" else resStr
                        } else if ((localExifData?.width ?: 0) > 0 && (localExifData?.height ?: 0) > 0) {
                            "${localExifData!!.width} × ${localExifData!!.height}"
                        } else "Unknown"

                        DetailKeyValueRowTight(
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_aspect_ratio),
                            label = stringResource(R.string.exif_dimensions),
                            value = dimValue
                        )

                        // File Size
                        DetailKeyValueRowTight(
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_description),
                            label = stringResource(R.string.exif_file_size),
                            value = "${formatDetailedSize(galleryItem.size)} (${String.format(Locale.US, "%,d", galleryItem.size)} B)"
                        )

                        // Dates
                        val createdDate = localExifData?.dateCreated
                        val modifiedDate = localExifData?.dateModified ?: (galleryItem.dateModified * 1000L)
                        val isModifiedDifferent = createdDate != null && kotlin.math.abs(modifiedDate - createdDate) > 60_000L

                        if (createdDate != null) {
                            DetailKeyValueRowTight(
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_calendar_month),
                                label = stringResource(R.string.exif_date_taken),
                                value = formatFullTimestamp(createdDate)
                            )
                        }

                        if (isModifiedDifferent || createdDate == null) {
                            DetailKeyValueRowTight(
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_schedule),
                                label = stringResource(R.string.exif_date_modified),
                                value = formatFullTimestamp(modifiedDate)
                            )
                        }

                        // Color Space & Format
                        val colorSpace = localExifData?.colorSpace ?: "sRGB"
                        val formatDesc = localExifData?.formatDescription ?: (if (galleryItem.isVideo) "Video" else "Image")
                        DetailKeyValueRowTight(
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_palette),
                            label = stringResource(R.string.exif_color_space),
                            value = "$colorSpace • $formatDesc"
                        )
                    }
                }
            }

            // ── Group 2: Camera & Optics Card ────────────────────────────────
            val hasCamera = localExifData?.cameraModel != null ||
                    localExifData?.cameraMaker != null ||
                    localExifData?.aperture != null ||
                    localExifData?.iso != null ||
                    localExifData?.shutterSpeed != null

            if (hasCamera) {
                item {
                    Surface(
                        shape = ShapeLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.exif_camera_optics),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            // Camera Model
                            val cameraName = listOfNotNull(localExifData?.cameraMaker, localExifData?.cameraModel)
                                .distinct()
                                .joinToString(" ")
                            if (cameraName.isNotBlank()) {
                                DetailKeyValueRowTight(
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                                    label = "Camera",
                                    value = cameraName
                                )
                            }

                            // Lens
                            localExifData?.lensModel?.let { lens ->
                                DetailKeyValueRowTight(
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_center_focus_strong),
                                    label = "Lens",
                                    value = lens
                                )
                            }

                            // Exposure parameters (f-stop, shutter, ISO combined)
                            val exposureParts = listOfNotNull(
                                localExifData?.aperture,
                                localExifData?.shutterSpeed,
                                localExifData?.iso?.let { "ISO $it" }
                            )
                            if (exposureParts.isNotEmpty()) {
                                DetailKeyValueRowTight(
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_tune),
                                    label = stringResource(R.string.exif_exposure),
                                    value = exposureParts.joinToString(" • ")
                                )
                            }

                            // Focal length
                            val focal = localExifData?.focalLength
                            val focal35 = localExifData?.focalLength35mm
                            val focalStr = if (focal != null && focal35 != null) {
                                "$focal ($focal35 equiv.)"
                            } else focal ?: focal35
                            if (focalStr != null) {
                                DetailKeyValueRowTight(
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_straighten),
                                    label = stringResource(R.string.exif_focal_length),
                                    value = focalStr
                                )
                            }

                            // Flash
                            localExifData?.flash?.let { fl ->
                                DetailKeyValueRowTight(
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_bolt),
                                    label = stringResource(R.string.exif_flash),
                                    value = fl
                                )
                            }

                            // White Balance
                            localExifData?.whiteBalance?.let { wb ->
                                DetailKeyValueRowTight(
                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_wb_twilight),
                                    label = stringResource(R.string.exif_white_balance),
                                    value = wb
                                )
                            }
                        }
                    }
                }
            }

            // ── Group 3: Storage Location Card (Replaces Maps) ───────────────
            item {
                Surface(
                    shape = ShapeLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_folder),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.exif_storage_location),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        SelectionContainer {
                            Text(
                                text = galleryItem.path,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("File Path", galleryItem.path))
                                Toast.makeText(context, context.getString(R.string.exif_path_copied), Toast.LENGTH_SHORT).show()
                            },
                            shape = ShapeFull,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_content_copy),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.exif_copy_path),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // ── Dual Action Buttons Row ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isImageOnly = !galleryItem.isVideo

                    if (isImageOnly) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTriggerStrip()
                            },
                            enabled = !isStripping,
                            shape = ShapeFull,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            if (isStripping) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_cleaning_services),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.exif_strip_button),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeepMetadataSheet = true
                        },
                        shape = ShapeFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(if (isImageOnly) 1f else 2f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_code),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.exif_deep_button)} (${localExifData?.allTags?.size ?: 0})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // ── Deep Metadata Inspector Modal ─────────────────────────────────────────
    if (showDeepMetadataSheet && localExifData != null) {
        DeepMetadataInspectorSheet(
            allTags = localExifData!!.allTags,
            onDismiss = { showDeepMetadataSheet = false }
        )
    }
}

// ── Strip EXIF Confirmation Dialog ───────────────────────────────────────────

@Composable
fun StripExifConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: (action: ExifStripAction, rememberChoice: Boolean) -> Unit
) {
    var selectedOption by remember { mutableStateOf(ExifStripAction.SAVE_AS_COPY) }
    var rememberChoice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_exif_strip_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.dialog_exif_strip_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Save as copy
                Surface(
                    shape = ShapeMedium,
                    color = if (selectedOption == ExifStripAction.SAVE_AS_COPY)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeMedium)
                        .clickable { selectedOption = ExifStripAction.SAVE_AS_COPY }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == ExifStripAction.SAVE_AS_COPY,
                            onClick = { selectedOption = ExifStripAction.SAVE_AS_COPY }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.dialog_exif_strip_option_copy),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.dialog_exif_strip_option_copy_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: Overwrite original
                Surface(
                    shape = ShapeMedium,
                    color = if (selectedOption == ExifStripAction.OVERWRITE_ORIGINAL)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeMedium)
                        .clickable { selectedOption = ExifStripAction.OVERWRITE_ORIGINAL }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == ExifStripAction.OVERWRITE_ORIGINAL,
                            onClick = { selectedOption = ExifStripAction.OVERWRITE_ORIGINAL }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.dialog_exif_strip_option_overwrite),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.dialog_exif_strip_option_overwrite_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Checkbox: Remember my choice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeSmall)
                        .clickable { rememberChoice = !rememberChoice }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dialog_exif_strip_remember),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedOption, rememberChoice) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == ExifStripAction.OVERWRITE_ORIGINAL)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (selectedOption == ExifStripAction.OVERWRITE_ORIGINAL) "Overwrite" else "Save Copy",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

// ── Deep Metadata Inspector Modal Sheet (Approach A) ─────────────────────────

@Composable
private fun DeepMetadataInspectorSheet(
    allTags: List<DeepExifTag>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(allTags) {
        allTags.map { it.category }.distinct()
    }

    val filteredTags = remember(searchQuery, selectedCategory, allTags) {
        allTags.filter { item ->
            (selectedCategory == null || item.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
             item.tag.contains(searchQuery, ignoreCase = true) ||
             item.value.contains(searchQuery, ignoreCase = true) ||
             item.category.contains(searchQuery, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = ShapeEdgeTop,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header with Copy All
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.exif_deep_metadata_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${filteredTags.size} tags available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val formattedDump = filteredTags.joinToString("\n") { "[${it.category}] ${it.tag} = ${it.value}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("EXIF Dump", formattedDump))
                        Toast.makeText(context, context.getString(R.string.exif_all_copied), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_content_copy),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.exif_copy_all),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text(stringResource(R.string.exif_filter_tags), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = ShapeLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${allTags.size})") }
                    )
                }
                items(categories) { category ->
                    val count = remember(category, allTags) {
                        allTags.count { it.category.equals(category, ignoreCase = true) }
                    }
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text("$category ($count)") }
                    )
                }
            }

            // Densely packed 2-column tabular view inside a unified Surface
            if (filteredTags.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No metadata tags match \"$searchQuery\"" else "No tags in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Surface(
                    shape = ShapeLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(filteredTags, key = { index, tag -> "${tag.category}_${tag.tag}_$index" }) { index, tagItem ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText(tagItem.tag, tagItem.value))
                                            Toast.makeText(context, "${tagItem.tag} copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText(tagItem.tag, "[${tagItem.category}] ${tagItem.tag} = ${tagItem.value}"))
                                            Toast.makeText(context, "${tagItem.tag} entry copied!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = tagItem.tag,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(135.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tagItem.value,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (index < filteredTags.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Reusable Component Helpers ────────────────────────────────────────────────

@Composable
fun DetailKeyValueRowTight(
    label: String,
    value: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelWidth: androidx.compose.ui.unit.Dp = 100.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.5.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth),
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailKeyValueRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricBadgeBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExposureBadge(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    style = TechnicalParamLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = value,
                    style = TechnicalParamValueStyle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatFullTimestamp(timestamp: Long): String {
    return SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDetailedSize(sizeBytes: Long): String {
    return when {
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
        sizeBytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", sizeBytes / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
    }
}
