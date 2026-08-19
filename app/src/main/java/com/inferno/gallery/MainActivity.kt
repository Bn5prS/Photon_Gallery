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
import com.inferno.gallery.ui.ThemeMode
import com.inferno.gallery.ui.SettingsViewModel
import com.materialkolor.PaletteStyle
import com.inferno.gallery.ui.theme.contrastPresetToDouble
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

class MainActivity : FragmentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        
        // Force display to its peak supported refresh rate (90Hz, 120Hz, etc.) for fluid scrolling
        try {
            val peakRefreshRate = display?.supportedModes
                ?.map { it.refreshRate }
                ?.maxOrNull() ?: 60f
            if (peakRefreshRate > 60f) {
                val layoutParams = window.attributes
                try {
                    val minField = layoutParams.javaClass.getField("preferredMinDisplayRefreshRate")
                    val maxField = layoutParams.javaClass.getField("preferredMaxDisplayRefreshRate")
                    minField.set(layoutParams, peakRefreshRate)
                    maxField.set(layoutParams, peakRefreshRate)
                    window.attributes = layoutParams
                } catch (noSuchField: NoSuchFieldException) {
                    // Fallback for older devices/APIs if fields don't exist
                    android.util.Log.w("MainActivity", "preferredMinDisplayRefreshRate fields not found on this SDK version")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to force peak refresh rate", e)
        }

        enableEdgeToEdge()

        setContent {
            val isLoading by settingsViewModel.isLoading.collectAsState()
            splashScreen.setKeepOnScreenCondition { isLoading }
            
            val useFullScreen by settingsViewModel.useFullScreen.collectAsState()

            val themeMode by settingsViewModel.themeMode.collectAsState()
            val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
            val useAmoledBlack by settingsViewModel.useAmoledBlack.collectAsState()
            
            val appSeedColor by settingsViewModel.appSeedColor.collectAsState()
            val themePaletteStyleStr by settingsViewModel.themePaletteStyle.collectAsState()
            val themeContrastLevel by settingsViewModel.themeContrastLevel.collectAsState()
            val invertThemeColors by settingsViewModel.invertThemeColors.collectAsState()
            val useSystemFont by settingsViewModel.useSystemFont.collectAsState()
            val secureRecentsEnabled by settingsViewModel.secureRecentsEnabled.collectAsState()
            val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()
            val hapticsStrength by settingsViewModel.hapticsStrength.collectAsState()
            val secondaryColorOverride by settingsViewModel.secondaryColorOverride.collectAsState()
            val tertiaryColorOverride by settingsViewModel.tertiaryColorOverride.collectAsState()
            val animateThemeTransitions by settingsViewModel.animateThemeTransitions.collectAsState()
            
            com.inferno.gallery.ui.utils.PremiumHapticsManager.enabled = hapticsEnabled
            com.inferno.gallery.ui.utils.PremiumHapticsManager.strength = hapticsStrength

            val themePaletteStyle = runCatching { PaletteStyle.valueOf(themePaletteStyleStr) }
                .getOrDefault(PaletteStyle.TonalSpot)

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
                useAmoledBlack = useAmoledBlack,
                appSeedColor = appSeedColor,
                paletteStyle = themePaletteStyle,
                contrastLevel = themeContrastLevel.toDouble(),
                invertColors = invertThemeColors,
                useSystemFont = useSystemFont,
                secondaryColorOverride = secondaryColorOverride,
                tertiaryColorOverride = tertiaryColorOverride,
                animateTransitions = animateThemeTransitions
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollFactory provides null
                ) {
                    val backgroundColor = MaterialTheme.colorScheme.background

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor
                ) {
                    NavigationGraph(
                        isLoading = isLoading,
                        settingsViewModel = settingsViewModel
                    )
                }
                }
            }
        }
    }
}
