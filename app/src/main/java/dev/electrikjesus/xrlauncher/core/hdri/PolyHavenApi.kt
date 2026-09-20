package dev.electrikjesus.xrlauncher.core.hdri

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Thin Poly Haven HTTP client (CC0 assets; API requires attribution + User-Agent).
 * @see <a href="https://polyhaven.com/our-api">Poly Haven API</a>
 */
object PolyHavenApi {
    private const val TAG = "PolyHavenApi"
    const val USER_AGENT = "XRLauncher/0.1.29 (https://github.com/electrikjesus/xrlauncher)"
    const val DEFAULT_ASSET_ID = "kloofendal_48d_partly_cloudy_puresky"
    private const val ASSETS_URL = "https://api.polyhaven.com/assets?t=hdris"
    private const val EQUIRECT_WIDTH = 2048
    private const val EQUIRECT_HEIGHT = 1024

    fun equirectPreviewUrl(assetId: String): String =
        "https://cdn.polyhaven.com/asset_img/primary/${assetId.trim()}.png" +
            "?width=$EQUIRECT_WIDTH&height=$EQUIRECT_HEIGHT"

    fun thumbnailUrl(assetId: String, width: Int = 256): String =
        "https://cdn.polyhaven.com/asset_img/thumbs/${assetId.trim()}.png?width=$width&height=$width"

    suspend fun listHdris(): Result<List<PolyHavenHdri>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = httpGetText(ASSETS_URL)
            parseAssetsJson(body)
        }.onFailure { Log.w(TAG, "listHdris failed", it) }
    }

    suspend fun downloadEquirectBitmap(assetId: String): Bitmap? = withContext(Dispatchers.IO) {
        val id = assetId.ifBlank { DEFAULT_ASSET_ID }
        runCatching {
            val bytes = httpGetBytes(equirectPreviewUrl(id))
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.onFailure {
            Log.w(TAG, "downloadEquirectBitmap($id) failed", it)
        }.getOrNull()
    }

    suspend fun downloadThumbnailBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = httpGetBytes(url)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    internal fun parseAssetsJson(body: String): List<PolyHavenHdri> {
        val root = JSONObject(body)
        val out = ArrayList<PolyHavenHdri>(root.length())
        val keys = root.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val obj = root.optJSONObject(id) ?: continue
            if (obj.optInt("type", -1) != 0) continue
            val authorsObj = obj.optJSONObject("authors")
            val authors = if (authorsObj != null) {
                authorsObj.keys().asSequence().toList().joinToString(", ")
            } else {
                ""
            }
            out += PolyHavenHdri(
                id = id,
                name = obj.optString("name", id),
                downloadCount = obj.optInt("download_count", 0),
                thumbnailUrl = obj.optString("thumbnail_url").ifBlank { thumbnailUrl(id) },
                authors = authors,
            )
        }
        return out.sortedByDescending { it.downloadCount }
    }

    private fun httpGetText(url: String): String {
        val connection = open(url)
        return try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun httpGetBytes(url: String): ByteArray {
        val connection = open(url)
        return try {
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            instanceFollowRedirects = true
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            error("HTTP $code for $url")
        }
        return connection
    }
}
