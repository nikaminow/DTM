package cloud.amnezia.dtm.data

import cloud.amnezia.dtm.tr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Запись протокола СМЭР: Ситуация → Мысли → Эмоции → Реакции (тело, поведение). */
data class SmerEntry(
    val id: Long,
    val time: Long,
    val situation: String,
    val thoughts: String,
    val emotions: List<String>,
    val body: String,
    val behavior: String,
)

object Smer {

    val EMOTIONS = listOf(
        tr("Тревога"), tr("Страх"), tr("Гнев"), tr("Раздражение"), tr("Обида"), tr("Грусть"),
        tr("Стыд"), tr("Вина"), tr("Беспомощность"), tr("Одиночество"), tr("Отвращение"), tr("Растерянность"),
    )

    private fun file(context: Context) = File(context.filesDir, "smer.enc")

    suspend fun load(context: Context): List<SmerEntry> = withContext(Dispatchers.IO) {
        val raw = runCatching { Crypto.readText(context, file(context)) }.getOrNull() ?: return@withContext emptyList()
        runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val em = o.optJSONArray("em")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                SmerEntry(o.getLong("id"), o.getLong("time"), o.optString("s"), o.optString("m"), em,
                    o.optString("b"), o.optString("p"))
            }.sortedByDescending { it.time }
        }.getOrDefault(emptyList())
    }

    suspend fun save(context: Context, list: List<SmerEntry>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("time", e.time); put("s", e.situation); put("m", e.thoughts)
                put("em", JSONArray(e.emotions)); put("b", e.body); put("p", e.behavior)
            })
        }
        Crypto.writeText(context, file(context), arr.toString())
    }
}
