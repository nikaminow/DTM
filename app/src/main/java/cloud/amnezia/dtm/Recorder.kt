package cloud.amnezia.dtm

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.amnezia.dtm.audio.NetworkTime
import cloud.amnezia.dtm.data.Crypto
import cloud.amnezia.dtm.data.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Состояние записи (наблюдается из UI) и доступ к сохранённым записям.
 *
 * Запись ведётся кусками по ~30 секунд. Каждый кусок — самостоятельный m4a-файл,
 * он шифруется сразу после завершения. Если телефон выключат на середине записи,
 * уцелеют все куски, кроме последнего. Куски одной записи объединены sessionId.
 */
object Recorder {
    var isRecording by mutableStateOf(false)
    var startedAtMs by mutableLongStateOf(0L)
    var lastError by mutableStateOf<String?>(null)
    var savedTick by mutableIntStateOf(0)

    const val CHUNK_MS = 30_000

    /** Один сохранённый кусок записи с метаданными-манифестом для суда. */
    data class Chunk(
        val file: File,
        val sessionId: Long,
        val part: Int,
        val startUtc: Long,
        val endUtc: Long,
        val networkTime: Boolean,
        val sha256: String,
        val size: Long,
        val lat: Double?,
        val lon: Double?,
    ) {
        val plainName: String get() = file.name.removeSuffix(".enc")
        val durationSec: Long get() = ((endUtc - startUtc) / 1000).coerceAtLeast(0)
    }

    /** Запись целиком — группа кусков одной сессии. */
    data class Item(val sessionId: Long, val chunks: List<Chunk>) {
        val startUtc: Long get() = chunks.minOf { it.startUtc }
        val durationSec: Long get() = chunks.sumOf { it.durationSec }
        val size: Long get() = chunks.sumOf { it.size }
        val networkTime: Boolean get() = chunks.all { it.networkTime }
        val lat: Double? get() = chunks.firstNotNullOfOrNull { it.lat }
        val lon: Double? get() = chunks.firstNotNullOfOrNull { it.lon }
    }

    private fun encDir(context: Context): File = File(context.filesDir, "recordings").apply { mkdirs() }
    private fun tmpDir(context: Context): File = File(context.filesDir, "rec_tmp").apply { mkdirs() }

    fun tempChunkFile(context: Context, sessionId: Long, part: Int): File =
        File(tmpDir(context), "${sessionId}_$part.m4a")

    /** Шифрует завершённый кусок и сохраняет его манифест. Не выполняется параллельно с восстановлением. */
    @Synchronized
    fun finalizeChunk(context: Context, plain: File, sessionId: Long, part: Int, startUtc: Long, endUtc: Long,
                      lat: Double?, lon: Double?) {
        if (!plain.exists() || plain.length() == 0L) { plain.delete(); return }
        val sha = Crypto.sha256(plain)
        val target = File(encDir(context), "${sessionId}_${part}.m4a.enc")
        Crypto.encryptFile(context, plain, target)
        SecureStore.putString(context, metaKey(target.name), JSONObject().apply {
            put("session", sessionId); put("part", part)
            put("start", startUtc); put("end", endUtc)
            put("net", NetworkTime.synced); put("sha", sha); put("size", plain.length())
            if (lat != null && lon != null) { put("lat", lat); put("lon", lon) }
        }.toString())
        plain.delete()
        savedTick++
    }

    /** Дошифровывает куски, оставшиеся после аварийного закрытия приложения. */
    @Synchronized
    fun recoverOrphans(context: Context) {
        if (isRecording) return
        tmpDir(context).listFiles()?.forEach { f ->
            // Свежий файл может прямо сейчас сохраняться штатно — не трогаем его.
            if (System.currentTimeMillis() - f.lastModified() < 20_000) return@forEach
            try {
                val name = f.nameWithoutExtension // sessionId_part
                val sid = name.substringBefore('_').toLongOrNull() ?: return@forEach
                val part = name.substringAfter('_').toIntOrNull() ?: 0
                if (f.length() > 0) finalizeChunk(context, f, sid, part, f.lastModified(), f.lastModified(), null, null)
                else f.delete()
            } catch (_: Exception) { f.delete() }
        }
    }

    fun count(context: Context): Int =
        encDir(context).listFiles()?.filter { it.name.endsWith(".enc") }?.map { f ->
            // Новые файлы: «сессия_часть»; старые (DTM_дата…) — каждый файл отдельная запись.
            val prefix = f.name.substringBefore('_')
            if (prefix.all(Char::isDigit)) prefix else f.name
        }?.distinct()?.size ?: 0

    suspend fun list(context: Context): List<Item> = withContext(Dispatchers.IO) {
        recoverOrphans(context)
        val chunks = encDir(context).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".enc") }
            ?.mapNotNull { f ->
                // Записи старых версий: без описания или без номера сессии — каждая отдельно.
                val meta = SecureStore.getString(context, metaKey(f.name))?.let { runCatching { JSONObject(it) }.getOrNull() }
                    ?: JSONObject().put("start", f.lastModified()).put("end", f.lastModified()).put("size", f.length())
                Chunk(
                    file = f,
                    sessionId = if (meta.has("session")) meta.optLong("session") else meta.optLong("start", f.lastModified()),
                    part = meta.optInt("part"),
                    startUtc = meta.optLong("start"),
                    endUtc = meta.optLong("end"),
                    networkTime = meta.optBoolean("net"),
                    sha256 = meta.optString("sha"),
                    size = meta.optLong("size", f.length()),
                    lat = if (meta.has("lat")) meta.optDouble("lat") else null,
                    lon = if (meta.has("lon")) meta.optDouble("lon") else null,
                )
            }
            ?: emptyList()
        chunks.groupBy { it.sessionId }
            .map { (sid, cs) -> Item(sid, cs.sortedBy { it.part }) }
            .sortedByDescending { it.startUtc }
    }

    suspend fun decryptChunk(context: Context, chunk: Chunk): File = withContext(Dispatchers.IO) {
        Crypto.decryptTo(context, chunk.file, File(Crypto.viewDir(context), chunk.plainName))
    }

    fun delete(context: Context, item: Item) {
        item.chunks.forEach { c ->
            c.file.delete()
            SecureStore.remove(context, metaKey(c.file.name))
        }
        savedTick++
    }

    fun newSessionFileName(sessionId: Long): String =
        tr("Запись_") + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(sessionId))

    private fun metaKey(encName: String) = "rec_meta_$encName"
}
