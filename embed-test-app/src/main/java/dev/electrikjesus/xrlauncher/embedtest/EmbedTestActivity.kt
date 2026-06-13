package dev.electrikjesus.xrlauncher.embedtest

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Minimal host for panel-embed CI and on-device testing (Phase 3.6). */
class EmbedTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = getString(R.string.embed_test_ready)
                textSize = 18f
                setPadding(48, 48, 48, 48)
            },
        )
    }
}
