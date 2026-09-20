package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeskPileOpsTest {
    @Before
    fun reset() {
        HomeSpaceDeskState.clear()
    }

    @Test
    fun createPile_folderRemovesMembersAndPersists() {
        HomeSpaceDeskState.restore(
            DeskLayout(
                items = listOf(
                    DeskPlacedItem("a/.Main", "A", "a", -20f, 0f),
                    DeskPlacedItem("b/.Main", "B", "b", -10f, 2f),
                    DeskPlacedItem("c/.Main", "C", "c", 0f, 0f),
                ),
            ),
        )
        assertTrue(HomeSpaceDeskState.createPile(setOf("a/.Main", "b/.Main"), DeskPileMode.FOLDER))
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals("c/.Main", HomeSpaceDeskState.placed.first().app.componentKey)
        assertEquals(1, HomeSpaceDeskState.piles.size)
        val pile = HomeSpaceDeskState.piles.first()
        assertEquals(DeskPileMode.FOLDER, pile.mode)
        assertEquals(2, pile.members.size)
        assertFalse(pile.expanded)

        val encoded = DeskJson.encode(HomeSpaceDeskState.toLayout())
        val decoded = DeskJson.decode(encoded)
        assertEquals(1, decoded.piles.size)
        assertEquals("FOLDER", decoded.piles.first().mode)
        assertEquals(2, decoded.piles.first().members.size)

        HomeSpaceDeskState.clear()
        HomeSpaceDeskState.restore(decoded)
        assertEquals(1, HomeSpaceDeskState.piles.size)
        assertEquals(DeskPileMode.FOLDER, HomeSpaceDeskState.piles.first().mode)
    }

    @Test
    fun createPile_stackAndToggleExpand() {
        HomeSpaceDeskState.restore(
            DeskLayout(
                items = listOf(
                    DeskPlacedItem("a/.Main", "A", "a", -8f, 0f),
                    DeskPlacedItem("b/.Main", "B", "b", 8f, 0f),
                ),
            ),
        )
        assertTrue(HomeSpaceDeskState.createPile(setOf("a/.Main", "b/.Main"), DeskPileMode.STACK))
        val id = HomeSpaceDeskState.piles.first().id
        assertTrue(HomeSpaceDeskState.togglePileExpanded(id))
        assertTrue(HomeSpaceDeskState.piles.first().expanded)
        assertTrue(HomeSpaceDeskState.piles.first().showsMembers)
        assertTrue(HomeSpaceDeskState.togglePileExpanded(id))
        assertFalse(HomeSpaceDeskState.piles.first().expanded)
        assertFalse(HomeSpaceDeskState.piles.first().showsMembers)
    }

    @Test
    fun togglePileFan_hidesCollapsedFace() {
        HomeSpaceDeskState.restore(
            DeskLayout(
                items = listOf(
                    DeskPlacedItem("a/.Main", "A", "a", -8f, 0f),
                    DeskPlacedItem("b/.Main", "B", "b", 8f, 0f),
                ),
            ),
        )
        HomeSpaceDeskState.createPile(setOf("a/.Main", "b/.Main"), DeskPileMode.FOLDER)
        val id = HomeSpaceDeskState.piles.first().id
        assertTrue(HomeSpaceDeskState.togglePileFan(id))
        assertTrue(HomeSpaceDeskState.piles.first().fannedOut)
        assertFalse(HomeSpaceDeskState.piles.first().expanded)
        assertTrue(HomeSpaceDeskState.piles.first().showsMembers)
        assertTrue(HomeSpaceDeskState.collapseOpenPiles())
        assertFalse(HomeSpaceDeskState.piles.first().showsMembers)
    }

    @Test
    fun breakPile_releasesMembers() {
        HomeSpaceDeskState.restore(
            DeskLayout(
                items = listOf(
                    DeskPlacedItem("a/.Main", "A", "a", -8f, 0f),
                    DeskPlacedItem("b/.Main", "B", "b", 8f, 0f),
                ),
            ),
        )
        HomeSpaceDeskState.createPile(setOf("a/.Main", "b/.Main"), DeskPileMode.STACK)
        val id = HomeSpaceDeskState.piles.first().id
        assertTrue(HomeSpaceDeskState.breakPile(id))
        assertTrue(HomeSpaceDeskState.piles.isEmpty())
        assertEquals(2, HomeSpaceDeskState.placed.size)
        // Released at rest — no leftover velocity that physics could orbit.
        assertTrue(HomeSpaceDeskState.placed.all { it.velYawDeg == 0f && it.velPitchDeg == 0f })
    }

    @Test
    fun breakApartYawStep_coversIconAngularSize() {
        val halfW = HomeSpaceDesk.ICON_HALF_WIDTH
        val halfYaw = HomeSpaceDesk.angularHalfYaw(halfW, sphereScale = 1f)
        val step = DeskPileOps.breakApartYawStepDeg(
            sphereScale = 1f,
            halfWidth = halfW,
            memberCount = 3,
        )
        assertTrue(step >= halfYaw * 2f)
    }

    @Test
    fun breakApart_spacesMembersAndZerosVelocity() {
        val pile = DeskPile(
            id = "pile_test",
            mode = DeskPileMode.STACK,
            name = "Pile",
            members = listOf(
                HomeSpaceDesk.AppRef("a/.Main", "A", "a"),
                HomeSpaceDesk.AppRef("b/.Main", "B", "b"),
                HomeSpaceDesk.AppRef("c/.Main", "C", "c"),
            ),
            yawDeg = -40f,
            pitchDeg = 5f,
        )
        val step = DeskPileOps.breakApartYawStepDeg(1f, HomeSpaceDesk.ICON_HALF_WIDTH, 3)
        val released = DeskPileOps.breakApart(pile, emptyList(), yawStepDeg = step)
        assertEquals(3, released.size)
        assertTrue(released.all { it.velYawDeg == 0f && it.velPitchDeg == 0f })
        val yaws = released.map { it.yawDeg }.sorted()
        assertTrue(yaws[1] - yaws[0] >= step - 0.01f)
        assertTrue(yaws[2] - yaws[1] >= step - 0.01f)
    }
}