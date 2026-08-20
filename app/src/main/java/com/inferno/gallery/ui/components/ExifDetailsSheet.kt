@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.inferno.gallery.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.graphics.Bitmap
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
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

// ── Raw EXIF Tag Names to Inspect ─────────────────────────────────────────────

private val KNOWN_EXIF_TAGS = listOf(
    // Image Structure
    "Image Structure" to listOf(
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_BITS_PER_SAMPLE,
        ExifInterface.TAG_COMPRESSION,
        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_SAMPLES_PER_PIXEL,
        ExifInterface.TAG_PLANAR_CONFIGURATION,
        ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
        ExifInterface.TAG_Y_CB_CR_POSITIONING,
        ExifInterface.TAG_X_RESOLUTION,
        ExifInterface.TAG_Y_RESOLUTION,
        ExifInterface.TAG_RESOLUTION_UNIT
    ),
    // Camera & Hardware
    "Camera & Hardware" to listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SPECIFICATION,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION
    ),
    // Capture & Exposure Parameters
    "Capture Parameters" to listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_BRIGHTNESS_VALUE,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_SUBJECT_DISTANCE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_COLOR_SPACE,
        ExifInterface.TAG_EXPOSURE_MODE,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
        ExifInterface.TAG_SCENE_CAPTURE_TYPE,
        ExifInterface.TAG_GAIN_CONTROL,
        ExifInterface.TAG_CONTRAST,
        ExifInterface.TAG_SATURATION,
        ExifInterface.TAG_SHARPNESS,
        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
        ExifInterface.TAG_SENSING_METHOD,
        ExifInterface.TAG_SCENE_TYPE
    ),
    // GPS & Positioning
    "GPS & Location" to listOf(
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_H_POSITIONING_ERROR
    )
)

// ── Extraction Helper ─────────────────────────────────────────────────────────

suspend fun extractDetailedExif(context: Context, item: GalleryItem): DetailedExifData = withContext(Dispatchers.IO) {
    val uri = item.uri
    val allTagsList = mutableListOf<DeepExifTag>()

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

    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)

            // Extract known tags into raw tag list
            for ((category, tags) in KNOWN_EXIF_TAGS) {
                for (tagName in tags) {
                    val value = exif.getAttribute(tagName)
                    if (!value.isNullOrBlank()) {
                        allTagsList.add(DeepExifTag(category, tagName, value.trim()))
                    }
                }
            }

            make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
            model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
            lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()
            software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim()

            aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
            iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }

            val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            shutterSpeed = when {
                exposureTime <= 0.0 -> null
                exposureTime < 1.0 -> {
                    val denominator = (1.0 / exposureTime).roundToInt()
                    "1/${denominator}s"
                }
                else -> String.format(Locale.US, "%.1fs", exposureTime)
            }

            focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" }
            focalLength35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let { "${it}mm (35mm eq)" }

            val flashVal = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
            if (flashVal != -1) {
                flash = if (flashVal and 1 != 0) "Fired" else "Did not fire"
            }

            val wbVal = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
            if (wbVal != -1) {
                whiteBalance = if (wbVal == ExifInterface.WHITE_BALANCE_AUTO.toInt()) "Auto White Balance" else "Manual White Balance"
            }

            val expModeVal = exif.getAttributeInt(ExifInterface.TAG_EXPOSURE_MODE, -1)
            if (expModeVal != -1) {
                exposureMode = when (expModeVal) {
                    ExifInterface.EXPOSURE_MODE_AUTO.toInt() -> "Auto Exposure"
                    ExifInterface.EXPOSURE_MODE_MANUAL.toInt() -> "Manual Exposure"
                    else -> "Auto Bracket"
                }
            }

            val meterVal = exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1)
            if (meterVal != -1) {
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
            if (csVal != -1) {
                colorSpaceStr = if (csVal == 1) "sRGB" else "Uncalibrated / Adobe RGB"
            }

            val orientVal = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            orientationStr = when (orientVal) {
                ExifInterface.ORIENTATION_NORMAL -> "0° (Normal)"
                ExifInterface.ORIENTATION_ROTATE_90 -> "90° Clockwise"
                ExifInterface.ORIENTATION_ROTATE_180 -> "180°"
                ExifInterface.ORIENTATION_ROTATE_270 -> "270° Clockwise"
                else -> "Default"
            }

            imgW = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            imgH = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

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
    } catch (e: Exception) {
        e.printStackTrace()
    }

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
        dateCreated = dateCreatedParsed ?: (item.dateModified * 1000L),
        dateModified = item.dateModified * 1000L,
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
    exifData: DetailedExifData?
) {
    if (!isOpen || galleryItem == null) return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeepMetadataSheet by remember { mutableStateOf(false) }

    val db = remember { DatabaseProvider.getDatabase(context) }
    val detectedFaces by remember(galleryItem.id) {
        val idLong = galleryItem.id.toLongOrNull() ?: -1L
        if (idLong > 0) {
            db.faceDao().observeFacesForMedia(idLong)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    val allClusters by remember { db.faceDao().observeAllClusters() }.collectAsState(initial = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Info & Metadata",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = exifData?.formatDescription ?: "Media Details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
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

            // ── People in this photo Card ────────────────────────────────────
            if (detectedFaces.isNotEmpty()) {
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_face),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "People in this photo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                for (face in detectedFaces) {
                                    val cluster = allClusters.find { it.clusterId == face.clusterId }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(64.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!face.cropCachePath.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(File(face.cropCachePath))
                                                        .size(120, 120)
                                                        .bitmapConfig(Bitmap.Config.HARDWARE)
                                                        .build(),
                                                    contentDescription = cluster?.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = cluster?.name ?: "Unnamed",
                                            style = MaterialTheme.typography.labelSmall,
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

            // ── File & Timeline Card ─────────────────────────────────────────
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
                        // File Name Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (galleryItem.isVideo) ImageVector.vectorResource(R.drawable.ic_ms_movie) else ImageVector.vectorResource(R.drawable.ic_ms_image),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = galleryItem.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = exifData?.formatDescription ?: "Image File",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Dates Table
                        val createdDate = exifData?.dateCreated
                        val modifiedDate = exifData?.dateModified ?: (galleryItem.dateModified * 1000L)
                        val isModifiedDifferent = createdDate != null && kotlin.math.abs(modifiedDate - createdDate) > 60_000L

                        if (createdDate != null) {
                            DetailKeyValueRow(
                                label = "Date Taken / Created",
                                value = formatFullTimestamp(createdDate),
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_calendar_month)
                            )
                        }

                        if (isModifiedDifferent) {
                            DetailKeyValueRow(
                                label = "Date Modified",
                                value = formatFullTimestamp(modifiedDate),
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_schedule)
                            )
                        }

                        // File Size Row
                        DetailKeyValueRow(
                            label = "File Size",
                            value = formatDetailedSize(galleryItem.size),
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_description)
                        )
                    }
                }
            }

            // ── Resolution, Pixel Dimensions & Aspect Ratio Card ─────────────
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Geometry & Dimensions",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pixel Dimensions
                            MetricBadgeBlock(
                                label = "Dimensions",
                                value = exifData?.resolutionString ?: "Unknown",
                                modifier = Modifier.weight(1f)
                            )
                            // Megapixels
                            if (!exifData?.megapixelsString.isNullOrEmpty()) {
                                MetricBadgeBlock(
                                    label = "Pixel Count",
                                    value = exifData!!.megapixelsString,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Aspect Ratio
                            if (!exifData?.aspectRatioString.isNullOrEmpty()) {
                                MetricBadgeBlock(
                                    label = "Aspect Ratio",
                                    value = exifData!!.aspectRatioString,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Color Space
                            exifData?.colorSpace?.let { cs ->
                                MetricBadgeBlock(
                                    label = "Color Space",
                                    value = cs,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Camera Optics & Shooting Parameters Card ─────────────────────
            val hasCamera = exifData?.cameraModel != null || exifData?.aperture != null || exifData?.iso != null || exifData?.shutterSpeed != null
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = exifData?.cameraModel ?: exifData?.cameraMaker ?: "Camera Optics",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (exifData?.lensModel != null) {
                                        Text(
                                            text = exifData.lensModel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // 4-Quadrant Exposure Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                exifData?.aperture?.let { ap ->
                                    ExposureBadge(
                                        label = "Aperture",
                                        value = ap,
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                exifData?.shutterSpeed?.let { ss ->
                                    ExposureBadge(
                                        label = "Shutter",
                                        value = ss,
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_schedule),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                exifData?.iso?.let { iso ->
                                    ExposureBadge(
                                        label = "ISO",
                                        value = iso,
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_light_mode),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                (exifData?.focalLength35mm ?: exifData?.focalLength)?.let { fl ->
                                    ExposureBadge(
                                        label = "Focal Length",
                                        value = fl,
                                        icon = ImageVector.vectorResource(R.drawable.ic_ms_center_focus_strong),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Additional parameters
                            exifData?.flash?.let { fl ->
                                DetailKeyValueRow(label = "Flash", value = fl, icon = ImageVector.vectorResource(R.drawable.ic_ms_bolt))
                            }
                            exifData?.whiteBalance?.let { wb ->
                                DetailKeyValueRow(label = "White Balance", value = wb, icon = ImageVector.vectorResource(R.drawable.ic_ms_tune))
                            }
                        }
                    }
                }
            }

            // ── GPS Location & Coordinates Card ──────────────────────────────
            if (exifData?.latitude != null && exifData.longitude != null) {
                val lat = exifData.latitude
                val lng = exifData.longitude
                val coordString = exifData.formattedCoordinates ?: "$lat, $lng"

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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_location_on),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Location Coordinates",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", "$lat, $lng"))
                                        Toast.makeText(context, "Coordinates copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_content_copy), contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                        Text(
                                            "Copy",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Text(
                                text = coordString,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )

                            // Open in Maps Button
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(Photo Location)"))
                                    mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No map application available", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = ShapeFull,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_ms_map), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Open in Maps",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // ── Storage Path & Album Card ────────────────────────────────────
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
                        Text(
                            text = "Storage Path & Album",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_folder), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = exifData?.albumName ?: "Gallery",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = galleryItem.path,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Deep Metadata Action Button ──────────────────────────────────
            item {
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
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_code), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "View Deep Metadata (${exifData?.allTags?.size ?: 0} Tags)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }

    // ── Deep Metadata Inspector Modal ─────────────────────────────────────────
    if (showDeepMetadataSheet && exifData != null) {
        DeepMetadataInspectorSheet(
            allTags = exifData.allTags,
            onDismiss = { showDeepMetadataSheet = false }
        )
    }
}

// ── Deep Metadata Inspector Modal Sheet ───────────────────────────────────────

@Composable
private fun DeepMetadataInspectorSheet(
    allTags: List<DeepExifTag>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredTags = remember(searchQuery, allTags) {
        if (searchQuery.isBlank()) allTags
        else allTags.filter {
            it.tag.contains(searchQuery, ignoreCase = true) ||
            it.value.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = ShapeEdgeTop,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header with Copy All
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Deep EXIF Metadata",
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
                        val formattedDump = allTags.joinToString("\n") { "[${it.category}] ${it.tag} = ${it.value}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("EXIF Dump", formattedDump))
                        Toast.makeText(context, "All metadata copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_content_copy), contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            "Copy All",
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
                    .padding(bottom = 12.dp),
                placeholder = { Text("Filter metadata tags…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_search), contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_close), contentDescription = "Clear", modifier = Modifier.size(16.dp))
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

            // Tags List
            if (filteredTags.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No metadata tags match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredTags) { tagItem ->
                        Surface(
                            shape = ShapeMedium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(tagItem.tag, tagItem.value))
                                    Toast.makeText(context, "${tagItem.tag} copied!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tagItem.tag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Surface(
                                        shape = ShapeFull,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(
                                            text = tagItem.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tagItem.value,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Reusable Component Helpers ────────────────────────────────────────────────

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
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
