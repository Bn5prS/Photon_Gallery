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
import com.inferno.gallery.ui.ThemeMode
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.toArgb
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.network.UpdateInfo
import com.inferno.gallery.update.UpdateManager
import com.inferno.gallery.update.UpdateNotificationHelper
import com.inferno.gallery.ui.components.UpdateDialog

class MainActivity : FragmentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val _viewIntentUri = kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>(null)
    val viewIntentUri: kotlinx.coroutines.flow.StateFlow<android.net.Uri?> = _viewIntentUri.asStateFlow()

    fun consumeViewIntentUri() {
        _viewIntentUri.value = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
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
                val twelveHours = 12 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastCheck > twelveHours) {
                    repo.updateLastUpdateCheckTimestamp(System.currentTimeMillis())
                    UpdateManager.checkForUpdates(lifecycleScope, manual = false)
                }
            }
        }

        setContent {
            val isLoading by settingsViewModel.isLoading.collectAsState()
            splashScreen.setKeepOnScreenCondition { isLoading }
            if (isLoading) return@setContent
            
            val useFullScreen by settingsViewModel.useFullScreen.collectAsState()

            val themeMode by settingsViewModel.themeMode.collectAsState()
            val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
            val colorPresetName by settingsViewModel.colorPresetName.collectAsState()
            val useAmoledBlack by settingsViewModel.useAmoledBlack.collectAsState()
            val useSystemFont by settingsViewModel.useSystemFont.collectAsState()
            val fontScale by settingsViewModel.fontScale.collectAsState()
            val secureRecentsEnabled by settingsViewModel.secureRecentsEnabled.collectAsState()
            val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()
            val hapticsStrength by settingsViewModel.hapticsStrength.collectAsState()
            val animateThemeTransitions by settingsViewModel.animateThemeTransitions.collectAsState()
            val motionStyle by settingsViewModel.motionStyle.collectAsState()
            
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

            LaunchedEffect(secureRecentsEnabled) {
                if (secureRecentsEnabled) {
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
                useAmoledBlack = useAmoledBlack,
                useSystemFont = useSystemFont,
                fontScale = fontScale,
                animateTransitions = animateThemeTransitions,
                motionStyle = motionStyle
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollFactory provides null
                ) {
                    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor
                ) {
                    NavigationGraph(
                        isLoading = isLoading,
                        settingsViewModel = settingsViewModel
                    )
                    UpdateDialog()
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        forcePeakDisplayRefreshRate()
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
        val action = intent?.action
        if (action == Intent.ACTION_VIEW ||
            action == "com.android.camera.action.REVIEW" ||
            action == android.provider.MediaStore.ACTION_REVIEW
        ) {
            val uri = intent.data
            if (uri != null) {
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
