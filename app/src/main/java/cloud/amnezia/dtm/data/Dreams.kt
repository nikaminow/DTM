package cloud.amnezia.dtm.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Запись сна. rating: 1 — плохо, 2 — средне, 3 — супер. */
data class Dream(
    val id: Long,
    val time: Long,
    val text: String,
    val rating: Int,
)

/** Дневник снов. Хранится зашифрованно. */
object Dreams {

    private fun file(context: Context) = File(context.filesDir, "dreams.enc")

    suspend fun load(context: Context): List<Dream> = withContext(Dispatchers.IO) {
        val raw = runCatching { Crypto.readText(context, file(context)) }.getOrNull() ?: return@withContext emptyList()
        runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Dream(o.getLong("id"), o.getLong("time"), o.optString("text"), o.optInt("rating", 2))
            }.sortedByDescending { it.time }
        }.getOrDefault(emptyList())
    }

    suspend fun save(context: Context, list: List<Dream>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(JSONObject().put("id", d.id).put("time", d.time).put("text", d.text).put("rating", d.rating))
        }
        Crypto.writeText(context, file(context), arr.toString())
    }

    suspend fun count(context: Context): Int = load(context).size

    fun word(r: Int) = when (r) { 1 -> "Плохой"; 2 -> "Средний"; else -> "Супер" }
}
