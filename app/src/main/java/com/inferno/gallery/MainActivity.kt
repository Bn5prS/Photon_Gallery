package com.inferno.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.inferno.gallery.ui.NavigationGraph
import com.inferno.gallery.ui.theme.PhotonGalleryTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.viewModels
import com.inferno.gallery.ui.ThemeMode
import com.inferno.gallery.ui.SettingsViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import okio.Path.Companion.toOkioPath
import coil3.request.crossfade
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        SingletonImageLoader.setSafe { ctx ->
            ImageLoader.Builder(ctx)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(ctx, 0.50)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(ctx.cacheDir.resolve("image_cache").toOkioPath())
                        .maxSizeBytes(500L * 1024 * 1024)
                        .build()
                }
                .crossfade(false)
                .build()
        }

        setContent {
            val isLoading by settingsViewModel.isLoading.collectAsState()
            splashScreen.setKeepOnScreenCondition { isLoading }
            
            val useFullScreen by settingsViewModel.useFullScreen.collectAsState()

            LaunchedEffect(useFullScreen) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (useFullScreen) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            val themeMode by settingsViewModel.themeMode.collectAsState()
            val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
            val useAmoledBlack by settingsViewModel.useAmoledBlack.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            PhotonGalleryTheme(
                darkTheme = isDark,
                dynamicColor = useMaterialYou,
                useAmoledBlack = useAmoledBlack
            ) {
                NavigationGraph(
                    isLoading = isLoading,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
