package dev.electrikjesus.xrlauncher.core.workspace

/** One primary (or nested) wedge in the BumpDesk radial menu. */
data class RadialMenuItem(
    val label: String,
    val iconRes: Int? = null,
    val subItems: List<RadialMenuItem>? = null,
    val action: (() -> Unit)? = null,
)
