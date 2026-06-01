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
                        // 25% keeps the grid bitmap pool within GC-safe bounds during fast scroll.
                        // The previous 50% caused GC storms at 120 Hz. Do not increase.
                        .maxSizePercent(ctx, 0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(ctx.cacheDir.resolve("image_cache").toOkioPath())
                        // 300 MB is sufficient for a 4-column grid with 1000+ items.
                        .maxSizeBytes(300L * 1024 * 1024)
                        .build()
                }
                // Do NOT set crossfade globally. Grid items skip crossfade for instant display
                // from cache. Detail screen enables it per-request for its own transitions.
                .build()
        }

        setContent {
            val isLoading by settingsViewModel.isLoading.collectAsState()
            splashScreen.setKeepOnScreenCondition { isLoading }
            

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
