package dev.electrikjesus.xrlauncher.core.hdri

import org.junit.Assert.assertTrue
import org.junit.Test

class PolyHavenApiTest {
    @Test
    fun equirectPreviewUrl_isTwoToOneCdnPrimary() {
        val url = PolyHavenApi.equirectPreviewUrl("kiara_1_dawn")
        assertTrue(url.contains("primary/kiara_1_dawn.png"))
        assertTrue(url.contains("width=2048"))
        assertTrue(url.contains("height=1024"))
    }

    @Test
    fun defaultAssetId_isNonBlank() {
        assertTrue(PolyHavenApi.DEFAULT_ASSET_ID.isNotBlank())
    }
}
