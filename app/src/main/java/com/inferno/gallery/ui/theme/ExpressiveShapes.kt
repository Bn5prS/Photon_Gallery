package com.inferno.gallery.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.star
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import android.graphics.RectF
import android.graphics.Matrix

/**
 * Creative Material 3 Expressive Shapes built using AndroidX Graphics Shapes library.
 */
class ExpressivePolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val bounds = RectF()
        val androidPath = polygon.toPath()
        androidPath.computeBounds(bounds, true)
        val matrix = Matrix()
        matrix.postTranslate(-bounds.left, -bounds.top)
        val scaleX = if (bounds.width() > 0) size.width / bounds.width() else 1f
        val scaleY = if (bounds.height() > 0) size.height / bounds.height() else 1f
        matrix.postScale(scaleX, scaleY)
        androidPath.transform(matrix)
        return Outline.Generic(androidPath.asComposePath())
    }
}

/**
 * Dynamic shape morphing between two RoundedPolygon shapes at a given [progress] (0f..1f).
 */
class ExpressiveMorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val bounds = RectF()
        val androidPath = morph.toPath(progress)
        androidPath.computeBounds(bounds, true)
        val matrix = Matrix()
        matrix.postTranslate(-bounds.left, -bounds.top)
        val scaleX = if (bounds.width() > 0) size.width / bounds.width() else 1f
        val scaleY = if (bounds.height() > 0) size.height / bounds.height() else 1f
        matrix.postScale(scaleX, scaleY)
        androidPath.transform(matrix)
        return Outline.Generic(androidPath.asComposePath())
    }
}

/**
 * Pre-instantiated Compose Shapes for official Material 3 Expressive shapes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object OfficialM3Shapes {
    val Sunny: Shape = ExpressivePolygonShape(MaterialShapes.Sunny)
    val VerySunny: Shape = ExpressivePolygonShape(MaterialShapes.VerySunny)
    val Cookie12Sided: Shape = ExpressivePolygonShape(MaterialShapes.Cookie12Sided)
    val Cookie9Sided: Shape = ExpressivePolygonShape(MaterialShapes.Cookie9Sided)
    val Cookie6Sided: Shape = ExpressivePolygonShape(MaterialShapes.Cookie6Sided)
    val Cookie4Sided: Shape = ExpressivePolygonShape(MaterialShapes.Cookie4Sided)
    val Clover4Leaf: Shape = ExpressivePolygonShape(MaterialShapes.Clover4Leaf)
    val Clover8Leaf: Shape = ExpressivePolygonShape(MaterialShapes.Clover8Leaf)
    val Flower: Shape = ExpressivePolygonShape(MaterialShapes.Flower)
    val Burst: Shape = ExpressivePolygonShape(MaterialShapes.Burst)
    val SoftBurst: Shape = ExpressivePolygonShape(MaterialShapes.SoftBurst)
    val Boom: Shape = ExpressivePolygonShape(MaterialShapes.Boom)
    val SoftBoom: Shape = ExpressivePolygonShape(MaterialShapes.SoftBoom)
    val PuffyDiamond: Shape = ExpressivePolygonShape(MaterialShapes.PuffyDiamond)
    val Gem: Shape = ExpressivePolygonShape(MaterialShapes.Gem)
    val Heart: Shape = ExpressivePolygonShape(MaterialShapes.Heart)
    val Arch: Shape = ExpressivePolygonShape(MaterialShapes.Arch)
    val PixelCircle: Shape = ExpressivePolygonShape(MaterialShapes.PixelCircle)
    val PixelTriangle: Shape = ExpressivePolygonShape(MaterialShapes.PixelTriangle)
    val ClamShell: Shape = ExpressivePolygonShape(MaterialShapes.ClamShell)
    val Pill: Shape = ExpressivePolygonShape(MaterialShapes.Pill)
    val SemiCircle: Shape = ExpressivePolygonShape(MaterialShapes.SemiCircle)
    val Diamond: Shape = ExpressivePolygonShape(MaterialShapes.Diamond)
    val Pentagon: Shape = ExpressivePolygonShape(MaterialShapes.Pentagon)
}

object ExpressiveShapes {
    // 8-point clover / flower
    val Clover8Leaf: Shape = ExpressivePolygonShape(
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = 1f,
            innerRadius = 0.82f,
            rounding = CornerRounding(0.35f)
        )
    )

    // 4-point soft clover / squircle-star
    val Clover4Leaf: Shape = ExpressivePolygonShape(
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            radius = 1f,
            innerRadius = 0.78f,
            rounding = CornerRounding(0.45f)
        )
    )

    // 12-point cookie / sunny badge
    val Cookie12Sided: Shape = ExpressivePolygonShape(
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            radius = 1f,
            innerRadius = 0.88f,
            rounding = CornerRounding(0.3f)
        )
    )

    // Soft diamond / burst 4-point
    val Diamond4Point: Shape = ExpressivePolygonShape(
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            radius = 1f,
            innerRadius = 0.58f,
            rounding = CornerRounding(0.25f)
        )
    )

    // 6-point hexalobe
    val Hexalobe: Shape = ExpressivePolygonShape(
        RoundedPolygon.star(
            numVerticesPerRadius = 6,
            radius = 1f,
            innerRadius = 0.85f,
            rounding = CornerRounding(0.4f)
        )
    )

    // Scallop badge
    val Scallop: Shape = ExpressivePolygonShape(
        RoundedPolygon.star(
            numVerticesPerRadius = 16,
            radius = 1f,
            innerRadius = 0.90f,
            rounding = CornerRounding(0.25f)
        )
    )
}
