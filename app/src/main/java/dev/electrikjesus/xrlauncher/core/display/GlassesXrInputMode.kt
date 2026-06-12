package dev.electrikjesus.xrlauncher.core.display

/** How the glasses workspace is steered during an active session. */
enum class GlassesXrInputMode {
    /** Phone touchpad moves cursor; optional phone gyro moves cursor (companion motion). */
    COMPANION,

    /** RayNeo USB HID IMU drives mouse-look view pan via companion cursor (not look sliders). */
    GLASSES_HEAD_TRACKING,
}
