package dev.electrikjesus.xrlauncher.core.input.bumpdesk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BumpDeskHostGestureTest {
    private lateinit var gesture: BumpDeskHostGesture

    @Before
    fun setUp() {
        gesture = BumpDeskHostGesture(touchSlopPx = 20f)
    }

    @Test
    fun primaryDown_onDesk_beginsHoldAtAbsolutePosition() {
        val actions = gesture.onPrimaryDown(100f, 200f, allowDeskGrab = true)
        assertTrue(actions.any { it is BumpDeskHostAction.CursorAt && it.x == 100f && it.y == 200f })
        assertTrue(actions.any { it === BumpDeskHostAction.BeginDeskHold })
        assertTrue(gesture.deskDragArmed)
    }

    @Test
    fun primaryDown_onChrome_doesNotBeginHold() {
        val actions = gesture.onPrimaryDown(10f, 10f, allowDeskGrab = false)
        assertFalse(actions.any { it === BumpDeskHostAction.BeginDeskHold })
        assertFalse(gesture.deskDragArmed)
    }

    @Test
    fun move_withinSlop_doesNotEmitDeskMove() {
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true)
        val actions = gesture.onMove(
            x = 110f,
            y = 105f,
            allowDeskGrab = true,
            fpsLook = true,
            dialogOpen = false,
        )
        assertFalse(actions.any { it === BumpDeskHostAction.DeskMoveWhilePressed })
    }

    @Test
    fun move_pastSlop_emitsDeskMoveWhilePressed() {
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true)
        val actions = gesture.onMove(
            x = 140f,
            y = 100f,
            allowDeskGrab = true,
            fpsLook = true,
            dialogOpen = false,
        )
        assertTrue(actions.any { it === BumpDeskHostAction.DeskMoveWhilePressed })
    }

    @Test
    fun unpressedMove_fpsLook_emitsLookPan() {
        val actions = gesture.onMove(
            x = 50f,
            y = 50f,
            allowDeskGrab = true,
            fpsLook = true,
            dialogOpen = false,
        )
        // First move from 0,0 → look pan
        assertTrue(actions.any { it is BumpDeskHostAction.LookPan })
    }

    @Test
    fun middleDrag_emitsLookPan() {
        gesture.onMiddleDown(100f, 100f)
        val actions = gesture.onMove(
            x = 120f,
            y = 110f,
            allowDeskGrab = true,
            fpsLook = false,
            dialogOpen = false,
        )
        val pan = actions.filterIsInstance<BumpDeskHostAction.LookPan>().single()
        assertEquals(20f, pan.dxPx, 0.01f)
        assertEquals(10f, pan.dyPx, 0.01f)
    }

    @Test
    fun secondaryDown_emitsRightClick() {
        val action = gesture.onSecondaryDown(30f, 40f)
        assertTrue(action is BumpDeskHostAction.RightClick)
        assertEquals(30f, (action as BumpDeskHostAction.RightClick).x)
        assertEquals(40f, action.y)
    }

    @Test
    fun primaryUp_afterDeskHold_endsHold() {
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true)
        val actions = gesture.onPrimaryUp(100f, 100f)
        assertTrue(actions.any { it === BumpDeskHostAction.EndDeskHold })
        assertFalse(gesture.primaryDown)
    }

    @Test
    fun primaryUp_onChrome_doesNotClickBus() {
        gesture.onPrimaryDown(10f, 10f, allowDeskGrab = false)
        val actions = gesture.onPrimaryUp(10f, 10f)
        assertFalse(actions.any { it === BumpDeskHostAction.EndDeskHold })
        assertFalse(actions.any { it is BumpDeskHostAction.LeftClick })
    }
}
