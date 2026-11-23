package com.inferno.gallery

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.inferno.gallery.ui.NavigationGraph
import com.inferno.gallery.ui.theme.PhotonGalleryTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.viewModels
import com.inferno.gallery.ui.SettingsViewModel
import com.inferno.gallery.ui.GalleryViewModel
import com.inferno.gallery.ui.ThemeMode
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.toArgb
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.inferno.gallery.data.AppLockAuthManager
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.network.UpdateInfo
import com.inferno.gallery.update.UpdateManager
import com.inferno.gallery.update.UpdateNotificationHelper
import com.inferno.gallery.ui.components.AppLockOverlay
import com.inferno.gallery.ui.components.UpdateDialog

class MainActivity : FragmentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val galleryViewModel: GalleryViewModel by viewModels()
    private val appLockAuthManager = AppLockAuthManager()

    private val _viewIntentUri = kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>(null)
    val viewIntentUri: kotlinx.coroutines.flow.StateFlow<android.net.Uri?> = _viewIntentUri.asStateFlow()

    private val _isEditIntent = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isEditIntent: kotlinx.coroutines.flow.StateFlow<Boolean> = _isEditIntent.asStateFlow()

    fun consumeViewIntentUri() {
        _viewIntentUri.value = null
    }

    fun consumeEditIntent() {
        _isEditIntent.value = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val startTime = android.os.SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            settingsViewModel.isLoading.value ||
                (!galleryViewModel.isNativeTimelineLoaded.value && (android.os.SystemClock.uptimeMillis() - startTime < 800L))
        }
        super.onCreate(savedInstanceState)
        
        forcePeakDisplayRefreshRate()

        enableEdgeToEdge()

        handleUpdateIntent(intent)
        handleViewMediaIntent(intent)

        lifecycleScope.launch {
            val repo = SettingsRepository.getInstance(applicationContext)
            val autoCheck = repo.autoCheckUpdatesFlow.first()
            if (autoCheck) {
                val lastCheck = repo.lastUpdateCheckTimestampFlow.first()
                val fiveMinutes = 5 * 60 * 1000L
                if (System.currentTimeMillis() - lastCheck > fiveMinutes) {
                    repo.updateLastUpdateCheckTimestamp(System.currentTimeMillis())
                    UpdateManager.checkForUpdates(lifecycleScope, manual = false, context = applicationContext)
                }
            }
        }

        setContent {
            val isLoading by settingsViewModel.isLoading.collectAsState()
            val isTimelineLoaded by galleryViewModel.isNativeTimelineLoaded.collectAsState()
            val isWaitingForTimeline = !isTimelineLoaded && (android.os.SystemClock.uptimeMillis() - startTime < 800L)
            if (isLoading || isWaitingForTimeline) return@setContent
            
            val useFullScreen by settingsViewModel.useFullScreen.collectAsState()

            val themeMode by settingsViewModel.themeMode.collectAsState()
            val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
            val colorPresetName by settingsViewModel.colorPresetName.collectAsState()
            val useAmoledBlack by settingsViewModel.useAmoledBlack.collectAsState()
            val useSystemFont by settingsViewModel.useSystemFont.collectAsState()
            val fontScale by settingsViewModel.fontScale.collectAsState()
            val secureRecentsEnabled by settingsViewModel.secureRecentsEnabled.collectAsState()
            val appLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
            val appLockBiometricEnabled by settingsViewModel.appLockBiometricEnabled.collectAsState()
            val appLockTimeoutSeconds by settingsViewModel.appLockTimeoutSeconds.collectAsState()
            var isAppLockAuthenticated by rememberSaveable { mutableStateOf(!appLockEnabled) }
            var hasUnlockedOnce by rememberSaveable { mutableStateOf(!appLockEnabled) }
            var lastBackgroundedAtMs by rememberSaveable { mutableStateOf(0L) }
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(appLockEnabled) {
                if (!appLockEnabled) {
                    isAppLockAuthenticated = true
                    hasUnlockedOnce = true
                    lastBackgroundedAtMs = 0L
                }
            }

            DisposableEffect(lifecycleOwner, appLockTimeoutSeconds, appLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (this@MainActivity.isChangingConfigurations) return@LifecycleEventObserver
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (appLockTimeoutSeconds <= 0) {
                                isAppLockAuthenticated = false
                                lastBackgroundedAtMs = 0L
                            } else if (isAppLockAuthenticated) {
                                lastBackgroundedAtMs = android.os.SystemClock.elapsedRealtime()
                            }
                        }
                        Lifecycle.Event.ON_START -> {
                            if (appLockEnabled && appLockTimeoutSeconds > 0 && isAppLockAuthenticated && lastBackgroundedAtMs > 0L) {
                                val elapsedMs = android.os.SystemClock.elapsedRealtime() - lastBackgroundedAtMs
                                if (elapsedMs >= appLockTimeoutSeconds * 1000L) {
                                    isAppLockAuthenticated = false
                                }
                            }
                            lastBackgroundedAtMs = 0L
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val isAppLocked = appLockEnabled &&
                appLockAuthManager.isPinConfigured(this@MainActivity) &&
                !isAppLockAuthenticated

            val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()
            val hapticsStrength by settingsViewModel.hapticsStrength.collectAsState()
            val animateThemeTransitions by settingsViewModel.animateThemeTransitions.collectAsState()
            val motionStyle by settingsViewModel.motionStyle.collectAsState()
            val themePaletteStyle by settingsViewModel.themePaletteStyle.collectAsState()
            val appSeedColor by settingsViewModel.appSeedColor.collectAsState()
            
            com.inferno.gallery.ui.utils.PremiumHapticsManager.enabled = hapticsEnabled
            com.inferno.gallery.ui.utils.PremiumHapticsManager.strength = hapticsStrength

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(isDark, useFullScreen) {
                val style = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)

                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (useFullScreen) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            val isVaultUnlocked by galleryViewModel.isVaultUnlocked.collectAsState()

            LaunchedEffect(secureRecentsEnabled, isAppLocked, isVaultUnlocked) {
                if (secureRecentsEnabled || isAppLocked || isVaultUnlocked) {
                    window.setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SECURE,
                        android.view.WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            PhotonGalleryTheme(
                darkTheme = isDark,
                dynamicColor = useMaterialYou,
                colorPresetName = colorPresetName,
                paletteStyleName = themePaletteStyle,
                appSeedColor = appSeedColor,
                useAmoledBlack = useAmoledBlack,
                useSystemFont = useSystemFont,
                fontScale = fontScale,
                animateTransitions = animateThemeTransitions,
                motionStyle = motionStyle
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollFactory provides null
                ) {
                    val backgroundColor = MaterialTheme.colorScheme.surface
                    var hasSyncedTaskDescription by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(backgroundColor) {
                        val opaqueSurfaceArgb = backgroundColor.toArgb() or 0xFF000000.toInt()
                        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(opaqueSurfaceArgb))
                        if (hasSyncedTaskDescription) {
                            kotlinx.coroutines.delay(120L)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            setTaskDescription(
                                android.app.ActivityManager.TaskDescription.Builder()
                                    .setBackgroundColor(opaqueSurfaceArgb)
                                    .setPrimaryColor(opaqueSurfaceArgb)
                                    .build()
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            setTaskDescription(
                                android.app.ActivityManager.TaskDescription(null, 0, opaqueSurfaceArgb)
                            )
                        }
                        hasSyncedTaskDescription = true
                    }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (hasUnlockedOnce || !isAppLocked) {
                            NavigationGraph(
                                isLoading = isLoading,
                                settingsViewModel = settingsViewModel
                            )
                        }
                        if (!isAppLocked) {
                            UpdateDialog()
                        }
                        if (isAppLocked) {
                            AppLockOverlay(
                                authManager = appLockAuthManager,
                                biometricEnabled = appLockBiometricEnabled,
                                onUnlocked = {
                                    hasUnlockedOnce = true
                                    isAppLockAuthenticated = true
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        forcePeakDisplayRefreshRate()
        triggerSyncOnResume()
    }

    private fun triggerSyncOnResume() {
        try {
            // One fast incremental sync on resume. The extra worker enqueued here with
            // REPLACE used to cancel whatever sync was running (losing minutes of work) and
            // start another full reconcile on every return to the app.
            galleryViewModel.runFastInitialSync()
        } catch (_: Exception) {}
    }

    private fun forcePeakDisplayRefreshRate() {
        try {
            val peakMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            if (peakMode != null && peakMode.refreshRate > 60f) {
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = peakMode.modeId
                layoutParams.preferredRefreshRate = peakMode.refreshRate
                try {
                    val minField = layoutParams.javaClass.getField("preferredMinDisplayRefreshRate")
                    val maxField = layoutParams.javaClass.getField("preferredMaxDisplayRefreshRate")
                    minField.set(layoutParams, peakMode.refreshRate)
                    maxField.set(layoutParams, peakMode.refreshRate)
                } catch (_: Throwable) {
                    // Fallback if hidden fields are absent on specific OEM Android builds
                }
                window.attributes = layoutParams
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to force peak refresh rate", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
        handleViewMediaIntent(intent)
    }

    private fun handleViewMediaIntent(intent: Intent?) {
        if (intent == null) return
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return

        val action = intent.action
        val type = intent.type
        val uri = intent.data ?: intent.clipData?.let { clip ->
            if (clip.itemCount > 0) clip.getItemAt(0).uri else null
        }

        // If intent is opening a media collection directory (e.g. from camera app tapping gallery shortcut,
        // or system gallery directory intent), we stay on the gallery overview grid (Photos tab).
        if (type?.startsWith("vnd.android.cursor.dir") == true ||
            uri == android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI ||
            uri == android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI ||
            uri?.toString() == "content://media/external/images/media" ||
            uri?.toString() == "content://media/external/video/media"
        ) {
            _viewIntentUri.value = null
            return
        }

        val isEdit = action == Intent.ACTION_EDIT || intent.getBooleanExtra("open_editor", false)
        if (action == Intent.ACTION_VIEW ||
            action == Intent.ACTION_EDIT ||
            action == "com.android.camera.action.REVIEW" ||
            action == android.provider.MediaStore.ACTION_REVIEW
        ) {
            if (uri != null) {
                // Force emission even if the exact same URI is tapped twice externally before clearing
                _viewIntentUri.value = null
                _isEditIntent.value = isEdit
                _viewIntentUri.value = uri
            }
        }
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("open_update_dialog", false) == true) {
            val latestVersion = intent.getStringExtra("update_version") ?: return
            val releaseTitle = intent.getStringExtra("update_title") ?: ""
            val changelog = intent.getStringExtra("update_changelog") ?: ""
            val downloadUrl = intent.getStringExtra("update_download_url")
            val apkFileName = intent.getStringExtra("update_apk_name")
            val apkSizeBytes = intent.getLongExtra("update_apk_size", 0L)
            val releasePageUrl = intent.getStringExtra("update_release_url") ?: "https://github.com/Bn5prS/Photon_Gallery/releases/latest"

            val info = UpdateInfo(
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = latestVersion,
                releaseTitle = releaseTitle,
                changelog = changelog,
                downloadUrl = downloadUrl,
                apkFileName = apkFileName,
                apkSizeBytes = apkSizeBytes,
                releasePageUrl = releasePageUrl,
                isAvailable = true
            )
            UpdateManager.setUpdateAvailable(info)
            UpdateNotificationHelper.dismissNotification(this)
        }
    }
}
