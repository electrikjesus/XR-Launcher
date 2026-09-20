package dev.electrikjesus.xrlauncher.core.workspace

import java.util.Locale

/**
 * Persisted Desktop icons + widgets + piles + All Apps tile pose on the Home Space sphere.
 * Format: `{drawerYaw};{drawerPitch}|{item},{item},…|{pile},{pile},…`
 * App item: `componentKey~label~package~yaw~pitch`
 * Widget item: `widget_{id}~label~package~yaw~pitch~WIDGET~halfW~halfH`
 * Pile: `pile_id~name~STACK|FOLDER~yaw~pitch~key;label;pkg|key;label;pkg|…`
 * (`_` = null drawer field; piles segment optional for older layouts).
 */
object DeskJson {
    private const val FIELD_SEP = "|"
    private const val ITEM_SEP = ","
    private const val ITEM_FIELD_SEP = "~"
    private const val DRAWER_SEP = ";"
    private const val MEMBER_SEP = "^"
    private const val MEMBER_FIELD_SEP = ";"
    private const val NULL_TOKEN = "_"

    fun encode(layout: DeskLayout): String {
        val drawer = listOf(
            layout.drawerYawDeg.toToken(),
            layout.drawerPitchDeg.toToken(),
        ).joinToString(DRAWER_SEP)
        val items = layout.items.joinToString(ITEM_SEP) { item ->
            buildList {
                add(item.componentKey)
                add(item.label.replace(ITEM_FIELD_SEP, " ").replace(ITEM_SEP, " "))
                add(item.packageName)
                add(item.yawDeg.toCompactString())
                add(item.pitchDeg.toCompactString())
                if (item.kind == "WIDGET") {
                    add("WIDGET")
                    add(item.halfWidth?.toCompactString() ?: NULL_TOKEN)
                    add(item.halfHeight?.toCompactString() ?: NULL_TOKEN)
                }
            }.joinToString(ITEM_FIELD_SEP)
        }
        val piles = layout.piles.joinToString(ITEM_SEP) { pile ->
            val members = pile.members.joinToString(MEMBER_SEP) { m ->
                listOf(
                    m.componentKey,
                    m.label.replace(MEMBER_FIELD_SEP, " ").replace(MEMBER_SEP, " "),
                    m.packageName,
                ).joinToString(MEMBER_FIELD_SEP)
            }
            listOf(
                pile.id,
                pile.name.replace(ITEM_FIELD_SEP, " ").replace(ITEM_SEP, " "),
                pile.mode,
                pile.yawDeg.toCompactString(),
                pile.pitchDeg.toCompactString(),
                members,
            ).joinToString(ITEM_FIELD_SEP)
        }
        return listOf(drawer, items, piles).joinToString(FIELD_SEP)
    }

    fun decode(raw: String): DeskLayout {
        if (raw.isBlank()) return DeskLayout()
        val parts = raw.split(FIELD_SEP, limit = 3)
        val drawerFields = parts.getOrElse(0) { "" }.split(DRAWER_SEP)
        val drawerYaw = drawerFields.getOrNull(0).fromToken()
        val drawerPitch = drawerFields.getOrNull(1).fromToken()
        val items = if (parts.size < 2 || parts[1].isBlank()) {
            emptyList()
        } else {
            parts[1].split(ITEM_SEP).mapNotNull { decodeItem(it) }
        }
        val piles = if (parts.size < 3 || parts[2].isBlank()) {
            emptyList()
        } else {
            parts[2].split(ITEM_SEP).mapNotNull { decodePile(it) }
        }
        return DeskLayout(
            items = items,
            piles = piles,
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
        val kind = fields.getOrNull(5)?.takeIf { it == "WIDGET" }
            ?: if (key.startsWith("widget_")) "WIDGET" else "APP"
        val halfW = fields.getOrNull(6).fromToken()
        val halfH = fields.getOrNull(7).fromToken()
        return DeskPlacedItem(
            componentKey = key,
            label = fields[1],
            packageName = fields[2],
            yawDeg = yaw,
            pitchDeg = pitch,
            kind = kind,
            halfWidth = halfW,
            halfHeight = halfH,
        )
    }

    private fun decodePile(raw: String): DeskPileItem? {
        val fields = raw.split(ITEM_FIELD_SEP)
        if (fields.size < 6) return null
        val id = fields[0].takeIf { it.startsWith(DeskPile.KEY_PREFIX) } ?: return null
        val mode = fields[2].takeIf { it == "FOLDER" || it == "STACK" } ?: "STACK"
        val yaw = fields[3].toFloatOrNull() ?: return null
        val pitch = fields[4].toFloatOrNull() ?: return null
        val members = fields[5].split(MEMBER_SEP).mapNotNull { memberRaw ->
            val mf = memberRaw.split(MEMBER_FIELD_SEP)
            if (mf.size < 3 || mf[0].isBlank()) return@mapNotNull null
            DeskPileMemberItem(
                componentKey = mf[0],
                label = mf[1],
                packageName = mf[2],
            )
        }
        if (members.size < 2) return null
        return DeskPileItem(
            id = id,
            name = fields[1].ifBlank { if (mode == "FOLDER") "Folder" else "Pile" },
            mode = mode,
            yawDeg = yaw,
            pitchDeg = pitch,
            members = members,
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
    val kind: String = "APP",
    val halfWidth: Float? = null,
    val halfHeight: Float? = null,
)

data class DeskPileMemberItem(
    val componentKey: String,
    val label: String,
    val packageName: String,
)

data class DeskPileItem(
    val id: String,
    val name: String,
    val mode: String,
    val yawDeg: Float,
    val pitchDeg: Float,
    val members: List<DeskPileMemberItem>,
)

data class DeskLayout(
    val items: List<DeskPlacedItem> = emptyList(),
    val piles: List<DeskPileItem> = emptyList(),
    val drawerYawDeg: Float? = null,
    val drawerPitchDeg: Float? = null,
)
