package dev.electrikjesus.xrlauncher.core.workspace.scene

/**
 * Project a sphere pose (where a radial menu was enacted) into screen pixels so the
 * menu stays world-locked while mouse-look aims the crosshair at wedges.
 */
object RadialMenuWorldLock {
    /**
     * @return screen center (px) or null when the pose is behind / outside the view.
     */
    fun projectToScreenPx(
        yawDeg: Float,
        pitchDeg: Float,
        camera: HomeSpaceScene.Camera,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        sphereScale: Float,
    ): Pair<Float, Float>? {
        if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return null
        val world = HomeSpaceScene.spherePoint(
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            radius = HomeSpaceScene.innerSphereRadius(sphereScale),
        )
        val projected = HomeSpaceScene.projectToView(
            world = world,
            camera = camera,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
        )
        if (projected.z >= -0.05f) return null
        return (viewportWidthPx * 0.5f + projected.x) to (viewportHeightPx * 0.5f + projected.y)
    }
}
