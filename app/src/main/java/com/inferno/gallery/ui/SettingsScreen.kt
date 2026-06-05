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
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect

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
            val useFullScreen by viewModel.useFullScreen.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dockStyle by viewModel.dockStyle.collectAsState()
            val gridCellsCount by galleryViewModel.gridCellsCount.collectAsState()
            val thumbnailCornerRadius by viewModel.thumbnailCornerRadius.collectAsState()
            val gridAutoPlay by galleryViewModel.gridAutoPlay.collectAsState()
            val ocrIndexWorkInfo by viewModel.ocrIndexWorkInfo.collectAsState(initial = null)
            val totalImagesCount by viewModel.totalImagesCount.collectAsState()
            val unindexedOcrImagesCount by viewModel.unindexedOcrImagesCount.collectAsState()
            
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
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(64.dp))
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

                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Fullscreen, contentDescription = null) },
                    headlineContent = { Text("Full Screen Mode") },
                    supportingContent = { Text("Hide status bar and navigation bar to maximize content area") },
                    trailingContent = {
                        Switch(
                            checked = useFullScreen,
                            onCheckedChange = { viewModel.setUseFullScreen(it) },
                            thumbContent = {
                                Icon(
                                    imageVector = if (useFullScreen) Icons.Outlined.Check else Icons.Outlined.Close,
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

            SettingsGroup(title = "Telegram Cloud Backup") {
                val savedTokens by viewModel.telegramBotTokens.collectAsState()
                val savedChatId by viewModel.telegramChatId.collectAsState()
                val backupEnabled by viewModel.telegramBackupEnabled.collectAsState()
                val stripLocation by viewModel.telegramStripLocation.collectAsState()
                val showFolderDialog = remember { mutableStateOf(false) }
                val selectedFolders by viewModel.telegramAutoBackupFolders.collectAsState()
                val allFolders by viewModel.allBucketNames.collectAsState()

                var passwordVisible by remember { mutableStateOf(false) }

                var localTokens by remember(savedTokens) { mutableStateOf(savedTokens) }
                var localChatId by remember(savedChatId) { mutableStateOf(savedChatId) }
                var localTokenInput by remember { mutableStateOf("") }

                val testResult by viewModel.connectionTestState.collectAsState()
                val hasChanges = localTokens != savedTokens || localChatId != savedChatId

                LaunchedEffect(testResult) {
                    val result = testResult
                    if (result is ConnectionTestResult.Migrated) {
                        localChatId = result.newChatId
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = localTokenInput,
                        onValueChange = { 
                            localTokenInput = it
                            viewModel.clearConnectionTestResult()
                        },
                        label = { Text("Add Telegram Bot Token") },
                        placeholder = { Text("123456:ABC-DEF...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (localTokenInput.isNotBlank() && !localTokens.contains(localTokenInput.trim())) {
                                    localTokens = localTokens + localTokenInput.trim()
                                    localTokenInput = ""
                                }
                            },
                            enabled = localTokenInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Bot")
                        }

                        FilledTonalButton(
                            onClick = { viewModel.testTelegramConnection(localTokenInput.trim(), localChatId.trim()) },
                            enabled = testResult != ConnectionTestResult.Testing && localTokenInput.isNotBlank() && localChatId.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Staged")
                        }
                    }

                    if (localTokens.isNotEmpty()) {
                        Text(
                            text = "Configured Bots (${localTokens.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            localTokens.forEachIndexed { index, token ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val displayToken = remember(token) {
                                        if (token.length > 15) {
                                            token.take(8) + "..." + token.takeLast(6)
                                        } else {
                                            token
                                        }
                                    }
                                    Text(
                                        text = "Bot #${index + 1}: $displayToken",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = {
                                            localTokens = localTokens.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove Bot",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = localChatId,
                        onValueChange = { 
                            localChatId = it
                            viewModel.clearConnectionTestResult()
                        },
                        label = { Text("Telegram Channel / Chat ID") },
                        placeholder = { Text("-100XXXXXXXXXX") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { 
                            viewModel.setTelegramBotTokens(localTokens)
                            viewModel.setTelegramChatId(localChatId)
                        },
                        enabled = hasChanges,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm / Save Changes")
                    }

                    if (testResult != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (val res = testResult) {
                                ConnectionTestResult.Testing -> {
                                    com.inferno.gallery.ui.components.WavyProgressIndicator(
                                        modifier = Modifier.size(width = 32.dp, height = 20.dp),
                                        strokeWidth = 2.dp,
                                        amplitude = 3.dp,
                                        frequency = 1.5f
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Testing connection…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                ConnectionTestResult.Success -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Success! Connection verified.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                is ConnectionTestResult.Migrated -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Group upgraded to supergroup! Stored ID updated.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                is ConnectionTestResult.Error -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Failed: ${res.message}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                }
                                else -> {}
                            }
                        }
                    }

                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        headlineContent = { Text("Strip Location Metadata") },
                        supportingContent = { Text("Remove GPS coordinates from images before uploading") },
                        trailingContent = {
                            Switch(
                                checked = stripLocation,
                                onCheckedChange = { viewModel.setTelegramStripLocation(it) },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (stripLocation) Icons.Outlined.Check else Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                        headlineContent = { Text("Automated Background Backup") },
                        supportingContent = { Text("Backup new media in background based on criteria below") },
                        trailingContent = {
                            Switch(
                                checked = backupEnabled,
                                onCheckedChange = { viewModel.setTelegramBackupEnabled(it) },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (backupEnabled) Icons.Outlined.Check else Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (backupEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val wifiOnly by viewModel.telegramAutoBackupWifiOnly.collectAsState()
                            val batteryPause by viewModel.telegramAutoBackupBatteryLowPause.collectAsState()

                            ListItem(
                                leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                headlineContent = { Text("Choose Folders") },
                                supportingContent = {
                                    Text(
                                        if (selectedFolders.isEmpty()) "No folders selected (Nothing will backup)"
                                        else selectedFolders.joinToString(", ")
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFolderDialog.value = true },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            ListItem(
                                leadingContent = { Icon(Icons.Outlined.Wifi, contentDescription = null) },
                                headlineContent = { Text("Wi-Fi Only") },
                                supportingContent = { Text("Backup only when connected to an unmetered Wi-Fi network") },
                                trailingContent = {
                                    Switch(
                                        checked = wifiOnly,
                                        onCheckedChange = { viewModel.setTelegramAutoBackupWifiOnly(it) },
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (wifiOnly) Icons.Outlined.Check else Icons.Outlined.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            ListItem(
                                leadingContent = { Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null) },
                                headlineContent = { Text("Battery Saver Pause") },
                                supportingContent = { Text("Pause backup if battery is less than 35% (unless charging)") },
                                trailingContent = {
                                    Switch(
                                        checked = batteryPause,
                                        onCheckedChange = { viewModel.setTelegramAutoBackupBatteryLowPause(it) },
                                        thumbContent = {
                                            Icon(
                                                imageVector = if (batteryPause) Icons.Outlined.Check else Icons.Outlined.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }

                    if (showFolderDialog.value) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showFolderDialog.value = false },
                            title = { Text("Select Backup Folders") },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (allFolders.isEmpty()) {
                                        Text("No folders found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        allFolders.forEach { folder ->
                                            val isChecked = selectedFolders.contains(folder)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val updated = if (isChecked) {
                                                            selectedFolders - folder
                                                        } else {
                                                            selectedFolders + folder
                                                        }
                                                        viewModel.setTelegramAutoBackupFolders(updated)
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                androidx.compose.material3.Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        val updated = if (checked == true) {
                                                            selectedFolders + folder
                                                        } else {
                                                            selectedFolders - folder
                                                        }
                                                        viewModel.setTelegramAutoBackupFolders(updated)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(folder, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showFolderDialog.value = false }) {
                                    Text("Done")
                                }
                            }
                        )
                    }
                }
            }

            SettingsGroup(title = "Local AI Engine") {

                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    headlineContent = { Text("Text (OCR) Indexing") },
                    supportingContent = { 
                        Column {
                            Text("Index photos locally for text search")
                            
                            val dbIndexed = totalImagesCount - unindexedOcrImagesCount
                            val workerIndexed = ocrIndexWorkInfo?.progress?.getInt("progress", 0) ?: 0
                            val indexed = maxOf(dbIndexed, workerIndexed)
                            val total = if (totalImagesCount > 0) totalImagesCount else (ocrIndexWorkInfo?.progress?.getInt("total", 0) ?: 0)
                            
                            if (total > 0) {
                                Spacer(Modifier.height(8.dp))
                                val progressFloat = indexed.toFloat() / total.toFloat()
                                
                                LinearProgressIndicator(
                                    progress = { progressFloat },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (indexed == total) "Indexing complete ($total images)" else "Indexed $indexed of $total images", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailingContent = {
                        val isRunning = ocrIndexWorkInfo?.state == WorkInfo.State.RUNNING || ocrIndexWorkInfo?.state == WorkInfo.State.ENQUEUED
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isRunning) {
                                FilledTonalButton(onClick = { viewModel.stopOcrIndexing() }) {
                                    Text("Stop")
                                }
                            } else {
                                FilledTonalButton(onClick = { viewModel.startOcrIndexing() }) {
                                    Text("Start")
                                }
                                androidx.compose.material3.OutlinedButton(onClick = { viewModel.rebuildOcrIndex() }) {
                                    Text("Rebuild")
                                }
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
