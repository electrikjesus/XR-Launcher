package dev.electrikjesus.xrlauncher.core.capability

enum class RuntimeTier {
    SPATIAL_DESKTOP,
    PHONE_SHELL,
    EXTERNAL_DISPLAY,
    PROJECTED_GLASSES,
    FULL_SPATIAL,
}

enum class LayoutFormFactor {
    COMPACT,
    EXPANDED,
}

data class DeviceCapabilities(
    val tier: RuntimeTier,
    val formFactor: LayoutFormFactor,
    val hasSecondaryDisplay: Boolean,
    val hasSpatialApi: Boolean,
    val secondaryDisplayIds: List<Int>,
)

object CapabilityLogic {
    fun resolveFormFactor(smallestWidthDp: Int): LayoutFormFactor {
        return if (smallestWidthDp >= WIDTH_EXPANDED_LOWER_BOUND) {
            LayoutFormFactor.EXPANDED
        } else {
            LayoutFormFactor.COMPACT
        }
    }

    fun resolveTier(
        formFactor: LayoutFormFactor,
        hasSecondaryDisplay: Boolean,
        hasSpatialApi: Boolean,
        isProjectedGlassesConnected: Boolean,
    ): RuntimeTier {
        if (isProjectedGlassesConnected) {
            return if (hasSpatialApi) RuntimeTier.FULL_SPATIAL else RuntimeTier.PROJECTED_GLASSES
        }
        if (hasSecondaryDisplay && formFactor == LayoutFormFactor.COMPACT) {
            return RuntimeTier.EXTERNAL_DISPLAY
        }
        if (formFactor == LayoutFormFactor.EXPANDED) {
            return RuntimeTier.SPATIAL_DESKTOP
        }
        return RuntimeTier.PHONE_SHELL
    }

    const val WIDTH_EXPANDED_LOWER_BOUND = 600
}
