package dev.electrikjesus.xrlauncher.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.hdri.PolyHavenApi
import dev.electrikjesus.xrlauncher.core.hdri.PolyHavenHdri

@Composable
fun PolyHavenHdriPickerDialog(
    selectedAssetId: String,
    onSelect: (PolyHavenHdri) -> Unit,
    onDismiss: () -> Unit,
) {
    var reloadToken by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var hdris by remember { mutableStateOf<List<PolyHavenHdri>>(emptyList()) }

    LaunchedEffect(reloadToken) {
        loading = true
        error = null
        val result = PolyHavenApi.listHdris()
        loading = false
        result.onSuccess { hdris = it }
            .onFailure { error = it.message ?: "Failed to load HDRIs" }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_wallpaper_polyhaven_picker_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_wallpaper_polyhaven_credit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_back))
                    }
                }
                Spacer(Modifier.height(8.dp))
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    error != null -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(error ?: "")
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { reloadToken++ }) {
                            Text(stringResource(R.string.settings_wallpaper_polyhaven_retry))
                        }
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(hdris, key = { it.id }) { hdri ->
                            PolyHavenHdriTile(
                                hdri = hdri,
                                selected = hdri.id == selectedAssetId.ifBlank {
                                    PolyHavenApi.DEFAULT_ASSET_ID
                                },
                                onClick = { onSelect(hdri) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolyHavenHdriTile(
    hdri: PolyHavenHdri,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var thumb by remember(hdri.thumbnailUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(hdri.thumbnailUrl) {
        thumb = PolyHavenApi.downloadThumbnailBitmap(hdri.thumbnailUrl)
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
            )
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = thumb
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = hdri.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp), strokeWidth = 2.dp)
            }
            if (selected) {
                Text(
                    text = stringResource(R.string.settings_wallpaper_polyhaven_selected_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .zIndex(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = hdri.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (hdri.authors.isNotBlank()) {
            Text(
                text = hdri.authors,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
