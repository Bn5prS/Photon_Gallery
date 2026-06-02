package com.inferno.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                .verticalScroll(rememberScrollState())
        ) {
            val themeMode by viewModel.themeMode.collectAsState()
            val useMaterialYou by viewModel.useMaterialYou.collectAsState()
            val useAmoledBlack by viewModel.useAmoledBlack.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isCurrentlyDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val dockStyle by viewModel.dockStyle.collectAsState()
            
            Column {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)
                )
                
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
                
                Spacer(modifier = Modifier.height(16.dp))

                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                    headlineContent = { Text("Material You") },
                    supportingContent = { Text("Use dynamic system colors") },
                    trailingContent = {
                        Switch(
                            checked = useMaterialYou,
                            onCheckedChange = { viewModel.setUseMaterialYou(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Contrast, contentDescription = null) },
                    headlineContent = { Text("AMOLED Black") },
                    supportingContent = { Text("Use pitch black background in dark mode") },
                    trailingContent = {
                        Switch(
                            checked = useAmoledBlack,
                            onCheckedChange = { viewModel.setUseAmoledBlack(it) },
                            enabled = isCurrentlyDark
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Text(
                text = "Navigation",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Use Full-Width Dock") },
                supportingContent = { Text("Switch from the floating pill to a standard edge-to-edge dock") },
                trailingContent = { 
                    Switch(
                        checked = dockStyle == com.inferno.gallery.data.DockStyle.FULL_WIDTH, 
                        onCheckedChange = { isChecked ->
                            viewModel.setDockStyle(if (isChecked) com.inferno.gallery.data.DockStyle.FULL_WIDTH else com.inferno.gallery.data.DockStyle.PILL)
                        }
                    ) 
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Text(
                text = "Grid Layout",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
            )
            val gridCellsCount by galleryViewModel.gridCellsCount.collectAsState()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)) {
                Text(text = "Grid Items per Row: $gridCellsCount", style = MaterialTheme.typography.bodyLarge)
                androidx.compose.material3.Slider(
                    value = gridCellsCount.toFloat(),
                    onValueChange = { galleryViewModel.setGridCellsCount(it.toInt()) },
                    valueRange = 2f..6f,
                    steps = 3
                )
            }

            val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Thumbnail Corner Radius", style = MaterialTheme.typography.bodyLarge)
                    // Live preview tile (Scaled for accurate visual representation relative to grid size)
                    val previewScale = 48f / 120f // 48dp preview size / ~120dp average grid item size
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

            Text(
                text = "Media",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
            )
            val gridAutoPlay by galleryViewModel.gridAutoPlay.collectAsState()
            ListItem(
                headlineContent = { Text("Auto-Play Animated Media") },
                supportingContent = { Text("Automatically play GIFs, WebPs, and AVIFs in the main grid") },
                trailingContent = { 
                    Switch(
                        checked = gridAutoPlay, 
                        onCheckedChange = { galleryViewModel.toggleGridAutoPlay() }
                    ) 
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { galleryViewModel.toggleGridAutoPlay() }
                    .padding(horizontal = 16.dp)
            )



            Text(
                text = "Storage & Cache",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Clear Image Cache") },
                supportingContent = { Text("Frees up space used by thumbnail previews") },
                trailingContent = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear cache")
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Text(
                text = "Local AI Engine",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Smart Search") },
                supportingContent = { Text("Index photos for semantic search (e.g., 'dog on a beach')") },
                trailingContent = { Switch(checked = false, onCheckedChange = null, enabled = false) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ListItem(
                headlineContent = { Text("Face Clustering") },
                supportingContent = { Text("Group photos by people locally on your device") },
                trailingContent = { Switch(checked = false, onCheckedChange = null, enabled = false) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
