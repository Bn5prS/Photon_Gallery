package com.inferno.gallery.ui.components

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.inferno.gallery.BuildConfig
import com.inferno.gallery.R
import com.inferno.gallery.ui.ClickableSettingItem
import com.inferno.gallery.ui.SettingsGroupCard
import com.inferno.gallery.ui.SwitchSettingItem
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.theme.photonColors
import com.inferno.gallery.ui.utils.tick
import com.inferno.gallery.update.UpdateState

/**
 * Material 3 Expressive About Screen for Photon Gallery.
 * Features:
 * - Fluid gradient Hero App presentation card with zero outlines
 * - Interactive version & update status pill with M3 morphing indicators
 * - Sleek gradient Developer spotlight card with zero outlines
 * - Sheet-styled segmented cards for updates, open source, and privacy guarantees
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreenContent(
    autoCheckUpdates: Boolean,
    updateState: UpdateState = UpdateState.Idle,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateLog: () -> Unit = {},
    onOpenLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = (surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f) < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Hero Brand Presentation (Pure Gradient Card, No Outlines) ─────────
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
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.launcher_icon_fg),
                        contentDescription = "Photon Gallery Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(76.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Photon Gallery",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── 2. Developer Spotlight (Pure Gradient Card, No Outlines) ──────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.60f else 0.80f),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = "https://github.com/Bn5prS.png",
                                contentDescription = "Bn5prS Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Bn5prS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = ShapeFull,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.about_developer),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.about_creator_maintainer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (isDark) 0.85f else 0.92f),
                            onClick = {
                                haptic.tick()
                                uriHandler.openUri("https://github.com/Bn5prS")
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_code),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "@Bn5prS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = {
                                haptic.tick()
                                uriHandler.openUri("https://github.com/Bn5prS/Photon_Gallery")
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.about_star_project),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3. Updates & Delivery (Sheet Design Segmented Group Card) ───────
        SettingsGroupCard(
            title = stringResource(R.string.about_updates_delivery),
            items = listOf<@Composable () -> Unit>(
                {
                    val updateSubtitle = when (updateState) {
                        is UpdateState.Checking -> stringResource(R.string.update_status_checking)
                        is UpdateState.UpdateAvailable -> stringResource(R.string.update_status_available, updateState.info.latestVersion)
                        is UpdateState.UpToDate -> stringResource(R.string.update_status_uptodate, BuildConfig.VERSION_NAME)
                        is UpdateState.ReadyToInstall -> stringResource(R.string.update_status_downloaded, updateState.info.latestVersion)
                        else -> stringResource(R.string.update_status_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                    }
                    ClickableSettingItem(
                        title = stringResource(R.string.about_check_updates),
                        subtitle = updateSubtitle,
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                        onClick = {
                            haptic.tick()
                            onCheckForUpdates()
                        }
                    )
                },
                {
                    ClickableSettingItem(
                        title = stringResource(R.string.about_update_log),
                        subtitle = stringResource(R.string.about_update_log_sub),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_history),
                        onClick = {
                            haptic.tick()
                            onOpenUpdateLog()
                        }
                    )
                },
                {
                    SwitchSettingItem(
                        title = stringResource(R.string.about_auto_check),
                        subtitle = stringResource(R.string.about_auto_check_sub),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_schedule),
                        checked = autoCheckUpdates,
                        onCheckedChange = { onAutoCheckUpdatesChange(it) }
                    )
                }
            )
        )

        // ── 4. Project & Community (Sheet Design Segmented Group Card) ──────
        SettingsGroupCard(
            title = stringResource(R.string.about_project_opensource),
            items = listOf<@Composable () -> Unit>(
                {
                    ClickableSettingItem(
                        title = stringResource(R.string.setting_github_repo),
                        subtitle = stringResource(R.string.setting_github_repo_sub),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_code),
                        onClick = {
                            haptic.tick()
                            uriHandler.openUri("https://github.com/Bn5prS/Photon_Gallery")
                        }
                    )
                },
                {
                    ClickableSettingItem(
                        title = stringResource(R.string.setting_report_bug),
                        subtitle = stringResource(R.string.setting_report_bug_sub),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_bug_report),
                        onClick = {
                            haptic.tick()
                            uriHandler.openUri("https://github.com/Bn5prS/Photon_Gallery/issues")
                        }
                    )
                },
                {
                    ClickableSettingItem(
                        title = stringResource(R.string.about_licenses),
                        subtitle = stringResource(R.string.about_licenses_sub),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_description),
                        onClick = {
                            haptic.tick()
                            onOpenLicenses()
                        }
                    )
                },
                {
                    ClickableSettingItem(
                        title = stringResource(R.string.setting_privacy_policy),
                        subtitle = stringResource(R.string.setting_privacy_policy_sub),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_shield),
                        onClick = {
                            haptic.tick()
                            onOpenPrivacy()
                        }
                    )
                }
            )
        )

        // ── 5. Clean Brand Footer ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.about_brand_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
