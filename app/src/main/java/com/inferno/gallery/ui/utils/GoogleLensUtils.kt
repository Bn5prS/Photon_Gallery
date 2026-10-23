package com.inferno.gallery.ui.utils

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object GoogleLensUtils {
    const val LENS_PACKAGE = "com.google.ar.lens"
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

    const val GOOGLE_APP_LENS_ACTIVITY = "com.google.android.apps.search.lens.LensShareEntryPointActivity"
    const val LENS_STANDALONE_ACTIVITY = "com.google.vr.apps.ornament.app.lens.LensLauncherActivity"

    fun createGoogleAppDirectIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("image", uri)
            component = ComponentName(GOOGLE_APP_PACKAGE, GOOGLE_APP_LENS_ACTIVITY)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createLensStandaloneDirectIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("image", uri)
            component = ComponentName(LENS_PACKAGE, LENS_STANDALONE_ACTIVITY)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createLensIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("image", uri)
            `package` = LENS_PACKAGE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createGoogleAppIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google://lens")
            `package` = GOOGLE_APP_PACKAGE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun launch(context: Context, uri: Uri) {
        // 1. Direct Google App Lens Share entrypoint (analyzes image immediately with AI overview)
        try {
            context.startActivity(createGoogleAppDirectIntent(uri))
            return
        } catch (_: Exception) {}

        // 2. Direct Standalone Google Lens entrypoint
        try {
            context.startActivity(createLensStandaloneDirectIntent(uri))
            return
        } catch (_: Exception) {}

        // 3. Fallback: package-directed ACTION_SEND to Google Lens
        try {
            context.startActivity(createLensIntent(uri))
            return
        } catch (_: Exception) {}

        // 4. Fallback: deep-link to Google App Lens
        try {
            context.startActivity(createGoogleAppIntent(uri))
            return
        } catch (_: Exception) {}

        Toast.makeText(context, "Google Lens is not installed", Toast.LENGTH_SHORT).show()
    }
}
