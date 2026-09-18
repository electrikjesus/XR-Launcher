package dev.electrikjesus.xrlauncher.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.electrikjesus.xrlauncher.core.workspace.PerspectiveCursorProbe

class PerspectiveProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == PerspectiveCursorProbe.ACTION) {
            PerspectiveCursorProbe.requestPlay()
        }
    }
}
