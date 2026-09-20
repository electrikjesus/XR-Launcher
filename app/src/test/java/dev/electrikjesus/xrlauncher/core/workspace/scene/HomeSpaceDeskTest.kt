package dev.electrikjesus.xrlauncher.core.workspace.scene

import dev.electrikjesus.xrlauncher.ui.spatial.gles.DeskIconBitmaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSpaceDeskTest {
    private val apps = listOf(
        HomeSpaceDesk.AppRef("a/.Main", "Alpha", "a"),
        HomeSpaceDesk.AppRef("b/.Main", "Beta", "b"),
        HomeSpaceDesk.AppRef("c/.Main", "Gamma", "c"),
    )

    @Test
    fun lookingLeft_facesTheDesktopOnTheSphere() {
        val home = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val desk = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        assertEquals(0f, home.pitchDeg, 0.2f)
        assertEquals(0f, desk.pitchDeg, 0.2f)
        assertTrue(desk.yawDeg < -20f)
        assertEquals(HomeSpaceDesk.yawDegrees(1920f, 1080f), desk.yawDeg, 0.2f)
    }

    @Test
    fun lookingLeft_centerRayPicksTheAllAppsTile() {
        val camera = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceScene.sphereHit(0.5f, 0.5f, camera, 1920f, 1080f)
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        assertEquals(1, icons.size)
        assertTrue(icons.first().isAppDrawer)
        assertEquals(
            HomeSpaceDesk.DRAWER_KEY,
            HomeSpaceDesk.pickIcon(hit.world, icons)!!.componentKey,
        )
    }

    @Test
    fun defaultIcons_areThinPancakesOnTheSphere() {
        assertTrue(HomeSpaceDesk.ICON_HALF_THICK * 4f < HomeSpaceDesk.ICON_HALF_WIDTH)
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        assertTrue(drawer.halfThick * 4f < drawer.halfWidth)
        assertEquals(
            HomeSpaceScene.innerSphereRadius(1f),
            drawer.center.length(),
            0.04f,
        )
    }

    @Test
    fun layout_scalesDeskIconsWithUiScale() {
        val atOne = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f, uiScale = 1f).first()
        val atUi = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f, uiScale = 1.2f).first()
        assertEquals(atOne.halfWidth * 1.2f, atUi.halfWidth, 0.001f)
        assertEquals(atOne.halfHeight * 1.2f, atUi.halfHeight, 0.001f)
    }

    @Test
    fun labeledDeskIcons_useTallerMeshSoRoundFacesStayRound() {
        assertEquals(
            DeskIconBitmaps.LABELED_ASPECT,
            HomeSpaceDesk.LABELED_ICON_ASPECT,
            0.001f,
        )
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first { it.isAppDrawer }
        assertEquals(
            drawer.halfWidth * HomeSpaceDesk.LABELED_ICON_ASPECT,
            drawer.halfHeight,
            0.001f,
        )
        val placed = HomeSpaceDesk.layout(
            placed = listOf(
                HomeSpaceDesk.Placed(
                    HomeSpaceDesk.AppRef("a/.Main", "A", "a"),
                    yawDeg = 10f,
                    pitchDeg = -5f,
                ),
            ),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        val app = placed.first { it.isDesktopApp }
        assertEquals(app.halfWidth * HomeSpaceDesk.LABELED_ICON_ASPECT, app.halfHeight, 0.001f)
        val face = HomeSpaceDesk.inwardFace(app)
        fun dist(a: Vec3, b: Vec3): Float {
            val dx = a.x - b.x
            val dy = a.y - b.y
            val dz = a.z - b.z
            return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        }
        val width = dist(face.bl, face.br)
        val height = dist(face.bl, face.tl)
        assertEquals(HomeSpaceDesk.LABELED_ICON_ASPECT, height / width, 0.01f)
    }

    @Test
    fun matchedIconHalf_tracksHomePaneIconDpUnderUiScale() {
        val half = HomeSpaceDesk.matchedIconHalfExtent(
            uiScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            panelScale = 0.70f,
            sphereScale = 1f,
            density = 2f,
        )
        assertEquals(HomeSpaceDesk.ICON_HALF_WIDTH, half, 0.01f)
        val doubled = HomeSpaceDesk.matchedIconHalfExtent(
            uiScale = 2f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            panelScale = 0.70f,
            sphereScale = 1f,
            density = 2f,
        )
        assertEquals(half * 2f, doubled, 0.001f)
    }

    @Test
    fun matchedIconHalf_staysGluedWhenSphereScaleChanges() {
        val atOne = HomeSpaceDesk.matchedIconHalfExtent(
            uiScale = 1.2f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            panelScale = 0.70f,
            sphereScale = 1f,
            density = 2f,
        )
        val atFar = HomeSpaceDesk.matchedIconHalfExtent(
            uiScale = 1.2f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            panelScale = 0.70f,
            sphereScale = 1.5f,
            density = 2f,
        )
        // Pane world size grows slightly with sphere; desk must follow, not stay fixed.
        assertTrue(atFar > atOne)
        assertTrue(atFar / atOne in 1.05f..1.25f)
    }

    @Test
    fun layout_opensABumpDeskFourByFourDrawer() {
        val closed = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = false,
            drawerApps = apps,
        )
        assertEquals(1, closed.size)
        val many = (0 until 20).map { i ->
            HomeSpaceDesk.AppRef("$i/.Main", "App$i", "p$i")
        }
        val open = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = true,
            drawerApps = many,
            drawerPage = 0,
        )
        val appsOnPage = open.filter { it.isDesktopApp }
        val backing = open.first { it.isBacking }
        val pager = open.filter { it.isPager }
        val closedDrawer = closed.first()
        assertFalse(open.any { it.isAppDrawer })
        assertTrue(open.first().isBacking)
        assertEquals(HomeSpaceDesk.DRAWER_PAGE_SIZE, appsOnPage.size)
        assertEquals(many[0].componentKey, appsOnPage.first().componentKey)
        assertEquals(2 + 2, pager.size) // prev + next + 2 page dots for 20 apps
        assertTrue(appsOnPage.first().center.length() < backing.center.length())
        assertTrue(
            "open drawer icons stay readable (may FOV-fit below full desk face)",
            appsOnPage.first().halfWidth >=
                HomeSpaceDesk.matchedIconHalfExtent(
                    uiScale = 1f,
                    viewportWidthPx = 1920f,
                    viewportHeightPx = 1080f,
                    panelScale = 1f,
                    sphereScale = 1f,
                    density = 2f,
                ) * 0.35f,
        )
        assertTrue(
            "backing should cover the 4-wide icon grid",
            backing.halfWidth > appsOnPage.first().halfWidth * 3.2f,
        )
        assertTrue(
            "row spacing should leave room for labels",
            HomeSpaceDesk.DRAWER_OPEN_ROW_SPACING > HomeSpaceDesk.DRAWER_OPEN_COL_SPACING,
        )
        val topApp = appsOnPage.first()
        val bottomPager = pager.first { it.kind == HomeSpaceDesk.Kind.PAGE_PREV }
        assertTrue(
            "pager sits below the grid with a gap",
            topApp.pitchDeg > bottomPager.pitchDeg + 2f,
        )
        assertTrue(
            "pager stays within cursor pitch reach",
            bottomPager.pitchDeg >= -HomeSpaceDesk.DRAWER_MAX_HALF_PITCH_DEG - 0.5f,
        )
        assertTrue(
            "open drawer half-height stays within FOV budget",
            HomeSpaceDesk.angularHalfPitch(backing.halfHeight, 1f) <=
                HomeSpaceDesk.DRAWER_MAX_HALF_PITCH_DEG + 0.25f,
        )
        assertTrue(
            "backing contains the top row",
            backing.pitchDeg + HomeSpaceDesk.angularHalfPitch(backing.halfHeight, 1f) >
                topApp.pitchDeg + HomeSpaceDesk.angularHalfPitch(topApp.halfHeight, 1f) * 0.5f,
        )
        val page1 = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = true,
            drawerApps = many,
            drawerPage = 1,
        )
        assertEquals(4, page1.filter { it.isDesktopApp }.size)
        assertEquals(many[16].componentKey, page1.first { it.isDesktopApp }.componentKey)
    }

    @Test
    fun iconMesh_inwardFaceUvsPutBitmapTopOnCameraTop() {
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        val mesh = HomeSpaceDesk.iconMesh(drawer)
        assertEquals(0f, mesh.interleaved[6], 0.001f)
        assertEquals(0f, mesh.interleaved[7], 0.001f)
        assertEquals(1f, mesh.interleaved[6 + HomeSpacePaneMesh.STRIDE], 0.001f)
        assertEquals(0f, mesh.interleaved[7 + HomeSpacePaneMesh.STRIDE], 0.001f)
    }

    @Test
    fun inwardFace_looksAtTheCameraAsATopDownPancakeNotRotated180Z() {
        val poses = listOf(-48f to 0f, 0f to 0f, 35f to 8f, -20f to -12f)
        poses.forEach { (yaw, pitch) ->
            val icon = HomeSpaceDesk.iconOf(
                app = HomeSpaceDesk.AppRef("desk/.Tile", "Tile", "desk", HomeSpaceDesk.Kind.APP_DRAWER),
                yawDeg = yaw,
                pitchDeg = pitch,
                sphereScale = 1f,
            )
            val camera = HomeSpaceScene.Camera(yawDeg = yaw, pitchDeg = pitch)
            val face = HomeSpaceDesk.inwardFace(icon)
            val bl = camera.viewPoint(face.bl)
            val br = camera.viewPoint(face.br)
            val tl = camera.viewPoint(face.tl)
            val tr = camera.viewPoint(face.tr)
            val inMid = camera.viewPoint((face.bl + face.tr) * 0.5f)
            val outMid = camera.viewPoint((face.outBl + face.outTr) * 0.5f)
            val towardCamera = camera.viewPoint(icon.center + face.inward) -
                camera.viewPoint(icon.center)
            val rightX = br.x - bl.x
            val upY = tl.y - bl.y
            val ccw = rightX * (tl.y - bl.y) - (br.y - bl.y) * (tl.x - bl.x)
            val thinZ = kotlin.math.abs(outMid.z - inMid.z)
            val wideX = kotlin.math.abs(br.x - bl.x)
            assertTrue("yaw=$yaw right should be +viewX, not 180Z", rightX > 0.05f)
            assertTrue("yaw=$yaw up should be +viewY, not 180Z", upY > 0.05f)
            assertTrue("yaw=$yaw inward winding faces the camera (CCW)", ccw > 0f)
            assertTrue("yaw=$yaw inward normal points at the camera", towardCamera.z > 0.5f)
            assertTrue("yaw=$yaw inward face is closer than the back face", inMid.z > outMid.z)
            assertTrue("yaw=$yaw pancake is thin toward the camera", thinZ * 4f < wideX)
            assertEquals("yaw=$yaw top-right is right and up of bottom-left", true, tr.x > bl.x && tr.y > bl.y)
        }
    }

    @Test
    fun layout_keepsTheDrawerWhenPlacingApps() {
        val placed = apps.mapIndexed { index, app ->
            HomeSpaceDesk.Placed(app, yawDeg = -40f + index * 8f, pitchDeg = -6f)
        }
        val icons = HomeSpaceDesk.layout(placed, 1f, 1920f, 1080f)
        assertEquals(1 + apps.size, icons.size)
        assertTrue(icons.first().isAppDrawer)
        assertTrue(icons[1].yawDeg != icons[2].yawDeg || icons[1].pitchDeg != icons[2].pitchDeg)
    }

    @Test
    fun pickIcon_hitsTheIconUnderTheRay() {
        val placed = apps.mapIndexed { index, app ->
            HomeSpaceDesk.Placed(app, yawDeg = -40f + index * 8f, pitchDeg = -6f)
        }
        val icons = HomeSpaceDesk.layout(placed, 1f, 1920f, 1080f)
        val first = icons.first()
        val picked = HomeSpaceDesk.pickIcon(first.center, icons)
        assertEquals(first.componentKey, picked!!.componentKey)
        assertNull(HomeSpaceDesk.pickIcon(Vec3(0f, 0f, -1f), icons))
    }

    @Test
    fun pickIcon_usesIconTangentAxes() {
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        val drawer = icons.first()
        val alongRight = HomeSpaceDesk.rightAxis(drawer.yawDeg)
        val miss = Vec3(
            drawer.center.x + alongRight.x * drawer.halfWidth * 2.2f,
            drawer.center.y,
            drawer.center.z + alongRight.z * drawer.halfWidth * 2.2f,
        )
        assertNull(HomeSpaceDesk.pickIcon(miss, icons))
        val hit = Vec3(
            drawer.center.x + alongRight.x * drawer.halfWidth * 0.4f,
            drawer.center.y,
            drawer.center.z + alongRight.z * drawer.halfWidth * 0.4f,
        )
        assertEquals(drawer.componentKey, HomeSpaceDesk.pickIcon(hit, icons)!!.componentKey)
    }

    @Test
    fun lookingHome_centerRayDoesNotPickTheDrawer() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceScene.sphereHit(0.5f, 0.5f, camera, 1920f, 1080f)
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        assertNull(HomeSpaceDesk.pickIcon(hit.world, icons))
    }

    @Test
    fun largerSphere_movesIconsFartherFromTheCamera() {
        val near = HomeSpaceDesk.pointOnSphere(-40f, 0f, 1f)
        val far = HomeSpaceDesk.pointOnSphere(-40f, 0f, 1.6f)
        assertTrue(far.length() > near.length() + 0.4f)
    }

    @Test
    fun hoverLift_movesTheIconTowardTheCamera() {
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        val face = HomeSpaceDesk.inwardFace(drawer, HomeSpaceDesk.HOVER_LIFT)
        val mid = (face.bl + face.tr) * 0.5f
        assertTrue(mid.length() < drawer.center.length() - 0.02f)
    }

    @Test
    fun layout_deskTilesMatchHomeIconReferenceSize() {
        val matched = HomeSpaceDesk.matchedIconHalfExtent(
            uiScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            panelScale = 0.70f,
            sphereScale = 1f,
            density = 2f,
        )
        val drawer = HomeSpaceDesk.defaultIcons(
            1f,
            1920f,
            1080f,
            panelScale = 0.70f,
            uiScale = 1f,
        ).first()
        // Drawer tile is DRAWER_SCALE × matched app face.
        assertEquals(matched * HomeSpaceDesk.DRAWER_SCALE, drawer.halfWidth, 0.01f)
    }

    @Test
    fun pickAlongRay_hitsALiftedWidgetCloserThanTheSphereWall() {
        val open = HomeSpaceDesk.defaultIcons(
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = true,
            drawerApps = apps,
        )
        val backing = open.first { it.isBacking }
        val camera = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        val ray = HomeSpaceScene.worldRay(0.5f, 0.5f, camera, 1920f, 1080f)
        val picked = HomeSpaceDesk.pickAlongRay(ray, open)
        assertEquals(backing.componentKey, picked!!.componentKey)
    }

    @Test
    fun layout_openDrawerBackingIsAtLeastAsWideAsTall() {
        val many = (0 until 20).map { i ->
            HomeSpaceDesk.AppRef("$i/.Main", "App$i", "p$i")
        }
        val open = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = true,
            drawerApps = many,
        )
        val backing = open.first { it.isBacking }
        assertTrue(
            "backing stays near-square landscape: w=${backing.halfWidth} h=${backing.halfHeight}",
            backing.halfWidth + 0.001f >= backing.halfHeight * 0.9f,
        )
    }

    @Test
    fun layout_drawerPoseOverridesDefaultYaw() {
        val icons = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerYawDeg = -55f,
            drawerPitchDeg = 8f,
        )
        val drawer = icons.first { it.isAppDrawer }
        assertEquals(-55f, drawer.yawDeg, 0.01f)
        assertEquals(8f, drawer.pitchDeg, 0.01f)
    }

    @Test
    fun pickAlongRay_prefersPagerControlsOverBacking() {
        val many = (0 until 20).map { i ->
            HomeSpaceDesk.AppRef("$i/.Main", "App$i", "p$i")
        }
        val open = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = true,
            drawerApps = many,
        )
        val next = open.first { it.kind == HomeSpaceDesk.Kind.PAGE_NEXT }
        val camera = HomeSpaceScene.Camera(yawDeg = next.yawDeg, pitchDeg = next.pitchDeg)
        val ray = next.center.normalized()
        val picked = HomeSpaceDesk.pickAlongRay(ray, open)
        assertEquals(HomeSpaceDesk.PAGE_NEXT_KEY, picked!!.componentKey)
    }

    @Test
    fun pickNearestPager_grabsSlightlyOffTargetPagination() {
        val many = (0 until 20).map { i ->
            HomeSpaceDesk.AppRef("$i/.Main", "App$i", "p$i")
        }
        val open = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = true,
            drawerApps = many,
        )
        val next = open.first { it.kind == HomeSpaceDesk.Kind.PAGE_NEXT }
        val off = HomeSpaceDesk.iconOf(
            next.app,
            yawDeg = next.yawDeg + 4f,
            pitchDeg = next.pitchDeg - 3f,
            sphereScale = 1f,
            halfWidth = next.halfWidth,
            halfHeight = next.halfHeight,
            lift = next.lift,
        )
        val ray = off.center.normalized()
        val nearest = HomeSpaceDesk.pickNearestPager(ray, open)
        assertEquals(HomeSpaceDesk.PAGE_NEXT_KEY, nearest!!.componentKey)
        assertTrue(HomeSpaceDesk.inOpenDrawerClickZone(ray, open))
    }

    @Test
    fun inOpenDrawerClickZone_falseFarFromWidget() {
        val many = (0 until 20).map { i ->
            HomeSpaceDesk.AppRef("$i/.Main", "App$i", "p$i")
        }
        val open = HomeSpaceDesk.layout(
            placed = emptyList(),
            sphereScale = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            drawerOpen = true,
            drawerApps = many,
        )
        val far = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef("x", "x", "x"),
            yawDeg = 40f,
            pitchDeg = 10f,
            sphereScale = 1f,
        )
        assertFalse(HomeSpaceDesk.inOpenDrawerClickZone(far.center.normalized(), open))
    }

    @Test
    fun moved_reposesAnIconOnTheSphere() {
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        val moved = HomeSpaceDesk.moved(drawer, yawDeg = 10f, pitchDeg = -8f, sphereScale = 1f)
        assertEquals(10f, moved.yawDeg, 0.01f)
        assertEquals(-8f, moved.pitchDeg, 0.01f)
        assertEquals(drawer.componentKey, moved.componentKey)
    }
}
