package dev.electrikjesus.xrlauncher.ui.spatial.gles

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * GLES 2.0 inner-cylinder backdrop + panel slot wireframes.
 * Shares [WorkspaceCylinderGeometry.CameraState] with the Compose workspace layer.
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
    var showPanelGuides: Boolean = true

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpHandle = 0

    private lateinit var cylinderBuffer: FloatBuffer
    private var cylinderVertexCount = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMvp")
        rebuildCylinderMesh()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projectionMatrix, 0, 52f, aspect, 0.05f, 40f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
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

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        drawCylinder()
        if (showPanelGuides && curvature > 0.01f) {
            drawPanelGuides()
        }
    }

    fun rebuildCylinderMesh() {
        val segments = 48
        val c = curvature.coerceIn(0f, 1f)
        val halfArcRad = Math.toRadians(
            (WorkspaceCylinderGeometry.MAX_ARC_YAW_DEGREES * c * workspaceWidth) / 2.0,
        ).toFloat()
        val radius = WorkspaceCylinderGeometry.RADIUS_X_FRACTION * c * workspaceWidth
        val verts = FloatArray((segments + 1) * 3)
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val theta = -halfArcRad + t * 2f * halfArcRad
            verts[i * 3] = radius * sin(theta)
            verts[i * 3 + 1] = -0.15f
            verts[i * 3 + 2] = -radius * cos(theta)
        }
        cylinderBuffer = verts.toFloatBuffer()
        cylinderVertexCount = segments + 1
    }

    private fun drawCylinder() {
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        cylinderBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, cylinderBuffer)
        GLES20.glUniform4f(colorHandle, 0.12f, 0.75f, 0.78f, 0.35f)
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, cylinderVertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun drawPanelGuides() {
        val slots = listOf(
            0.17f to 0.25f,
            0.17f to 0.74f,
            0.50f to 0.50f,
            0.83f to 0.50f,
        )
        slots.forEach { (x, y) ->
            drawPanelQuad(x, y)
        }
    }

    private fun drawPanelQuad(centerX: Float, centerY: Float) {
        val c = curvature.coerceIn(0f, 1f)
        val halfArcRad = Math.toRadians(
            (WorkspaceCylinderGeometry.MAX_ARC_YAW_DEGREES * c * workspaceWidth) / 2.0,
        ).toFloat()
        val theta = (centerX - 0.5f) * 2f * halfArcRad
        val radius = WorkspaceCylinderGeometry.RADIUS_X_FRACTION * c * workspaceWidth
        val cx = radius * sin(theta)
        val cz = -radius * cos(theta)
        val cy = (centerY - 0.5f) * 0.8f
        val w = 0.22f
        val h = 0.28f
        val corners = floatArrayOf(
            cx - w, cy - h, cz,
            cx + w, cy - h, cz,
            cx + w, cy + h, cz,
            cx - w, cy + h, cz,
            cx - w, cy - h, cz,
        )
        val buffer = corners.toFloatBuffer()
        val localMvp = mvpMatrix.copyOf()
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, localMvp, 0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        buffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glUniform4f(colorHandle, 0.03f, 0.85f, 0.82f, 0.55f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, 5)
        GLES20.glDisableVertexAttribArray(positionHandle)
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
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec4 aPosition;
            void main() {
                gl_Position = uMvp * aPosition;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """
    }
}
