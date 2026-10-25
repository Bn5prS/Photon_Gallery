package com.inferno.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.BuildConfig
import com.inferno.gallery.R
import com.inferno.gallery.data.network.UpdateInfo
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import kotlinx.coroutines.delay

// ── Update Log Data (newest first; keep in sync with each release) ─────────────

data class UpdateLogEntry(
    val versionName: String,
    val buildCode: Int,
    val date: String,
    val changes: List<String>,
    val isBeta: Boolean = false
)

val UpdateLogEntries = listOf(
    UpdateLogEntry(
        versionName = "2.1.3",
        buildCode = 10,
        date = "Oct 23, 2026",
        changes = listOf(
            "Google Lens visual search button in the viewer action dock",
            "Direct share entrypoint for Google Lens",
            "Standardized album section header accent bar and layout",
            "Release build signing guard for CI"
        )
    ),
    UpdateLogEntry(
        versionName = "2.1.2",
        buildCode = 9,
        date = "Oct 22, 2026",
        changes = listOf(
            "Optional Videos tab in the navigation dock",
            "Smart capture-mode filters: Night, Portrait, Macro, Time-lapse, Panorama & Spheres",
            "Redesigned viewer toolbars and video playback controls",
            "Photo spheres differentiated from panoramas via XMP metadata"
        )
    ),
    UpdateLogEntry(
        versionName = "2.1.1",
        buildCode = 8,
        date = "Oct 22, 2026",
        changes = listOf(
            "Refined the in-app update card and delivery flow"
        )
    ),
    UpdateLogEntry(
        versionName = "2.1.0",
        buildCode = 7,
        date = "Oct 22, 2026",
        changes = listOf(
            "Editorial Mosaic layout for the photo feed",
            "Unified MediaStore favorites across the gallery",
            "Modernized update dialog with expressive shapes and wavy progress",
            "Dynamic wallpaper color enabled by default, with update onboarding"
        )
    ),
    UpdateLogEntry(
        versionName = "2.0.0",
        buildCode = 6,
        date = "Oct 22, 2026",
        changes = listOf(
            "Full Material 3 Expressive design overhaul",
            "Deep zoom up to 25X in the viewer",
            "Memories reel fixes and smart fallback highlights",
            "Major performance optimizations across the grid"
        )
    ),
    UpdateLogEntry(
        versionName = "1.0.1",
        buildCode = 2,
        date = "Aug 21, 2026",
        isBeta = true,
        changes = listOf(
            "Scroll position preserved when returning from the viewer",
            "Room schema migration and database stability fixes",
            "Exact MIME types and permissions for media sharing",
            "Feed top bar and grid typography polish"
        )
    ),
    UpdateLogEntry(
        versionName = "1.0.0",
        buildCode = 1,
        date = "Aug 19, 2026",
        isBeta = true,
        changes = listOf(
            "First public beta: photos, albums, search, and full image editor",
            "Collage, stitch, and duplicate cleaner tools",
            "Private Space vault with biometric lock",
            "Photo map with automatic places clustering"
        )
    )
)

// ── Update Log Timeline Screen ─────────────────────────────────────────────────

/**
 * Material 3 Expressive update timeline: gradient hero header with the installed
 * build, then a release timeline (version, build code, date, changelog bullets)
 * with staggered entrance motion.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateLogContent(
    pendingUpdate: UpdateInfo? = null,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = (surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f) < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Hero Header ────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.65f else 0.85f),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_history),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Update Log",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Every Photon Gallery release, newest first",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "Installed v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // ── Release Timeline ───────────────────────────────────────────────────
        // Show only releases up to the installed build; anything newer appears
        // as a single pending-update card fed by the GitHub update checker.
        val installedEntries = remember {
            UpdateLogEntries.filter { it.buildCode <= BuildConfig.VERSION_CODE }
        }

        pendingUpdate?.let { info ->
            PendingUpdateTimelineItem(info = info, index = 0)
        }

        installedEntries.forEachIndexed { index, entry ->
            UpdateLogTimelineItem(
                entry = entry,
                isCurrent = entry.buildCode == BuildConfig.VERSION_CODE,
                index = index + (if (pendingUpdate != null) 1 else 0)
            )
        }

        Text(
            text = "${installedEntries.size} releases shipped since Aug 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun UpdateLogTimelineItem(
    entry: UpdateLogEntry,
    isCurrent: Boolean,
    index: Int
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        appeared = true
    }

    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(animationSpec = MotionTokens.snappySpring()) +
                slideInVertically(
                    animationSpec = MotionTokens.snappySpring(),
                    initialOffsetY = { it / 6 }
                )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(IntrinsicSize.Min)
        ) {
            // Timeline rail: node + connector line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(20.dp)
            ) {
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Release card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 4.dp),
                shape = ShapeLarge,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Version row: name + build badge + status tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "v${entry.versionName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = "Build ${entry.buildCode}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (isCurrent) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "LATEST",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else if (entry.isBeta) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(
                                    text = "BETA",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Date row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_calendar_month),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = entry.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Changelog bullets
                    entry.changes.forEach { change ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Text(
                                text = change,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Pending Update Card (newer than the installed build) ───────────────────────

@Composable
private fun PendingUpdateTimelineItem(
    info: UpdateInfo,
    index: Int
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        appeared = true
    }

    val changes = remember(info) {
        info.changelog.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line -> line.removePrefix("- ").removePrefix("* ").trim() }
            .take(10)
    }

    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(animationSpec = MotionTokens.snappySpring()) +
                slideInVertically(
                    animationSpec = MotionTokens.snappySpring(),
                    initialOffsetY = { it / 6 }
                )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(IntrinsicSize.Min)
        ) {
            // Timeline rail: node + connector line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(20.dp)
            ) {
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 4.dp),
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "v${info.latestVersion}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = "AVAILABLE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Ready to install — update from Check for Updates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    changes.forEach { change ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                            Text(
                                text = change,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
