package cloud.amnezia.dtm.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    /** Импорт временного файла (например, снимка с камеры); исходник удаляется. */
    suspend fun importFile(context: Context, src: File, displayName: String) = withContext(Dispatchers.IO) {
        try {
            Crypto.encryptFile(context, src, target(context, displayName))
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
