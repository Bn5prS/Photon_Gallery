@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.inferno.gallery.ui.components

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.OfficialM3Shapes
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeExtraLargeIncreased
import com.inferno.gallery.ui.theme.ShapeExtraSmall
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import com.inferno.gallery.update.UpdateManager
import com.inferno.gallery.update.UpdateState

data class ChangelogSection(
    val title: String,
    val iconRes: Int,
    val shape: Shape = OfficialM3Shapes.Clover4Leaf,
    val items: List<String>
)

private val EMOJI_AND_SYMBOLS_REGEX = Regex(
    """[\p{So}\p{Sk}\uFE0F\u200D\u20E3]|[\uD83C-\uDBFF\uDC00-\uDFFF]|[\u2600-\u27BF]"""
)

internal fun stripEmojis(text: String): String {
    return text.replace(EMOJI_AND_SYMBOLS_REGEX, "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

internal fun resolveSectionIconAndShape(title: String): Pair<Int, Shape> {
    val t = title.lowercase()
    return when {
        t.contains("people") || t.contains("face") || t.contains("person") ->
            R.drawable.ic_ms_person to OfficialM3Shapes.Cookie12Sided

        t.contains("edit") || t.contains("markup") || t.contains("paint") || t.contains("draw") || t.contains("annotation") ->
            R.drawable.ic_ms_edit to OfficialM3Shapes.Burst

        t.contains("notification") || t.contains("alert") || t.contains("schedule") ->
            R.drawable.ic_ms_schedule to OfficialM3Shapes.Sunny

        t.contains("fix") || t.contains("bug") || t.contains("crash") || t.contains("stability") || t.contains("issue") ->
            R.drawable.ic_ms_bug_report to OfficialM3Shapes.Cookie7Sided

        t.contains("privacy") || t.contains("vault") || t.contains("secure") || t.contains("safety") || t.contains("lock") ->
            R.drawable.ic_ms_shield_lock to OfficialM3Shapes.Clover4Leaf

        t.contains("perf") || t.contains("speed") || t.contains("fast") || t.contains("search") || t.contains("index") ->
            R.drawable.ic_ms_bolt to OfficialM3Shapes.Boom

        t.contains("album") || t.contains("pin") || t.contains("folder") ->
            R.drawable.ic_ms_photo_album to OfficialM3Shapes.Cookie9Sided

        t.contains("view") || t.contains("fullscreen") || t.contains("player") ->
            R.drawable.ic_ms_visibility to OfficialM3Shapes.SoftBurst

        t.contains("font") || t.contains("type") ->
            R.drawable.ic_ms_text_fields to OfficialM3Shapes.SoftBoom

        t.contains("layout") || t.contains("grid") || t.contains("mosaic") ->
            R.drawable.ic_ms_grid_view to OfficialM3Shapes.PuffyDiamond

        t.contains("favorite") ->
            R.drawable.ic_ms_favorite_filled to OfficialM3Shapes.Heart

        else ->
            R.drawable.ic_ms_auto_fix_high to OfficialM3Shapes.Clover8Leaf
    }
}

internal fun parseChangelog(raw: String): List<ChangelogSection> {
    if (raw.isBlank()) return emptyList()

    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    val hasSectionHeaders = lines.any { it.startsWith("#") || it.startsWith("**Features") || it.startsWith("**Bug Fixes") }

    if (hasSectionHeaders) {
        val sections = mutableListOf<ChangelogSection>()
        var currentTitle: String? = null
        val currentItems = mutableListOf<String>()

        fun flushSection() {
            val title = currentTitle ?: return
            val lowerTitle = title.lowercase()
            // Ignore APK downloads, checksums, and assets sections in update notes
            if (lowerTitle.contains("apk") || lowerTitle.contains("download") || lowerTitle.contains("checksum") || lowerTitle.contains("asset")) {
                currentItems.clear()
                return
            }
            if (currentItems.isNotEmpty()) {
                val (iconRes, shape) = resolveSectionIconAndShape(title)
                sections.add(ChangelogSection(title, iconRes, shape, currentItems.toList()))
                currentItems.clear()
            }
        }

        for (line in lines) {
            val isHeader = line.startsWith("#") || (line.startsWith("**") && line.endsWith("**") && line.length < 50)
            if (isHeader) {
                flushSection()
                val rawTitle = line.trim('#', '*', ' ').replace("**", "").trim()
                currentTitle = stripEmojis(rawTitle)
            } else {
                val cleaned = cleanChangelogItem(line)
                if (cleaned.isNotBlank() && !isIgnoredLine(cleaned)) {
                    currentItems.add(cleaned)
                }
            }
        }
        flushSection()
        val isGenericTitle = { t: String ->
            val lower = t.lowercase()
            lower.contains("what's changed") || lower.contains("whats changed") || lower.contains("release note") ||
                lower.contains("changelog") || lower.contains("highlights") || lower.contains("what’s new") || lower.contains("whats new")
        }
        if (sections.isNotEmpty() && !sections.all { isGenericTitle(it.title) }) {
            return sections
        }
    }

    val features = mutableListOf<String>()
    val fixes = mutableListOf<String>()
    val other = mutableListOf<String>()

    for (line in lines) {
        if (line.startsWith("#") || (line.startsWith("**") && line.endsWith("**") && line.length < 50)) continue
        val cleaned = cleanChangelogItem(line)
        if (cleaned.isBlank() || isIgnoredLine(cleaned)) continue

        val lower = cleaned.lowercase()
        when {
            lower.startsWith("fix") || lower.contains("bug") || lower.contains("crash") ||
            lower.contains("resolve") || lower.contains("issue") || lower.contains("error") -> {
                fixes.add(cleaned)
            }
            lower.startsWith("feat") || lower.startsWith("add") || lower.startsWith("new") ||
            lower.contains("redesign") || lower.contains("support") || lower.contains("implement") -> {
                features.add(cleaned)
            }
            else -> {
                other.add(cleaned)
            }
        }
    }

    val result = mutableListOf<ChangelogSection>()
    if (features.isNotEmpty()) {
        result.add(
            ChangelogSection(
                title = "Features & Enhancements",
                iconRes = R.drawable.ic_ms_auto_fix_high,
                shape = OfficialM3Shapes.Clover8Leaf,
                items = features
            )
        )
    }
    if (fixes.isNotEmpty()) {
        result.add(
            ChangelogSection(
                title = "Fixes & Improvements",
                iconRes = R.drawable.ic_ms_bug_report,
                shape = OfficialM3Shapes.Cookie7Sided,
                items = fixes
            )
        )
    }
    if (other.isNotEmpty()) {
        val title = if (result.isEmpty()) "What's New" else "Other Changes"
        val icon = if (result.isEmpty()) R.drawable.ic_ms_auto_fix_high else R.drawable.ic_ms_description
        val shape = if (result.isEmpty()) OfficialM3Shapes.Clover8Leaf else OfficialM3Shapes.Clover4Leaf
        result.add(ChangelogSection(title, icon, shape, other))
    }

    return result
}

internal fun cleanChangelogItem(line: String): String {
    var s = line.trim()
    s = s.replace(Regex("""^[\*\-\+•]\s*"""), "")
    s = s.replace("**", "")
    s = stripEmojis(s)
    s = s.replace(Regex("""\s+by\s+@[\w\-]+(\s+in\s+#\d+)?""", RegexOption.IGNORE_CASE), "")
    return s.trim()
}

internal fun isIgnoredLine(line: String): Boolean {
    val l = line.trim().lowercase()
    if (l.isEmpty() || l.all { it == '-' || it == '=' || it == '*' || it == '_' }) return true
    return l.contains("full changelog") ||
        l.contains("/compare/") ||
        l.startsWith("https://github.com/") ||
        l.startsWith("http://") ||
        l.contains(".apk") ||
        l.contains("apk download") ||
        l.contains("checksum") ||
        l.contains("sha256") ||
        l.contains("recommended for") ||
        l.contains("standalone build") ||
        l.contains("legacy support") ||
        l.contains("emulators and devices")
}

@Composable
fun ChangelogCard(changelog: String, modifier: Modifier = Modifier) {
    val sections = remember(changelog) { parseChangelog(changelog) }
    if (sections.isEmpty() && changelog.isBlank()) return

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.M),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.L)
            ) {
                if (sections.isEmpty()) {
                    Text(
                        text = changelog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    sections.forEach { section ->
                        val isFix = section.iconRes == R.drawable.ic_ms_bug_report ||
                            section.title.contains("Fix", ignoreCase = true) ||
                            section.title.contains("Bug", ignoreCase = true)
                        val isEdit = section.iconRes == R.drawable.ic_ms_edit ||
                            section.title.contains("Edit", ignoreCase = true) ||
                            section.title.contains("Markup", ignoreCase = true) ||
                            section.title.contains("Paint", ignoreCase = true)
                        val isPerson = section.iconRes == R.drawable.ic_ms_person ||
                            section.title.contains("People", ignoreCase = true) ||
                            section.title.contains("Face", ignoreCase = true)
                        val isFeature = section.title.contains("Feat", ignoreCase = true) ||
                            section.title.contains("New", ignoreCase = true) ||
                            section.title.contains("Enhance", ignoreCase = true)

                        val (pillContainer, pillContent, dotColor) = when {
                            isFix -> Triple(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.onErrorContainer,
                                MaterialTheme.colorScheme.error
                            )
                            isEdit -> Triple(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer,
                                MaterialTheme.colorScheme.tertiary
                            )
                            isPerson || isFeature -> Triple(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                            else -> Triple(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                                MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            // Section header capsule featuring Material 3 Expressive Shape container & Material Symbol
                            Surface(
                                shape = ShapeFull,
                                color = pillContainer,
                                contentColor = pillContent
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 4.dp, end = SpacingTokens.M, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                                ) {
                                    Surface(
                                        shape = section.shape,
                                        color = pillContent.copy(alpha = 0.16f),
                                        contentColor = pillContent,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(section.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            // Items under section with M3 micro squircle bullet
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = SpacingTokens.XS),
                                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                            ) {
                                section.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = ShapeExtraSmall,
                                            color = dotColor.copy(alpha = 0.85f),
                                            modifier = Modifier
                                                .padding(top = 7.dp)
                                                .size(width = 6.dp, height = 6.dp)
                                        ) {}
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top gradient fade mask when scrolled down
            AnimatedVisibility(
                visible = scrollState.value > 0,
                enter = fadeIn(MotionTokens.fastEffectsSpec()),
                exit = fadeOut(MotionTokens.fastEffectsSpec()),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Bottom gradient fade mask when scrollable content remains below
            AnimatedVisibility(
                visible = scrollState.value < scrollState.maxValue,
                enter = fadeIn(MotionTokens.fastEffectsSpec()),
                exit = fadeOut(MotionTokens.fastEffectsSpec()),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog() {
    val updateState by UpdateManager.state.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    if (updateState is UpdateState.Idle) return
    if (updateState is UpdateState.Checking && !(updateState as UpdateState.Checking).manual) return

    BasicAlertDialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading) {
                UpdateManager.dismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val isUpdateAvailable = updateState is UpdateState.UpdateAvailable
        Surface(
            modifier = if (isUpdateAvailable) {
                Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.90f)
                    .widthIn(max = 560.dp)
                    .clip(ShapeExtraLargeIncreased)
            } else {
                Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .clip(ShapeExtraLarge)
            },
            shape = if (isUpdateAvailable) ShapeExtraLargeIncreased else ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = (if (isUpdateAvailable) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .padding(
                        horizontal = if (isUpdateAvailable) SpacingTokens.L else SpacingTokens.XL,
                        vertical = if (isUpdateAvailable) SpacingTokens.L else SpacingTokens.XL
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = updateState) {
                    is UpdateState.Checking -> {
                        Surface(
                            shape = OfficialM3Shapes.Clover4Leaf,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                ContainedLoadingIndicator(
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Checking for Updates…",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.XS))
                        Text(
                            text = "Connecting to GitHub releases",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateState.UpdateAvailable -> {
                        val info = state.info
                        Surface(
                            shape = OfficialM3Shapes.Cookie9Sided,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(IconSizeTokens.L)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.M))
                        Text(
                            text = "Update Available",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.S))

                        // Version transition pill & size badge row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                                ) {
                                    Text(
                                        text = info.currentVersion,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(IconSizeTokens.S)
                                    )
                                    Text(
                                        text = info.latestVersion,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (info.apkSizeBytes > 0) {
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = SpacingTokens.XS),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = Formatter.formatShortFileSize(context, info.apkSizeBytes),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(SpacingTokens.M))

                        // Features & Fixes Categorized Changelog
                        if (info.changelog.isNotBlank()) {
                            ChangelogCard(
                                changelog = info.changelog,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            Spacer(modifier = Modifier.height(SpacingTokens.M))
                        }

                        // Action Buttons Dock
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            Button(
                                onClick = {
                                    UpdateManager.downloadAndInstall(context, info, coroutineScope)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .pressScale(),
                                shape = ShapeFull,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                                Spacer(modifier = Modifier.width(SpacingTokens.S))
                                Text(
                                    text = "Download & Install",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                            ) {
                                FilledTonalButton(
                                    onClick = { uriHandler.openUri(info.releasePageUrl) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .pressScale(),
                                    shape = ShapeFull,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_new),
                                        contentDescription = null,
                                        modifier = Modifier.size(IconSizeTokens.S)
                                    )
                                    Spacer(modifier = Modifier.width(SpacingTokens.XS))
                                    Text(
                                        text = "GitHub Notes",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                TextButton(
                                    onClick = { UpdateManager.dismiss() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .pressScale(),
                                    shape = ShapeFull
                                ) {
                                    Text(
                                        text = "Later",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }

                    is UpdateState.Downloading -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = state.progress,
                            animationSpec = MotionTokens.snappySpring(),
                            label = "download_progress"
                        )

                        val infiniteTransition = rememberInfiniteTransition(label = "download_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.96f,
                            targetValue = 1.04f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = MotionTokens.StandardEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )

                        Surface(
                            shape = OfficialM3Shapes.Clover4Leaf,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(IconSizeTokens.XL)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Downloading Update…",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.XS))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                        ) {
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${Formatter.formatShortFileSize(context, state.downloadedBytes)} / ${Formatter.formatShortFileSize(context, state.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(SpacingTokens.L))

                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )

                        Spacer(modifier = Modifier.height(SpacingTokens.L))

                        TextButton(
                            onClick = { UpdateManager.dismiss() },
                            modifier = Modifier.pressScale(),
                            shape = ShapeFull
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    is UpdateState.ReadyToInstall -> {
                        Surface(
                            shape = OfficialM3Shapes.Cookie9Sided,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(IconSizeTokens.XL)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Ready to Install",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.XS))
                        Text(
                            text = "Download complete. Tap below to launch installer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.L))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            TextButton(
                                onClick = { UpdateManager.dismiss() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .pressScale(),
                                shape = ShapeFull
                            ) {
                                Text(
                                    text = "Later",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Button(
                                onClick = { UpdateManager.installApk(context, state.apkFile) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .pressScale(),
                                shape = ShapeFull,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                                Spacer(modifier = Modifier.width(SpacingTokens.XS))
                                Text(
                                    text = "Install Now",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    is UpdateState.UpToDate -> {
                        Surface(
                            shape = OfficialM3Shapes.Clover4Leaf,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(IconSizeTokens.XL)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "You're Up to Date!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.XS))
                        Text(
                            text = "Photon Gallery ${state.currentVersion} is currently the newest version available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = SpacingTokens.S)
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        FilledTonalButton(
                            onClick = { UpdateManager.dismiss() },
                            shape = ShapeFull,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pressScale()
                        ) {
                            Text(
                                text = "OK",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    is UpdateState.Error -> {
                        Surface(
                            shape = OfficialM3Shapes.Cookie9Sided,
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_info),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(IconSizeTokens.XL)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Update Check Failed",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.XS))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = SpacingTokens.S)
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        FilledTonalButton(
                            onClick = { UpdateManager.dismiss() },
                            shape = ShapeFull,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pressScale()
                        ) {
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
