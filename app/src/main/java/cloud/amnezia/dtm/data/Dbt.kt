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

data class DbtModule(val id: Int, val title: String, val about: String)

data class DbtLesson(
    val id: String,
    val module: Int,
    val title: String,
    val summary: String,
    val theory: List<String>,
    val practiceIntro: String,
    val prompts: List<String>,
    val scale: Boolean = true,
)

data class DbtEntry(val lesson: String, val time: Long, val answers: List<String>, val before: Int, val after: Int)

data class DiaryDay(val day: Long, val emotions: Map<String, Int>, val urge: Int, val skills: List<String>, val note: String)

object Dbt {

    val MODULES = listOf(
        DbtModule(0, tr("Что такое ДПТ"), tr("Вводный блок: что это и зачем")),
        DbtModule(1, tr("Осознанность"), tr("Кнопка «пауза» внутри: быть здесь и сейчас")),
        DbtModule(2, tr("Стрессоустойчивость"), tr("Пережить бурю и не сделать хуже")),
        DbtModule(3, tr("Регуляция эмоций"), tr("Понимать чувства и влиять на них")),
        DbtModule(4, tr("Отношения"), tr("Просить, отказывать и уважать себя")),
    )

    val DIARY_EMOTIONS = listOf(tr("Грусть"), tr("Тревога"), tr("Гнев"), tr("Стыд"), tr("Радость"))

    val SKILLS = listOf(
        tr("Наблюдать"), tr("Описывать"), tr("Участвовать"), tr("Безоценочность"), tr("Мудрый разум"),
        tr("СТОП"), tr("ТРУД"), "5-4-3-2-1", tr("Самоуспокоение"), tr("За и против"),
        tr("Противоположное действие"), tr("Проверка фактов"), tr("АВС-забота"), tr("ПОПРОСИ"), tr("ЧЕСТЬ"), tr("ДРУГ"),
    )

    fun module(id: Int) = MODULES.first { it.id == id }
    fun lessonsOf(m: Int) = LESSONS.filter { it.module == m }

    // ---- Прогресс ----

    private val lock = Mutex()
    private fun practiceFile(c: Context) = File(c.filesDir, "dbt_practice.enc")
    private fun diaryFile(c: Context) = File(c.filesDir, "dbt_diary.enc")

    fun done(c: Context): Set<String> =
        (SecureStore.getString(c, "dbt_done") ?: "").split(',').filter { it.isNotBlank() }.toSet()

    fun isDone(c: Context, id: String) = id in done(c)

    fun setDone(c: Context, id: String) {
        SecureStore.putString(c, "dbt_done", (done(c) + id).joinToString(","))
    }

    fun progress(c: Context, m: Int): Pair<Int, Int> {
        val l = lessonsOf(m); val d = done(c)
        return l.count { it.id in d } to l.size
    }

    suspend fun addPractice(c: Context, e: DbtEntry) = lock.withLock {
        val list = practice(c) + e
        withContext(Dispatchers.IO) {
            val a = JSONArray()
            list.forEach { a.put(JSONObject().put("l", it.lesson).put("t", it.time)
                .put("a", JSONArray(it.answers)).put("b", it.before).put("af", it.after)) }
            Crypto.writeText(c, practiceFile(c), a.toString())
        }
        setDone(c, e.lesson)
    }

    suspend fun practice(c: Context): List<DbtEntry> = withContext(Dispatchers.IO) {
        val raw = runCatching { Crypto.readText(c, practiceFile(c)) }.getOrNull() ?: return@withContext emptyList()
        runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
                val ans = o.optJSONArray("a")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
                DbtEntry(o.optString("l"), o.optLong("t"), ans, o.optInt("b"), o.optInt("af"))
            }
        }.getOrDefault(emptyList())
    }

    suspend fun saveDiary(c: Context, d: DiaryDay) = lock.withLock {
        val list = diary(c).filter { !sameDay(it.day, d.day) } + d
        withContext(Dispatchers.IO) {
            val a = JSONArray()
            list.sortedBy { it.day }.forEach { day ->
                val em = JSONObject(); day.emotions.forEach { (k, v) -> em.put(k, v) }
                a.put(JSONObject().put("d", day.day).put("em", em).put("u", day.urge)
                    .put("sk", JSONArray(day.skills)).put("n", day.note))
            }
            Crypto.writeText(c, diaryFile(c), a.toString())
        }
    }

    suspend fun diary(c: Context): List<DiaryDay> = withContext(Dispatchers.IO) {
        val raw = runCatching { Crypto.readText(c, diaryFile(c)) }.getOrNull() ?: return@withContext emptyList()
        runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
                val em = o.optJSONObject("em") ?: JSONObject()
                val map = em.keys().asSequence().associateWith { em.optInt(it) }
                val sk = o.optJSONArray("sk")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
                DiaryDay(o.optLong("d"), map, o.optInt("u"), sk, o.optString("n"))
            }.sortedBy { it.day }
        }.getOrDefault(emptyList())
    }

    fun diaryFor(list: List<DiaryDay>, day: Long) = list.firstOrNull { sameDay(it.day, day) }

    private fun sameDay(a: Long, b: Long) = a / 86_400_000L == b / 86_400_000L
}
