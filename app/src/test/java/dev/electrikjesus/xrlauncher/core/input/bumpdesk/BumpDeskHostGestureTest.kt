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
        val actions = gesture.onPrimaryDown(100f, 200f, allowDeskGrab = true, fpsLook = false)
        assertTrue(actions.any { it is BumpDeskHostAction.CursorAt && it.x == 100f && it.y == 200f })
        assertTrue(actions.any { it === BumpDeskHostAction.BeginDeskHold })
        assertTrue(gesture.deskDragArmed)
    }

    @Test
    fun primaryDown_fpsLook_doesNotBeginDeskHold() {
        val actions = gesture.onPrimaryDown(100f, 200f, allowDeskGrab = true, fpsLook = true)
        assertFalse(actions.any { it === BumpDeskHostAction.BeginDeskHold })
        assertFalse(gesture.deskDragArmed)
    }

    @Test
    fun primaryDown_onChrome_doesNotBeginHold() {
        val actions = gesture.onPrimaryDown(10f, 10f, allowDeskGrab = false, fpsLook = false)
        assertFalse(actions.any { it === BumpDeskHostAction.BeginDeskHold })
        assertFalse(gesture.deskDragArmed)
    }

    @Test
    fun move_withinSlop_doesNotEmitDeskMove() {
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true, fpsLook = false)
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
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true, fpsLook = false)
        val actions = gesture.onMove(
            x = 140f,
            y = 100f,
            allowDeskGrab = true,
            fpsLook = false,
            dialogOpen = false,
        )
        assertTrue(actions.any { it === BumpDeskHostAction.DeskMoveWhilePressed })
    }

    @Test
    fun gestureLook_dragPastSlop_isDeskOnly_noLookPan() {
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true, fpsLook = false)
        val actions = gesture.onMove(
            x = 140f,
            y = 120f,
            allowDeskGrab = true,
            fpsLook = false,
            dialogOpen = false,
            gestureLook = true,
        )
        assertFalse(actions.any { it is BumpDeskHostAction.LookPan })
        assertTrue(actions.any { it === BumpDeskHostAction.DeskMoveWhilePressed })
    }

    @Test
    fun gestureLook_unpressedMove_doesNotLook() {
        gesture.onMove(100f, 100f, allowDeskGrab = true, fpsLook = false, dialogOpen = false, gestureLook = true)
        val actions = gesture.onMove(
            x = 160f,
            y = 140f,
            allowDeskGrab = true,
            fpsLook = false,
            dialogOpen = false,
            gestureLook = true,
        )
        assertFalse(actions.any { it is BumpDeskHostAction.LookPan })
    }

    @Test
    fun fpsLook_primaryDrag_emitsLookPan() {
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true, fpsLook = true)
        val actions = gesture.onMove(
            x = 140f,
            y = 100f,
            allowDeskGrab = true,
            fpsLook = true,
            dialogOpen = false,
        )
        assertTrue(actions.any { it is BumpDeskHostAction.LookPan })
        assertFalse(actions.any { it === BumpDeskHostAction.DeskMoveWhilePressed })
    }

    @Test
    fun unpressedMove_fpsLook_emitsLookPanAfterSeed() {
        // First sample seeds position (no jump from 0,0).
        val seed = gesture.onMove(
            x = 100f,
            y = 100f,
            allowDeskGrab = true,
            fpsLook = true,
            dialogOpen = false,
        )
        assertFalse(seed.any { it is BumpDeskHostAction.LookPan })
        val actions = gesture.onMove(
            x = 140f,
            y = 110f,
            allowDeskGrab = true,
            fpsLook = true,
            dialogOpen = false,
        )
        val pan = actions.filterIsInstance<BumpDeskHostAction.LookPan>().single()
        assertEquals(40f, pan.dxPx, 0.01f)
        assertEquals(10f, pan.dyPx, 0.01f)
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
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true, fpsLook = false)
        val actions = gesture.onPrimaryUp(100f, 100f)
        assertTrue(actions.any { it === BumpDeskHostAction.EndDeskHold })
        assertFalse(gesture.primaryDown)
    }

    @Test
    fun primaryUp_onChrome_doesNotClickBus() {
        gesture.onPrimaryDown(10f, 10f, allowDeskGrab = false, fpsLook = false)
        val actions = gesture.onPrimaryUp(10f, 10f)
        assertFalse(actions.any { it === BumpDeskHostAction.EndDeskHold })
        assertFalse(actions.any { it is BumpDeskHostAction.LeftClick })
    }

    @Test
    fun fpsLook_tapWithoutDrag_emitsLeftClick() {
        gesture.onPrimaryDown(100f, 100f, allowDeskGrab = true, fpsLook = true)
        val actions = gesture.onPrimaryUp(102f, 101f)
        assertTrue(actions.any { it is BumpDeskHostAction.LeftClick })
    }

    @Test
    fun pinchMove_emitsLookPanAndZoom() {
        gesture.onPinchBegin(100f, 200f, 300f)
        val actions = gesture.onPinchMove(120f, 230f, 310f)
        assertTrue(actions.any { it is BumpDeskHostAction.LookPan })
        assertTrue(actions.any { it is BumpDeskHostAction.PinchZoom })
        val pan = actions.filterIsInstance<BumpDeskHostAction.LookPan>().single()
        assertEquals(30f, pan.dxPx, 0.01f)
        assertEquals(10f, pan.dyPx, 0.01f)
    }

    @Test
    fun gestureLook_pinchMidpointTravel_locksPanOnly() {
        gesture = BumpDeskHostGesture(touchSlopPx = 20f, pinchZoomThresholdPx = 36f)
        gesture.onPinchBegin(100f, 200f, 300f)
        // Midpoint moves past slop; span stays put.
        val actions = gesture.onPinchMove(100f, 250f, 300f, gestureLook = true)
        assertEquals(BumpDeskHostGesture.PinchLock.PAN, gesture.pinchLock)
        assertTrue(actions.any { it is BumpDeskHostAction.LookPan })
        assertFalse(actions.any { it is BumpDeskHostAction.PinchZoom })
        // Later span change must not zoom once PAN is locked.
        val later = gesture.onPinchMove(180f, 260f, 305f, gestureLook = true)
        assertFalse(later.any { it is BumpDeskHostAction.PinchZoom })
        assertTrue(later.any { it is BumpDeskHostAction.LookPan })
    }

    @Test
    fun gestureLook_pinchSpanChange_locksZoomOnly() {
        gesture = BumpDeskHostGesture(touchSlopPx = 20f, pinchZoomThresholdPx = 36f)
        gesture.onPinchBegin(100f, 200f, 300f)
        val actions = gesture.onPinchMove(150f, 205f, 302f, gestureLook = true)
        assertEquals(BumpDeskHostGesture.PinchLock.ZOOM, gesture.pinchLock)
        assertTrue(actions.any { it is BumpDeskHostAction.PinchZoom })
        assertFalse(actions.any { it is BumpDeskHostAction.LookPan })
        // Later midpoint travel must not pan once ZOOM is locked.
        val later = gesture.onPinchMove(155f, 280f, 340f, gestureLook = true)
        assertFalse(later.any { it is BumpDeskHostAction.LookPan })
        assertTrue(later.any { it is BumpDeskHostAction.PinchZoom })
    }
}
