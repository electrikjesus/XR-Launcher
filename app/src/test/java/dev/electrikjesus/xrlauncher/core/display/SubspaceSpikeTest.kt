package dev.electrikjesus.xrlauncher.core.display

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubspaceSpikeTest {
    @Test
    fun decision_preferSubspaceWhenSpatialApiOrForced() {
        val apiOnly = SubspaceSpike.Decision(
            hasSpatialApi = true,
            forcedForSpike = false,
            preferSubspaceShell = true,
        )
        assertTrue(apiOnly.preferSubspaceShell)

        val forcedOnly = SubspaceSpike.Decision(
            hasSpatialApi = false,
            forcedForSpike = true,
            preferSubspaceShell = true,
        )
        assertTrue(forcedOnly.preferSubspaceShell)

        val flat = SubspaceSpike.Decision(
            hasSpatialApi = false,
            forcedForSpike = false,
            preferSubspaceShell = false,
        )
        assertFalse(flat.preferSubspaceShell)
    }
}
