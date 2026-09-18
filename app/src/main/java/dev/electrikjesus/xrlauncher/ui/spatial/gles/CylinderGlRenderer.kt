package dev.electrikjesus.xrlauncher.ui.spatial.gles

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconSnapshot
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeSpace3d
import dev.electrikjesus.xrlauncher.core.workspace.PanelTextureSnapshot
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGrid
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceGlesConfig
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpacePaneMesh
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpacePaneSlot
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.core.workspace.scene.Vec3
import dev.electrikjesus.xrlauncher.core.workspace.scene.normalized
import dev.electrikjesus.xrlauncher.core.workspace.scene.paneMesh
import dev.electrikjesus.xrlauncher.core.workspace.scene.sphereHit
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * GLES 2.0 inner-cylinder scene: wallpaper backdrop, wireframe guides, textured panel quads.
 */
class CylinderGlRenderer : GLSurfaceView.Renderer {
    var camera: WorkspaceCylinderGeometry.CameraState = WorkspaceCylinderGeometry.CameraState(
        yawDegrees = 0f,
        pitchDegrees = 0f,
        panNormX = 0f,
        panNormY = 0f,
    )
    var curvature: Float = 0.35f
    var workspaceWidth: Float = 1f
    var workspaceHeight: Float = 0.7f
    var viewportWidthPx: Float = 1f
    var viewportHeightPx: Float = 1f
    var panelGuideCenters: List<WorkspaceCylinderGrid.SlotCenter> = emptyList()
    var showWallpaperCylinder: Boolean = true

    /** Closed 360° room around the camera (BumpDesk-style) so look never shows wallpaper edges. */
    var surroundRoom: Boolean = false
    /** GLES surround-room radius; scales with [WorkspaceAppearance.sphereScale]. */
    var roomRadius: Float = GlassesHomeSpace3d.ROOM_RADIUS
    var homeSpaceSlots: List<HomeSpacePaneSlot> = emptyList()
    var homeSpacePanelScale: Float = 1f
    var homeSpaceSphereScale: Float = 1f
    var homeSpacePanesEnabled: Boolean = false
    var cursorX: Float = 0.5f
    var cursorY: Float = 0.5f
    var showSphereCursor: Boolean = false
    var deskIcons: List<HomeSpaceDesk.Icon> = emptyList()
    var deskHoveredKey: String? = null

    @Volatile
    private var pendingDeskTextures: List<DeskIconSnapshot> = emptyList()

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val sceneMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var lineProgram = 0
    private var linePositionHandle = 0
    private var lineColorHandle = 0
    private var lineMvpHandle = 0

    private var texturedProgram = 0
    private var texPositionHandle = 0
    private var texCoordHandle = 0
    private var texMvpHandle = 0
    private var texSamplerHandle = 0

    private var wallpaperProgram = 0
    private var wallpaperPositionHandle = 0
    private var wallpaperTexCoordHandle = 0
    private var wallpaperMvpHandle = 0
    private var wallpaperSamplerHandle = 0

    private var litProgram = 0
    private var litPositionHandle = 0
    private var litNormalHandle = 0
    private var litTexCoordHandle = 0
    private var litMvpHandle = 0
    private var litSamplerHandle = 0
    private var litLightHandle = 0
    private var litAmbientHandle = 0
    private var litTintHandle = 0
    private var litHighlightHandle = 0
    private var litUseTextureHandle = 0

    private val paneMeshBuffers = LinkedHashMap<String, FloatBuffer>()
    private val paneMeshVertexCounts = LinkedHashMap<String, Int>()
    private var paneMeshKey: String = ""

    private lateinit var cylinderLineBuffer: FloatBuffer
    private var cylinderLineVertexCount = 0

    private lateinit var wallpaperMeshBuffer: FloatBuffer
    private var wallpaperMeshVertexCount = 0
    private lateinit var roomCapBuffer: FloatBuffer
    private var roomCapVertexCount = 0

    private val quadBuffer: FloatBuffer = floatArrayOf(
        -0.5f, -0.5f, 0f, 0f, 1f,
        0.5f, -0.5f, 0f, 1f, 1f,
        -0.5f, 0.5f, 0f, 0f, 0f,
        0.5f, 0.5f, 0f, 1f, 0f,
    ).toFloatBuffer()

    @Volatile
    private var pendingTextures: List<PanelTextureSnapshot> = emptyList()

    @Volatile
    private var pendingWallpaper: Bitmap? = null

    @Volatile
    private var pendingWallpaperGeneration: Long = -1L

    private var wallpaperTextureId: Int = 0
    private var uploadedWallpaperGeneration: Long = -1L

    private data class GlTextureEntry(
        val textureId: Int,
        var generation: Long = -1L,
    )

    private val uploadedTextures = LinkedHashMap<String, GlTextureEntry>()
    private val uploadedDeskTextures = LinkedHashMap<String, GlTextureEntry>()
    private var deskPlaneBuffer: FloatBuffer? = null
    private var deskPlaneVertexCount = 0
    private var deskMeshKey: String = ""
    private val deskIconBuffers = LinkedHashMap<String, FloatBuffer>()
    private val deskIconVertexCounts = LinkedHashMap<String, Int>()

    fun setPanelTextures(snapshots: List<PanelTextureSnapshot>) {
        pendingTextures = snapshots
    }

    fun setDeskTextures(snapshots: List<DeskIconSnapshot>) {
        pendingDeskTextures = snapshots
    }

    fun setWallpaperBitmap(bitmap: Bitmap?, generation: Long) {
        pendingWallpaper = bitmap
        pendingWallpaperGeneration = generation
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        lineProgram = buildProgram(LINE_VERTEX_SHADER, LINE_FRAGMENT_SHADER)
        linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "aPosition")
        lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "uColor")
        lineMvpHandle = GLES20.glGetUniformLocation(lineProgram, "uMvp")

        texturedProgram = buildProgram(TEXTURED_VERTEX_SHADER, TEXTURED_FRAGMENT_SHADER)
        texPositionHandle = GLES20.glGetAttribLocation(texturedProgram, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(texturedProgram, "aTexCoord")
        texMvpHandle = GLES20.glGetUniformLocation(texturedProgram, "uMvp")
        texSamplerHandle = GLES20.glGetUniformLocation(texturedProgram, "uTexture")

        wallpaperProgram = buildProgram(WALLPAPER_VERTEX_SHADER, WALLPAPER_FRAGMENT_SHADER)
        wallpaperPositionHandle = GLES20.glGetAttribLocation(wallpaperProgram, "aPosition")
        wallpaperTexCoordHandle = GLES20.glGetAttribLocation(wallpaperProgram, "aTexCoord")
        wallpaperMvpHandle = GLES20.glGetUniformLocation(wallpaperProgram, "uMvp")
        wallpaperSamplerHandle = GLES20.glGetUniformLocation(wallpaperProgram, "uTexture")

        litProgram = buildProgram(LIT_VERTEX_SHADER, LIT_FRAGMENT_SHADER)
        litPositionHandle = GLES20.glGetAttribLocation(litProgram, "aPosition")
        litNormalHandle = GLES20.glGetAttribLocation(litProgram, "aNormal")
        litTexCoordHandle = GLES20.glGetAttribLocation(litProgram, "aTexCoord")
        litMvpHandle = GLES20.glGetUniformLocation(litProgram, "uMvp")
        litSamplerHandle = GLES20.glGetUniformLocation(litProgram, "uTexture")
        litLightHandle = GLES20.glGetUniformLocation(litProgram, "uLightPos")
        litAmbientHandle = GLES20.glGetUniformLocation(litProgram, "uAmbient")
        litTintHandle = GLES20.glGetUniformLocation(litProgram, "uTint")
        litHighlightHandle = GLES20.glGetUniformLocation(litProgram, "uHighlight")
        litUseTextureHandle = GLES20.glGetUniformLocation(litProgram, "uUseTexture")

        rebuildCylinderMeshes()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidthPx = width.toFloat().coerceAtLeast(1f)
        viewportHeightPx = height.toFloat().coerceAtLeast(1f)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(
            projectionMatrix,
            0,
            GlassesHomeSpace3d.FOV_Y_DEGREES,
            aspect,
            0.05f,
            40f,
        )
        rebuildCylinderMeshes()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        buildViewMatrix()
        applySceneSpan()
        uploadPendingWallpaper()
        uploadPendingTextures()
        uploadPendingDeskTextures()
        pruneStaleTextures()

        if (shouldDrawRoom()) {
            drawWallpaperCylinder()
            if (surroundRoom) {
                drawRoomCaps()
            }
        }
        if (homeSpacePanesEnabled && surroundRoom) {
            drawHomeSpacePanes()
            drawDesk()
            drawSphereCursor()
        }
        if (WorkspaceGlesConfig.showGuideWireframe && curvature > 0.01f) {
            drawCylinderGuideLine()
            drawPanelGuides()
        }
        if (WorkspaceGlesConfig.texturedPanelsEnabled && curvature > 0.01f && !homeSpacePanesEnabled) {
            drawTexturedPanels()
        }
    }

    private fun shouldDrawRoom(): Boolean {
        if (!showWallpaperCylinder) return false
        if (surroundRoom) return true
        return WorkspaceGlesConfig.showWallpaperCylinder && curvature > 0.01f
    }

    private fun buildViewMatrix() {
        Matrix.setLookAtM(
            viewMatrix,
            0,
            0f,
            0f,
            0f,
            0f,
            0f,
            -1f,
            0f,
            1f,
            0f,
        )
        Matrix.rotateM(viewMatrix, 0, camera.pitchDegrees, 1f, 0f, 0f)
        Matrix.rotateM(viewMatrix, 0, camera.yawDegrees, 0f, 1f, 0f)
        if (!surroundRoom) {
            Matrix.translateM(viewMatrix, 0, camera.panNormX * 1.4f, camera.panNormY * 0.9f, 0f)
        }
    }

    private fun applySceneSpan() {
        if (surroundRoom) return
        Matrix.setIdentityM(sceneMatrix, 0)
        Matrix.scaleM(sceneMatrix, 0, workspaceWidth, workspaceHeight, 1f)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, sceneMatrix, 0)
        System.arraycopy(tempMatrix, 0, viewMatrix, 0, 16)
    }

    fun rebuildCylinderMesh() {
        rebuildCylinderMeshes()
    }

    private fun rebuildCylinderMeshes() {
        rebuildCylinderGuideLine()
        rebuildWallpaperMesh()
        rebuildRoomCaps()
        if (homeSpacePanesEnabled) {
            paneMeshKey = ""
            rebuildHomeSpacePaneMeshes()
        }
    }

    private fun rebuildHomeSpacePaneMeshes() {
        val key = listOf(
            viewportWidthPx,
            viewportHeightPx,
            homeSpacePanelScale,
            homeSpaceSphereScale,
            homeSpaceSlots.joinToString { "${it.panelId}:${it.worldX}" },
        ).joinToString("|")
        if (key == paneMeshKey && paneMeshBuffers.isNotEmpty()) return
        paneMeshKey = key
        paneMeshBuffers.clear()
        paneMeshVertexCounts.clear()
        homeSpaceSlots.forEach { slot ->
            val mesh = HomeSpaceScene.paneMesh(
                worldX = slot.worldX,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                panelScale = homeSpacePanelScale,
                sphereScale = homeSpaceSphereScale,
            )
            paneMeshBuffers[slot.panelId] = mesh.interleaved.toFloatBuffer()
            paneMeshVertexCounts[slot.panelId] = mesh.vertexCount
        }
    }

    private fun drawHomeSpacePanes() {
        rebuildHomeSpacePaneMeshes()
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(litProgram)
        GLES20.glUniformMatrix4fv(litMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform3f(litLightHandle, 0f, 0f, 0f)
        GLES20.glUniform1f(litAmbientHandle, 0.28f)
        GLES20.glUniform3f(litTintHandle, 1f, 1f, 1f)
        GLES20.glUniform1f(litHighlightHandle, 0f)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        homeSpaceSlots.forEach { slot ->
            val buffer = paneMeshBuffers[slot.panelId] ?: return@forEach
            val count = paneMeshVertexCounts[slot.panelId] ?: return@forEach
            val textureId = uploadedTextures[slot.panelId]?.textureId ?: 0
            GLES20.glUniform1i(litUseTextureHandle, if (textureId != 0) 1 else 0)
            if (textureId != 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                GLES20.glUniform1i(litSamplerHandle, 0)
            }
            val stride = HomeSpacePaneMesh.STRIDE * 4
            buffer.position(0)
            GLES20.glEnableVertexAttribArray(litPositionHandle)
            GLES20.glVertexAttribPointer(litPositionHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(3)
            GLES20.glEnableVertexAttribArray(litNormalHandle)
            GLES20.glVertexAttribPointer(litNormalHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(6)
            GLES20.glEnableVertexAttribArray(litTexCoordHandle)
            GLES20.glVertexAttribPointer(litTexCoordHandle, 2, GLES20.GL_FLOAT, false, stride, buffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
            GLES20.glDisableVertexAttribArray(litPositionHandle)
            GLES20.glDisableVertexAttribArray(litNormalHandle)
            GLES20.glDisableVertexAttribArray(litTexCoordHandle)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun rebuildDeskMeshes() {
        val key = listOf(
            homeSpaceSphereScale,
            homeSpacePanelScale,
            deskHoveredKey.orEmpty(),
            deskIcons.joinToString { "${it.componentKey}:${it.yawDeg}:${it.pitchDeg}:${it.center.x}" },
        ).joinToString("|")
        if (key == deskMeshKey && deskIconBuffers.isNotEmpty()) return
        deskMeshKey = key
        deskPlaneBuffer = null
        deskPlaneVertexCount = 0
        deskIconBuffers.clear()
        deskIconVertexCounts.clear()
        deskIcons.forEach { icon ->
            val lift = if (icon.componentKey == deskHoveredKey) HomeSpaceDesk.HOVER_LIFT else 0f
            val mesh = HomeSpaceDesk.iconMesh(icon, lift)
            deskIconBuffers[icon.componentKey] = mesh.interleaved.toFloatBuffer()
            deskIconVertexCounts[icon.componentKey] = mesh.vertexCount
        }
    }

    private fun drawDesk() {
        rebuildDeskMeshes()
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(litProgram)
        GLES20.glUniformMatrix4fv(litMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform3f(litLightHandle, 0.2f, 1.4f, 0.4f)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        val stride = HomeSpacePaneMesh.STRIDE * 4

        fun drawMesh(
            buffer: FloatBuffer,
            count: Int,
            textureId: Int,
            ambient: Float,
            useTexture: Boolean,
            highlight: Boolean = false,
        ) {
            GLES20.glUniform1f(litAmbientHandle, ambient)
            GLES20.glUniform1f(litHighlightHandle, if (highlight) 1f else 0f)
            if (highlight) {
                GLES20.glUniform3f(litTintHandle, 0.45f, 0.78f, 1f)
            } else {
                GLES20.glUniform3f(litTintHandle, 1f, 1f, 1f)
            }
            GLES20.glUniform1i(litUseTextureHandle, if (useTexture && textureId != 0) 1 else 0)
            if (useTexture && textureId != 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                GLES20.glUniform1i(litSamplerHandle, 0)
            }
            buffer.position(0)
            GLES20.glEnableVertexAttribArray(litPositionHandle)
            GLES20.glVertexAttribPointer(litPositionHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(3)
            GLES20.glEnableVertexAttribArray(litNormalHandle)
            GLES20.glVertexAttribPointer(litNormalHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(6)
            GLES20.glEnableVertexAttribArray(litTexCoordHandle)
            GLES20.glVertexAttribPointer(litTexCoordHandle, 2, GLES20.GL_FLOAT, false, stride, buffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

        deskIcons.forEach { icon ->
            val buffer = deskIconBuffers[icon.componentKey] ?: return@forEach
            val count = deskIconVertexCounts[icon.componentKey] ?: return@forEach
            val textureId = uploadedDeskTextures[icon.componentKey]?.textureId ?: 0
            val hovered = icon.componentKey == deskHoveredKey
            if (hovered) {
                val pad = HomeSpaceDesk.hoverPadMesh(
                    icon,
                    lift = HomeSpaceDesk.HOVER_LIFT,
                )
                drawMesh(
                    pad.interleaved.toFloatBuffer(),
                    pad.vertexCount,
                    0,
                    ambient = 0.95f,
                    useTexture = false,
                    highlight = true,
                )
            }
            drawMesh(
                buffer,
                count,
                textureId,
                ambient = if (hovered) 0.95f else 0.42f,
                useTexture = true,
                highlight = hovered,
            )
        }
        GLES20.glDisableVertexAttribArray(litPositionHandle)
        GLES20.glDisableVertexAttribArray(litNormalHandle)
        GLES20.glDisableVertexAttribArray(litTexCoordHandle)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun drawSphereCursor() {
        if (!showSphereCursor) return
        val sceneCamera = HomeSpaceScene.Camera(camera.yawDegrees, camera.pitchDegrees)
        val sphere = HomeSpaceScene.sphereHit(
            cursorX = cursorX,
            cursorY = cursorY,
            camera = sceneCamera,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            sphereScale = homeSpaceSphereScale,
        )
        val origin = sphere.world
        val radial = origin.normalized()
        val up = Vec3(0f, 1f, 0f)
        var tangent = Vec3(
            up.y * radial.z - up.z * radial.y,
            up.z * radial.x - up.x * radial.z,
            up.x * radial.y - up.y * radial.x,
        )
        if (tangent.lengthSq() < 1e-6f) {
            tangent = Vec3(1f, 0f, 0f)
        } else {
            tangent = tangent.normalized()
        }
        val bitangent = Vec3(
            radial.y * tangent.z - radial.z * tangent.y,
            radial.z * tangent.x - radial.x * tangent.z,
            radial.x * tangent.y - radial.y * tangent.x,
        )
        val radius = 0.038f * homeSpaceSphereScale.coerceAtLeast(0.5f)
        val center = origin * 0.988f
        fun corner(sx: Float, sy: Float): Vec3 = Vec3(
            center.x + tangent.x * sx * radius + bitangent.x * sy * radius,
            center.y + tangent.y * sx * radius + bitangent.y * sy * radius,
            center.z + tangent.z * sx * radius + bitangent.z * sy * radius,
        )
        val bl = corner(-1f, -1f)
        val br = corner(1f, -1f)
        val tl = corner(-1f, 1f)
        val tr = corner(1f, 1f)
        val disc = floatArrayOf(
            bl.x, bl.y, bl.z,
            br.x, br.y, br.z,
            tl.x, tl.y, tl.z,
            tl.x, tl.y, tl.z,
            br.x, br.y, br.z,
            tr.x, tr.y, tr.z,
        ).toFloatBuffer()
        val near = radial * 0.14f
        val shaft = floatArrayOf(
            near.x, near.y, near.z,
            origin.x, origin.y, origin.z,
        ).toFloatBuffer()

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(lineProgram)
        GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnableVertexAttribArray(linePositionHandle)

        GLES20.glUniform4f(lineColorHandle, 0.55f, 0.82f, 1f, 0.28f)
        GLES20.glVertexAttribPointer(linePositionHandle, 3, GLES20.GL_FLOAT, false, 0, shaft)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)

        GLES20.glUniform4f(lineColorHandle, 0.82f, 0.93f, 1f, 0.92f)
        GLES20.glVertexAttribPointer(linePositionHandle, 3, GLES20.GL_FLOAT, false, 0, disc)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        GLES20.glDisableVertexAttribArray(linePositionHandle)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun rebuildCylinderGuideLine() {
        val segments = 48
        val c = curvature.coerceIn(0f, 1f)
        val halfArcRad = Math.toRadians(
            (WorkspaceCylinderGeometry.MAX_ARC_YAW_DEGREES * c * workspaceWidth) / 2.0,
        ).toFloat()
        val radius = WorkspaceCylinderGeometry.sceneRadiusX(viewportWidthPx, workspaceWidth, c)
        val baseDepth = WorkspaceCylinderGeometry.sceneBaseDepth(viewportWidthPx, c)
        val verts = FloatArray((segments + 1) * 3)
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val theta = -halfArcRad + t * 2f * halfArcRad
            verts[i * 3] = radius * sin(theta)
            verts[i * 3 + 1] = -0.15f
            verts[i * 3 + 2] = -(baseDepth + radius * (1f - cos(theta)))
        }
        cylinderLineBuffer = verts.toFloatBuffer()
        cylinderLineVertexCount = segments + 1
    }

    private fun rebuildWallpaperMesh() {
        val horizSegments = if (surroundRoom) 72 else 56
        val vertSegments = if (surroundRoom) 36 else 28
        val c = curvature.coerceIn(0f, 1f)
        val halfArcRad = if (surroundRoom) {
            Math.PI.toFloat()
        } else {
            Math.toRadians(
                (WorkspaceCylinderGeometry.MAX_ARC_YAW_DEGREES * c * workspaceWidth) / 2.0,
            ).toFloat()
        }
        val radius = if (surroundRoom) {
            roomRadius
        } else {
            WorkspaceCylinderGeometry.sceneRadiusX(viewportWidthPx, workspaceWidth, c)
        }
        val baseDepth = if (surroundRoom) {
            0f
        } else {
            WorkspaceCylinderGeometry.sceneBaseDepth(viewportWidthPx, c)
        }
        val wallHeight = if (surroundRoom) {
            6.4f * (roomRadius / GlassesHomeSpace3d.ROOM_RADIUS).coerceAtLeast(0.5f)
        } else {
            2.4f
        }
        val halfHeight = wallHeight / 2f

        val verts = mutableListOf<Float>()
        for (row in 0..vertSegments) {
            val v = row / vertSegments.toFloat()
            val y = -halfHeight + v * wallHeight
            for (col in 0..horizSegments) {
                val t = col / horizSegments.toFloat()
                val theta = -halfArcRad + t * 2f * halfArcRad
                val x = radius * sin(theta)
                val z = if (surroundRoom) {
                    -radius * cos(theta)
                } else {
                    -(baseDepth + radius * (1f - cos(theta)))
                }
                verts += x
                verts += y
                verts += z
                verts += t
                verts += v
            }
        }

        val indices = mutableListOf<Int>()
        val rowStride = horizSegments + 1
        for (row in 0 until vertSegments) {
            for (col in 0 until horizSegments) {
                val topLeft = row * rowStride + col
                val topRight = topLeft + 1
                val bottomLeft = (row + 1) * rowStride + col
                val bottomRight = bottomLeft + 1
                indices += topLeft
                indices += bottomLeft
                indices += topRight
                indices += topRight
                indices += bottomLeft
                indices += bottomRight
            }
        }

        val interleaved = FloatArray(indices.size * 5)
        indices.forEachIndexed { index, vertexIndex ->
            val base = vertexIndex * 5
            interleaved[index * 5] = verts[base]
            interleaved[index * 5 + 1] = verts[base + 1]
            interleaved[index * 5 + 2] = verts[base + 2]
            interleaved[index * 5 + 3] = verts[base + 3]
            interleaved[index * 5 + 4] = verts[base + 4]
        }
        wallpaperMeshBuffer = interleaved.toFloatBuffer()
        wallpaperMeshVertexCount = indices.size
    }

    private fun rebuildRoomCaps() {
        if (!surroundRoom) {
            roomCapBuffer = floatArrayOf().toFloatBuffer()
            roomCapVertexCount = 0
            return
        }
        val radius = roomRadius
        val halfHeight = 3.2f * (roomRadius / GlassesHomeSpace3d.ROOM_RADIUS).coerceAtLeast(0.5f)
        val verts = mutableListOf<Float>()
        fun addCap(y: Float, yUp: Boolean) {
            val uvs = if (yUp) {
                arrayOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 0f, 1f to 1f, 0f to 1f)
            } else {
                arrayOf(0f to 1f, 0f to 0f, 1f to 1f, 0f to 0f, 1f to 0f, 1f to 1f)
            }
            val corners = if (yUp) {
                arrayOf(
                    -radius to -radius, radius to -radius, -radius to radius,
                    radius to -radius, radius to radius, -radius to radius,
                )
            } else {
                arrayOf(
                    -radius to radius, -radius to -radius, radius to radius,
                    -radius to -radius, radius to -radius, radius to radius,
                )
            }
            corners.forEachIndexed { i, (x, z) ->
                verts += x
                verts += y
                verts += z
                verts += uvs[i].first
                verts += uvs[i].second
            }
        }
        addCap(-halfHeight, yUp = true)
        addCap(halfHeight, yUp = false)
        roomCapBuffer = verts.toFloatArray().toFloatBuffer()
        roomCapVertexCount = verts.size / 5
    }

    private fun drawRoomCaps() {
        if (wallpaperTextureId == 0 || roomCapVertexCount == 0) return
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(wallpaperProgram)
        GLES20.glUniformMatrix4fv(wallpaperMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, wallpaperTextureId)
        GLES20.glUniform1i(wallpaperSamplerHandle, 0)
        roomCapBuffer.position(0)
        GLES20.glEnableVertexAttribArray(wallpaperPositionHandle)
        GLES20.glVertexAttribPointer(
            wallpaperPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            20,
            roomCapBuffer,
        )
        GLES20.glEnableVertexAttribArray(wallpaperTexCoordHandle)
        roomCapBuffer.position(3)
        GLES20.glVertexAttribPointer(
            wallpaperTexCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            20,
            roomCapBuffer,
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, roomCapVertexCount)
        GLES20.glDisableVertexAttribArray(wallpaperPositionHandle)
        GLES20.glDisableVertexAttribArray(wallpaperTexCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun drawWallpaperCylinder() {
        if (wallpaperTextureId == 0) return
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(wallpaperProgram)
        GLES20.glUniformMatrix4fv(wallpaperMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, wallpaperTextureId)
        GLES20.glUniform1i(wallpaperSamplerHandle, 0)

        wallpaperMeshBuffer.position(0)
        GLES20.glEnableVertexAttribArray(wallpaperPositionHandle)
        GLES20.glVertexAttribPointer(
            wallpaperPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            20,
            wallpaperMeshBuffer,
        )
        GLES20.glEnableVertexAttribArray(wallpaperTexCoordHandle)
        wallpaperMeshBuffer.position(3)
        GLES20.glVertexAttribPointer(
            wallpaperTexCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            20,
            wallpaperMeshBuffer,
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, wallpaperMeshVertexCount)

        GLES20.glDisableVertexAttribArray(wallpaperPositionHandle)
        GLES20.glDisableVertexAttribArray(wallpaperTexCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun drawCylinderGuideLine() {
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(lineProgram)
        GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(linePositionHandle)
        cylinderLineBuffer.position(0)
        GLES20.glVertexAttribPointer(linePositionHandle, 3, GLES20.GL_FLOAT, false, 0, cylinderLineBuffer)
        GLES20.glUniform4f(lineColorHandle, 0.12f, 0.75f, 0.78f, 0.35f)
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, cylinderLineVertexCount)
        GLES20.glDisableVertexAttribArray(linePositionHandle)
    }

    private fun drawPanelGuides() {
        val slots = panelGuideCenters.ifEmpty {
            WorkspaceCylinderGrid.guideSlotsForPanels(Workspace.defaultPanels())
        }
        slots.forEach { slot ->
            drawPanelGuideQuad(slot.centerX, slot.centerY, slot.widthNorm, slot.heightNorm)
        }
    }

    private fun drawPanelGuideQuad(
        centerX: Float,
        centerY: Float,
        widthNorm: Float,
        heightNorm: Float,
    ) {
        val frame = worldFrameForSlot(centerX, centerY, widthNorm, heightNorm)
        drawLineQuad(frame, color = floatArrayOf(0.03f, 0.85f, 0.82f, 0.55f))
    }

    private fun drawTexturedPanels() {
        pendingTextures.forEach { snapshot ->
            val entry = uploadedTextures[snapshot.panelId] ?: return@forEach
            val frame = worldFrameForSnapshot(snapshot)
            drawTexturedQuad(entry.textureId, frame)
        }
    }

    private fun drawTexturedQuad(textureId: Int, frame: WorkspaceCylinderGeometry.PanelWorldFrame) {
        buildModelMatrix(frame)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUseProgram(texturedProgram)
        GLES20.glUniformMatrix4fv(texMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(texSamplerHandle, 0)

        quadBuffer.position(0)
        GLES20.glEnableVertexAttribArray(texPositionHandle)
        GLES20.glVertexAttribPointer(texPositionHandle, 3, GLES20.GL_FLOAT, false, 20, quadBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        quadBuffer.position(3)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, quadBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(texPositionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun drawLineQuad(frame: WorkspaceCylinderGeometry.PanelWorldFrame, color: FloatArray) {
        buildModelMatrix(frame)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        val corners = floatArrayOf(
            -0.5f, -0.5f, 0f,
            0.5f, -0.5f, 0f,
            0.5f, 0.5f, 0f,
            -0.5f, 0.5f, 0f,
            -0.5f, -0.5f, 0f,
        )
        val buffer = corners.toFloatBuffer()
        GLES20.glUseProgram(lineProgram)
        GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(linePositionHandle)
        buffer.position(0)
        GLES20.glVertexAttribPointer(linePositionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glUniform4f(lineColorHandle, color[0], color[1], color[2], color[3])
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, 5)
        GLES20.glDisableVertexAttribArray(linePositionHandle)
    }

    private fun buildModelMatrix(frame: WorkspaceCylinderGeometry.PanelWorldFrame) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, frame.positionX, frame.positionY, frame.positionZ)
        Matrix.rotateM(modelMatrix, 0, frame.rotationYDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, frame.rotationXDeg, 1f, 0f, 0f)
        Matrix.scaleM(
            modelMatrix,
            0,
            frame.widthScene * frame.scale,
            frame.heightScene * frame.scale,
            1f,
        )
    }

    private fun worldFrameForSnapshot(snapshot: PanelTextureSnapshot): WorkspaceCylinderGeometry.PanelWorldFrame =
        WorkspaceCylinderGeometry.panelWorldFrame(
            centerXNorm = snapshot.centerXNorm,
            centerYNorm = snapshot.centerYNorm,
            widthNorm = snapshot.widthNorm,
            heightNorm = snapshot.heightNorm,
            curvature = curvature,
            workspaceWidth = workspaceWidth,
            workspaceHeight = workspaceHeight,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
        )

    private fun worldFrameForSlot(
        centerX: Float,
        centerY: Float,
        widthNorm: Float,
        heightNorm: Float,
    ): WorkspaceCylinderGeometry.PanelWorldFrame =
        WorkspaceCylinderGeometry.panelWorldFrame(
            centerXNorm = centerX,
            centerYNorm = centerY,
            widthNorm = widthNorm,
            heightNorm = heightNorm,
            curvature = curvature,
            workspaceWidth = workspaceWidth,
            workspaceHeight = workspaceHeight,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
        )

    private fun uploadPendingWallpaper() {
        val bitmap = pendingWallpaper ?: return
        val generation = pendingWallpaperGeneration
        if (generation == uploadedWallpaperGeneration && wallpaperTextureId != 0) return
        if (wallpaperTextureId == 0) {
            wallpaperTextureId = createTextureId()
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, wallpaperTextureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        uploadedWallpaperGeneration = generation
    }

    private fun uploadPendingTextures() {
        pendingTextures.forEach { snapshot ->
            val entry = uploadedTextures.getOrPut(snapshot.panelId) {
                GlTextureEntry(textureId = createTextureId())
            }
            if (entry.generation == snapshot.generation) return@forEach
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, entry.textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, snapshot.bitmap, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            entry.generation = snapshot.generation
        }
    }

    private fun uploadPendingDeskTextures() {
        pendingDeskTextures.forEach { snapshot ->
            val entry = uploadedDeskTextures.getOrPut(snapshot.componentKey) {
                GlTextureEntry(textureId = createTextureId())
            }
            if (entry.generation == snapshot.generation) return@forEach
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, entry.textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, snapshot.bitmap, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            entry.generation = snapshot.generation
        }
    }

    private fun pruneStaleTextures() {
        val activeIds = pendingTextures.map { it.panelId }.toSet()
        val stale = uploadedTextures.keys.filter { it !in activeIds }
        stale.forEach { panelId ->
            uploadedTextures.remove(panelId)?.let { entry ->
                GLES20.glDeleteTextures(1, intArrayOf(entry.textureId), 0)
            }
        }
        val activeDesk = pendingDeskTextures.map { it.componentKey }.toSet()
        uploadedDeskTextures.keys.filter { it !in activeDesk }.forEach { key ->
            uploadedDeskTextures.remove(key)?.let { entry ->
                GLES20.glDeleteTextures(1, intArrayOf(entry.textureId), 0)
            }
        }
    }

    private fun createTextureId(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return ids[0]
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertex)
            GLES20.glAttachShader(program, fragment)
            GLES20.glLinkProgram(program)
        }
    }

    private fun compileShader(type: Int, source: String): Int =
        GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
        }

    private fun FloatArray.toFloatBuffer(): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(this@toFloatBuffer)
            position(0)
        }

    companion object {
        private const val LINE_VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec4 aPosition;
            void main() {
                gl_Position = uMvp * aPosition;
            }
        """
        private const val LINE_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """
        private const val TEXTURED_VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMvp * aPosition;
                vTexCoord = aTexCoord;
            }
        """
        private const val TEXTURED_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTexCoord;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
        private const val WALLPAPER_VERTEX_SHADER = TEXTURED_VERTEX_SHADER
        private const val WALLPAPER_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTexCoord;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float vertical = smoothstep(0.0, 0.12, vTexCoord.y) *
                    smoothstep(1.0, 0.88, vTexCoord.y);
                color.rgb *= mix(0.72, 1.0, vertical);
                gl_FragColor = color;
            }
        """
        private const val LIT_VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aTexCoord;
            varying vec3 vNormal;
            varying vec3 vPosition;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMvp * aPosition;
                vPosition = aPosition.xyz;
                vNormal = aNormal;
                vTexCoord = aTexCoord;
            }
        """
        private const val LIT_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform vec3 uLightPos;
            uniform float uAmbient;
            uniform vec3 uTint;
            uniform float uHighlight;
            uniform int uUseTexture;
            varying vec3 vNormal;
            varying vec3 vPosition;
            varying vec2 vTexCoord;
            void main() {
                vec4 base;
                if (vTexCoord.x < -1.5) {
                    base = vec4(uTint, 0.70);
                } else if (vTexCoord.x < 0.0) {
                    base = vec4(0.16, 0.18, 0.22, 0.32);
                } else if (uUseTexture == 1) {
                    base = texture2D(uTexture, vec2(vTexCoord.x, 1.0 - vTexCoord.y));
                } else {
                    base = vec4(0.18, 0.20, 0.24, 0.10);
                }
                if (base.a < 0.08) discard;
                vec3 n = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vPosition);
                float diffuse = max(dot(n, lightDir), 0.0);
                vec3 lit = base.rgb * (uAmbient + diffuse * 1.35);
                if (uHighlight > 0.5) {
                    lit = mix(lit, uTint, 0.38) * 1.28;
                }
                gl_FragColor = vec4(lit, base.a);
            }
        """
    }
}
