package cloud.amnezia.dtm.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Дневник событий: что, когда и как произошло. Хранится зашифрованно. */
data class JournalEntry(
    val id: Long,
    val time: Long,
    val kind: String,
    val text: String,
    val witnesses: String = "",
    val attachments: List<String> = emptyList(), // имена зашифрованных файлов вложений
)

object Journal {

    val KINDS = listOf(
        "Психологическое", "Физическое", "Угрозы", "Контроль и слежка",
        "Экономическое", "Сексуализированное", "Другое"
    )

    private fun file(context: Context) = File(context.filesDir, "journal.enc")

    suspend fun load(context: Context): List<JournalEntry> = withContext(Dispatchers.IO) {
        val raw = try { Crypto.readText(context, file(context)) } catch (_: Exception) { null }
            ?: return@withContext emptyList()
        try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val att = o.optJSONArray("att")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                JournalEntry(o.getLong("id"), o.getLong("time"), o.optString("kind"), o.optString("text"), o.optString("wit"), att)
            }.sortedByDescending { it.time }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun save(context: Context, list: List<JournalEntry>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("time", e.time); put("kind", e.kind); put("text", e.text); put("wit", e.witnesses)
                put("att", JSONArray(e.attachments))
            })
        }
        Crypto.writeText(context, file(context), arr.toString())
        // Удаляем файлы вложений, на которые больше никто не ссылается.
        JournalMedia.gc(context, list.flatMap { it.attachments }.toSet())
    }

    suspend fun count(context: Context): Int = load(context).size
}

/** Вложения дневника (фото, видео, аудио). Каждый файл зашифрован отдельно. */
object JournalMedia {

    enum class Kind { IMAGE, VIDEO, AUDIO, OTHER }

    data class Att(val name: String, val file: File, val display: String) {
        private val ext get() = display.substringAfterLast('.', "").lowercase()
        val kind: Kind get() = when {
            ext in setOf("jpg", "jpeg", "png", "webp", "heic", "gif") -> Kind.IMAGE
            ext in setOf("mp4", "3gp", "mkv", "webm", "mov") -> Kind.VIDEO
            ext in setOf("m4a", "mp3", "ogg", "wav", "aac", "amr") -> Kind.AUDIO
            else -> Kind.OTHER
        }
    }

    private fun dir(context: Context) = File(context.filesDir, "journal_att").apply { mkdirs() }

    private fun make(context: Context, display: String): File {
        val safe = display.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(60).ifBlank { "file" }
        return File(dir(context), "${System.currentTimeMillis()}_${(0..9999).random()}__$safe.enc")
    }

    private fun displayOf(fileName: String): String {
        val base = fileName.removeSuffix(".enc")
        val i = base.indexOf("__")
        return if (i >= 0) base.substring(i + 2) else base
    }

    fun att(context: Context, name: String): Att = Att(name, File(dir(context), name), displayOf(name))

    suspend fun importUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val display = queryName(context, uri)
        val target = make(context, display)
        context.contentResolver.openInputStream(uri)?.use { Crypto.encryptStream(context, it, target) }
        target.name
    }

    suspend fun importFile(context: Context, src: File, display: String): String = withContext(Dispatchers.IO) {
        val target = make(context, display)
        try { Crypto.encryptFile(context, src, target) } finally { src.delete() }
        target.name
    }

    suspend fun decrypt(context: Context, name: String): File = withContext(Dispatchers.IO) {
        val a = att(context, name)
        Crypto.decryptTo(context, a.file, File(Crypto.viewDir(context), a.display))
    }

    fun gc(context: Context, keep: Set<String>) {
        dir(context).listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    private fun queryName(context: Context, uri: Uri): String {
        var name = "file"
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        } catch (_: Exception) {}
        return name
    }
}
