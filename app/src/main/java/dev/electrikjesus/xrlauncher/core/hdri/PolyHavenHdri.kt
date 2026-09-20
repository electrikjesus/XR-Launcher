package dev.electrikjesus.xrlauncher.core.hdri

/** One Poly Haven HDRI listed for the in-app picker. */
data class PolyHavenHdri(
    val id: String,
    val name: String,
    val downloadCount: Int,
    val thumbnailUrl: String,
    val authors: String,
)
