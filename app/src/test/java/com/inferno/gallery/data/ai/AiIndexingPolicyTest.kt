package com.inferno.gallery.data.ai

import com.inferno.gallery.data.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiIndexingPolicyTest {

    @Test
    fun defaultConfigAllowsNormalFoldersAndBlocksTrashNullAndGalleryExcludedWhenRespected() {
        val config = AiIndexingFolderConfig()
        val galleryExcluded = setOf("WhatsApp Images", "Messenger")

        for (feature in AiIndexingFeature.entries) {
            assertTrue(config.isBucketEligible(feature, "Camera", galleryExcluded))
            assertTrue(config.isBucketEligible(feature, "Screenshots", galleryExcluded))
            assertFalse(config.isBucketEligible(feature, "WhatsApp Images", galleryExcluded))
            assertFalse(config.isBucketEligible(feature, "Messenger", galleryExcluded))
            assertFalse(config.isBucketEligible(feature, "Trash", galleryExcluded))
            assertFalse(config.isBucketEligible(feature, null, galleryExcluded))
            assertFalse(config.isBucketEligible(feature, "   ", galleryExcluded))
        }
    }

    @Test
    fun respectGalleryExcludedFalseAllowsGalleryHiddenFoldersUnlessExcludedPerAiFeature() {
        val config = AiIndexingFolderConfig(
            respectGalleryExcluded = false,
            rules = mapOf(
                AiIndexingFeature.FACE_RECOGNITION to AiFeatureFolderRule(
                    mode = AiFolderScopeMode.EXCLUDE_SELECTED,
                    excludedFolders = setOf("Messenger")
                )
            )
        )
        val galleryExcluded = setOf("WhatsApp Images", "Messenger")

        assertTrue(config.isBucketEligible(AiIndexingFeature.SMART_SEARCH, "WhatsApp Images", galleryExcluded))
        assertTrue(config.isBucketEligible(AiIndexingFeature.SMART_SEARCH, "Messenger", galleryExcluded))
        assertTrue(config.isBucketEligible(AiIndexingFeature.FACE_RECOGNITION, "WhatsApp Images", galleryExcluded))
        assertFalse(config.isBucketEligible(AiIndexingFeature.FACE_RECOGNITION, "Messenger", galleryExcluded))
    }

    @Test
    fun onlySelectedEmptyBlocksAllBucketsEvenCamera() {
        val config = AiIndexingFolderConfig(
            respectGalleryExcluded = true,
            rules = mapOf(
                AiIndexingFeature.FACE_RECOGNITION to AiFeatureFolderRule(
                    mode = AiFolderScopeMode.ONLY_SELECTED,
                    includedFolders = emptySet()
                )
            )
        )

        assertFalse(config.isBucketEligible(AiIndexingFeature.FACE_RECOGNITION, "Camera", emptySet()))
        assertFalse(config.isBucketEligible(AiIndexingFeature.FACE_RECOGNITION, "Screenshots", emptySet()))

        val query = AiIndexingQueryBuilder.buildEligibleTotalImagesCountQuery(
            feature = AiIndexingFeature.FACE_RECOGNITION,
            config = config,
            galleryExcluded = emptySet()
        )
        assertTrue(query.sql.contains("1 = 0"))
        assertTrue(query.args.isEmpty())
    }

    @Test
    fun onlySelectedRespectsGalleryExcludedPrecedence() {
        val config = AiIndexingFolderConfig(
            respectGalleryExcluded = true,
            rules = mapOf(
                AiIndexingFeature.SMART_SEARCH to AiFeatureFolderRule(
                    mode = AiFolderScopeMode.ONLY_SELECTED,
                    includedFolders = setOf("Camera", "PrivateMemes")
                )
            )
        )
        val galleryExcluded = setOf("PrivateMemes")

        assertTrue(config.isBucketEligible(AiIndexingFeature.SMART_SEARCH, "Camera", galleryExcluded))
        assertFalse(config.isBucketEligible(AiIndexingFeature.SMART_SEARCH, "PrivateMemes", galleryExcluded))
        assertFalse(config.isBucketEligible(AiIndexingFeature.SMART_SEARCH, "Downloads", galleryExcluded))

        val unindexed = AiIndexingQueryBuilder.buildUnindexedSmartSearchQuery(
            countOnly = false,
            config = config,
            galleryExcluded = galleryExcluded
        )
        assertTrue(unindexed.sql.contains("cm.bucketName IN (?)"))
        assertTrue(unindexed.args.contains("Camera"))
        assertFalse(unindexed.args.drop(1).contains("PrivateMemes") && unindexed.sql.contains("cm.bucketName IN (?,?)"))
    }

    @Test
    fun perFeatureRulesOperateIndependentlyAndCountEligiblePhotosAccurately() {
        val bucketCounts = mapOf(
            "Camera" to 1200,
            "Screenshots" to 450,
            "WhatsApp Images" to 8000,
            "Messenger" to 3000
        )
        val galleryExcluded = setOf("Messenger")
        val config = AiIndexingFolderConfig(
            respectGalleryExcluded = true,
            rules = mapOf(
                AiIndexingFeature.SMART_SEARCH to AiFeatureFolderRule(
                    mode = AiFolderScopeMode.EXCLUDE_SELECTED,
                    excludedFolders = setOf("WhatsApp Images")
                ),
                AiIndexingFeature.FACE_RECOGNITION to AiFeatureFolderRule(
                    mode = AiFolderScopeMode.ONLY_SELECTED,
                    includedFolders = setOf("Camera")
                ),
                AiIndexingFeature.OCR_TEXT to AiFeatureFolderRule(
                    mode = AiFolderScopeMode.ONLY_SELECTED,
                    includedFolders = setOf("Camera", "Screenshots")
                )
            )
        )

        assertFalse(config.areAllRulesUniform())
        assertEquals(3, config.countCustomizedFeatures())
        assertEquals(1650, config.countEligiblePhotos(AiIndexingFeature.SMART_SEARCH, bucketCounts, galleryExcluded))
        assertEquals(1200, config.countEligiblePhotos(AiIndexingFeature.FACE_RECOGNITION, bucketCounts, galleryExcluded))
        assertEquals(1650, config.countEligiblePhotos(AiIndexingFeature.OCR_TEXT, bucketCounts, galleryExcluded))
    }

    @Test
    fun sqlBuilderChunksLargeFolderListsWithinSqliteParameterBounds() {
        val manyFolders = (1..850).map { "Folder_$it" }.toSet()
        val rule = AiFeatureFolderRule(
            mode = AiFolderScopeMode.ONLY_SELECTED,
            includedFolders = manyFolders
        )
        val spec = AiIndexingQueryBuilder.buildBucketInclusionCondition(
            alias = "cm",
            rule = rule,
            galleryExcluded = emptySet(),
            respectGalleryExcluded = false
        )
        // 850 items should be split across multiple IN clauses rather than a single 850-param IN clause
        val inMatches = Regex("""cm\.bucketName IN \(""").findAll(spec.sql).count()
        assertEquals(3, inMatches)
        assertEquals(850, spec.args.size)
    }

    @Test
    fun modeSwitchingPreservesOppositeFolderListAndTogglesCleanly() {
        val initial = AiFeatureFolderRule(
            mode = AiFolderScopeMode.EXCLUDE_SELECTED,
            excludedFolders = setOf("WhatsApp Images"),
            includedFolders = setOf("Camera")
        )
        val afterExcludeToggle = initial.withBucketIncluded("Telegram", included = false)
        assertEquals(setOf("WhatsApp Images", "Telegram"), afterExcludeToggle.excludedFolders)
        assertEquals(setOf("Camera"), afterExcludeToggle.includedFolders)

        val switchedToInclude = afterExcludeToggle.copy(mode = AiFolderScopeMode.ONLY_SELECTED)
        assertEquals(setOf("Camera"), switchedToInclude.activeFolders)
        val afterIncludeAdd = switchedToInclude.withBucketIncluded("Screenshots", included = true)
        assertEquals(setOf("Camera", "Screenshots"), afterIncludeAdd.includedFolders)
        assertEquals(setOf("WhatsApp Images", "Telegram"), afterIncludeAdd.excludedFolders)
    }

    @Test
    fun aiIndexingSettingsKeysRoundTripThroughSettingsJsonBackup() {
        val sample = linkedMapOf<String, Any?>(
            SettingsRepository.AI_RESPECT_GALLERY_EXCLUDED.name to false,
            SettingsRepository.AI_SMART_FOLDER_MODE.name to AiFolderScopeMode.EXCLUDE_SELECTED.storageValue,
            SettingsRepository.AI_SMART_EXCLUDED_FOLDERS.name to setOf("WhatsApp Images", "Messenger"),
            SettingsRepository.AI_FACE_FOLDER_MODE.name to AiFolderScopeMode.ONLY_SELECTED.storageValue,
            SettingsRepository.AI_FACE_INCLUDED_FOLDERS.name to setOf("Camera"),
            SettingsRepository.AI_OCR_FOLDER_MODE.name to AiFolderScopeMode.ONLY_SELECTED.storageValue,
            SettingsRepository.AI_OCR_INCLUDED_FOLDERS.name to setOf("Camera", "Screenshots")
        )

        val json = SettingsRepository.encodeSettingsMapToJson(sample, exportedAtMs = 1700000000000L)
        val decoded = SettingsRepository.decodeSettingsJsonToTypedMap(json)

        assertEquals(SettingsRepository.TypedSettingValue.BoolVal(false), decoded[SettingsRepository.AI_RESPECT_GALLERY_EXCLUDED.name])
        assertEquals(
            SettingsRepository.TypedSettingValue.StringSetVal(setOf("WhatsApp Images", "Messenger")),
            decoded[SettingsRepository.AI_SMART_EXCLUDED_FOLDERS.name]
        )
        assertEquals(
            SettingsRepository.TypedSettingValue.StringVal(AiFolderScopeMode.ONLY_SELECTED.storageValue),
            decoded[SettingsRepository.AI_FACE_FOLDER_MODE.name]
        )
        assertEquals(
            SettingsRepository.TypedSettingValue.StringSetVal(setOf("Camera")),
            decoded[SettingsRepository.AI_FACE_INCLUDED_FOLDERS.name]
        )
        assertEquals(
            SettingsRepository.TypedSettingValue.StringSetVal(setOf("Camera", "Screenshots")),
            decoded[SettingsRepository.AI_OCR_INCLUDED_FOLDERS.name]
        )
    }

    @Test
    fun convertingModePreservesCurrentFolderStatesWithoutJumping() {
        val known = listOf("Camera", "Screenshots", "WhatsApp Images", "Messenger")
        val initialExcludeRule = AiFeatureFolderRule(
            mode = AiFolderScopeMode.EXCLUDE_SELECTED,
            excludedFolders = setOf("WhatsApp Images", "Messenger")
        )

        val convertedToInclude = initialExcludeRule.convertingModePreservingSelection(
            targetMode = AiFolderScopeMode.ONLY_SELECTED,
            knownBuckets = known
        )
        assertEquals(AiFolderScopeMode.ONLY_SELECTED, convertedToInclude.mode)
        assertTrue(convertedToInclude.allowsBucket("Camera", emptySet(), respectGalleryExcluded = false))
        assertTrue(convertedToInclude.allowsBucket("Screenshots", emptySet(), respectGalleryExcluded = false))
        assertFalse(convertedToInclude.allowsBucket("WhatsApp Images", emptySet(), respectGalleryExcluded = false))
        assertFalse(convertedToInclude.allowsBucket("Messenger", emptySet(), respectGalleryExcluded = false))
        // Future unknown folder defaults to OFF in ONLY_SELECTED
        assertFalse(convertedToInclude.allowsBucket("NewAppFolder", emptySet(), respectGalleryExcluded = false))

        val convertedBackToExclude = convertedToInclude.convertingModePreservingSelection(
            targetMode = AiFolderScopeMode.EXCLUDE_SELECTED,
            knownBuckets = known
        )
        assertEquals(AiFolderScopeMode.EXCLUDE_SELECTED, convertedBackToExclude.mode)
        assertTrue(convertedBackToExclude.allowsBucket("Camera", emptySet(), respectGalleryExcluded = false))
        assertFalse(convertedBackToExclude.allowsBucket("WhatsApp Images", emptySet(), respectGalleryExcluded = false))
        // Future unknown folder defaults to ON in EXCLUDE_SELECTED
        assertTrue(convertedBackToExclude.allowsBucket("NewAppFolder", emptySet(), respectGalleryExcluded = false))
    }
}
