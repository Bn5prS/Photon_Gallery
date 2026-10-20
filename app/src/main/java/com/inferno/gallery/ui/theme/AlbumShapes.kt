@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Shape

/**
 * Material 3 Expressive shapes selectable for individual albums.
 */
data class AlbumShapeItem(
    val id: String,
    val displayName: String,
    val shape: Shape
)

object AlbumShapes {
    const val DEFAULT_ID = "default"

    val items: List<AlbumShapeItem> by lazy {
        listOf(
            AlbumShapeItem(
                id = DEFAULT_ID,
                displayName = "Squircle",
                shape = ShapeLarge
            ),
            AlbumShapeItem(
                id = "cookie_12",
                displayName = "Cookie (12)",
                shape = ExpressivePolygonShape(MaterialShapes.Cookie12Sided)
            ),
            AlbumShapeItem(
                id = "cookie_9",
                displayName = "Cookie (9)",
                shape = ExpressivePolygonShape(MaterialShapes.Cookie9Sided)
            ),
            AlbumShapeItem(
                id = "cookie_7",
                displayName = "Cookie (7)",
                shape = ExpressivePolygonShape(MaterialShapes.Cookie7Sided)
            ),
            AlbumShapeItem(
                id = "cookie_6",
                displayName = "Cookie (6)",
                shape = ExpressivePolygonShape(MaterialShapes.Cookie6Sided)
            ),
            AlbumShapeItem(
                id = "cookie_4",
                displayName = "Cookie (4)",
                shape = ExpressivePolygonShape(MaterialShapes.Cookie4Sided)
            ),
            AlbumShapeItem(
                id = "clover_8",
                displayName = "Clover (8)",
                shape = ExpressivePolygonShape(MaterialShapes.Clover8Leaf)
            ),
            AlbumShapeItem(
                id = "clover_4",
                displayName = "Clover (4)",
                shape = ExpressivePolygonShape(MaterialShapes.Clover4Leaf)
            ),
            AlbumShapeItem(
                id = "sunny",
                displayName = "Sunny",
                shape = ExpressivePolygonShape(MaterialShapes.Sunny)
            ),
            AlbumShapeItem(
                id = "very_sunny",
                displayName = "Very Sunny",
                shape = ExpressivePolygonShape(MaterialShapes.VerySunny)
            ),
            AlbumShapeItem(
                id = "soft_burst",
                displayName = "Soft Burst",
                shape = ExpressivePolygonShape(MaterialShapes.SoftBurst)
            ),
            AlbumShapeItem(
                id = "burst",
                displayName = "Burst",
                shape = ExpressivePolygonShape(MaterialShapes.Burst)
            ),
            AlbumShapeItem(
                id = "boom",
                displayName = "Boom",
                shape = ExpressivePolygonShape(MaterialShapes.Boom)
            ),
            AlbumShapeItem(
                id = "soft_boom",
                displayName = "Soft Boom",
                shape = ExpressivePolygonShape(MaterialShapes.SoftBoom)
            ),
            AlbumShapeItem(
                id = "flower",
                displayName = "Flower",
                shape = ExpressivePolygonShape(MaterialShapes.Flower)
            ),
            AlbumShapeItem(
                id = "puffy",
                displayName = "Puffy",
                shape = ExpressivePolygonShape(MaterialShapes.Puffy)
            ),
            AlbumShapeItem(
                id = "puffy_diamond",
                displayName = "Puffy Diamond",
                shape = ExpressivePolygonShape(MaterialShapes.PuffyDiamond)
            ),
            AlbumShapeItem(
                id = "pixel_circle",
                displayName = "Pixel Circle",
                shape = ExpressivePolygonShape(MaterialShapes.PixelCircle)
            ),
            AlbumShapeItem(
                id = "pixel_triangle",
                displayName = "Pixel Triangle",
                shape = ExpressivePolygonShape(MaterialShapes.PixelTriangle)
            ),
            AlbumShapeItem(
                id = "bun",
                displayName = "Bun",
                shape = ExpressivePolygonShape(MaterialShapes.Bun)
            ),
            AlbumShapeItem(
                id = "heart",
                displayName = "Heart",
                shape = ExpressivePolygonShape(MaterialShapes.Heart)
            ),
            AlbumShapeItem(
                id = "fan",
                displayName = "Fan",
                shape = ExpressivePolygonShape(MaterialShapes.Fan)
            ),
            AlbumShapeItem(
                id = "arch",
                displayName = "Arch",
                shape = ExpressivePolygonShape(MaterialShapes.Arch)
            ),
            AlbumShapeItem(
                id = "slanted",
                displayName = "Slanted",
                shape = ExpressivePolygonShape(MaterialShapes.Slanted)
            ),
            AlbumShapeItem(
                id = "gem",
                displayName = "Gem",
                shape = ExpressivePolygonShape(MaterialShapes.Gem)
            ),
            AlbumShapeItem(
                id = "clam_shell",
                displayName = "Clam Shell",
                shape = ExpressivePolygonShape(MaterialShapes.ClamShell)
            ),
            AlbumShapeItem(
                id = "pentagon",
                displayName = "Pentagon",
                shape = ExpressivePolygonShape(MaterialShapes.Pentagon)
            ),
            AlbumShapeItem(
                id = "diamond",
                displayName = "Diamond",
                shape = ExpressivePolygonShape(MaterialShapes.Diamond)
            ),
            AlbumShapeItem(
                id = "diamond_4_point",
                displayName = "Diamond (4-Pt)",
                shape = ExpressiveShapes.Diamond4Point
            ),
            AlbumShapeItem(
                id = "triangle",
                displayName = "Triangle",
                shape = ExpressivePolygonShape(MaterialShapes.Triangle)
            ),
            AlbumShapeItem(
                id = "arrow",
                displayName = "Arrow",
                shape = ExpressivePolygonShape(MaterialShapes.Arrow)
            ),
            AlbumShapeItem(
                id = "ghostish",
                displayName = "Ghostish",
                shape = ExpressivePolygonShape(MaterialShapes.Ghostish)
            ),
            AlbumShapeItem(
                id = "semi_circle",
                displayName = "Semi Circle",
                shape = ExpressivePolygonShape(MaterialShapes.SemiCircle)
            ),
            AlbumShapeItem(
                id = "oval",
                displayName = "Oval",
                shape = ExpressivePolygonShape(MaterialShapes.Oval)
            ),
            AlbumShapeItem(
                id = "pill",
                displayName = "Pill",
                shape = ExpressivePolygonShape(MaterialShapes.Pill)
            ),
            AlbumShapeItem(
                id = "square",
                displayName = "Square",
                shape = ExpressivePolygonShape(MaterialShapes.Square)
            ),
            AlbumShapeItem(
                id = "hexalobe",
                displayName = "Hexalobe",
                shape = ExpressiveShapes.Hexalobe
            ),
            AlbumShapeItem(
                id = "scallop",
                displayName = "Scallop",
                shape = ExpressiveShapes.Scallop
            ),
            AlbumShapeItem(
                id = "circle",
                displayName = "Circle",
                shape = CircleShape
            )
        )
    }

    private val shapeMap: Map<String, Shape> by lazy {
        items.associate { it.id to it.shape }
    }

    fun getShape(id: String?): Shape {
        if (id == null || id == DEFAULT_ID) return ShapeLarge
        return shapeMap[id] ?: ShapeLarge
    }
}
