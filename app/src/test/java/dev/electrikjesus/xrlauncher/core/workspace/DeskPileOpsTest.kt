package dev.electrikjesus.xrlauncher.core.workspace

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
        assertTrue(HomeSpaceDeskState.togglePileExpanded(id))
        assertFalse(HomeSpaceDeskState.piles.first().expanded)
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
    }
}
