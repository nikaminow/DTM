package cloud.amnezia.dtm.data

import cloud.amnezia.dtm.tr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

/** Отметка состояния: 1 — плохо, 2 — средне, 3 — отлично. */
data class MoodEntry(val time: Long, val value: Int)

/** Запись о выполненном упражнении для дневника практик. */
data class LogEntry(val time: Long, val title: String, val detail: String)

/** Трекер самочувствия и дневник упражнений. Хранится зашифрованно. */
object Tracker {

    private fun moodFile(context: Context) = File(context.filesDir, "mood.enc")
    private fun logFile(context: Context) = File(context.filesDir, "exlog.enc")

    private fun readArray(context: Context, file: File): JSONArray {
        val text = runCatching { Crypto.readText(context, file) }.getOrNull() ?: return JSONArray()
        return runCatching { JSONArray(text) }.getOrDefault(JSONArray())
    }

    // ---- Настроение ----

    suspend fun moods(context: Context): List<MoodEntry> = withContext(Dispatchers.IO) {
        val arr = readArray(context, moodFile(context))
        (0 until arr.length()).map { arr.getJSONObject(it) }
            .map { MoodEntry(it.getLong("t"), it.getInt("v")) }
            .sortedBy { it.time }
    }

    // «Прочитать → дописать → сохранить» под замком: быстрые нажатия подряд не теряют
    // отметки и не пишут файл одновременно.
    private val moodLock = Mutex()
    private val logLock = Mutex()

    suspend fun addMood(context: Context, value: Int) {
        // Отмечать можно сколько угодно раз в день — на графике берётся среднее за день.
        moodLock.withLock {
            val list = moods(context).toMutableList()
            list.add(MoodEntry(System.currentTimeMillis(), value.coerceIn(1, 3)))
            withContext(Dispatchers.IO) {
                val arr = JSONArray()
                list.sortedBy { it.time }.forEach { arr.put(JSONObject().put("t", it.time).put("v", it.value)) }
                Crypto.writeText(context, moodFile(context), arr.toString())
            }
        }
        addLog(context, tr("Отметка состояния"), moodWord(value))
    }

    suspend fun hasMoodToday(context: Context): Boolean =
        moods(context).any { sameDay(it.time, System.currentTimeMillis()) }

    // ---- Дневник упражнений ----

    suspend fun logs(context: Context): List<LogEntry> = withContext(Dispatchers.IO) {
        val arr = readArray(context, logFile(context))
        (0 until arr.length()).map { arr.getJSONObject(it) }
            .map { LogEntry(it.getLong("t"), it.optString("title"), it.optString("detail")) }
            .sortedByDescending { it.time }
    }

    suspend fun addLog(context: Context, title: String, detail: String = "") = logLock.withLock {
        val existing = logs(context)
        withContext(Dispatchers.IO) {
            val arr = JSONArray()
            (listOf(LogEntry(System.currentTimeMillis(), title, detail)) + existing).take(500).forEach {
                arr.put(JSONObject().put("t", it.time).put("title", it.title).put("detail", it.detail))
            }
            Crypto.writeText(context, logFile(context), arr.toString())
        }
    }

    suspend fun logCount(context: Context): Int = logs(context).size

    fun moodWord(v: Int) = when (v) { 1 -> tr("Плохо"); 2 -> tr("Средне"); else -> tr("Отлично") }

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
