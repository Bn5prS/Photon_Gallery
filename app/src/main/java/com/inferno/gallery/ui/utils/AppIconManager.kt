package com.inferno.gallery.ui.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.inferno.gallery.R

enum class AppIconOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val aliasName: String,
    val bgRes: Int?,
    val fgRes: Int
) {
    DEFAULT(
        id = "default",
        title = "Photon (Default)",
        subtitle = "Official modern Photon Gallery icon",
        aliasName = "com.inferno.gallery.MainActivityDefault",
        bgRes = null,
        fgRes = R.drawable.launcher_icon_fg
    ),
    CLASSIC(
        id = "classic",
        title = "Classic Gallery",
        subtitle = "Standard photo album & landscape design",
        aliasName = "com.inferno.gallery.MainActivityClassic",
        bgRes = R.drawable.ic_launcher_classic_bg,
        fgRes = R.drawable.ic_launcher_classic_fg
    ),
    MINIMAL(
        id = "minimal",
        title = "Minimalist Dark",
        subtitle = "Stealth dark camera aperture ring",
        aliasName = "com.inferno.gallery.MainActivityMinimal",
        bgRes = R.drawable.ic_launcher_minimal_bg,
        fgRes = R.drawable.ic_launcher_minimal_fg
    ),
    VIBRANT(
        id = "vibrant",
        title = "Vibrant Prism",
        subtitle = "Expressive multi-colored photo shutter",
        aliasName = "com.inferno.gallery.MainActivityVibrant",
        bgRes = R.drawable.ic_launcher_vibrant_bg,
        fgRes = R.drawable.ic_launcher_vibrant_fg
    );

    companion object {
        fun fromId(id: String?): AppIconOption {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}

object AppIconManager {
    fun getCurrentIcon(context: Context): AppIconOption {
        val pm = context.packageManager
        for (option in AppIconOption.entries) {
            val component = ComponentName(context, option.aliasName)
            val state = pm.getComponentEnabledSetting(component)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return option
            }
        }
        return AppIconOption.DEFAULT
    }

    fun applyAppIcon(context: Context, target: AppIconOption) {
        val pm = context.packageManager
        // 1. Enable the desired target alias first
        val targetComponent = ComponentName(context, target.aliasName)
        try {
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            android.util.Log.e("AppIconManager", "Failed to enable alias ${target.aliasName}: ${e.message}")
        }

        // 2. Disable all other aliases
        for (option in AppIconOption.entries) {
            if (option != target) {
                val component = ComponentName(context, option.aliasName)
                try {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AppIconManager", "Failed to disable alias ${option.aliasName}: ${e.message}")
                }
            }
        }
    }
}
