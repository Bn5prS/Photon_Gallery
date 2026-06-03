package com.inferno.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.work.WorkInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = viewModel(),
    galleryViewModel: GalleryViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 16.dp).weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val themeMode by viewModel.themeMode.collectAsState()
            val useMaterialYou by viewModel.useMaterialYou.collectAsState()
            val useAmoledBlack by viewModel.useAmoledBlack.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dockStyle by viewModel.dockStyle.collectAsState()
            val gridCellsCount by galleryViewModel.gridCellsCount.collectAsState()
            val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
            val gridAutoPlay by galleryViewModel.gridAutoPlay.collectAsState()
            val aiIndexWorkInfo by viewModel.aiIndexWorkInfo.collectAsState(initial = null)
            val isCurrentlyDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

            // 1. App Info Card
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Photon Gallery", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Version 1.0 (Expressive)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Crafted with ♥ by Bn5prS", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://github.com/Bn5prS/Photon_Gallery") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text("GitHub Repository")
                    }
                }
            }

            SettingsGroup(title = "Appearance") {
                Column(modifier = Modifier.padding(16.dp)) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            selected = themeMode == ThemeMode.SYSTEM,
                            icon = { Icon(Icons.Outlined.BrightnessAuto, contentDescription = null) },
                            label = { Text("System", style = MaterialTheme.typography.labelMedium) }
                        )
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            selected = themeMode == ThemeMode.LIGHT,
                            icon = { Icon(Icons.Outlined.LightMode, contentDescription = null) },
                            label = { Text("Light", style = MaterialTheme.typography.labelMedium) }
                        )
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            selected = themeMode == ThemeMode.DARK,
                            icon = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
                            label = { Text("Dark", style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                    headlineContent = { Text("Material You") },
                    supportingContent = { Text("Use dynamic system colors") },
                    trailingContent = {
                        Switch(
                            checked = useMaterialYou,
                            onCheckedChange = { viewModel.setUseMaterialYou(it) },
                            thumbContent = {
                                Icon(
                                    imageVector = if (useMaterialYou) Icons.Outlined.Check else Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Contrast, contentDescription = null) },
                    headlineContent = { Text("AMOLED Black") },
                    supportingContent = { Text("Use pitch black background in dark mode") },
                    trailingContent = {
                        Switch(
                            checked = useAmoledBlack,
                            onCheckedChange = { viewModel.setUseAmoledBlack(it) },
                            enabled = isCurrentlyDark,
                            thumbContent = {
                                Icon(
                                    imageVector = if (useAmoledBlack) Icons.Outlined.Check else Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            SettingsGroup(title = "Layout & Navigation") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Menu, contentDescription = null) },
                    headlineContent = { Text("Full-Width Dock") },
                    supportingContent = { Text("Use standard edge-to-edge dock instead of floating pill") },
                    trailingContent = {
                        val isDockFullWidth = dockStyle == com.inferno.gallery.data.DockStyle.FULL_WIDTH
                        Switch(
                            checked = isDockFullWidth, 
                            onCheckedChange = { isChecked ->
                                viewModel.setDockStyle(if (isChecked) com.inferno.gallery.data.DockStyle.FULL_WIDTH else com.inferno.gallery.data.DockStyle.PILL)
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isDockFullWidth) Icons.Outlined.Check else Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Grid Items per Row: $gridCellsCount", style = MaterialTheme.typography.bodyLarge)
                    }
                    androidx.compose.material3.Slider(
                        value = gridCellsCount.toFloat(),
                        onValueChange = { galleryViewModel.setGridCellsCount(it.toInt()) },
                        valueRange = 2f..6f,
                        steps = 3
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Thumbnail Corner Radius", style = MaterialTheme.typography.bodyLarge)
                        val previewScale = 48f / 120f
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape((thumbnailCornerRadius * previewScale).dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {}
                    }
                    androidx.compose.material3.Slider(
                        value = thumbnailCornerRadius,
                        onValueChange = { viewModel.setThumbnailCornerRadius(it) },
                        valueRange = 0f..24f
                    )
                }
            }

            SettingsGroup(title = "Media & Storage") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null) },
                    headlineContent = { Text("Auto-Play Media") },
                    supportingContent = { Text("Play GIFs and videos in grid") },
                    trailingContent = {
                        Switch(
                            checked = gridAutoPlay,
                            onCheckedChange = { galleryViewModel.toggleGridAutoPlay() },
                            thumbContent = {
                                Icon(
                                    imageVector = if (gridAutoPlay) Icons.Outlined.Check else Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

            }

            SettingsGroup(title = "Local AI Engine") {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.SmartToy, contentDescription = null) },
                    headlineContent = { Text("Smart Search Indexing") },
                    supportingContent = { 
                        Column {
                            Text("Index photos locally for semantic search")
                            
                            val state = aiIndexWorkInfo?.state
                            if (state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING) {
                                Spacer(Modifier.height(8.dp))
                                val progress = aiIndexWorkInfo?.progress
                                val indexed = progress?.getInt("progress", 0) ?: 0
                                val total = progress?.getInt("total", 0) ?: 0
                                
                                val progressFloat = if (total > 0) indexed.toFloat() / total.toFloat() else 0f
                                
                                LinearProgressIndicator(
                                    progress = { progressFloat },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Indexed $indexed of $total images", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailingContent = {
                        val isRunning = aiIndexWorkInfo?.state == WorkInfo.State.RUNNING || aiIndexWorkInfo?.state == WorkInfo.State.ENQUEUED
                        if (isRunning) {
                            FilledTonalButton(onClick = { viewModel.stopAiIndexing() }) {
                                Text("Pause")
                            }
                        } else {
                            FilledTonalButton(onClick = { viewModel.startAiIndexing() }) {
                                Text("Start")
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Face, contentDescription = null) },
                    headlineContent = { Text("Face Clustering") },
                    supportingContent = { Text("Group photos by people locally") },
                    trailingContent = {
                        Switch(
                            checked = false,
                            onCheckedChange = null,
                            enabled = false,
                            thumbContent = {
                                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}
