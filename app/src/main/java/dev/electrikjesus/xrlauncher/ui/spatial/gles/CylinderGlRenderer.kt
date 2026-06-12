package dev.electrikjesus.xrlauncher.ui.spatial.gles

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import dev.electrikjesus.xrlauncher.core.workspace.PanelTextureSnapshot
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGrid
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceGlesConfig
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

    private lateinit var cylinderLineBuffer: FloatBuffer
    private var cylinderLineVertexCount = 0

    private lateinit var wallpaperMeshBuffer: FloatBuffer
    private var wallpaperMeshVertexCount = 0

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

    fun setPanelTextures(snapshots: List<PanelTextureSnapshot>) {
        pendingTextures = snapshots
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

        rebuildCylinderMeshes()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidthPx = width.toFloat().coerceAtLeast(1f)
        viewportHeightPx = height.toFloat().coerceAtLeast(1f)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projectionMatrix, 0, 52f, aspect, 0.05f, 40f)
        rebuildCylinderMeshes()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        buildViewMatrix()
        applySceneSpan()
        uploadPendingWallpaper()
        uploadPendingTextures()
        pruneStaleTextures()

        if (WorkspaceGlesConfig.showWallpaperCylinder && curvature > 0.01f) {
            drawWallpaperCylinder()
        }
        if (WorkspaceGlesConfig.showGuideWireframe && curvature > 0.01f) {
            drawCylinderGuideLine()
            drawPanelGuides()
        }
        if (WorkspaceGlesConfig.texturedPanelsEnabled && curvature > 0.01f) {
            drawTexturedPanels()
        }
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
        Matrix.translateM(viewMatrix, 0, camera.panNormX * 1.4f, camera.panNormY * 0.9f, 0f)
    }

    private fun applySceneSpan() {
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
        val horizSegments = 56
        val vertSegments = 28
        val c = curvature.coerceIn(0f, 1f)
        val halfArcRad = Math.toRadians(
            (WorkspaceCylinderGeometry.MAX_ARC_YAW_DEGREES * c * workspaceWidth) / 2.0,
        ).toFloat()
        val radius = WorkspaceCylinderGeometry.sceneRadiusX(viewportWidthPx, workspaceWidth, c)
        val baseDepth = WorkspaceCylinderGeometry.sceneBaseDepth(viewportWidthPx, c)
        val wallHeight = 2.4f
        val halfHeight = wallHeight / 2f

        val verts = mutableListOf<Float>()
        for (row in 0..vertSegments) {
            val v = row / vertSegments.toFloat()
            val y = -halfHeight + v * wallHeight
            for (col in 0..horizSegments) {
                val t = col / horizSegments.toFloat()
                val theta = -halfArcRad + t * 2f * halfArcRad
                val x = radius * sin(theta)
                val z = -(baseDepth + radius * (1f - cos(theta)))
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

    private fun pruneStaleTextures() {
        val activeIds = pendingTextures.map { it.panelId }.toSet()
        val stale = uploadedTextures.keys.filter { it !in activeIds }
        stale.forEach { panelId ->
            uploadedTextures.remove(panelId)?.let { entry ->
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
                float vertical = smoothstep(0.0, 0.14, vTexCoord.y) *
                    smoothstep(1.0, 0.86, vTexCoord.y);
                float horizontal = smoothstep(0.0, 0.06, vTexCoord.x) *
                    smoothstep(1.0, 0.94, vTexCoord.x);
                float vignette = vertical * horizontal;
                color.rgb *= mix(0.55, 1.0, vignette);
                gl_FragColor = color;
            }
        """
    }
}
