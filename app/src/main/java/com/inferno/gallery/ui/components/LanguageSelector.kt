package com.inferno.gallery.ui.components

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.SpacingTokens
import java.util.Locale

data class SupportedLanguage(
    val tag: String,
    val displayName: String,
    val flagEmoji: String
)

val APP_SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage("", "System Default", "🌐"),
    SupportedLanguage("en", "English", "🇺🇸"),
    SupportedLanguage("es", "Español", "🇪🇸"),
    SupportedLanguage("pt-BR", "Português (Brasil)", "🇧🇷"),
    SupportedLanguage("fr", "Français", "🇫🇷"),
    SupportedLanguage("de", "Deutsch", "🇩🇪")
)

fun getCurrentAppLanguageTag(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val locales = localeManager?.applicationLocales
        if (locales != null && !locales.isEmpty) {
            locales[0].toLanguageTag()
        } else {
            ""
        }
    } else {
        context.resources.configuration.locales[0]?.toLanguageTag() ?: ""
    }
}

fun isLanguageSelected(currentTag: String, langTag: String): Boolean {
    if (langTag.isEmpty()) {
        return currentTag.isEmpty()
    }
    if (currentTag.isEmpty()) {
        return false
    }
    return currentTag.equals(langTag, ignoreCase = true) ||
            currentTag.startsWith("$langTag-", ignoreCase = true) ||
            langTag.startsWith("$currentTag-", ignoreCase = true) ||
            currentTag.replace('-', '_').equals(langTag.replace('-', '_'), ignoreCase = true)
}

fun setAppLanguage(context: Context, languageTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val localeList = if (languageTag.isEmpty()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(languageTag)
        }
        localeManager?.applicationLocales = localeList
    } else {
        val locale = if (languageTag.isEmpty()) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageTag)
        }
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    // Recreate top activity to ensure immediate full-tree recomposition with new resources
    (context as? Activity)?.recreate()
}

fun getActiveLanguage(context: Context, currentTag: String): SupportedLanguage {
    if (currentTag.isNotEmpty()) {
        val matched = APP_SUPPORTED_LANGUAGES.find { isLanguageSelected(currentTag, it.tag) }
        if (matched != null) return matched
    }
    // If system default or unmatched, match against device configuration
    val sysLocales = context.resources.configuration.locales
    val primarySysTag = if (sysLocales.size() > 0) sysLocales[0].toLanguageTag() else ""
    return APP_SUPPORTED_LANGUAGES.filter { it.tag.isNotEmpty() }
        .find { isLanguageSelected(primarySysTag, it.tag) }
        ?: APP_SUPPORTED_LANGUAGES[1] // Default to English
}

@Composable
fun OnboardingLanguagePill(
    currentLanguageTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeLang = getActiveLanguage(context, currentLanguageTag)
    val pillName = if (activeLang.tag == "pt-BR") "Português" else activeLang.displayName

    Surface(
        onClick = onClick,
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
        ) {
            Text(
                text = "🌐",
                fontSize = 14.sp
            )
            Text(
                text = pillName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguageTag: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ShapeExtraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
            ) {
                APP_SUPPORTED_LANGUAGES.forEach { lang ->
                    val isSelected = isLanguageSelected(currentLanguageTag, lang.tag)
                    val displayName = if (lang.tag.isEmpty()) {
                        stringResource(R.string.language_system_default)
                    } else {
                        lang.displayName
                    }

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelectLanguage(lang.tag)
                            onDismiss()
                        },
                        shape = ShapeLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.M),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
                            ) {
                                Text(
                                    text = lang.flagEmoji,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(IconSizeTokens.M)
                                )
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = SpacingTokens.XS))
                    TextButton(
                        onClick = {
                            onDismiss()
                            try {
                                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "System Language Settings ↗",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_close),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    )
}

