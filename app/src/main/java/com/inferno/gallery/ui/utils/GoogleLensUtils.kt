package com.inferno.gallery.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object GoogleLensUtils {
    const val LENS_PACKAGE = "com.google.ar.lens"
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

    fun createLensIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            `package` = LENS_PACKAGE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createGoogleAppIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            `package` = GOOGLE_APP_PACKAGE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun launch(context: Context, uri: Uri) {
        val lensIntent = createLensIntent(uri)
        try {
            context.startActivity(lensIntent)
            return
        } catch (_: Exception) {}

        try {
            val googleAppIntent = createGoogleAppIntent(uri)
            context.startActivity(googleAppIntent)
            return
        } catch (_: Exception) {}

        Toast.makeText(context, "Google Lens is not installed", Toast.LENGTH_SHORT).show()
    }
}
