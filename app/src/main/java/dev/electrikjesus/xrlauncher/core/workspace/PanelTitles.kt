package dev.electrikjesus.xrlauncher.core.workspace

fun panelTitleFor(panel: PanelState): String = panelTitleForId(panel.id, panel.kind)

fun panelTitleForId(panelId: String, kind: PanelKind = PanelKind.WIDGET): String = when (panelId) {
    "widget_clock" -> "Clock"
    "widget_calendar" -> "Calendar"
    "app_drawer" -> "Apps"
    "hotseat" -> "Hotseat"
    "empty_slot" -> "App slot"
    else -> when (kind) {
        PanelKind.WIDGET -> panelId
        PanelKind.APP_DRAWER -> "Apps"
        PanelKind.HOTSEAT -> "Hotseat"
        PanelKind.EMPTY_SLOT -> "App slot"
    }
}

fun PanelKind.supportsWindowControls(): Boolean =
    this == PanelKind.WIDGET || this == PanelKind.EMPTY_SLOT
