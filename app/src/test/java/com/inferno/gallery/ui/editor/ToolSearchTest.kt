package com.inferno.gallery.ui.editor

import com.inferno.gallery.ui.editor.model.AdjustTool
import com.inferno.gallery.ui.editor.model.EditorTab
import com.inferno.gallery.ui.editor.model.FilterPreset
import com.inferno.gallery.ui.editor.studios.ToolSearchCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSearchTest {

    @Test
    fun testCatalogHasAllRequiredTools() {
        val allTools = ToolSearchCatalog.ALL_TOOLS
        assertTrue("Catalog should contain tools", allTools.isNotEmpty())

        // 1. Check uniqueness of all tool IDs
        val ids = allTools.map { it.id }
        assertEquals("All tool IDs must be unique", ids.size, ids.distinct().size)

        // 2. All tools have valid non-blank properties
        allTools.forEach { tool ->
            assertFalse("Tool ID must not be blank", tool.id.isBlank())
            assertFalse("Tool name must not be blank for ${tool.id}", tool.name.isBlank())
            assertFalse("Tool description must not be blank for ${tool.id}", tool.description.isBlank())
            assertTrue("Icon resource must be valid for ${tool.id}", tool.iconRes != 0)
            assertNotNull("Tab must be defined for ${tool.id}", tool.tab)
        }
    }

    @Test
    fun testCatalogContainsAllThirteenAdjustTools() {
        val allTools = ToolSearchCatalog.ALL_TOOLS
        val adjustTools = allTools.filter { it.tab == EditorTab.ADJUST }
        assertEquals("Must contain 13 adjust tools", 13, adjustTools.size)

        val adjustIds = adjustTools.map { it.id.lowercase() }.toSet()
        val expectedTools = listOf(
            "brightness", "contrast", "tone", "white_point",
            "highlights", "shadows", "black_point", "saturation",
            "warmth", "tint", "skin_tone", "blue_tone", "vignette"
        )
        expectedTools.forEach { expectedId ->
            assertTrue("Catalog must include adjust tool: $expectedId", adjustIds.contains(expectedId))
        }
    }

    @Test
    fun testCatalogContainsCropTools() {
        val allTools = ToolSearchCatalog.ALL_TOOLS
        val cropTools = allTools.filter { it.tab == EditorTab.CROP }
        assertTrue("Crop tools must exist", cropTools.size >= 4)

        val cropIds = cropTools.map { it.id }.toSet()
        assertTrue("Must include crop & aspect", cropIds.contains("crop_aspect"))
        assertTrue("Must include straighten", cropIds.contains("straighten"))
        assertTrue("Must include rotate", cropIds.contains("rotate"))
        assertTrue("Must include perspective", cropIds.contains("perspective"))
    }

    @Test
    fun testCatalogContainsMarkupTools() {
        val allTools = ToolSearchCatalog.ALL_TOOLS
        val markupTools = allTools.filter { it.tab == EditorTab.MARKUP }
        assertTrue("Markup tools must exist", markupTools.size >= 4)

        val markupIds = markupTools.map { it.id }.toSet()
        assertTrue("Must include pen", markupIds.contains("markup_pen"))
        assertTrue("Must include highlighter", markupIds.contains("markup_highlighter"))
        assertTrue("Must include redact", markupIds.contains("markup_redact"))
        assertTrue("Must include text", markupIds.contains("markup_text"))
    }

    @Test
    fun testCatalogContainsFilterPresets() {
        val allTools = ToolSearchCatalog.ALL_TOOLS
        val filterTools = allTools.filter { it.tab == EditorTab.FILTERS }
        assertTrue("Filters tools must exist", filterTools.size >= 14)

        val filterIds = filterTools.map { it.id }.toSet()
        assertTrue("Must include Playa filter", filterIds.contains("filter_playa"))
        assertTrue("Must include Honey filter", filterIds.contains("filter_honey"))
        assertTrue("Must include Isla filter", filterIds.contains("filter_isla"))
        assertTrue("Must include Desert filter", filterIds.contains("filter_desert"))
        assertTrue("Must include Clay filter", filterIds.contains("filter_clay"))
        assertTrue("Must include Palma filter", filterIds.contains("filter_palma"))
        assertTrue("Must include Metro filter", filterIds.contains("filter_metro"))
        assertTrue("Must include Eiffel filter", filterIds.contains("filter_eiffel"))
        assertTrue("Must include Modena filter", filterIds.contains("filter_modena"))
        assertTrue("Must include Breeze filter", filterIds.contains("filter_breeze"))
        assertTrue("Must include Vogue filter", filterIds.contains("filter_vogue"))
        assertTrue("Must include Onyx filter", filterIds.contains("filter_onyx"))
    }

    @Test
    fun testEmptyOrBlankQueryReturnsAllTools() {
        val all = ToolSearchCatalog.ALL_TOOLS
        val emptyResult = ToolSearchCatalog.filterTools("")
        assertEquals("Empty query should return all tools", all.size, emptyResult.size)

        val blankResult = ToolSearchCatalog.filterTools("   ")
        assertEquals("Blank query should return all tools", all.size, blankResult.size)
    }

    @Test
    fun testCaseInsensitiveSearch() {
        val lowerResults = ToolSearchCatalog.filterTools("brightness")
        val upperResults = ToolSearchCatalog.filterTools("BRIGHTNESS")
        val mixedResults = ToolSearchCatalog.filterTools("BriGhtNess")

        assertFalse("Search should find Brightness", lowerResults.isEmpty())
        assertEquals("Case should not affect result count", lowerResults.size, upperResults.size)
        assertEquals("Case should not affect result count", lowerResults.size, mixedResults.size)
        assertEquals("Top match should be brightness", "brightness", lowerResults.first().id)
    }

    @Test
    fun testKeywordSearch() {
        // Redact tool should be found when searching for privacy, blur, or mosaic
        val privacyResults = ToolSearchCatalog.filterTools("privacy")
        assertTrue("Searching 'privacy' should find redact", privacyResults.any { it.id == "markup_redact" })

        val mosaicResults = ToolSearchCatalog.filterTools("mosaic")
        assertTrue("Searching 'mosaic' should find redact", mosaicResults.any { it.id == "markup_redact" })

        // Black and white filters should be found when searching 'bw' or 'black and white'
        val bwResults = ToolSearchCatalog.filterTools("bw")
        assertTrue("Searching 'bw' should find Vogue or Onyx", bwResults.any { it.id == "filter_vogue" || it.id == "filter_onyx" })

        // Warmth tool should be found when searching 'kelvin'
        val kelvinResults = ToolSearchCatalog.filterTools("kelvin")
        assertTrue("Searching 'kelvin' should find warmth", kelvinResults.any { it.id == "warmth" })
    }

    @Test
    fun testDescriptionSearch() {
        val results = ToolSearchCatalog.filterTools("lightness or darkness")
        assertTrue("Should match Brightness description", results.any { it.id == "brightness" })

        val skinResults = ToolSearchCatalog.filterTools("specifically for skin tones")
        assertTrue("Should match Skin tone description", skinResults.any { it.id == "skin_tone" })
    }

    @Test
    fun testTabCategorySearch() {
        val cropResults = ToolSearchCatalog.filterTools("Crop")
        assertTrue("Searching 'Crop' should return crop tools", cropResults.any { it.tab == EditorTab.CROP })

        val adjustResults = ToolSearchCatalog.filterTools("Adjust")
        assertTrue("Searching 'Adjust' should return adjust tools", adjustResults.any { it.tab == EditorTab.ADJUST })

        val markupResults = ToolSearchCatalog.filterTools("Markup")
        assertTrue("Searching 'Markup' should return markup tools", markupResults.any { it.tab == EditorTab.MARKUP })
    }

    @Test
    fun testNonMatchingQueryReturnsEmptyList() {
        val results = ToolSearchCatalog.filterTools("nonexistent_tool_xyz_12345")
        assertTrue("Non-matching query should return empty list", results.isEmpty())
    }
}
