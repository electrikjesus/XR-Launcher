package dev.electrikjesus.xrlauncher.core.input

/**
 * Pure dwell-click progress: stable target fills to 1.0, then fires and enters cooldown.
 * Press or target change resets fill (cooldown still runs after a fire).
 */
class DwellClickProgress(
    var dwellMs: Long = DwellClickStore.DEFAULT_DWELL_MS,
    var cooldownMs: Long = 300L,
) {
    var progress: Float = 0f
        private set

    private var targetKey: String? = null
    private var cooldownRemainingMs: Float = 0f

    /** @return true when a click should be delivered this tick. */
    fun tick(
        active: Boolean,
        targetKey: String?,
        pressed: Boolean,
        deltaMs: Float,
    ): Boolean {
        val dt = deltaMs.coerceAtLeast(0f)
        if (cooldownRemainingMs > 0f) {
            cooldownRemainingMs = (cooldownRemainingMs - dt).coerceAtLeast(0f)
            progress = 0f
            this.targetKey = targetKey
            return false
        }
        if (!active || pressed || targetKey == null) {
            progress = 0f
            this.targetKey = targetKey
            return false
        }
        if (targetKey != this.targetKey) {
            this.targetKey = targetKey
            progress = 0f
        }
        val dwell = dwellMs.coerceAtLeast(1L).toFloat()
        progress = (progress + dt / dwell).coerceAtMost(1f)
        if (progress < 1f) return false
        progress = 0f
        cooldownRemainingMs = cooldownMs.toFloat()
        return true
    }

    fun reset() {
        progress = 0f
        targetKey = null
        cooldownRemainingMs = 0f
    }
}
