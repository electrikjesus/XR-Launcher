package dev.electrikjesus.xrlauncher.ui.spatial.gles

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import dev.electrikjesus.xrlauncher.core.workspace.PanelTextureSnapshot
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceGlesConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * GLES 2.0 inner-cylinder scene: wireframe guides + textured panel quads on the wall.
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

    private lateinit var cylinderBuffer: FloatBuffer
    private var cylinderVertexCount = 0

    private val quadBuffer: FloatBuffer = floatArrayOf(
        -0.5f, -0.5f, 0f, 0f, 1f,
        0.5f, -0.5f, 0f, 1f, 1f,
        -0.5f, 0.5f, 0f, 0f, 0f,
        0.5f, 0.5f, 0f, 1f, 0f,
    ).toFloatBuffer()

    @Volatile
    private var pendingTextures: List<PanelTextureSnapshot> = emptyList()

    private data class GlTextureEntry(
        val textureId: Int,
        var generation: Long = -1L,
    )

    private val uploadedTextures = LinkedHashMap<String, GlTextureEntry>()

    fun setPanelTextures(snapshots: List<PanelTextureSnapshot>) {
        pendingTextures = snapshots
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
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

        rebuildCylinderMesh()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidthPx = width.toFloat().coerceAtLeast(1f)
        viewportHeightPx = height.toFloat().coerceAtLeast(1f)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projectionMatrix, 0, 52f, aspect, 0.05f, 40f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        buildViewMatrix()
        applySceneSpan()
        uploadPendingTextures()
        pruneStaleTextures()

        if (WorkspaceGlesConfig.showGuideWireframe && curvature > 0.01f) {
            drawCylinder()
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
        cylinderBuffer = verts.toFloatBuffer()
        cylinderVertexCount = segments + 1
    }

    private fun drawCylinder() {
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUseProgram(lineProgram)
        GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(linePositionHandle)
        cylinderBuffer.position(0)
        GLES20.glVertexAttribPointer(linePositionHandle, 3, GLES20.GL_FLOAT, false, 0, cylinderBuffer)
        GLES20.glUniform4f(lineColorHandle, 0.12f, 0.75f, 0.78f, 0.35f)
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, cylinderVertexCount)
        GLES20.glDisableVertexAttribArray(linePositionHandle)
    }

    private fun drawPanelGuides() {
        val slots = listOf(
            0.17f to 0.25f,
            0.17f to 0.74f,
            0.50f to 0.50f,
            0.83f to 0.50f,
        )
        slots.forEach { (x, y) -> drawPanelGuideQuad(x, y) }
    }

    private fun drawPanelGuideQuad(centerX: Float, centerY: Float) {
        val frame = worldFrameForSlot(centerX, centerY, 0.22f, 0.28f)
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
    }
}
