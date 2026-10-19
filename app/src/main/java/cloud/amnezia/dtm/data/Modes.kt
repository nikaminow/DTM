package cloud.amnezia.dtm.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Запись дневника режимов (схема-терапия). */
data class ModeEntry(
    val id: Long,
    val time: Long,
    val modes: List<String>,
    val trigger: String,
    val voice: String,       // что «говорит» или делает режим
    val healthyAdult: String, // ответ Здорового Взрослого
)

/** Справочник режимов. */
data class ModeInfo(val name: String, val group: String, val about: String)

object Modes {

    val CATALOG = listOf(
        // Детские режимы
        ModeInfo("Уязвимый Ребёнок", "Детские режимы", "Боль, страх, стыд, одиночество, беспомощность. Чувство, что вас бросили или не любят."),
        ModeInfo("Сердитый Ребёнок", "Детские режимы", "Гнев и обида из-за того, что потребности не удовлетворены или нарушены границы."),
        ModeInfo("Импульсивный Ребёнок", "Детские режимы", "Действие на импульсе, «хочу сейчас», без учёта последствий."),
        ModeInfo("Счастливый Ребёнок", "Детские режимы", "Спокойствие, игра, лёгкость: потребности удовлетворены, вы в безопасности."),
        // Дисфункциональные копинги
        ModeInfo("Отстранённый Защитник", "Защитные режимы", "Отключение чувств, «пустота», уход в себя, избегание близости, чтобы не было больно."),
        ModeInfo("Отстранённый Самоуспокоитель", "Защитные режимы", "Заглушить чувства едой, работой, сериалами, алкоголем, прокрастинацией."),
        ModeInfo("Уступчивый Капитулянт", "Защитные режимы", "Подчиниться, угождать, терпеть, лишь бы избежать конфликта или отвержения."),
        ModeInfo("Гиперкомпенсатор", "Защитные режимы", "Контроль, нападение, перфекционизм, доказывание — «нападение как защита»."),
        // Дисфункциональные родительские
        ModeInfo("Карающий Родитель", "Родительские режимы", "Внутренний голос, который наказывает, стыдит: «ты плохой», «ты виноват», «ты заслужил»."),
        ModeInfo("Требовательный Родитель", "Родительские режимы", "«Ты должен», «недостаточно хорошо», постоянное давление и завышенные требования."),
        // Здоровый
        ModeInfo("Здоровый Взрослый", "Здоровый режим", "Заботится об Уязвимом Ребёнке, ставит границы, успокаивает, принимает взвешенные решения."),
    )

    val GROUPS = CATALOG.map { it.group }.distinct()

    private fun file(context: Context) = File(context.filesDir, "modes.enc")

    suspend fun load(context: Context): List<ModeEntry> = withContext(Dispatchers.IO) {
        val raw = runCatching { Crypto.readText(context, file(context)) }.getOrNull() ?: return@withContext emptyList()
        runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val m = o.optJSONArray("modes")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                ModeEntry(o.getLong("id"), o.getLong("time"), m, o.optString("trigger"), o.optString("voice"), o.optString("ha"))
            }.sortedByDescending { it.time }
        }.getOrDefault(emptyList())
    }

    suspend fun save(context: Context, list: List<ModeEntry>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("time", e.time); put("modes", JSONArray(e.modes))
                put("trigger", e.trigger); put("voice", e.voice); put("ha", e.healthyAdult)
            })
        }
        Crypto.writeText(context, file(context), arr.toString())
    }

    suspend fun count(context: Context): Int = load(context).size
}
