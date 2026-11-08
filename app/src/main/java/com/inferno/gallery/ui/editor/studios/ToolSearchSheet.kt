package com.inferno.gallery.ui.editor.studios

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.model.EditorTab

/**
 * Representation of an individual tool in the editor tool catalog.
 */
data class EditorToolItem(
    val id: String,
    val name: String,
    val description: String,
    @param:DrawableRes val iconRes: Int,
    val tab: EditorTab,
    val keywords: List<String> = emptyList()
)

/**
 * Catalog of all editing tools available in Photo Editor with rich search indexing.
 */
object ToolSearchCatalog {
    val ALL_TOOLS: List<EditorToolItem> = listOf(
        // ── Auto Suggestions ──
        EditorToolItem(
            id = "auto_enhance",
            name = "Enhance",
            description = "Automatic image-adaptive exposure, contrast and color correction",
            iconRes = R.drawable.ic_ms_auto_fix_high,
            tab = EditorTab.AUTO,
            keywords = listOf("auto", "enhance", "fix", "magic", "smart", "one-tap", "automatic")
        ),
        EditorToolItem(
            id = "auto_dynamic",
            name = "Dynamic",
            description = "Boost shadow details, vibrance and local dynamic tone",
            iconRes = R.drawable.ic_ms_auto_fix_high,
            tab = EditorTab.AUTO,
            keywords = listOf("dynamic", "hdr", "vibrance", "shadow boost", "punch")
        ),

        // ── Crop Studio Tools ──
        EditorToolItem(
            id = "crop_aspect",
            name = "Crop & Aspect Ratio",
            description = "Freeform, square, and standard aspect ratio cropping",
            iconRes = R.drawable.ic_ms_crop,
            tab = EditorTab.CROP,
            keywords = listOf("crop", "aspect", "ratio", "framing", "cut", "resize", "square", "16:9", "4:3")
        ),
        EditorToolItem(
            id = "straighten",
            name = "Straighten",
            description = "Level horizon and adjust tilt angle (-45° to +45°)",
            iconRes = R.drawable.ic_ms_straighten,
            tab = EditorTab.CROP,
            keywords = listOf("straighten", "level", "horizon", "angle", "tilt", "ruler", "framing")
        ),
        EditorToolItem(
            id = "rotate",
            name = "Rotate",
            description = "Rotate image 90 degrees counter-clockwise",
            iconRes = R.drawable.ic_ms_rotate_90_degrees_ccw,
            tab = EditorTab.CROP,
            keywords = listOf("rotate", "turn", "orientation", "ccw", "spin")
        ),
        EditorToolItem(
            id = "mirror",
            name = "Mirror",
            description = "Flip image horizontally",
            iconRes = R.drawable.ic_ms_flip,
            tab = EditorTab.CROP,
            keywords = listOf("mirror", "flip", "invert", "horizontal flip", "reverse")
        ),
        EditorToolItem(
            id = "perspective",
            name = "Perspective",
            description = "Correct vertical and horizontal keystone perspective",
            iconRes = R.drawable.ic_ms_perspective,
            tab = EditorTab.CROP,
            keywords = listOf("perspective", "keystone", "tilt", "skew", "geometry", "vertical", "horizontal")
        ),
        EditorToolItem(
            id = "reset_crop",
            name = "Reset crop",
            description = "Reset crop boundaries, rotation and straighten",
            iconRes = R.drawable.ic_ms_refresh,
            tab = EditorTab.CROP,
            keywords = listOf("reset", "clear", "revert", "original", "undo crop")
        ),

        // ── Adjust Studio Tools (13) ──
        EditorToolItem(
            id = "brightness",
            name = "Brightness",
            description = "Adjust the overall lightness or darkness",
            iconRes = R.drawable.ic_ms_light_mode,
            tab = EditorTab.ADJUST,
            keywords = listOf("brightness", "light", "dark", "exposure", "illumination")
        ),
        EditorToolItem(
            id = "contrast",
            name = "Contrast",
            description = "Adjust the difference between light and dark areas",
            iconRes = R.drawable.ic_ms_contrast,
            tab = EditorTab.ADJUST,
            keywords = listOf("contrast", "punch", "pop", "range", "dynamics")
        ),
        EditorToolItem(
            id = "tone",
            name = "Tone HDR",
            description = "Enhance dynamic range and local contrast",
            iconRes = R.drawable.ic_ms_auto_fix_high,
            tab = EditorTab.ADJUST,
            keywords = listOf("tone", "hdr", "dynamic range", "local contrast", "clarity", "details")
        ),
        EditorToolItem(
            id = "white_point",
            name = "White point",
            description = "Adjust the brightest parts of the image",
            iconRes = R.drawable.ic_ms_circle,
            tab = EditorTab.ADJUST,
            keywords = listOf("white point", "white", "peak brightness", "blown out", "clipping")
        ),
        EditorToolItem(
            id = "highlights",
            name = "Highlights",
            description = "Recover or boost bright details",
            iconRes = R.drawable.ic_ms_brightness_medium,
            tab = EditorTab.ADJUST,
            keywords = listOf("highlights", "bright details", "sky", "recover", "glare")
        ),
        EditorToolItem(
            id = "shadows",
            name = "Shadows",
            description = "Lighten or deepen dark details",
            iconRes = R.drawable.ic_ms_brightness_low,
            tab = EditorTab.ADJUST,
            keywords = listOf("shadows", "dark details", "lift shadows", "underexposed")
        ),
        EditorToolItem(
            id = "black_point",
            name = "Black point",
            description = "Adjust the darkest parts of the image",
            iconRes = R.drawable.ic_ms_circle,
            tab = EditorTab.ADJUST,
            keywords = listOf("black point", "black", "pure black", "shadow floor", "crush")
        ),
        EditorToolItem(
            id = "saturation",
            name = "Saturation",
            description = "Make colors more or less vibrant",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.ADJUST,
            keywords = listOf("saturation", "vibrancy", "color intensity", "vivid", "colorful")
        ),
        EditorToolItem(
            id = "warmth",
            name = "Warmth",
            description = "Make the image appear warmer or cooler",
            iconRes = R.drawable.ic_ms_wb_twilight,
            tab = EditorTab.ADJUST,
            keywords = listOf("warmth", "temperature", "cool", "kelvin", "golden", "amber", "blue")
        ),
        EditorToolItem(
            id = "tint",
            name = "Tint",
            description = "Adjust color balance between magenta and green",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.ADJUST,
            keywords = listOf("tint", "green", "magenta", "color balance", "white balance")
        ),
        EditorToolItem(
            id = "skin_tone",
            name = "Skin tone",
            description = "Adjust warmth and exposure specifically for skin tones",
            iconRes = R.drawable.ic_ms_person,
            tab = EditorTab.ADJUST,
            keywords = listOf("skin tone", "skin", "portrait", "face", "complexion")
        ),
        EditorToolItem(
            id = "blue_tone",
            name = "Blue tone",
            description = "Make skies and blues more vivid or softened",
            iconRes = R.drawable.ic_ms_cloud,
            tab = EditorTab.ADJUST,
            keywords = listOf("blue tone", "sky", "water", "ocean", "blue", "cyan")
        ),
        EditorToolItem(
            id = "vignette",
            name = "Vignette",
            description = "Darken the edges of the image",
            iconRes = R.drawable.ic_ms_center_focus_strong,
            tab = EditorTab.ADJUST,
            keywords = listOf("vignette", "edges", "border", "radial", "fade", "corner darkening")
        ),

        // ── Filter Presets ──
        EditorToolItem(
            id = "filters_overview",
            name = "Filters",
            description = "Apply color grading presets with adjustable strength",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("filters", "presets", "styles", "color grade", "lut")
        ),
        EditorToolItem(
            id = "filter_original",
            name = "Original",
            description = "Unfiltered natural photo",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("original", "none", "natural", "reset filter")
        ),
        EditorToolItem(
            id = "filter_lite",
            name = "Lite",
            description = "Subtle exposure lift, slight desaturation, clean film look",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("lite", "film", "clean", "subtle", "soft")
        ),
        EditorToolItem(
            id = "filter_playa",
            name = "Playa",
            description = "Warm golden hour warmth, amber highlights, gentle fade",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("playa", "warm", "golden hour", "amber", "beach")
        ),
        EditorToolItem(
            id = "filter_honey",
            name = "Honey",
            description = "Rich warm tones, deep contrast, honey-tinted midtones",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("honey", "warm", "rich", "golden", "contrast")
        ),
        EditorToolItem(
            id = "filter_isla",
            name = "Isla",
            description = "Vibrant tropical blues, crisp contrast, saturated greens",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("isla", "tropical", "vibrant", "blue", "green", "vacation")
        ),
        EditorToolItem(
            id = "filter_desert",
            name = "Desert",
            description = "Muted greens, boosted ochre/sand tones, matte shadows",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("desert", "sand", "ochre", "muted", "matte")
        ),
        EditorToolItem(
            id = "filter_clay",
            name = "Clay",
            description = "Earthy terracotta reds, softened highlights",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("clay", "terracotta", "red", "earthy")
        ),
        EditorToolItem(
            id = "filter_palma",
            name = "Palma",
            description = "Clean Mediterranean light, balanced contrast, vivid cyan sky",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("palma", "mediterranean", "cyan", "sky", "clean")
        ),
        EditorToolItem(
            id = "filter_metro",
            name = "Metro",
            description = "Urban cool tones, blue cast in shadows, punchy contrast",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("metro", "urban", "cool", "city", "blue shadows")
        ),
        EditorToolItem(
            id = "filter_eiffel",
            name = "Eiffel",
            description = "Classic moody French film, gentle magenta/sepia warmth",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("eiffel", "film", "sepia", "paris", "vintage", "moody")
        ),
        EditorToolItem(
            id = "filter_modena",
            name = "Modena",
            description = "High contrast, deep blacks, saturated primaries",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("modena", "high contrast", "deep blacks", "saturated", "bold")
        ),
        EditorToolItem(
            id = "filter_breeze",
            name = "Breeze",
            description = "Airy pastel tones, lifted shadows, soft highlight glow",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("breeze", "pastel", "airy", "soft", "glow")
        ),
        EditorToolItem(
            id = "filter_vogue",
            name = "Vogue",
            description = "Monochrome B&W high fashion contrast, pure whites",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("vogue", "black and white", "monochrome", "bw", "fashion", "grayscale")
        ),
        EditorToolItem(
            id = "filter_onyx",
            name = "Onyx",
            description = "Deep cinematic B&W, heavy shadows, film grain feel",
            iconRes = R.drawable.ic_ms_palette,
            tab = EditorTab.FILTERS,
            keywords = listOf("onyx", "black and white", "cinematic", "bw", "dark", "grayscale")
        ),

        // ── Markup Tools ──
        EditorToolItem(
            id = "markup_pen",
            name = "Pen",
            description = "Draw freehand strokes with customizable color and width",
            iconRes = R.drawable.ic_ms_edit,
            tab = EditorTab.MARKUP,
            keywords = listOf("pen", "draw", "stroke", "freehand", "doodle", "sketch", "pencil")
        ),
        EditorToolItem(
            id = "markup_highlighter",
            name = "Highlighter",
            description = "Highlight details with semi-transparent broad strokes",
            iconRes = R.drawable.ic_ms_edit_note,
            tab = EditorTab.MARKUP,
            keywords = listOf("highlighter", "highlight", "marker", "semi-transparent", "accent")
        ),
        EditorToolItem(
            id = "markup_arrow",
            name = "Arrow",
            description = "Draw directional arrows to point out details",
            iconRes = R.drawable.ic_ms_north_west,
            tab = EditorTab.MARKUP,
            keywords = listOf("arrow", "pointer", "point", "direction", "callout", "annotate")
        ),
        EditorToolItem(
            id = "markup_eraser",
            name = "Eraser",
            description = "Erase drawn markup strokes or clear all annotations",
            iconRes = R.drawable.ic_ms_ink_eraser,
            tab = EditorTab.MARKUP,
            keywords = listOf("eraser", "erase", "rub", "delete", "clear", "remove", "stroke", "draw", "brush", "cleanup")
        ),
        EditorToolItem(
            id = "markup_redact",
            name = "Redact",
            description = "Privacy mosaic pixelation or blackout redaction",
            iconRes = R.drawable.ic_ms_shield,
            tab = EditorTab.MARKUP,
            keywords = listOf("redact", "mosaic", "pixelate", "blur", "blackout", "privacy", "hide", "censor")
        ),
        EditorToolItem(
            id = "markup_text",
            name = "Text",
            description = "Add text captions and labels over the image",
            iconRes = R.drawable.ic_ms_text_fields,
            tab = EditorTab.MARKUP,
            keywords = listOf("text", "font", "caption", "label", "write", "type", "title")
        )
    )

    /**
     * Filters the catalog according to user query with intelligent multi-term matching and relevance scoring.
     */
    fun filterTools(
        query: String,
        tools: List<EditorToolItem> = ALL_TOOLS
    ): List<EditorToolItem> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return tools

        val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return tools

        return tools.filter { tool ->
            val nameLower = tool.name.lowercase()
            val descLower = tool.description.lowercase()
            val tabLower = tool.tab.label.lowercase()
            val keywordsLower = tool.keywords.map { it.lowercase() }

            tokens.all { token ->
                nameLower.contains(token) ||
                descLower.contains(token) ||
                tabLower.contains(token) ||
                keywordsLower.any { it.contains(token) }
            }
        }.sortedByDescending { tool ->
            var score = 0
            val nameLower = tool.name.lowercase()
            if (nameLower == trimmed) score += 100
            else if (nameLower.startsWith(trimmed)) score += 50
            else if (nameLower.contains(trimmed)) score += 25

            if (tool.keywords.any { it.equals(trimmed, ignoreCase = true) }) score += 40
            else if (tool.keywords.any { it.contains(trimmed, ignoreCase = true) }) score += 20

            if (tool.description.contains(trimmed, ignoreCase = true)) score += 10
            if (tool.tab.label.contains(trimmed, ignoreCase = true)) score += 5
            score
        }
    }
}

/**
 * Full-screen "Search editing tools" sheet.
 *
 * Provides instant real-time searching through all editor tools, preset filters,
 * crop actions, and markup capabilities, switching directly to the focused tool.
 */
@Composable
fun ToolSearchSheet(
    onSelectTool: (EditorTab, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredTools = remember(searchQuery) {
        ToolSearchCatalog.filterTools(searchQuery)
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Header Search Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ms_arrow_back),
                        contentDescription = "Close search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search editing tools",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ms_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_close),
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    ),
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Tool List or Empty State ──
            if (filteredTools.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No editing tools found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Try searching for brightness, crop, filters, or pen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredTools,
                        key = { it.id }
                    ) { tool ->
                        ToolListItem(
                            tool = tool,
                            onClick = {
                                onSelectTool(tool.tab, tool.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overload accepting [onToolSelected] callback matching alternative plan invocation signatures.
 */
@Composable
fun ToolSearchSheet(
    onToolSelected: (EditorTab, Any?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") marker: Unit = Unit
) = ToolSearchSheet(
    onSelectTool = { tab, id -> onToolSelected(tab, id) },
    onDismiss = onDismiss,
    modifier = modifier
)

@Composable
private fun ToolListItem(
    tool: EditorToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle icon container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = tool.iconRes),
                        contentDescription = tool.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Tool label and description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Category Tab Badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.clip(CircleShape)
            ) {
                Text(
                    text = tool.tab.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
