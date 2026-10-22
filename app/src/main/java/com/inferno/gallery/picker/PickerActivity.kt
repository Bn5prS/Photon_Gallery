package com.inferno.gallery.picker

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.inferno.gallery.ui.SettingsViewModel
import com.inferno.gallery.ui.ThemeMode
import com.inferno.gallery.ui.theme.PhotonGalleryTheme


/**
 * System Media Picker Activity supporting:
 * - android.intent.action.GET_CONTENT
 * - android.intent.action.PICK
 *
 * Honors EXTRA_ALLOW_MULTIPLE, filters by MIME type, grants FLAG_GRANT_READ_URI_PERMISSION,
 * and returns results via Intent data or ClipData.
 */
class PickerActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        val mimeType = intent.type ?: "*/*"

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
            val colorPresetName by settingsViewModel.colorPresetName.collectAsState()
            val useAmoledBlack by settingsViewModel.useAmoledBlack.collectAsState()
            val useSystemFont by settingsViewModel.useSystemFont.collectAsState()
            val fontScale by settingsViewModel.fontScale.collectAsState()

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
