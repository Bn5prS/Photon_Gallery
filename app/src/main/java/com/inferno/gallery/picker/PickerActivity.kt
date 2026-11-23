package com.inferno.gallery.picker

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.inferno.gallery.data.AppLockAuthManager
import com.inferno.gallery.ui.SettingsViewModel
import com.inferno.gallery.ui.ThemeMode
import com.inferno.gallery.ui.components.AppLockOverlay
import com.inferno.gallery.ui.theme.PhotonGalleryTheme


/**
 * System Media Picker Activity supporting:
 * - android.intent.action.GET_CONTENT
 * - android.intent.action.PICK
 *
 * Honors EXTRA_ALLOW_MULTIPLE, filters by MIME type, grants FLAG_GRANT_READ_URI_PERMISSION,
 * and returns results via Intent data or ClipData.
 */
class PickerActivity : FragmentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val appLockAuthManager = AppLockAuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        val mimeType = intent.type ?: "*/*"

        setContent {
            val isLoading by settingsViewModel.isLoading.collectAsState()
            if (isLoading) return@setContent

            val themeMode by settingsViewModel.themeMode.collectAsState()
            val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
            val colorPresetName by settingsViewModel.colorPresetName.collectAsState()
            val useAmoledBlack by settingsViewModel.useAmoledBlack.collectAsState()
            val useSystemFont by settingsViewModel.useSystemFont.collectAsState()
            val fontScale by settingsViewModel.fontScale.collectAsState()
            val secureRecentsEnabled by settingsViewModel.secureRecentsEnabled.collectAsState()
            val appLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
            val appLockBiometricEnabled by settingsViewModel.appLockBiometricEnabled.collectAsState()

            var isAppLockAuthenticated by rememberSaveable { mutableStateOf(!appLockEnabled) }
            var hasUnlockedOnce by rememberSaveable { mutableStateOf(!appLockEnabled) }
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(appLockEnabled) {
                if (!appLockEnabled) {
                    isAppLockAuthenticated = true
                    hasUnlockedOnce = true
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP && !this@PickerActivity.isChangingConfigurations) {
                        isAppLockAuthenticated = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val isAppLocked = appLockEnabled &&
                appLockAuthManager.isPinConfigured(this@PickerActivity) &&
                !isAppLockAuthenticated

            LaunchedEffect(secureRecentsEnabled, isAppLocked) {
                if (secureRecentsEnabled || isAppLocked) {
                    window.setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SECURE,
                        android.view.WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            PhotonGalleryTheme(
                darkTheme = isDark,
                dynamicColor = useMaterialYou,
                colorPresetName = colorPresetName,
                useAmoledBlack = useAmoledBlack,
                useSystemFont = useSystemFont,
                fontScale = fontScale,
            ) {
                val backgroundColor = MaterialTheme.colorScheme.surface
                LaunchedEffect(backgroundColor) {
                    val opaqueSurfaceArgb = backgroundColor.toArgb() or 0xFF000000.toInt()
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(opaqueSurfaceArgb))
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
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    if (hasUnlockedOnce || !isAppLocked) {
                        PickerScreen(
                            allowMultiple = allowMultiple,
                            mimeType = mimeType,
                            onMediaSelected = { uris -> returnResults(uris) },
                            onCancel = {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        )
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

    private fun returnResults(uris: List<Uri>) {
        if (uris.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val resultIntent = Intent().apply {
            data = uris.first()
            if (uris.size > 1) {
                val clipData = ClipData.newUri(contentResolver, "Selected Media", uris.first())
                for (i in 1 until uris.size) {
                    clipData.addItem(ClipData.Item(uris[i]))
                }
                this.clipData = clipData
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
