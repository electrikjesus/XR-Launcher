package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HomeSpacePaneMeshTest {
    @Test
    fun frontFace_sitsOnTheInnerSphere() {
        val mesh = HomeSpaceScene.paneMesh(0f, 1920f, 1080f)
        val inner = HomeSpaceScene.sphereRadius() - HomeSpacePaneMesh.THICKNESS
        mesh.frontVertices().forEach { vertex ->
            assertEquals(inner, vertex.length(), 0.04f)
        }
    }

    @Test
    fun frontFace_bowsOffTheCornerChord() {
        val mesh = HomeSpaceScene.paneMesh(0f, 1920f, 1080f)
        val left = mesh.frontVertexNear(0f, 0.5f)
        val right = mesh.frontVertexNear(1f, 0.5f)
        val mid = mesh.frontVertexNear(0.5f, 0.5f)
        val chord = Vec3(
            x = (left.x + right.x) * 0.5f,
            y = (left.y + right.y) * 0.5f,
            z = (left.z + right.z) * 0.5f,
        )
        assertTrue(
            "sphere midpoint should sit farther from the camera than a straight chord",
            mid.length() > chord.length() + 0.004f,
        )
    }

    @Test
    fun mesh_hasThicknessTowardTheRoom() {
        val mesh = HomeSpaceScene.paneMesh(0f, 1920f, 1080f)
        val front = mesh.frontVertexNear(0.5f, 0.5f)
        val farthest = (0 until mesh.vertexCount).maxOf { mesh.position(it).length() }
        assertTrue(farthest > front.length() + HomeSpacePaneMesh.THICKNESS * 0.5f)
    }

    @Test
    fun perspective_turnsASidePaneIntoATrapezoid() {
        val mesh = HomeSpaceScene.paneMesh(1f, 1920f, 1080f)
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val bl = HomeSpaceScene.projectToView(mesh.frontVertexNear(0f, 0f), camera, 1920f, 1080f)
        val br = HomeSpaceScene.projectToView(mesh.frontVertexNear(1f, 0f), camera, 1920f, 1080f)
        val tl = HomeSpaceScene.projectToView(mesh.frontVertexNear(0f, 1f), camera, 1920f, 1080f)
        val tr = HomeSpaceScene.projectToView(mesh.frontVertexNear(1f, 1f), camera, 1920f, 1080f)
        val bottomWidth = abs(br.x - bl.x)
        val topWidth = abs(tr.x - tl.x)
        val leftHeight = abs(tl.y - bl.y)
        val rightHeight = abs(tr.y - br.y)
        assertTrue("looking at a side pane, vertical edges must not stay parallel", abs(leftHeight - rightHeight) > 8f)
        assertTrue(bottomWidth > 10f && topWidth > 10f)
    }
}
