package cloud.amnezia.dtm.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import cloud.amnezia.dtm.audio.NetworkTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Зашифрованный сейф для сканов документов, фото и файлов.
 * Файлы лежат во внутренней памяти приложения, каждый зашифрован отдельно.
 */
object Vault {

    data class Item(val name: String, val file: File, val size: Long, val added: Long) {
        val ext: String get() = name.substringAfterLast('.', "").lowercase()
        val isImage: Boolean get() = ext in setOf("jpg", "jpeg", "png", "webp", "heic", "gif")
        val isPdf: Boolean get() = ext == "pdf"
    }

    data class Stamp(val sha256: String, val time: Long, val networkTime: Boolean, val lat: Double?, val lon: Double?)

    private fun metaKey(name: String) = "vaultmeta_$name"

    fun stamp(context: Context, item: Item): Stamp? =
        SecureStore.getString(context, metaKey(item.file.name))?.let { runCatching { JSONObject(it) }.getOrNull() }?.let { o ->
            Stamp(o.optString("sha"), o.optLong("t"), o.optBoolean("net"),
                if (o.has("lat")) o.optDouble("lat") else null, if (o.has("lon")) o.optDouble("lon") else null)
        }

    private fun saveStamp(context: Context, encName: String, sha: String, lat: Double?, lon: Double?) {
        SecureStore.putString(context, metaKey(encName), JSONObject().apply {
            put("sha", sha); put("t", NetworkTime.now()); put("net", NetworkTime.synced)
            if (lat != null && lon != null) { put("lat", lat); put("lon", lon) }
        }.toString())
    }

    private fun dir(context: Context): File = File(context.filesDir, "vault").apply { mkdirs() }

    suspend fun list(context: Context): List<Item> = withContext(Dispatchers.IO) {
        dir(context).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".enc") }
            ?.map { f ->
                val base = f.name.removeSuffix(".enc")
                val idx = base.indexOf("__")
                val ts = if (idx > 0) base.substring(0, idx).toLongOrNull() ?: f.lastModified() else f.lastModified()
                Item(if (idx >= 0) base.substring(idx + 2) else base, f, f.length(), ts)
            }
            ?.sortedByDescending { it.added }
            ?: emptyList()
    }

    fun count(context: Context): Int =
        dir(context).listFiles()?.count { it.name.endsWith(".enc") } ?: 0

    private fun target(context: Context, displayName: String): File {
        val safe = displayName.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(80).ifBlank { "file" }
        return File(dir(context), "${System.currentTimeMillis()}__$safe.enc")
    }

    suspend fun importUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val name = queryName(context, uri)
        context.contentResolver.openInputStream(uri)?.use { Crypto.encryptStream(context, it, target(context, name)) }
    }

    /** Импорт снимка с камеры: сохраняем отпечаток исходника, время и координаты. */
    suspend fun importFile(context: Context, src: File, displayName: String, lat: Double? = null, lon: Double? = null) = withContext(Dispatchers.IO) {
        val sha = runCatching { Crypto.sha256(src) }.getOrNull()
        val t = target(context, displayName)
        try {
            Crypto.encryptFile(context, src, t)
            if (sha != null) saveStamp(context, t.name, sha, lat, lon)
        } finally {
            src.delete()
        }
    }

    suspend fun decrypt(context: Context, item: Item): File = withContext(Dispatchers.IO) {
        Crypto.decryptTo(context, item.file, File(Crypto.viewDir(context), item.name))
    }

    fun delete(item: Item): Boolean = item.file.delete()

    private fun queryName(context: Context, uri: Uri): String {
        var name = "file"
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        } catch (_: Exception) {
        }
        return name
    }
}
