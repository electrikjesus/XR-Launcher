package dev.electrikjesus.xrlauncher.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompanionTouchpadMultiTouchTest {
    private lateinit var multi: CompanionTouchpadMultiTouch

    @Before
    fun setUp() {
        multi = CompanionTouchpadMultiTouch(touchSlopPx = 20f, pinchZoomThresholdPx = 30f)
    }

    @Test
    fun twoFingerDrag_locksToScroll() {
        multi.begin(2, midX = 100f, midY = 100f, distance = 80f)
        val actions = multi.move(2, midX = 100f, midY = 160f, distance = 80f)
        assertEquals(CompanionTouchpadMultiTouch.Lock.SCROLL, multi.lock)
        assertTrue(actions.any { it is CompanionTouchpadMultiTouch.Action.Scroll })
    }

    @Test
    fun twoFingerPinch_locksToZoom() {
        multi.begin(2, midX = 100f, midY = 100f, distance = 80f)
        val actions = multi.move(2, midX = 100f, midY = 100f, distance = 140f)
        assertEquals(CompanionTouchpadMultiTouch.Lock.ZOOM, multi.lock)
        assertTrue(actions.any { it is CompanionTouchpadMultiTouch.Action.Zoom })
    }

    @Test
    fun threeFingerDrag_lookPan() {
        multi.begin(3, midX = 100f, midY = 100f, distance = 80f)
        val actions = multi.move(3, midX = 140f, midY = 100f, distance = 80f)
        assertEquals(CompanionTouchpadMultiTouch.Lock.LOOK, multi.lock)
        assertTrue(actions.any { it is CompanionTouchpadMultiTouch.Action.LookPan })
    }

    @Test
    fun twoFingerTap_emitsRightClick() {
        multi.begin(2, midX = 100f, midY = 100f, distance = 80f)
        val end = multi.end()
        assertEquals(CompanionTouchpadMultiTouch.Action.RightClick, end)
    }

    @Test
    fun twoFingerScroll_doesNotRightClickOnEnd() {
        multi.begin(2, midX = 100f, midY = 100f, distance = 80f)
        multi.move(2, midX = 100f, midY = 160f, distance = 80f)
        assertEquals(null, multi.end())
    }
}
