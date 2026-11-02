package com.inferno.gallery.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/**
 * Design tokens and geometric constants for Photon Gallery connected card groupings.
 */
object PhotonConnectedCardDefaults {
    /** Outer curvature for boundary corners: 28dp (Material 3 Extra Large). */
    val OuterCornerRadius: Dp = 28.dp

    /** Inner curvature for adjacent connected seams: 8dp (Material 3 Small). */
    val InnerCornerRadius: Dp = 8.dp

    /** Default curvature for standalone cards: 16dp (Material 3 Large). */
    val StandaloneCornerRadius: Dp = 16.dp

    /** Vertical spacing between connected card segments: 2dp. */
    val ItemSpacing: Dp = 2.dp

    /** Default internal padding for connected card items: 16dp. */
    val ContentPadding: PaddingValues = PaddingValues(16.dp)

    /** Spring damping ratio for press corner morphing: Medium Bouncy. */
    val SpringDampingRatio: Float = Spring.DampingRatioMediumBouncy

    /** Spring stiffness for press corner morphing: Medium Low. */
    val SpringStiffness: Float = Spring.StiffnessMediumLow

    /**
     * Resolves the static connected shape for an item positioned at [index] within [count] items.
     */
    fun shapeForPosition(
        index: Int?,
        count: Int?,
        outerCornerRadius: Dp = OuterCornerRadius,
        innerCornerRadius: Dp = InnerCornerRadius,
        standaloneCornerRadius: Dp = StandaloneCornerRadius
    ): CornerBasedShape {
        return when {
            index != null && count != null -> {
                when {
                    count <= 1 -> RoundedCornerShape(outerCornerRadius)
                    index == 0 -> RoundedCornerShape(
                        topStart = outerCornerRadius,
                        topEnd = outerCornerRadius,
                        bottomStart = innerCornerRadius,
                        bottomEnd = innerCornerRadius
                    )
                    index == count - 1 -> RoundedCornerShape(
                        topStart = innerCornerRadius,
                        topEnd = innerCornerRadius,
                        bottomStart = outerCornerRadius,
                        bottomEnd = outerCornerRadius
                    )
                    else -> RoundedCornerShape(innerCornerRadius)
                }
            }
            else -> RoundedCornerShape(standaloneCornerRadius)
        }
    }
}

/**
 * Photon Gallery Connected Card Grouping.
 *
 * Renders connected card items in an expressive vertical stack with 2dp spacing
 * and optional outer envelope clipping.
 *
 * @param modifier Modifier applied to the outer container.
 * @param shape Optional outer boundary shape to clip the entire grouping.
 * @param clipToOutline When true and [shape] is null, clips the group to the outer 28dp curvature.
 * @param verticalArrangement Spacing arrangement between cards, defaulting to 2dp.
 * @param content Column scope content containing card items.
 */
@Composable
fun PhotonConnectedCardGroup(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    clipToOutline: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PhotonConnectedCardDefaults.ItemSpacing),
    content: @Composable ColumnScope.() -> Unit
) {
    val containerModifier = when {
        shape != null -> modifier.clip(shape)
        clipToOutline -> modifier.clip(RoundedCornerShape(PhotonConnectedCardDefaults.OuterCornerRadius))
        else -> modifier
    }

    Column(
        modifier = containerModifier,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/**
 * Photon Connected Card Group overload that accepts a list of data models.
 *
 * Automatically provides the [index] and total [count] to each item builder so
 * connected seam curvatures are derived seamlessly.
 *
 * @param items List of data items to display.
 * @param modifier Modifier applied to the group container.
 * @param shape Optional outer boundary shape to clip the group.
 * @param clipToOutline When true, clips the container to the outer 28dp radius.
 * @param verticalArrangement Spacing arrangement between cards, defaulting to 2dp.
 * @param itemContent Builder for each card item with index, count, and model item.
 */
@Composable
fun <T> PhotonConnectedCardGroup(
    items: List<T>,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    clipToOutline: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PhotonConnectedCardDefaults.ItemSpacing),
    itemContent: @Composable ColumnScope.(index: Int, count: Int, item: T) -> Unit
) {
    PhotonConnectedCardGroup(
        modifier = modifier,
        shape = shape,
        clipToOutline = clipToOutline,
        verticalArrangement = verticalArrangement
    ) {
        val totalCount = items.size
        items.forEachIndexed { index, item ->
            itemContent(index, totalCount, item)
        }
    }
}

/**
 * DSL scope for building connected card groups with automatic index and count tracking.
 */
class PhotonConnectedCardGroupScope {
    internal val itemBuilders = mutableListOf<@Composable (index: Int, count: Int) -> Unit>()

    /**
     * Appends a connected card item with automatically resolved position.
     */
    fun item(
        title: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        leadingContent: (@Composable () -> Unit)? = null,
        trailingContent: (@Composable () -> Unit)? = null,
        enabled: Boolean = true,
        containerColor: Color? = null,
        contentPadding: PaddingValues = PhotonConnectedCardDefaults.ContentPadding,
        onClick: (() -> Unit)? = null,
        shape: CornerBasedShape? = null,
        content: (@Composable ColumnScope.() -> Unit)? = null
    ) {
        itemBuilders.add { index, count ->
            PhotonConnectedCardItem(
                title = title,
                modifier = modifier,
                subtitle = subtitle,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                enabled = enabled,
                containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow,
                contentPadding = contentPadding,
                onClick = onClick,
                index = index,
                count = count,
                shape = shape,
                content = content
            )
        }
    }

    /**
     * Appends a custom composable item into the connected group.
     */
    fun customItem(
        content: @Composable (index: Int, count: Int) -> Unit
    ) {
        itemBuilders.add(content)
    }
}

/**
 * DSL-based Photon Connected Card Group.
 *
 * Collects card items declared via [PhotonConnectedCardGroupScope.item] and renders them
 * with automatically computed [index] and [count] values for corner rounding and press morphing.
 */
@Composable
fun PhotonConnectedCardDslGroup(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    clipToOutline: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PhotonConnectedCardDefaults.ItemSpacing),
    builder: PhotonConnectedCardGroupScope.() -> Unit
) {
    val groupScope = remember(builder) {
        PhotonConnectedCardGroupScope().apply(builder)
    }
    val items = groupScope.itemBuilders

    PhotonConnectedCardGroup(
        items = items,
        modifier = modifier,
        shape = shape,
        clipToOutline = clipToOutline,
        verticalArrangement = verticalArrangement
    ) { index, count, cardComposable ->
        cardComposable(index, count)
    }
}

/**
 * Photon Gallery Connected Card Item with real-time interactive spring press morphing.
 *
 * When idle, corners follow connected group curvature:
 * - Single item (count == 1): 28dp all around.
 * - Top item (index == 0): 28dp top, 8dp bottom seams.
 * - Bottom item (index == count - 1): 8dp top seams, 28dp bottom.
 * - Middle items: 8dp all around.
 * - Standalone: 16dp all around.
 *
 * When pressed, interactive spring physics morph all corner radii towards 28dp using
 * [lerp] and a bouncy spring spec, visually expanding the active item from the group.
 *
 * @param title Primary title text.
 * @param modifier Modifier applied to the card item.
 * @param subtitle Optional secondary subtitle text.
 * @param leadingContent Optional composable slot at the start of the card.
 * @param trailingContent Optional composable slot at the end of the card.
 * @param enabled Whether the card is interactive and fully opaque.
 * @param containerColor Background color for the card container.
 * @param contentPadding Padding applied inside the card.
 * @param onClick Optional click handler; enables ripple and interactive spring morphing.
 * @param index Zero-based position index within the connected group.
 * @param count Total number of items in the connected group.
 * @param shape Optional custom base shape overriding index-derived curvature.
 * @param content Optional extra content placed below the subtitle in the text column.
 */
@Composable
fun PhotonConnectedCardItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentPadding: PaddingValues = PhotonConnectedCardDefaults.ContentPadding,
    onClick: (() -> Unit)? = null,
    index: Int? = null,
    count: Int? = null,
    shape: CornerBasedShape? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val density = LocalDensity.current

    // Resolve base corner radii in Dp according to connected position or custom shape
    val (baseTopStart, baseTopEnd, baseBottomEnd, baseBottomStart) = remember(shape, index, count, density) {
        if (shape != null) {
            val dummySize = Size(1000f, 1000f)
            with(density) {
                listOf(
                    shape.topStart.toPx(dummySize, this).toDp(),
                    shape.topEnd.toPx(dummySize, this).toDp(),
                    shape.bottomEnd.toPx(dummySize, this).toDp(),
                    shape.bottomStart.toPx(dummySize, this).toDp()
                )
            }
        } else if (index != null && count != null) {
            when {
                count <= 1 -> listOf(
                    PhotonConnectedCardDefaults.OuterCornerRadius,
                    PhotonConnectedCardDefaults.OuterCornerRadius,
                    PhotonConnectedCardDefaults.OuterCornerRadius,
                    PhotonConnectedCardDefaults.OuterCornerRadius
                )
                index == 0 -> listOf(
                    PhotonConnectedCardDefaults.OuterCornerRadius,
                    PhotonConnectedCardDefaults.OuterCornerRadius,
                    PhotonConnectedCardDefaults.InnerCornerRadius,
                    PhotonConnectedCardDefaults.InnerCornerRadius
                )
                index == count - 1 -> listOf(
                    PhotonConnectedCardDefaults.InnerCornerRadius,
                    PhotonConnectedCardDefaults.InnerCornerRadius,
                    PhotonConnectedCardDefaults.OuterCornerRadius,
                    PhotonConnectedCardDefaults.OuterCornerRadius
                )
                else -> listOf(
                    PhotonConnectedCardDefaults.InnerCornerRadius,
                    PhotonConnectedCardDefaults.InnerCornerRadius,
                    PhotonConnectedCardDefaults.InnerCornerRadius,
                    PhotonConnectedCardDefaults.InnerCornerRadius
                )
            }
        } else {
            listOf(
                PhotonConnectedCardDefaults.StandaloneCornerRadius,
                PhotonConnectedCardDefaults.StandaloneCornerRadius,
                PhotonConnectedCardDefaults.StandaloneCornerRadius,
                PhotonConnectedCardDefaults.StandaloneCornerRadius
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate press progress with bouncy spring physics
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled && onClick != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = PhotonConnectedCardDefaults.SpringDampingRatio,
            stiffness = PhotonConnectedCardDefaults.SpringStiffness
        ),
        label = "PhotonConnectedCardPressMorph"
    )

    // Morph corner radii towards 28dp via lerp
    val targetRadius = PhotonConnectedCardDefaults.OuterCornerRadius
    val currentTopStart = lerp(baseTopStart, targetRadius, pressProgress).coerceAtLeast(0.dp)
    val currentTopEnd = lerp(baseTopEnd, targetRadius, pressProgress).coerceAtLeast(0.dp)
    val currentBottomEnd = lerp(baseBottomEnd, targetRadius, pressProgress).coerceAtLeast(0.dp)
    val currentBottomStart = lerp(baseBottomStart, targetRadius, pressProgress).coerceAtLeast(0.dp)

    val morphShape = RoundedCornerShape(
        topStart = currentTopStart,
        topEnd = currentTopEnd,
        bottomEnd = currentBottomEnd,
        bottomStart = currentBottomStart
    )

    val contentAlpha = if (enabled) 1f else 0.38f

    @Composable
    fun CardBody() {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingContent != null) {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        leadingContent()
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f * contentAlpha)
                        )
                    }

                    content?.invoke(this)
                }

                Spacer(modifier = Modifier.weight(1f))

                if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = morphShape,
            color = containerColor,
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .clip(morphShape)
        ) {
            CardBody()
        }
    } else {
        Surface(
            shape = morphShape,
            color = containerColor,
            modifier = modifier
                .fillMaxWidth()
                .clip(morphShape)
        ) {
            CardBody()
        }
    }
}

/**
 * Photon Connected Card Container Wrapper.
 *
 * Wraps arbitrary child composables (e.g. settings rows, switches, custom pickers)
 * inside a connected card Surface with index-aware curvature and interactive spring
 * press morphing.
 */
@Composable
fun PhotonConnectedCardWrapper(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val (baseTopStart, baseTopEnd, baseBottomEnd, baseBottomStart) = remember(index, count, density) {
        when {
            count <= 1 -> listOf(
                PhotonConnectedCardDefaults.OuterCornerRadius,
                PhotonConnectedCardDefaults.OuterCornerRadius,
                PhotonConnectedCardDefaults.OuterCornerRadius,
                PhotonConnectedCardDefaults.OuterCornerRadius
            )
            index == 0 -> listOf(
                PhotonConnectedCardDefaults.OuterCornerRadius,
                PhotonConnectedCardDefaults.OuterCornerRadius,
                PhotonConnectedCardDefaults.InnerCornerRadius,
                PhotonConnectedCardDefaults.InnerCornerRadius
            )
            index == count - 1 -> listOf(
                PhotonConnectedCardDefaults.InnerCornerRadius,
                PhotonConnectedCardDefaults.InnerCornerRadius,
                PhotonConnectedCardDefaults.OuterCornerRadius,
                PhotonConnectedCardDefaults.OuterCornerRadius
            )
            else -> listOf(
                PhotonConnectedCardDefaults.InnerCornerRadius,
                PhotonConnectedCardDefaults.InnerCornerRadius,
                PhotonConnectedCardDefaults.InnerCornerRadius,
                PhotonConnectedCardDefaults.InnerCornerRadius
            )
        }
    }

    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = PhotonConnectedCardDefaults.SpringDampingRatio,
            stiffness = PhotonConnectedCardDefaults.SpringStiffness
        ),
        label = "PhotonConnectedCardPressMorph"
    )

    val targetRadius = PhotonConnectedCardDefaults.OuterCornerRadius
    val currentTopStart = lerp(baseTopStart, targetRadius, pressProgress).coerceAtLeast(0.dp)
    val currentTopEnd = lerp(baseTopEnd, targetRadius, pressProgress).coerceAtLeast(0.dp)
    val currentBottomEnd = lerp(baseBottomEnd, targetRadius, pressProgress).coerceAtLeast(0.dp)
    val currentBottomStart = lerp(baseBottomStart, targetRadius, pressProgress).coerceAtLeast(0.dp)

    val morphShape = RoundedCornerShape(
        topStart = currentTopStart,
        topEnd = currentTopEnd,
        bottomEnd = currentBottomEnd,
        bottomStart = currentBottomStart
    )

    Surface(
        shape = morphShape,
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            isPressed = true
                        } else if (event.changes.all { !it.pressed }) {
                            isPressed = false
                        }
                    }
                }
            }
            .clip(morphShape)
    ) {
        content()
    }
}

