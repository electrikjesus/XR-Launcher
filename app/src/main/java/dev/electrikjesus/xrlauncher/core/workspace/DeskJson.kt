package dev.electrikjesus.xrlauncher.core.workspace

import java.util.Locale

/**
 * Persisted Desktop icons + All Apps tile pose on the Home Space sphere.
 * Format: `{drawerYaw};{drawerPitch}|{item},{item},…`
 * Item: `componentKey~label~package~yaw~pitch` (`_` = null drawer field).
 */
object DeskJson {
    private const val FIELD_SEP = "|"
    private const val ITEM_SEP = ","
    private const val ITEM_FIELD_SEP = "~"
    private const val DRAWER_SEP = ";"
    private const val NULL_TOKEN = "_"

    fun encode(layout: DeskLayout): String {
        val drawer = listOf(
            layout.drawerYawDeg.toToken(),
            layout.drawerPitchDeg.toToken(),
        ).joinToString(DRAWER_SEP)
        val items = layout.items.joinToString(ITEM_SEP) { item ->
            listOf(
                item.componentKey,
                item.label.replace(ITEM_FIELD_SEP, " ").replace(ITEM_SEP, " "),
                item.packageName,
                item.yawDeg.toCompactString(),
                item.pitchDeg.toCompactString(),
            ).joinToString(ITEM_FIELD_SEP)
        }
        return listOf(drawer, items).joinToString(FIELD_SEP)
    }

    fun decode(raw: String): DeskLayout {
        if (raw.isBlank()) return DeskLayout()
        val parts = raw.split(FIELD_SEP, limit = 2)
        val drawerFields = parts.getOrElse(0) { "" }.split(DRAWER_SEP)
        val drawerYaw = drawerFields.getOrNull(0).fromToken()
        val drawerPitch = drawerFields.getOrNull(1).fromToken()
        val items = if (parts.size < 2 || parts[1].isBlank()) {
            emptyList()
        } else {
            parts[1].split(ITEM_SEP).mapNotNull { decodeItem(it) }
        }
        return DeskLayout(
            items = items,
            drawerYawDeg = drawerYaw,
            drawerPitchDeg = drawerPitch,
        )
    }

    private fun decodeItem(raw: String): DeskPlacedItem? {
        val fields = raw.split(ITEM_FIELD_SEP)
        if (fields.size < 5) return null
        val key = fields[0].takeIf { it.isNotBlank() } ?: return null
        val yaw = fields[3].toFloatOrNull() ?: return null
        val pitch = fields[4].toFloatOrNull() ?: return null
        return DeskPlacedItem(
            componentKey = key,
            label = fields[1],
            packageName = fields[2],
            yawDeg = yaw,
            pitchDeg = pitch,
        )
    }

    private fun Float?.toToken(): String =
        this?.toCompactString() ?: NULL_TOKEN

    private fun String?.fromToken(): Float? =
        this?.takeIf { it.isNotBlank() && it != NULL_TOKEN }?.toFloatOrNull()

    private fun Float.toCompactString(): String = String.format(Locale.US, "%.4f", this)
}

data class DeskPlacedItem(
    val componentKey: String,
    val label: String,
    val packageName: String,
    val yawDeg: Float,
    val pitchDeg: Float,
)

data class DeskLayout(
    val items: List<DeskPlacedItem> = emptyList(),
    val drawerYawDeg: Float? = null,
    val drawerPitchDeg: Float? = null,
)
