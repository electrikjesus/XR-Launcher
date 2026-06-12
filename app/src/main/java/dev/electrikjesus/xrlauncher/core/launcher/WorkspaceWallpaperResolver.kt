package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWallpaperChoice

object WorkspaceWallpaperResolver {
    fun resolveBitmap(context: Context, choice: WorkspaceWallpaperChoice): Bitmap =
        when (choice) {
            WorkspaceWallpaperChoice.SYSTEM -> SystemWallpaperLoader.loadBitmap(context)
            WorkspaceWallpaperChoice.GRADIENT_TWILIGHT -> SystemWallpaperLoader.createFallbackBitmap()
            WorkspaceWallpaperChoice.GRADIENT_AURORA -> createGradientBitmap(
                top = "#061A2E",
                mid = "#0E3D5C",
                bottom = "#020810",
            )
            WorkspaceWallpaperChoice.GRADIENT_EMISSIVE -> createGradientBitmap(
                top = "#120A24",
                mid = "#2A1450",
                bottom = "#05010A",
            )
        }

    private fun createGradientBitmap(top: String, mid: String, bottom: String): Bitmap {
        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf(
                android.graphics.Color.parseColor(top),
                android.graphics.Color.parseColor(mid),
                android.graphics.Color.parseColor(bottom),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
