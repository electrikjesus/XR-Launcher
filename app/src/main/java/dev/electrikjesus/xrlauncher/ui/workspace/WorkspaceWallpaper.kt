package dev.electrikjesus.xrlauncher.ui.workspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import dev.electrikjesus.xrlauncher.core.launcher.WorkspaceWallpaperResolver
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWallpaperChoice
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWraparound

/**
 * Flat workspace backdrop with cursor parallax — stays outside [WorkspaceWraparoundLayer]
 * so it does not inherit cylinder scale/rotation (avoids misaligned GLES debug layers).
 */
@Composable
fun WorkspaceWallpaper(
    wallpaperChoice: WorkspaceWallpaperChoice = WorkspaceWallpaperChoice.SYSTEM,
    hdriAssetId: String = "",
    parallaxX: Float = 0f,
    parallaxY: Float = 0f,
    lookYawDegrees: Float = 0f,
    lookPitchDegrees: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    var wallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    val backdropYaw = lookYawDegrees * WorkspaceWraparound.BACKDROP_LOOK_RATIO
    val backdropPitch = lookPitchDegrees * WorkspaceWraparound.BACKDROP_LOOK_RATIO

    DisposableEffect(context, wallpaperChoice) {
        if (wallpaperChoice != WorkspaceWallpaperChoice.SYSTEM) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
                    reloadToken++
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    androidx.compose.runtime.LaunchedEffect(context, wallpaperChoice, hdriAssetId, reloadToken) {
        wallpaper = WorkspaceWallpaperResolver.resolveBitmap(context, wallpaperChoice, hdriAssetId)
            .asImageBitmap()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        wallpaper?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = -backdropYaw
                        rotationX = -backdropPitch
                        scaleX = 1.06f
                        scaleY = 1.06f
                        translationX = parallaxX * 24f
                        translationY = parallaxY * 16f
                        transformOrigin = TransformOrigin(0.5f, 0.45f)
                    },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f)),
        )
        SpatialEnvironmentScrim(modifier = Modifier.fillMaxSize())
    }
}

/** Soft horizon glow + vignette so a 2D wallpaper reads more like Android XR Home Space. */
@Composable
private fun SpatialEnvironmentScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.18f),
                    Color.Black.copy(alpha = 0.55f),
                ),
            ),
        ),
    )
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x66101828),
                    Color.Transparent,
                    Color(0x99020812),
                ),
            ),
        ),
    )
}
