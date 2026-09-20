package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import java.util.UUID

/** BumpDesk [Pile.LayoutMode] for sphere desk groups. */
enum class DeskPileMode {
    /** Physical stack — collapsed as layered icons; expand fans members. */
    STACK,
    /** Folder — collapsed 2×2 preview; expand opens a grid of members. */
    FOLDER,
}

data class DeskPile(
    val id: String,
    val mode: DeskPileMode,
    val name: String,
    val members: List<HomeSpaceDesk.AppRef>,
    val yawDeg: Float,
    val pitchDeg: Float,
    val expanded: Boolean = false,
    /** BumpDesk [Pile.isFannedOut] — members spread in an arc without folder-grid expand. */
    val fannedOut: Boolean = false,
) {
    val componentKey: String get() = id
    val isFolder: Boolean get() = mode == DeskPileMode.FOLDER
    val isStack: Boolean get() = mode == DeskPileMode.STACK
    /** Collapsed preview face is hidden while members are shown. */
    val showsMembers: Boolean get() = expanded || fannedOut

    fun kind(): HomeSpaceDesk.Kind = when (mode) {
        DeskPileMode.STACK -> HomeSpaceDesk.Kind.PILE_STACK
        DeskPileMode.FOLDER -> HomeSpaceDesk.Kind.PILE_FOLDER
    }

    companion object {
        const val KEY_PREFIX = "pile_"

        fun newId(): String = KEY_PREFIX + UUID.randomUUID().toString().take(8)

        fun isPileKey(key: String): Boolean = key.startsWith(KEY_PREFIX)

        fun defaultName(mode: DeskPileMode, members: List<HomeSpaceDesk.AppRef>): String = when (mode) {
            DeskPileMode.FOLDER -> members.firstOrNull()?.label?.take(12)?.let { "$it+" }
                ?: "Folder"
            DeskPileMode.STACK -> "Pile"
        }
    }
}

object DeskPileOps {
    /**
     * Build a pile from free desk apps at their yaw/pitch centroid.
     * @return null when fewer than 2 APP members match [keys].
     */
    fun create(
        placed: List<HomeSpaceDesk.Placed>,
        keys: Set<String>,
        mode: DeskPileMode,
    ): Pair<DeskPile, List<HomeSpaceDesk.Placed>>? {
        val targets = placed.filter {
            it.app.componentKey in keys && it.app.kind == HomeSpaceDesk.Kind.APP
        }
        if (targets.size < 2) return null
        val yaw = targets.map { it.yawDeg }.average().toFloat()
        val pitch = targets.map { it.pitchDeg }.average().toFloat()
        val members = targets.map { it.app }
        val pile = DeskPile(
            id = DeskPile.newId(),
            mode = mode,
            name = DeskPile.defaultName(mode, members),
            members = members,
            yawDeg = yaw,
            pitchDeg = pitch,
            expanded = false,
        )
        val removeKeys = targets.map { it.app.componentKey }.toSet()
        val remaining = placed.filter { it.app.componentKey !in removeKeys }
        return pile to remaining
    }

    fun toggleExpanded(pile: DeskPile): DeskPile =
        pile.copy(expanded = !pile.expanded, fannedOut = false)

    fun toggleFan(pile: DeskPile): DeskPile =
        pile.copy(fannedOut = !pile.fannedOut, expanded = false)

    fun collapse(pile: DeskPile): DeskPile = pile.copy(expanded = false, fannedOut = false)

    /** Release members back onto the desk around the pile pose. */
    fun breakApart(
        pile: DeskPile,
        placed: List<HomeSpaceDesk.Placed>,
        yawStepDeg: Float = 6f,
    ): List<HomeSpaceDesk.Placed> {
        val released = pile.members.mapIndexed { index, app ->
            val t = index - (pile.members.size - 1) * 0.5f
            HomeSpaceDesk.Placed(
                app = app,
                yawDeg = pile.yawDeg + t * yawStepDeg,
                pitchDeg = pile.pitchDeg,
            )
        }
        return placed + released
    }

    fun pruneMembers(
        piles: List<DeskPile>,
        validComponentKeys: Set<String>,
    ): List<DeskPile> = piles.mapNotNull { pile ->
        val kept = pile.members.filter { it.componentKey in validComponentKeys }
        when {
            kept.size >= 2 -> pile.copy(members = kept)
            kept.size == 1 -> null // singleton released by caller
            else -> null
        }
    }

    fun singletonReleases(
        piles: List<DeskPile>,
        validComponentKeys: Set<String>,
    ): List<HomeSpaceDesk.Placed> = piles.mapNotNull { pile ->
        val kept = pile.members.filter { it.componentKey in validComponentKeys }
        if (kept.size != 1) return@mapNotNull null
        HomeSpaceDesk.Placed(
            app = kept.first(),
            yawDeg = pile.yawDeg,
            pitchDeg = pile.pitchDeg,
        )
    }

    fun memberKeys(piles: List<DeskPile>): Set<String> =
        piles.flatMap { it.members.map { m -> m.componentKey } }.toSet()
}
