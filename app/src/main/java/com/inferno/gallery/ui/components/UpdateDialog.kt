@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.inferno.gallery.ui.components

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.update.UpdateManager
import com.inferno.gallery.update.UpdateState

data class ChangelogSection(
    val title: String,
    val iconRes: Int,
    val items: List<String>
)

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
            if (currentItems.isNotEmpty()) {
                val iconRes = when {
                    title.contains("Fix", ignoreCase = true) || title.contains("Bug", ignoreCase = true) -> R.drawable.ic_ms_bug_report
                    title.contains("Improve", ignoreCase = true) || title.contains("Perf", ignoreCase = true) -> R.drawable.ic_ms_check_circle
                    else -> R.drawable.ic_ms_auto_fix_high
                }
                sections.add(ChangelogSection(title, iconRes, currentItems.toList()))
                currentItems.clear()
            }
        }

        for (line in lines) {
            val isHeader = line.startsWith("#") || (line.startsWith("**") && line.endsWith("**") && line.length < 40)
            if (isHeader) {
                flushSection()
                currentTitle = line.trim('#', '*', ' ').trim()
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
            lower.contains("what's changed") || lower.contains("whats changed") || lower.contains("release note") || lower.contains("changelog")
        }
        if (sections.isNotEmpty() && !sections.all { isGenericTitle(it.title) }) {
            return sections
        }
    }

    val features = mutableListOf<String>()
    val fixes = mutableListOf<String>()
    val other = mutableListOf<String>()

    for (line in lines) {
        if (line.startsWith("#") || (line.startsWith("**") && line.endsWith("**") && line.length < 40)) continue
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
        result.add(ChangelogSection("Features & Enhancements", R.drawable.ic_ms_auto_fix_high, features))
    }
    if (fixes.isNotEmpty()) {
        result.add(ChangelogSection("Fixes & Improvements", R.drawable.ic_ms_check_circle, fixes))
    }
    if (other.isNotEmpty()) {
        val title = if (result.isEmpty()) "What's New" else "Other Changes"
        val icon = if (result.isEmpty()) R.drawable.ic_ms_auto_fix_high else R.drawable.ic_ms_description
        result.add(ChangelogSection(title, icon, other))
    }

    return result
}

private fun cleanChangelogItem(line: String): String {
    var s = line.trim()
    s = s.replace(Regex("""^\*\*([^\*]+)\*\*:\s*"""), "$1: ")
    s = s.replace(Regex("""^[\*\-\+•]\s*"""), "")
    s = s.replace(Regex("""\s+by\s+@[\w\-]+(\s+in\s+#\d+)?""", RegexOption.IGNORE_CASE), "")
    s = s.replace(Regex("""\s+in\s+#\d+""", RegexOption.IGNORE_CASE), "")
    return s.trim()
}

private fun isIgnoredLine(line: String): Boolean {
    val l = line.lowercase()
    return l.contains("full changelog") || l.contains("/compare/") || l.startsWith("https://github.com/") || l.startsWith("http://")
}

@Composable
fun ChangelogCard(changelog: String, modifier: Modifier = Modifier) {
    val sections = remember(changelog) { parseChangelog(changelog) }
    if (sections.isEmpty() && changelog.isBlank()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SpacingTokens.M),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.M)
        ) {
            if (sections.isEmpty()) {
                Text(
                    text = changelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                sections.forEach { section ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(section.iconRes),
                                contentDescription = null,
                                tint = if (section.iconRes == R.drawable.ic_ms_bug_report) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (section.iconRes == R.drawable.ic_ms_bug_report) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        section.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = SpacingTokens.XS),
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                )
                                Text(
                                    text = item,
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
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeExtraLarge),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingTokens.XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = updateState) {
                    is UpdateState.Checking -> {
                        ContainedLoadingIndicator(
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Checking for Updates…",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.S))
                        Text(
                            text = "Connecting to GitHub releases",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateState.UpdateAvailable -> {
                        val info = state.info
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(IconSizeTokens.XL)
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Update Available",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.S))

                        // Version & Size badge row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${info.currentVersion} → ${info.latestVersion}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS)
                                )
                            }

                            if (info.apkSizeBytes > 0) {
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = Formatter.formatShortFileSize(context, info.apkSizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = SpacingTokens.XS)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(SpacingTokens.L))

                        // Features & Fixes Changelog card
                        if (info.changelog.isNotBlank()) {
                            ChangelogCard(changelog = info.changelog)
                            Spacer(modifier = Modifier.height(SpacingTokens.L))
                        }

                        // Action buttons
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
                                    .height(48.dp),
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
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S)
                            ) {
                                OutlinedButton(
                                    onClick = { uriHandler.openUri(info.releasePageUrl) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = ShapeFull
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_new),
                                        contentDescription = null,
                                        modifier = Modifier.size(IconSizeTokens.S)
                                    )
                                    Spacer(modifier = Modifier.width(SpacingTokens.XS))
                                    Text(
                                        text = "GitHub",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                TextButton(
                                    onClick = { UpdateManager.dismiss() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = ShapeFull
                                ) {
                                    Text(
                                        text = "Later",
                                        style = MaterialTheme.typography.labelLarge
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

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(IconSizeTokens.XL)
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Downloading Update…",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.S))

                        Text(
                            text = "${(state.progress * 100).toInt()}% • ${Formatter.formatShortFileSize(context, state.downloadedBytes)} / ${Formatter.formatShortFileSize(context, state.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(SpacingTokens.L))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(ShapeFull),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )

                        Spacer(modifier = Modifier.height(SpacingTokens.L))

                        TextButton(
                            onClick = { UpdateManager.dismiss() },
                            shape = ShapeFull
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    is UpdateState.ReadyToInstall -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(IconSizeTokens.XL)
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Ready to Install",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.S))
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
                                modifier = Modifier.weight(1f),
                                shape = ShapeFull
                            ) {
                                Text("Later", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = { UpdateManager.installApk(context, state.apkFile) },
                                modifier = Modifier.weight(1f),
                                shape = ShapeFull
                            ) {
                                Text("Install Now", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    is UpdateState.UpToDate -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(IconSizeTokens.XL)
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "You're Up to Date!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.S))
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    is UpdateState.Error -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(IconSizeTokens.XL)
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingTokens.L))
                        Text(
                            text = "Update Check Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.S))
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
