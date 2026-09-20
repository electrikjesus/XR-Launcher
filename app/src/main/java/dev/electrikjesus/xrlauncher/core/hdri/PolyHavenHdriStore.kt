package dev.electrikjesus.xrlauncher.core.hdri

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Disk cache for Poly Haven equirectangular LDR previews used as GLES surround textures. */
object PolyHavenHdriStore {
    private const val TAG = "PolyHavenHdriStore"
    private const val DIR = "polyhaven_hdris"
    private val mutex = Mutex()

    suspend fun loadBitmap(context: Context, assetId: String): Bitmap? = mutex.withLock {
        val id = assetId.ifBlank { PolyHavenApi.DEFAULT_ASSET_ID }
        withContext(Dispatchers.IO) {
            val file = cacheFile(context, id)
            if (file.isFile && file.length() > 0L) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { return@withContext it }
            }
            val bitmap = PolyHavenApi.downloadEquirectBitmap(id) ?: return@withContext null
            runCatching {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
            }.onFailure { Log.w(TAG, "cache write failed for $id", it) }
            bitmap
        }
    }

    fun cacheFile(context: Context, assetId: String): File =
        File(File(context.applicationContext.filesDir, DIR), "${assetId.trim()}.jpg")
}
